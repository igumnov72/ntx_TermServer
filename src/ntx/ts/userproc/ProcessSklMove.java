package ntx.ts.userproc;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_LG_STOCK_S;
import ntx.sap.struct.ZTS_MAT_ABC_S;
import ntx.sap.struct.ZTS_VED_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.delDecZeros;
import static ntx.ts.sysproc.ProcessUtil.delZeros;
import static ntx.ts.sysproc.ProcessUtil.fillZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Перемещение товара по складу Состояния: START SEL_SKL SEL_CELL SEL_PAL
 * SEL_TOV_CELL QTY TO_PAL
 */
public class ProcessSklMove extends ProcessTask {

  private final SklMoveData d = new SklMoveData();

  public ProcessSklMove(long procId) throws Exception {
    super(ProcType.SKL_MOVE, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctxUser) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    TaskContext ctx = new TaskContext(ctxUser, this);

    if (getTaskState() == TaskState.START) {
      return init(ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    return htmlGet(false, ctx);
  }

  private FileData htmlGet(boolean playSound, TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case SEL_SKL:
        try {
          return htmlSelLgort(null, playSound, ctx);
        } catch (Exception e) {
          callSetErr(e.getMessage(), ctx);
          return (new HtmlPageMessage(getLastErr(), null, null, null)).getPage();
        }

      case QTY:
        BigDecimal qty = getScanQty(d.getLastScan());
        return htmlWork("Перемещение", playSound, delDecZeros(qty.toString()), ctx);

      default:
        return htmlWork("Перемещение", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      d.callSetLgort(lgorts[0], TaskState.SEL_CELL, ctx);
      callTaskNameChange(ctx);
    } else {
      callSetTaskState(TaskState.SEL_SKL, ctx);
    }
    return htmlGet(false, ctx);
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu(ctx);
    }

    switch (getTaskState()) {
      case SEL_CELL:
        return handleScanCell(scan, ctx);

      case SEL_PAL:
        return handleScanPal(scan, ctx);

      case SEL_TOV_CELL:
        return handleScanTovCell(scan, ctx);

      case QTY:
        return handleScanQty(scan, ctx);

      case TO_PAL:
        return handleScanPal2(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (getTaskState() == TaskState.SEL_SKL) {
      // обработка выбора склада
      callClearErrMsg(ctx);
      if ((menu.length() == 12) && menu.startsWith("sellgort")) {
        String lg = menu.substring(8);
        if (!rightsLgort(lg, ctx)) {
          callSetErr("Нет прав по складу " + lg, ctx);
          return htmlGet(true, ctx);
        } else {
          d.callSetLgort(lg, TaskState.SEL_CELL, ctx);
          callTaskNameChange(ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(false, ctx);
    } else if (menu.equals("cancel")) {
      callClearErrMsg(ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      callSetMsg("Перемещение из " + d.getCell1() + " отменено", ctx);
      callAddHist("! Перемещение из " + d.getCell1() + " отменено", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("ask_qty_on")) {
      return handleAskQtyOn(ctx);
    } else if (menu.equals("ask_qty_off")) {
      return handleAskQtyOff(ctx);
    } else if (menu.equals("new_cell")) {
      callClearErrMsg(ctx);
      return handleNewCell(ctx);
    } else if (menu.equals("abc")) {
      callClearErrMsg(ctx);
      return htmlShowABC(ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleNewCell(TaskContext ctx) throws Exception {
    callSetTaskState(TaskState.SEL_TOV_CELL, ctx);
    callSetMsg("Укажите другую ячейку-получателя", ctx);
    callAddHist("Ячейка-получатель будет другая", ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleAskQtyOn(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(true, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleAskQtyOff(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(false, ctx);
    return htmlGet(false, ctx);
  }

  private boolean haveTov() {
    if (d.getTovData().isEmpty()) {
      return false;
    }
    for (Entry<String, BigDecimal> i : d.getTovData().entrySet()) {
      if (i.getValue().signum() == 1) {
        return true;
      }
    }
    return false;
  }

  private String appendAbc(String s, String abc) {
    return (abc == null) || abc.isEmpty() ? s : s + " (ABC: " + abc + ")";
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_SKL_MOVE1 f = new Z_TS_SKL_MOVE1();
    f.CELL = scan.substring(1);
    f.LGNUM = d.getLgnum();
    f.LGORT = d.getLgort();
    f.execute();

    if (!f.isErr) {
      if (f.NO_PAL.equals("X")) {
        // паллету сканировать не нужно
        d.callSetCell1(f.LGPLA, f.LGTYP, TaskState.SEL_TOV_CELL, ctx);
        d.callSetPal1(f.PAL1, TaskState.SEL_TOV_CELL, ctx);
        d.callClearTovData(ctx);
        String s = "Перемещение из " + d.getCell1() + " " + d.getPal1() + " (БЕЗ СКАНИРОВАНИЯ ПАЛЛЕТЫ)";
        s = appendAbc(s, f.ABC);
        if (f.NOT_WHOLE.equals("X")) {
          s = s + " <br>!!! ОТКРЫТЫЕ ТРАНСП. ЗАКАЗЫ, ВЕСЬ ТОВАР С ПАЛЛЕТЫ ПЕРЕМЕСТИТЬ НЕЛЬЗЯ";
        }
        s = s + " " + d.getStockStr();
        callSetMsg(s, ctx);
        callAddHist("Источник: " + d.getCell1() + " " + d.getPal1(), ctx);
        d.callClearTovData(ctx);
        if (f.NOT_WHOLE.equals("X")) {
          return htmlGet(false, ctx);
        }
      } else {
        d.callSetCell1(f.LGPLA, f.LGTYP, TaskState.SEL_PAL, ctx);
        callSetMsg("Перемещение из " + d.getCell1(), ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_SKL_MOVE2 f = new Z_TS_SKL_MOVE2();
    f.LGPLA = d.getCell1();
    f.LGNUM = d.getLgnum();
    f.LGTYP = d.getLgtyp1();
    f.PAL = scan.substring(1);
    f.execute();

    if (!f.isErr) {
      d.callSetPal1(scan.substring(1), TaskState.SEL_TOV_CELL, ctx);
      String s = "Перемещение из " + d.getCell1() + " " + d.getPal1();
      s = appendAbc(s, f.ABC);
      if (f.NOT_WHOLE.equals("X")) {
        s = s + " <br>!!! ОТКРЫТЫЕ ТРАНСП. ЗАКАЗЫ, ВЕСЬ ТОВАР С ПАЛЛЕТЫ ПЕРЕМЕСТИТЬ НЕЛЬЗЯ";
      }
      s = s + " " + d.getStockStr();
      callSetMsg(s, ctx);
      callAddHist("Источник: " + d.getCell1() + " " + d.getPal1(), ctx);
      d.callClearTovData(ctx);
      if (f.NOT_WHOLE.equals("X")) {
        return htmlGet(false, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanTovCell(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCell2(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTov(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать товар или ячейку (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanQty(String scan, TaskContext ctx) throws Exception {
    if (scan.isEmpty() || isScanCell(scan) || isScanPal(scan)
            || isScanTovMk(scan) || !isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanTovDo(d.getLastScan(), getScanCharg(d.getLastScan()), qty, ctx);
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    ScanChargQty scanInf; 
    scanInf = getScanChargQty(scan);
    if (!scanInf.err.isEmpty()) {
      callSetErr(scanInf.err + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    try {
      String charg = scanInf.charg;// getScanCharg(scan);
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      if (ctx.user.getAskQty() && !isScanMk(scan)) {
        String s = c.matnr + "/" + charg + " " + RefMat.getName(c.matnr);
        callSetMsg(s, ctx);
        callSetTaskState(TaskState.QTY, ctx);
        d.callSetLastScan(scan, ctx);
        return htmlGet(true, ctx);
      } else {
        return handleScanTovDo(scan, charg, scanInf.qty// getScanQty(scan)
                , ctx);
      }
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = "ОШИБКА ! Сообщите разработчику";
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovDo(String scan, String charg, 
          BigDecimal qty, TaskContext ctx) throws Exception {

    if (d.scanIsDouble(scan)) {
      callSetErr("ШК дублирован (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
      
    Z_TS_SKL_MOVE3 f = new Z_TS_SKL_MOVE3();
    f.LGNUM = d.getLgnum();
    f.LGORT = d.getLgort();
    f.LGTYP = d.getLgtyp1();
    f.LGPLA = d.getCell1();
    f.PAL = d.getPal1();
    f.A_CHARG = fillZeros(charg, 10);
    f.A_QTY = qty;
    f.SHK = scan;

    if (!d.getTovDataM().isEmpty()) {
      int n = d.getTovDataM().size();
      f.IT_MQ_create(n);
      int j = 0;
      for (Entry<String, BigDecimal> i : d.getTovDataM().entrySet()) {
        f.IT_MQ[j].MATNR = fillZeros(i.getKey(), 18);
        f.IT_MQ[j].QTY = i.getValue();
        j++;
      }
    }

    f.execute();

    if (!f.isErr) {
      f.MATNR = delZeros(f.MATNR);
      f.CHARG = delZeros(f.CHARG);
      d.callAddTov(f.MATNR, f.CHARG, f.QTY, scan, ctx);

      String s = delDecZeros(f.QTY.toString()) + "ед " + f.MATNR + " (" + f.CHARG + ") " + f.MAKTX;
      //s = appendAbc(s, f.ABC);
      s = RefAbc.appendAbcXyz(s, d.getLgort(), f.MATNR);

      if (!f.MAT_CELL.isEmpty()) {
        s = s + " (" + f.MAT_CELL + ")";
      }
      s = s + " " + d.getStockChargStr(f.CHARG);
      callAddHist(s, ctx);
      callSetMsg(s, ctx);

      callSetTaskState(TaskState.SEL_TOV_CELL, ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanCell2(String scan, TaskContext ctx) throws Exception {
    if (!d.getTovData().isEmpty() && !haveTov()) {
      callSetErr("Отсканирован товар, но его кол-во нулевое (требуется отсканировать товар либо отменить перемещение)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_SKL_MOVE4 f = new Z_TS_SKL_MOVE4();
    f.LGTYP1 = d.getLgtyp1();
    f.CELL1 = d.getCell1();
    f.CELL2 = scan.substring(1);
    f.LGNUM = d.getLgnum();
    f.LGORT = d.getLgort();
    f.PAL1 = d.getPal1();
    f.TSD_USER = ctx.user.getUserSHK();

    if (!d.getTovDataM().isEmpty()) {
      int n = d.getTovDataM().size();
      f.IT_MQ_create(n);
      int j = 0;
      for (Entry<String, BigDecimal> i : d.getTovDataM().entrySet()) {
        f.IT_MQ[j].MATNR = fillZeros(i.getKey(), 18);
        f.IT_MQ[j].QTY = i.getValue();
        j++;
      }
    }

    if (haveTov()) {
      int n = d.getTovData().size();
      f.IT_CQ_create(n);
      int j = 0;
      for (Entry<String, BigDecimal> i : d.getTovData().entrySet()) {
        f.IT_CQ[j].CHARG = fillZeros(i.getKey(), 10);
        f.IT_CQ[j].QTY = i.getValue();
        j++;
      }
    }

    f.execute();

    if (!f.isErr) {
      if (f.NO_PAL.equals("X")) {
        // сканирование паллеты не требуется
        d.callSetCell2(scan.substring(1), f.LGTYP2, TaskState.SEL_CELL, ctx);
        d.callSetPal2(f.PAL2, f.KEY1, ctx);
        String s;
        if (haveTov()) {
          s = "Перемещение части товара из " + d.getCell1() + " " + d.getPal1() + " в " + d.getCell2() + " " + d.getPal2() + " успешно проведено";
        } else if (d.getPal1().equals(d.getPal2())) {
          s = "Перемещение паллеты " + d.getPal1() + " из " + d.getCell1() + " в " + d.getCell2() + " успешно проведено";
        } else {
          s = "Перемещение всего товара из " + d.getCell1() + " " + d.getPal1() + " в " + d.getCell2() + " " + d.getPal2() + " успешно проведено";
        }
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        d.callClearTovData(ctx);
      } else {
        d.callSetCell2(scan.substring(1), f.LGTYP2, TaskState.TO_PAL, ctx);
        callSetMsg("Перемещение в " + d.getCell2(), ctx);
        callAddHist("Получатель: " + d.getCell2(), ctx);
      }
    } else if (!f.isSapErr) {
      if (f.TR_ZAK_CREATED.isEmpty()) {
        callSetErrMsg(f.err, "Возможно, следует вернуть товар в "
                + d.getCell1() + " " + d.getPal1() + " и отменить перемещение", ctx);
        callAddHist("Ошибка при перем. в " + d.getCell2() + " " + scan.substring(1), ctx);
      } else {
        callSetErrMsg(f.err, "Перемещение завершено, следует разобраться с ошибкой в ztsd20", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        d.callSetPal2(scan.substring(1), f.KEY1, ctx);
        callAddHist("Перемещение в " + d.getCell2() + " " + d.getPal2()
                + " завершено, следует разобраться с ошибкой в ztsd20 строка " + f.KEY1, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanPal2(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать паллету-получатель (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_SKL_MOVE5 f = new Z_TS_SKL_MOVE5();
    f.LGORT = d.getLgort();
    f.LGNUM = d.getLgnum();
    f.LGTYP1 = d.getLgtyp1();
    f.CELL1 = d.getCell1();
    f.PAL1 = d.getPal1();
    f.LGTYP2 = d.getLgtyp2();
    f.CELL2 = d.getCell2();
    f.PAL2 = scan.substring(1);
    f.TSD_USER = ctx.user.getUserSHK();

    if (haveTov()) {
      int n = d.getTovData().size();
      f.IT_CQ_create(n);
      int j = 0;
      for (Entry<String, BigDecimal> i : d.getTovData().entrySet()) {
        f.IT_CQ[j].CHARG = fillZeros(i.getKey(), 10);
        f.IT_CQ[j].QTY = i.getValue();
        j++;
      }
    }

    f.execute();

    if (!f.isErr) {
      d.callSetPal2(scan.substring(1), f.KEY1, ctx);
      String s;
      if (haveTov()) {
        s = "Перемещение части товара из " + d.getCell1() + " " + d.getPal1() + " в " + d.getCell2() + " " + d.getPal2() + " успешно проведено";
      } else if (d.getPal1().equals(d.getPal2())) {
        s = "Перемещение паллеты " + d.getPal1() + " из " + d.getCell1() + " в " + d.getCell2() + " успешно проведено";
      } else {
        s = "Перемещение всего товара из " + d.getCell1() + " " + d.getPal1() + " в " + d.getCell2() + " " + d.getPal2() + " успешно проведено";
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      d.callClearTovData(ctx);
    } else if (!f.isSapErr) {
      if (f.TR_ZAK_CREATED.isEmpty()) {
        callSetErrMsg(f.err, "Возможно, следует вернуть товар в "
                + d.getCell1() + " " + d.getPal1() + " и отменить перемещение", ctx);
        callAddHist("Ошибка при перем. в " + d.getCell2() + " " + scan.substring(1), ctx);
      } else {
        callSetErrMsg(f.err, "Перемещение завершено, следует разобраться с ошибкой в ztsd20", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        d.callSetPal2(scan.substring(1), f.KEY1, ctx);
        callAddHist("Перемещение в " + d.getCell2() + " " + d.getPal2()
                + " завершено, следует разобраться с ошибкой в ztsd20 строка " + f.KEY1, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;
    String askQtyMenu;
    if (ctx.user.getAskQty()) {
      askQtyMenu = "ask_qty_off:Выкл ввод кол-ва";
    } else {
      askQtyMenu = "ask_qty_on:Вкл ввод кол-ва";
    }

    switch (getTaskState()) {
      case QTY:
        definition = "cont:Продолжить;later:Отложить";
        break;

      case SEL_CELL:
        definition = "cont:Продолжить;later:Отложить;" + askQtyMenu;
        break;

      case TO_PAL:
        definition = "cont:Продолжить;later:Отложить;new_cell:В другую ячейку";
        break;

      case SEL_PAL:
      case SEL_TOV_CELL:
        definition = "cont:Назад;abc:Товары ABC;cancel:Отменить перемещение;later:Отложить;" + askQtyMenu;
        break;

      default:
        definition = "cont:Назад;cancel:Отменить перемещение;later:Отложить;" + askQtyMenu;
        break;
    }

    if (getTaskState() == TaskState.SEL_CELL) {
      definition = definition + ";fin:Завершить";
    }

    if (RefInfo.haveInfo(ProcType.SKL_MOVE)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Перемещение", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData htmlShowABC(TaskContext ctx) throws Exception {
    // отображение ABC-признака товара в ячейке

    Z_TS_SKL_MOVE6 f = new Z_TS_SKL_MOVE6();
    f.LGORT = d.getLgort();
    f.LGPLA = d.getCell1();
    f.LGNUM = d.getLgnum();
    f.LGTYP = d.getLgtyp1();
    f.PAL = d.getPal1();
    f.execute();

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    int n = f.IT.length;

    HtmlPage p = new HtmlPage();
    p.title = "Признаки ABC";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Признаки ABC товара в ячейке:</b>");
    p.addNewLine();
    String s;
    ZTS_MAT_ABC_S r;
    for (int i = 0; i < n; i++) {
      r = f.IT[i];
      r.MATNR = delZeros(r.MATNR);

      s = "<b><font color=red>" + r.ZABCN + "</font>";
      s = s + " " + r.MATNR;
      s = s + "</b> " + RefMat.getFullName(r.MATNR) + " <b>"
              + delDecZeros(r.QTY.toString()) + " ед</b>";
      p.addLine(s);
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    return getProcType().text + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    String askQtyStr = ctx.user.getAskQty() ? "<b>ввод кол-ва</b>" : "<b>БЕЗ ввода кол-ва</b>";

    if (!d.getLgort().isEmpty()) {
      return d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name + "; " + askQtyStr;
    } else {
      return askQtyStr;
    }
  }
}

class SklMoveData extends ProcData {

  private String cell1 = ""; // ячейка, из которой берем
  private String lgtyp1 = "";
  private String pal1 = ""; // паллета, с которой берем
  private String cell2 = ""; // ячейка, в которую помещаем
  private String lgtyp2 = "";
  private String pal2 = ""; // паллета, на которую помещаем
  private String lgort = ""; // склад
  private String lgnum = "";
  private int key1 = 0; // номер строки в журнале ztsd20
  private String lastScan = null; // предыдущее сканирование
  private HashMap<String, BigDecimal> tovData = new HashMap<String, BigDecimal>(); // товар по партиям
  private HashMap<String, BigDecimal> tovDataM = new HashMap<String, BigDecimal>(); // товар по материалам
  private final ArrayList<ZTS_LG_STOCK_S> stock = new ArrayList<ZTS_LG_STOCK_S>();
  private final ArrayList<String> tovScans = new ArrayList<String>();

  public boolean scanIsDouble(String scan) {
    int n = tovScans.size();
    for (int i = 0; i < n; i++) 
        if (tovScans.get(i).equals(scan) && !isScanTov(scan)) return true;
    return false;
  }
  
  public String getStockStr() {
    String stock_str = "";
    if (!lgnum.equals("333") && !lgnum.equals("223")) return stock_str;
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(1);
    df.setMinimumFractionDigits(0);
    df.setGroupingUsed(false);
    for (ZTS_LG_STOCK_S stock_item : stock) {
      if (!stock_str.isEmpty()) stock_str = stock_str + "; ";
      stock_str = stock_str + delZeros(stock_item.MATNR) + " (" + 
        stock_item.CHARG + ", З " + df.format(stock_item.GESME) +
        ", ДЗ " + df.format(stock_item.VERME) + ")";
    }
    return stock_str;
  }

  public String getStockChargStr(String charg) {
    String stock_str = "";
    if (!lgnum.equals("333") && !lgnum.equals("223")) return stock_str;
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(1);
    df.setMinimumFractionDigits(0);
    df.setGroupingUsed(false);
    for (ZTS_LG_STOCK_S stock_item : stock) {
      if (delZeros(stock_item.CHARG).equalsIgnoreCase(charg)) {
          BigDecimal qty = tovData.get(charg);
          BigDecimal qty_z = new BigDecimal(0);
          BigDecimal qty_dz = new BigDecimal(0);
          if (qty != null) {
            qty_z = stock_item.GESME.subtract(qty);
            qty_dz = stock_item.GESME.subtract(qty);
          } 
          stock_str = "(З " + df.format(qty_z) +
            ", ДЗ " + df.format(qty_dz) + ")";
          return stock_str;
      }
    }
    return stock_str;
  }
  
  public String getLastScan() {
    return lastScan;
  }

  public String getCell1() {
    return cell1;
  }

  public String getLgtyp1() {
    return lgtyp1;
  }

  public String getPal1() {
    return pal1;
  }

  public String getCell2() {
    return cell2;
  }

  public String getLgtyp2() {
    return lgtyp2;
  }

  public String getPal2() {
    return pal2;
  }

  public String getLgort() {
    return lgort;
  }

  public String getLgnum() {
    return lgnum;
  }

  public int getKey1() {
    return key1;
  }

  public HashMap<String, BigDecimal> getTovData() {
    return tovData;
  }

  public HashMap<String, BigDecimal> getTovDataM() {
    return tovDataM;
  }

  public void callSetLgort(String lgort, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
      String lgnum;
      if (!strEq(lgort, null)) {
        RefLgortStruct l = RefLgort.get(lgort);
        lgnum = l.lgnum;
      } else {
        lgnum = "";
      }
      if (!strEq(lgnum, this.lgnum)) {
        dr.setS(FieldType.LGNUM, lgnum);
      }
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell1(String cell, String lgtyp, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(cell, cell1)) {
      dr.setS(FieldType.CELL, cell);
    }
    if (!strEq(lgtyp, lgtyp1)) {
      dr.setS(FieldType.LGTYP1, lgtyp);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell2(String cell, String lgtyp, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(cell, cell2)) {
      dr.setS(FieldType.CELL2, cell);
    }
    if (!strEq(lgtyp, lgtyp2)) {
      dr.setS(FieldType.LGTYP2, lgtyp);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal1(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(pal, pal1)) {
      dr.setS(FieldType.PAL, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal2(String pal, int key1, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(pal, pal2)) {
      dr.setS(FieldType.PAL2, pal);
    }
    if (key1 != this.key1) {
      dr.setI(FieldType.KEY1, key1);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTovData(TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!tovData.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, BigDecimal qty, 
          String scan, TaskContext ctx) throws Exception {
    // добавление данных о товаре (партия - без ведущих нулей)
    if (qty.signum() == 0) {
      return;
    }
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    dr.setS(FieldType.SHK, scan);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLastScan(String scan, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.lastScan, scan)) {
      dr.setS(FieldType.LAST_SCAN, scan);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = (String) dr.getVal(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.LGNUM)) {
          lgnum = (String) dr.getVal(FieldType.LGNUM);
        }
        if (dr.haveVal(FieldType.CELL)) {
          cell1 = dr.getValStr(FieldType.CELL);
        }
        if (dr.haveVal(FieldType.LGTYP1)) {
          lgtyp1 = dr.getValStr(FieldType.LGTYP1);
        }
        if (dr.haveVal(FieldType.PAL)) {
          pal1 = dr.getValStr(FieldType.PAL);
          
          stock.clear();
          Z_TS_LG_STOCK f = new Z_TS_LG_STOCK();
          f.LGNUM = lgnum;
          f.LGTYP = lgtyp1;
          f.LGPLA = cell1;
          f.LENUM = pal1;
          f.execute();
          if (!f.isErr) {
            for (int i = 0; i < f.IT.length; i++) {
              ZTS_LG_STOCK_S stock_item = new ZTS_LG_STOCK_S();
              stock_item.MATNR = f.IT[i].MATNR;
              stock_item.CHARG = f.IT[i].CHARG;
              stock_item.GESME = f.IT[i].GESME;
              stock_item.VERME = f.IT[i].VERME;
              stock.add(stock_item);
            }
          }
          
        }
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          tovData.clear();
          tovDataM.clear();
          tovScans.clear();
        }
        if (dr.haveVal(FieldType.MATNR) && dr.haveVal(FieldType.CHARG) && 
                dr.haveVal(FieldType.QTY) && dr.haveVal(FieldType.SHK)) {
          String charg = dr.getValStr(FieldType.CHARG);
          String scan = dr.getValStr(FieldType.SHK);
          BigDecimal nn = (BigDecimal) dr.getVal(FieldType.QTY);
          BigDecimal nn0 = tovData.get(charg);
          if (nn0 != null) {
            nn = nn.add(nn0);
          }
          tovData.put(charg, nn);
          String matnr = dr.getValStr(FieldType.MATNR);
          nn = (BigDecimal) dr.getVal(FieldType.QTY);
          nn0 = tovDataM.get(matnr);
          if (nn0 != null) {
            nn = nn.add(nn0);
          }
          tovDataM.put(matnr, nn);
          tovScans.add(scan);
        }
        if (dr.haveVal(FieldType.CELL2)) {
          cell2 = dr.getValStr(FieldType.CELL2);
        }
        if (dr.haveVal(FieldType.LGTYP2)) {
          lgtyp2 = dr.getValStr(FieldType.LGTYP2);
        }
        if (dr.haveVal(FieldType.PAL2)) {
          pal2 = dr.getValStr(FieldType.PAL2);
        }
        if (dr.haveVal(FieldType.LAST_SCAN)) {
          lastScan = dr.getValStr(FieldType.LAST_SCAN);
        }
        break;
    }
  }
}
