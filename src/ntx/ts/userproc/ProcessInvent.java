package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.sap.fm.*;
import ntx.sap.refs.RefAbc;
import ntx.sap.refs.RefAbcStruct;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
import ntx.sap.refs.RefInfo;
import ntx.sap.refs.RefLgort;
import ntx.sap.refs.RefLgortStruct;
import ntx.sap.refs.RefMat;
import ntx.sap.refs.RefMatStruct;
import ntx.sap.struct.ZTS_INV_QTY_S;
import ntx.sap.struct.ZTS_MP_QTY_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.MapItemArray;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Инвентаризация адресного склада Состояния: START SEL_SKL SEL_CELL PAL_CELL
 * TOV_CELL TOV_PAL_CELL QTY
 */
public class ProcessInvent extends ProcessTask {

  private final InventData d = new InventData();

  public ProcessInvent(long procId) throws Exception {
    super(ProcType.INVENT, procId);
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
        return htmlWork("Инвентаризация", playSound, delDecZeros(qty.toString()), ctx);

      default:
        return htmlWork("Инвентаризация", playSound, ctx);
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

      case PAL_CELL:
        return handleScanPalCell(scan, ctx);

      case TOV_CELL:
        return handleScanTovCell(scan, ctx);

      case TOV_PAL_CELL:
        return handleScanTovPalCell(scan, ctx);

      case QTY:
        return handleScanQty(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanQty(String scan, TaskContext ctx) throws Exception {
    if (scan.isEmpty() || isScanCell(scan) || isScanPal(scan)
            || isScanTov(scan) || !isNumber(scan)) {
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

    return handleScanQtyDo(qty, ctx);
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanCellDo(scan, false, ctx);
  }

  private FileData handleScanCellDo(String scan, boolean haveCnf, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    Z_TS_INV1 f = new Z_TS_INV1();
    f.LGORT = d.getLgort();
    f.LGPLA = scan.substring(1);
    f.TSD_USER = ctx.user.getUserSHK();
    if (haveCnf) {
      f.DO_CRE = "X";
    }

    f.execute();

    if (!f.isErr) {
      if (f.CELL_INV_DONE.equals("X") && !haveCnf) {
        // инв ячейки уже делалась, требуется подтверждение
        d.callSetCell(f.LGPLA, ctx);
        return htmlCnfCell();
      } else {
        d.callSetInv0(f.LGPLA, f.LGNUM, f.LGTYP, f.IVNUM, f.MISCH, f.WERKS, f.INV_ID, f.IT, TaskState.PAL_CELL, ctx);
        String m1 = getLastMsg(); // может иметься сообщение о завершении инв пред. ячейки
        String m2 = "Инв ячейки " + f.LGPLA
                + " (кол-во по САПу: " + delDecZeros(d.getQtySAP().toString()) + ")";
        callSetMsg(m1 == null ? m2 : m1 + "<hr>\r\n" + m2, ctx);
        callAddHist(m2, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanPalCell(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPalDo(scan, ctx);
    } else if (isScanCell(scan)) {
      return handleScanCellFinDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК паллеты или ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanPalDo(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен

    if (d.getMisch().equals("X")) {
      // проверяем что паллета еще не сканировалась
      String pal = scan.substring(1);
      ArrayList<InventTovData> tov = d.getTov();
      int n = tov.size();
      for (int i = 0; i < n; i++) {
        if (tov.get(i).pal.equals(pal)) {
          callSetErr("Паллета " + pal + " уже сканировалась в ячейке "
                  + d.getLgpla() + ", сначала удалите данные по паллете", ctx);
          return htmlGet(true, ctx);
        }
      }
    }

    Z_TS_INV2 f = new Z_TS_INV2();
    f.LGNUM = d.getLgnum();
    f.LGTYP = d.getLgtyp();
    f.LGPLA = d.getLgpla();
    f.IVNUM = d.getIvnum();
    f.LENUM = scan.substring(1);

    f.execute();

    if (!f.isErr) {
      d.callSetPal(f.LENUM, ctx);
      callSetWorkState(ctx);
      String m = "Паллета " + f.LENUM;
      callSetMsg(m, ctx);
      callAddHist(m, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanTovCell(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCellFinDo(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovPalCell(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCellFinDo(scan, ctx);
    } else if (isScanPal(scan)) {
      return handleScanPalDo(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private String appendAbc(String s, RefAbcStruct a) {
    return (a == null) || a.abc.isEmpty() ? s : s + " (ABC: " + a.abc + ")";
  }

  private FileData handleScanTovDo(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    try {
      ScanChargQty scanInf; 
      scanInf = getScanChargQty(scan);
      if (!scanInf.err.isEmpty()) {
        callSetErr(scanInf.err + " (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
        
      String charg = scanInf.charg;// getScanCharg(scan);
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      RefMatStruct m = RefMat.getNoNull(c.matnr);
      BigDecimal qty = scanInf.qty;// getScanQty(scan);
      RefAbcStruct a = RefAbc.get(d.getLgort(), c.matnr);

      if (ctx.user.getAskQty() && isScanTov(scan)) {
        String s = c.matnr + "/" + charg + " " + m.name;
        s = appendAbc(s, a);
        callSetMsg(s, ctx);
        callSetTaskState(TaskState.QTY, ctx);
        d.callSetLastScan(scan, ctx);
      } else {
        d.callAddTov(c.matnr, charg, qty, ctx);
        String s = delDecZeros(qty.toString()) + " ед: "
                + c.matnr + "/" + charg + " " + m.name
                + " (всего " + d.getDifStr(false) + ")";
        s = appendAbc(s, a);
        callAddHist(s, ctx);
        callSetMsg(s, ctx);
      }
    } catch (Exception e) {
      callSetErr(e.getMessage(), ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanQtyDo(BigDecimal qty, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    try {
      if (qty.signum() <= 0) {
        callSetMsg("Сканирование товара отменено", ctx);
        callSetWorkState(ctx);
        return htmlGet(true, ctx);
      }
      String charg = getScanCharg(d.getLastScan());
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Ошибка получения описания партии", ctx);
        callSetWorkState(ctx);
        return htmlGet(true, ctx);
      }
      RefAbcStruct a = RefAbc.get(d.getLgort(), c.matnr);
      d.callAddTov(c.matnr, charg, qty, ctx);
      String s = delDecZeros(qty.toString()) + " ед: "
              + c.matnr + "/" + charg + " " + RefMat.getName(c.matnr)
              + " (всего " + d.getDifStr(false) + ")";
      s = appendAbc(s, a);
      callAddHist(s, ctx);
      callSetMsg(s, ctx);
      callSetWorkState(ctx);
    } catch (Exception e) {
      callSetErr(e.getMessage(), ctx);
    }
    return htmlGet(true, ctx);
  }

  private void callSetWorkState(TaskContext ctx) throws Exception {
    if (d.getMisch().equals("X")) {
      callSetTaskState(TaskState.TOV_PAL_CELL, ctx);
    } else {
      callSetTaskState(TaskState.TOV_CELL, ctx);
    }
  }

  private FileData handleScanCellFinDo(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен

    String nextScan = scan.substring(1).equals(d.getLgpla()) ? null : scan;

    if (d.haveDif()) {
      // есть расхождения, запрашиваем подтверждение
      d.callSetLastScan(nextScan, ctx);
      return htmlCnfFin(ctx);
    } else {
      // сразу завершаем инв
      return handleMenuCnfFinCellDo(nextScan, ctx);
    }
  }

  private FileData htmlCnfCell() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Товар в ячейке " + d.getLgpla() + " уже подсчитывался, провести повторный подсчет?",
            "no:Нет;do_cnf_cell:Да", "no", null, getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlCnfFin(TaskContext ctx) throws Exception {
    callSetMsg("Имеются расхождения данных учета с просканированным товаром:<br>\r\n"
            + d.getDifStr(true), ctx);
    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Завершить инвентаризацию ячейки " + d.getLgpla() + "?",
            "no:Нет;do_cnf_fin:Да", "no", null, getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlCnfExit() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Выйти из инвентаризации?",
            "no:Нет;do_cnf_exit:Да", "no", null, getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlCnfDelAll() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Удалить все полученные данные по ячейке " + d.getLgpla() + "?",
            "no:Нет;do_cnf_delall:Да", "no", null, null, false);
    return p.getPage();
  }

  private FileData htmlMenuDelPal() throws Exception {
    ArrayList<InventTovData> tov = d.getTov();
    int n = tov.size();
    String pal = "";
    String pal2;
    String def = "";
    for (int i = 0; i < n; i++) {
      pal2 = tov.get(i).pal;
      if (!pal.equals(pal2)) {
        pal = pal2;
        def = def + "delpal" + pal + ":" + pal + ";";
      }
    }
    def = def + "cont:Назад";

    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Удаление данных по паллете:",
            def, "cont", null, null, false);
    return p.getPage();
  }

  private FileData htmlCnfCancel() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация",
            "Отменить инвентаризацию ячейки " + d.getLgpla() + "?",
            "no:Нет;do_cnf_cancel:Да", "no", null, null, false);
    return p.getPage();
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String def;
    String askQtyMenu;
    if (ctx.user.getAskQty()) {
      askQtyMenu = "ask_qty_off:Выкл ввод кол-ва";
    } else {
      askQtyMenu = "ask_qty_on:Вкл ввод кол-ва";
    }
    String delMenu = "";
    if (!d.getTov().isEmpty()) {
      if (d.getMisch().equals("X")) {
        delMenu = ";delpal:Удалить по паллете;delall:Удалить всё по ячейке";
      } else {
        delMenu = ";delall:Удалить всё по ячейке";
      }
    }

    switch (getTaskState()) {
      case SEL_CELL:
        def = "cont:Продолжить;later:Отложить;" + askQtyMenu + ";fin:Выход";
        break;

      case PAL_CELL:
        def = "cont:Продолжить;later:Отложить;difmat:Расхождения" + delMenu
                + ";cancel:Отменить инв ячейки;" + askQtyMenu;
        break;

      case TOV_CELL:
      case TOV_PAL_CELL:
        def = "cont:Продолжить;later:Отложить;difmat:Расхождения" + delMenu
                + ";dellast:Удалить последнее сканирование;cancel:Отменить инв ячейки;" + askQtyMenu;
        break;

      case QTY:
        def = "cont:Продолжить;later:Отложить";
        break;

      default:
        def = "cont:Продолжить;later:Отложить;" + askQtyMenu;
        break;
    }

    if (RefInfo.haveInfo(ProcType.INVENT)) {
      def = def + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Инвентаризация", "Выберите действие",
            def, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (getTaskState() == TaskState.SEL_SKL) {
      // обработка выбора склада
      callClearErrMsg(ctx);
      return handleMenuSelSkl(menu, ctx);
    } else if (menu.equals("no")) {
      callClearErr(ctx);
      callSetMsg("Операция отменена", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("do_cnf_cell")) {
      callClearErrMsg(ctx);
      return handleMenuCnfCellDo(ctx);
    } else if (menu.equals("do_cnf_fin")) {
      callClearErrMsg(ctx);
      return handleMenuCnfFinCellDo(d.getLastScan(), ctx);
    } else if (menu.equals("do_cnf_exit")) {
      callClearErrMsg(ctx);
      return handleMenuCnfExit(ctx);
    } else if (menu.equals("do_cnf_cancel")) {
      callClearErrMsg(ctx);
      return handleMenuCnfCancelDo(ctx);
    } else if (menu.equals("cont")) {
      return htmlGet(false, ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("fin")) {
      callClearErrMsg(ctx);
      return handleMenuFin(ctx);
    } else if (menu.equals("cancel")) {
      callClearErrMsg(ctx);
      return htmlCnfCancel();
    } else if (menu.equals("ask_qty_on")) {
      return handleAskQtyOn(ctx);
    } else if (menu.equals("ask_qty_off")) {
      return handleAskQtyOff(ctx);
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delall")) {
      return htmlCnfDelAll(); // удалить всё по ячейке
    } else if (menu.equals("delpal")) {
      return htmlMenuDelPal();
    } else if (menu.startsWith("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPal(menu, ctx);
    } else if (menu.equals("do_cnf_delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx);
    } else if (menu.equals("difmat")) {
      callClearErrMsg(ctx);
      return handleMenuDifMat();
    } else {
      callClearErrMsg(ctx);
      callSetErr("Неизвестный выбор меню: " + menu + " (сообщите разработчику)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuDifMat() throws Exception {
    HtmlPage p = new HtmlPage();
    p.title = "Расхождения";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addText("<div style=\"font-size:" + TSparams.fontSize3
            + "pt\"><table cellpadding=1 cellspacing=0>\r\n");
    p.addText("<tr><td align=right><b>По САПу:</b></td><td align=right> "
            + delDecZeros(d.getQtySAP().toString()) + "</td></tr>\r\n");
    p.addText("<tr><td align=right><b>Отсканировано:</b></td><td align=right> "
            + delDecZeros(d.getQtyScan().toString()) + "</td></tr>\r\n");
    p.addText("<tr><td align=right><b>Недостачи:</b></td><td align=right> "
            + delDecZeros(d.getQtyNedost().toString()) + "</td></tr>\r\n");
    p.addText("<tr><td align=right><b>Излишки:</b></td><td align=right> "
            + delDecZeros(d.getQtyIzl().toString()) + "</td></tr>\r\n");
    p.addText("</table></div>\r\n");

    p.addNewLine();

    ConcurrentSkipListMap<String, InventTovDataM> tovM = new ConcurrentSkipListMap<String, InventTovDataM>();
    for (Entry<String, InventTovDataM> i : d.getTovM().entrySet()) {
      tovM.put(i.getKey(), i.getValue());
    }
    InventTovDataM tt;

    p.addText("<div style=\"font-size:" + TSparams.fontSize3
            + "pt\"><table width=100% border=1 cellpadding=1 cellspacing=0>\r\n");
    p.addText("<tr><td colspan=4><b>Название материала</b></td></tr>\r\n");
    p.addText("<tr><td align=center><b>Материал</b></td><td align=center><b>САП</b></td><td align=center><b>Скан</b></td><td align=center><b>Разница</b></td></tr>\r\n");
    for (Entry<String, InventTovDataM> i : tovM.entrySet()) {
      tt = i.getValue();
      p.addText("<tr><td colspan=4><font color=blue>" + RefMat.getFullName(tt.matnr) + "</font></td></tr>\r\n");
      p.addText("<tr><td>" + tt.matnr
              + "</td><td align=right><font color=blue>" + delDecZeros(tt.qtyTobe.toString())
              + "</font></td><td align=right>" + delDecZeros(tt.qty.toString())
              + "</td><td align=center>" + delDecZeros(tt.qty.subtract(tt.qtyTobe).toString())
              + "</td></tr>\r\n");
    }
    p.addText("</table></div>\r\n");

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleMenuDelPal(String menu, TaskContext ctx) throws Exception {
    String pal = menu.substring(6);
    d.callDelPal(pal, ctx);
    callSetMsg("Данные подсчета по паллете " + pal + " удалены", ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    d.callDelAll(ctx);
    callSetMsg("Все данные сканирования по ячейке " + d.getLgpla() + " удалены", ctx);
    callAddHist("Удалено всё по ячейке " + d.getLgpla(), ctx);
    callSetTaskState(TaskState.PAL_CELL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    // Отменить последнее сканирование
    ArrayList<InventTovData> tov = d.getTov();
    if (tov.isEmpty()) {
      callSetErr("Нет последнего сканирования", ctx);
      return htmlGet(true, ctx);
    }

    String pal = d.getPal();
    InventTovData td = tov.get(tov.size() - 1);
    if (!td.pal.equals(pal)) {
      callSetErr("Нет последнего сканирования на текущей паллете (" + pal + ")", ctx);
      return htmlGet(true, ctx);
    }

    int nn = d.getNScan() - 1;
    String s = "Отменено: " + delDecZeros(td.qty.toString())
            + " ед: " + td.matnr + "/" + td.charg + " "
            + RefMat.getName(td.matnr) + " (" + nn + " скан в яч)";
    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    d.callDelLast(ctx);
    if (d.getTov().isEmpty()) {
      callSetTaskState(TaskState.PAL_CELL, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleAskQtyOn(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(true, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleAskQtyOff(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(false, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuSelSkl(String menu, TaskContext ctx) throws Exception {
    // обработка выбора склада
    if ((menu.length() == 12) && menu.startsWith("sellgort")) {
      String lg = menu.substring(8);
      if (!rightsLgort(lg, ctx)) {
        callSetErr("Нет прав по складу " + lg, ctx);
        return htmlGet(true, ctx);
      } else {
        d.callSetLgort(lg, TaskState.SEL_CELL, ctx);
        callTaskNameChange(ctx);
        Z_TS_INV5 f = new Z_TS_INV5();
        f.LGNUM = d.getLgnum();
        f.execute();
        if (!f.isErr) {
          d.callSetInvId(f.INV_ID, ctx);
          if (!f.MSG.isEmpty()) {
            callSetMsg(f.MSG, ctx);
          }
        }
        return htmlGet(false, ctx);
      }
    } else {
      callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuCnfCellDo(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case SEL_CELL:
        return handleScanCellDo("C" + d.getLgpla(), true, ctx);

      default:
        callSetErr("Неверное состояние задачи при подтверждении ячейки", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuCnfFinCellDo(String nextScan, TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case PAL_CELL:
      case TOV_CELL:
      case TOV_PAL_CELL:
        // завершение инв ячейки

        Z_TS_INV3 f = new Z_TS_INV3();
        f.WERKS = d.getWerks();
        f.LGORT = d.getLgort();
        f.LGNUM = d.getLgnum();
        f.IVNUM = d.getIvnum();
        f.INV_ID = d.getInvId();
        f.TSD_USER = ctx.user.getUserSHK();
        f.LGTYP = d.getLgtyp();
        f.LGPLA = d.getLgpla();
        f.IT_INV = d.getTovData();

        f.execute();

        if (!f.isErr) {
          String s = "Инвентаризация ячейки " + d.getLgpla() + " завершена"
                  + "<br>\r\nКОЛ-ВО " + d.getDifStr(true);
          if (f.NOT_CLOSED.equals("X")) {
            s = s + "<br>\r\n(разницы не списаны из-за ошибки, сообщите оператору)";
          }
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
          callSetTaskState(TaskState.SEL_CELL, ctx);
          d.callClearTovData(ctx);
          if (nextScan == null) {
            return htmlGet(true, ctx);
          } else {
            return handleScanCellDo(nextScan, false, ctx);
          }
        } else if (nextScan == null) {
          callSetErr(f.err, ctx);
        } else {
          callSetErr(f.err + "<br>\r\n(сканирование " + nextScan + " не принято)", ctx);
        }

        return htmlGet(true, ctx);

      default:
        callSetErr("Неверное состояние задачи при завершении инв ячейки", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuCnfCancelDo(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case PAL_CELL:
      case TOV_CELL:
      case TOV_PAL_CELL:
        // отмена (удаление) документа инвентаризации

        Z_TS_INV4 f = new Z_TS_INV4();
        f.LGNUM = d.getLgnum();
        f.IVNUM = d.getIvnum();
        f.INV_ID = d.getInvId();

        f.execute();

        if (!f.isErr) {
          callSetTaskState(TaskState.SEL_CELL, ctx);
          d.callClearTovData(ctx);
          String s = "Инвентаризация ячейки " + d.getLgpla() + " отменена";
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else {
          callSetErr(f.err, ctx);
          d.callSetErrOnCancel(ctx);
        }

        return htmlGet(true, ctx);

      default:
        callSetErr("Неверное состояние задачи при отмене инв ячейки", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuFin(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case SEL_CELL:
        if (d.getInvId() > 0) {
          Z_TS_INV5 f = new Z_TS_INV5();
          f.LGNUM = d.getLgnum();
          f.execute();
          if (!f.isErr) {
            if (f.MSG.isEmpty()) {
              callTaskFinish(ctx);
              return null;
            } else {
              callSetMsg(f.MSG, ctx);
              return htmlCnfExit();
            }
          } else {
            callSetErr(f.err, ctx);
            return htmlGet(true, ctx);
          }
        } else {
          callTaskFinish(ctx);
          return null;
        }

      default:
        if (d.getErrOnCancel()) {
          callTaskFinish(ctx);
          return null;
        } else {
          callSetErr("Неверное состояние задачи при её завершении", ctx);
          return htmlGet(true, ctx);
        }
    }
  }

  private FileData handleMenuCnfExit(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case SEL_CELL:
        callTaskFinish(ctx);
        return null;

      default:
        callSetErr("Неверное состояние задачи при её завершении", ctx);
        return htmlGet(true, ctx);
    }
  }

  @Override
  public String procName() {
    return getProcType().text + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    String askQtyStr = ctx.user.getAskQty() ? "<b>ввод кол-ва</b>" : "<b>БЕЗ ввода кол-ва</b>";

    if (!d.getLgort().isEmpty()) {
      return d.getLgort() + "; " + askQtyStr;
    } else {
      return askQtyStr;
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }
}

class InventTovData {

  public String pal;
  public String matnr;
  public String charg;
  public BigDecimal qty;

  public InventTovData(String pal, String matnr, String charg, BigDecimal qty) {
    this.pal = pal;
    this.matnr = matnr;
    this.charg = charg;
    this.qty = qty;
  }
}

class InventTovDataM {

  public String matnr;
  public BigDecimal qty;
  public BigDecimal qtyTobe;

  public InventTovDataM(String matnr, BigDecimal qty, BigDecimal qtyTobe) {
    this.matnr = matnr;
    this.qty = qty;
    this.qtyTobe = qtyTobe;
  }
}

class InventData extends ProcData {

  private String lgort = ""; // Склад
  private String lgpla = ""; // Складское место
  private String lgnum = ""; // Номер склада/комплекс
  private String lgtyp = ""; // Тип склада
  private String ivnum = ""; // Номер инвентаризационной описи
  private String misch = ""; // Индикатор СмешанСкладирования
  private String werks = ""; // Завод
  private int invId = 0; // ID из журнала инв
  private int nScan = 0; // число кип (сканирований)
  private String pal = ""; // текущая паллета
  private String lastScan = null; // предыдущее сканирование
  private final ArrayList<InventTovData> tov = new ArrayList<InventTovData>(); // товар в ячейке
  private final HashMap<String, InventTovDataM> tovM = new HashMap<String, InventTovDataM>(); // сравнение по материалам
  private BigDecimal qtyIzl = new BigDecimal(0); // общее кол-во излишков (по материалам)
  private BigDecimal qtyNedost = new BigDecimal(0); // общее кол-во недостач (по материалам)
  private BigDecimal qtySAP = new BigDecimal(0);
  private BigDecimal qtyScan = new BigDecimal(0);
  private boolean errOnCancel = false;

  public boolean getErrOnCancel() {
    return errOnCancel;
  }

  public int getNScan() {
    return nScan;
  }

  public ArrayList<InventTovData> getTov() {
    return tov;
  }

  public HashMap<String, InventTovDataM> getTovM() {
    return tovM;
  }

  public boolean haveDif() {
    // проверка наличия расхождений
    InventTovDataM v;
    for (Entry<String, InventTovDataM> i : tovM.entrySet()) {
      v = i.getValue();
      if (v.qty.compareTo(v.qtyTobe) != 0) {
        return true;
      }
    }
    return false;
  }

  public String getDifStr(boolean showAll) {
    String ret = "недост: " + ProcessTask.delDecZeros(qtyNedost.toString())
            + " изл: " + ProcessTask.delDecZeros(qtyIzl.toString());
    if (showAll) {
      ret = "по САПу: " + ProcessTask.delDecZeros(qtySAP.toString())
              + " скан: " + ProcessTask.delDecZeros(qtyScan.toString())
              + " " + ret;
    }
    ret = ret + "; " + nScan + " скан в яч";
    return ret;
  }

  public ZTS_INV_QTY_S[] getTovData() {
    // данные подсчета
    ZTS_INV_QTY_S[] ret = new ZTS_INV_QTY_S[tov.size()];
    InventTovData v;
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new ZTS_INV_QTY_S();
      v = tov.get(i);
      ret[i].LENUM = v.pal;
      ret[i].MATNR = ProcessTask.fillZeros(v.matnr, 18);
      ret[i].CHARG = ProcessTask.fillZeros(v.charg, 10);
      ret[i].QTY = v.qty;
    }
    return ret;
  }

  public BigDecimal getQtySAP() {
    return qtySAP;
  }

  public BigDecimal getQtyScan() {
    return qtyScan;
  }

  public BigDecimal getQtyIzl() {
    return qtyIzl;
  }

  public BigDecimal getQtyNedost() {
    return qtyNedost;
  }

  public String getLastScan() {
    return lastScan;
  }

  public String getLgort() {
    return lgort;
  }

  public String getLgpla() {
    return lgpla;
  }

  public String getLgnum() {
    return lgnum;
  }

  public String getLgtyp() {
    return lgtyp;
  }

  public String getIvnum() {
    return ivnum;
  }

  public String getMisch() {
    return misch;
  }

  public String getWerks() {
    return werks;
  }

  public int getInvId() {
    return invId;
  }

  public String getPal() {
    return pal;
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetErrOnCancel(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.ERR_ON_CANCEL);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelAll(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_ALL);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelPal(String pal, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.DEL_PAL, pal);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetInvId(int invId, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (this.invId != invId) {
      dr.setI(FieldType.INV_ID, invId);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetInv0(String lgpla, String lgnum, String lgtyp,
          String ivnum, String misch, String werks, int invId,
          ZTS_MP_QTY_S[] tov, TaskState state, TaskContext ctx) throws Exception {

    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.lgpla, lgpla)) {
      dr.setS(FieldType.CELL, lgpla);
    }
    if (!strEq(this.lgnum, lgnum)) {
      dr.setS(FieldType.LGNUM, lgnum);
    }
    if (!strEq(this.lgtyp, lgtyp)) {
      dr.setS(FieldType.LGTYP1, lgtyp);
    }
    if (!strEq(this.ivnum, ivnum)) {
      dr.setS(FieldType.IVNUM, ivnum);
    }
    if (!strEq(this.misch, misch)) {
      dr.setS(FieldType.MISCH, misch);
    }
    if (!strEq(this.werks, werks)) {
      dr.setS(FieldType.WERKS, werks);
    }
    if (this.invId != invId) {
      dr.setI(FieldType.INV_ID, invId);
    }
    if (tov != null) {
      int n = tov.length;
      String[] m = new String[n];
      String[] c = new String[n];
      BigDecimal[] q = new BigDecimal[n];
      for (int i = 0; i < n; i++) {
        m[i] = ProcessTask.delZeros(tov[i].MATNR);
        c[i] = ProcessTask.delZeros(tov[i].CHARG);
        q[i] = tov[i].QTY;
      }
      dr.setSa(FieldType.MATNRS, m);
      dr.setSa(FieldType.CHARGS, c);
      dr.setNa(FieldType.QTYS, q);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell(String lgpla, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.lgpla, lgpla)) {
      dr.setS(FieldType.CELL, lgpla);
      dr.setI(FieldType.LOG, LogType.SET_CELL.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal(String pal, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.pal, pal)) {
      dr.setS(FieldType.PAL, pal);
    }
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

  public void callSetLgort(String lgort, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
      String lgnum1;
      if (!strEq(lgort, null)) {
        RefLgortStruct l = RefLgort.get(lgort);
        lgnum1 = l.lgnum;
      } else {
        lgnum1 = "";
      }
      if (!strEq(lgnum1, this.lgnum)) {
        dr.setS(FieldType.LGNUM, lgnum1);
      }
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTovData(TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!tov.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.PAL, pal);
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void addTovM(InventTovData td) throws Exception {
    // добавление в tovM
    if (td.qty.signum() == 0) {
      return;
    }
    InventTovDataM v = tovM.get(td.matnr);
    if (v == null) {
      v = new InventTovDataM(td.matnr, td.qty, new BigDecimal(0));
      tovM.put(td.matnr, v);
      qtyIzl = qtyIzl.add(td.qty);
    } else {
      int cmp = v.qty.compareTo(v.qtyTobe);
      if (cmp < 0) {
        qtyNedost = qtyNedost.add(v.qty).subtract(v.qtyTobe);
      } else if (cmp > 0) {
        qtyIzl = qtyIzl.add(v.qtyTobe).subtract(v.qty);
      }
      v.qty = v.qty.add(td.qty);
      cmp = v.qty.compareTo(v.qtyTobe);
      if (cmp < 0) {
        qtyNedost = qtyNedost.add(v.qtyTobe).subtract(v.qty);
      } else if (cmp > 0) {
        qtyIzl = qtyIzl.add(v.qty).subtract(v.qtyTobe);
      }
      if ((v.qty.signum() == 0) && (v.qtyTobe.signum() == 0)) {
        tovM.remove(td.matnr);
      }
    }
    qtyScan = qtyScan.add(td.qty);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = dr.getValStr(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.CELL)) {
          lgpla = dr.getValStr(FieldType.CELL);
        }
        if (dr.haveVal(FieldType.LGNUM)) {
          lgnum = dr.getValStr(FieldType.LGNUM);
        }
        if (dr.haveVal(FieldType.LGTYP1)) {
          lgtyp = dr.getValStr(FieldType.LGTYP1);
        }
        if (dr.haveVal(FieldType.IVNUM)) {
          ivnum = dr.getValStr(FieldType.IVNUM);
        }
        if (dr.haveVal(FieldType.MISCH)) {
          misch = dr.getValStr(FieldType.MISCH);
        }
        if (dr.haveVal(FieldType.WERKS)) {
          werks = dr.getValStr(FieldType.WERKS);
        }
        if (dr.haveVal(FieldType.INV_ID)) {
          invId = (Integer) dr.getVal(FieldType.INV_ID);
        }
        if (dr.haveVal(FieldType.PAL)) {
          pal = dr.getValStr(FieldType.PAL);
        }
        if (dr.haveVal(FieldType.LAST_SCAN)) {
          lastScan = dr.getValStr(FieldType.LAST_SCAN);
        }
        if (dr.haveVal(FieldType.MATNRS) && dr.haveVal(FieldType.CHARGS)
                && dr.haveVal(FieldType.QTYS)) {
          tovM.clear();
          qtyIzl = new BigDecimal(0);
          qtyNedost = new BigDecimal(0);
          qtySAP = new BigDecimal(0);
          nScan = 0;
          String[] mm = (String[]) dr.getVal(FieldType.MATNRS);
          BigDecimal[] qq = (BigDecimal[]) dr.getVal(FieldType.QTYS);
          InventTovDataM v;
          String matnr;
          BigDecimal qty;
          for (int i = 0; i < mm.length; i++) {
            matnr = ProcessTask.delZeros(mm[i]);
            qty = qq[i];
            v = tovM.get(matnr);
            if (v == null) {
              v = new InventTovDataM(matnr, new BigDecimal(0), qty);
              tovM.put(matnr, v);
            } else {
              v.qtyTobe = v.qtyTobe.add(qty);
            }
            qtyNedost = qtyNedost.add(qty);
            qtySAP = qtySAP.add(qty);
          }
        }
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          tov.clear();
          tovM.clear();
          qtyIzl = new BigDecimal(0);
          qtyNedost = new BigDecimal(0);
          qtySAP = new BigDecimal(0);
          qtyScan = new BigDecimal(0);
          nScan = 0;
        }
        if (dr.haveVal(FieldType.PAL) && dr.haveVal(FieldType.MATNR)
                && dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY)) {
          // добавление в tov
          InventTovData td = new InventTovData(dr.getValStr(FieldType.PAL),
                  dr.getValStr(FieldType.MATNR),
                  dr.getValStr(FieldType.CHARG),
                  (BigDecimal) dr.getVal(FieldType.QTY));
          tov.add(td);
          // добавление в tovM
          addTovM(td);
          nScan++;
        }
        if (dr.haveVal(FieldType.DEL_LAST)) {
          if (!tov.isEmpty()) {
            int i = tov.size() - 1;
            InventTovData td = tov.get(i);
            tov.remove(i);
            td.qty = td.qty.negate();
            addTovM(td);
            nScan--;
          }
        }
        if (dr.haveVal(FieldType.ERR_ON_CANCEL)) {
          errOnCancel = true;
        }
        if (dr.haveVal(FieldType.DEL_PAL)) {
          String pal1 = dr.getValStr(FieldType.DEL_PAL);
          int n = tov.size();
          InventTovData td;
          for (int i = n - 1; i >= 0; i--) {
            td = tov.get(i);
            if (td.pal.equals(pal1)) {
              tov.remove(i);
              td.qty = td.qty.negate();
              addTovM(td);
              nScan--;
            }
          }
        }
        if (dr.haveVal(FieldType.DEL_ALL)) {
          tov.clear();
          qtyIzl = new BigDecimal(0);
          qtyScan = new BigDecimal(0);
          qtyNedost = new BigDecimal(0);
          nScan = 0;
          int n = tovM.size();
          InventTovDataM dd;
          MapItemArray tmp = new MapItemArray(tovM);
          for (Entry<String, InventTovDataM> i : tovM.entrySet()) {
            dd = i.getValue();
            if (dd.qtyTobe.signum() == 0) {
              tmp.MarkItem(i.getKey());
            } else {
              dd.qty = new BigDecimal(0);
              qtyNedost = qtyNedost.add(dd.qtyTobe);
            }
          }
          tmp.RemoveAll();
        }
        break;
    }
  }
}
