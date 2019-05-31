package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_PRT_QTY_S;
import ntx.ts.html.HtmlPage;
import ntx.ts.html.HtmlPageMenu;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Приемка на склад с производства Состояния: START SEL_SKL TOV TOV_PAL SEL_CELL
 */
public class ProcessProdPriemka extends ProcessTask {

  private final ProdPriemkaData d = new ProdPriemkaData();

  public ProcessProdPriemka(long procId) throws Exception {
    super(ProcType.PRODPRI, procId);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
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

      default:
        return htmlWork("Приход с пр-ва", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      Z_TS_PROD7 f = new Z_TS_PROD7();
      f.LGORT = lgorts[0];
      f.execute();
      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlGet(true, ctx);
      }
      d.callSetLgort(lgorts[0], !f.LGNUM.isEmpty(), TaskState.TOV, ctx);
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
      case TOV:
        return handleScanTov(scan, ctx);

      case TOV_PAL:
        return handleScanTovPal(scan, ctx);

      case SEL_CELL:
        return handleScanCell(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    if (isScanTov(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPalDo(scan, ctx);
    } else if (isScanTov(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovDo(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    String charg = getScanCharg(scan);
    RefChargStruct c = RefCharg.get(charg, null);
    if (c == null) {
      callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
    BigDecimal q = getScanQty(scan);
    if (q.signum() <= 0) {
      callSetErr("Кол-во в штрих-коде должно быть больше нуля (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    ProdPriemkaTovData r = d.getPrtQtyNull(charg);
    boolean ok = false;
    BigDecimal qtyTot = q;

    String abc = "";

    if (r != null) {
      // партия уже сканировалась, проверяем имеющееся макс кол-во
      qtyTot = qtyTot.add(r.qty);
      if (qtyTot.compareTo(r.qtyMax) <= 0) {
        ok = true;
        abc = r.abc;
        d.callAddTov(charg, q, r.qtyMax, abc, null, ctx);
      }
    }

    if (!ok) {
      Z_TS_PROD1 f = new Z_TS_PROD1();
      f.LGORT = d.getLgort();
      f.CHARG = fillZeros(charg, 10);
      f.EBELNS = d.getEbelns();
      f.QTY_TOT = qtyTot;

      f.execute();

      if (!f.isErr) {
        abc = f.ABC;
        d.callAddTov(charg, q, f.QTY_MAX, abc, TaskState.TOV_PAL, ctx);
        d.callSetEbelns(f.EBELNS2, ctx);
        ok = true;
      } else {
        callSetErr(f.err, ctx);
      }
    }

    if (ok) {
      String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr)
              + ": " + delDecZeros(q.toString()) + " ед (на паллете: "
              + delDecZeros(d.getQtyPal().toString()) + " ед; "
              + d.getNScan() + " скан)";
      if (!abc.isEmpty()) {
        s += " (ABC: " + abc + ")";
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanPalDo(String scan, TaskContext ctx) throws Exception {
    if (d.isAddrSkl()) {
      return handleScanPalDoAddr(scan, ctx);
    } else {
      return handleScanPalDoNaddr(scan, ctx);
    }
  }

  private FileData handleScanPalDoAddr(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    Z_TS_PROD2 f = new Z_TS_PROD2();
    f.PAL = scan.substring(1);

    f.execute();

    if (!f.isErr) {
      d.callSetPal(f.PAL, TaskState.SEL_CELL, ctx);
      callSetMsg("Товар привязан к паллете " + f.PAL, ctx);
      callAddHist("Паллета " + f.PAL, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanPalDoNaddr(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    Z_TS_PROD3 f = new Z_TS_PROD3();
    f.PAL = scan.substring(1);
    f.LGORT = d.getLgort();
    f.EBELNS = d.getEbelns();
    f.USER_CODE = ctx.user.getUserSHK();
    f.IT_TOV = d.getScanData();

    f.execute();
    String vbeln = delZeros(f.VBELN);

    if (!f.isErr) {
      d.callSetVbeln(f.VBELN, ctx);
      d.callSetPal(f.PAL, TaskState.TOV, ctx);
      callSetMsg("Товар привязан к паллете " + f.PAL + "; создана поставка " + vbeln, ctx);
      callAddHist("Паллета " + f.PAL + "; поставка " + vbeln, ctx);
    } else {
      callSetErr(f.err, ctx);
      if (f.J_CREATED.equals("X")) {
        d.callClearTovData(TaskState.TOV, ctx);
        callAddHist("Создана поставка " + vbeln + " с ошибкой в журнале ZTSD30", ctx);
      }
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_PROD3 f = new Z_TS_PROD3();
    f.LGORT = d.getLgort();
    f.EBELNS = d.getEbelns();
    f.PAL = d.getPal();
    f.LGPLA = scan.substring(1);
    f.USER_CODE = ctx.user.getUserSHK();
    f.IT_TOV = d.getScanData();

    f.execute();
    String vbeln = delZeros(f.VBELN);

    if (!f.isErr) {
      d.callSetCellVbeln(f.LGPLA, f.VBELN, TaskState.TOV, ctx);
      callSetMsg("Товар размещен в " + f.LGPLA + "; создана поставка " + vbeln, ctx);
      callAddHist("Ячейка " + f.LGPLA + "; создана поставка " + vbeln, ctx);
    } else {
      callSetErr(f.err, ctx);
      if (f.J_CREATED.equals("X")) {
        d.callClearTovData(TaskState.TOV, ctx);
        callAddHist("Создана поставка " + vbeln + " с ошибкой в журнале ZTSD30", ctx);
      }
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;

    switch (getTaskState()) {
      case TOV:
        definition = "cont:Продолжить;later:Отложить;fin:Завершить";
        break;

      case TOV_PAL:
        definition = "cont:Продолжить;later:Отложить;showtov:Показать общее кол-во;"
                + "showzak:Показать заказы на поставку;"
                + "dellast:Отменить последнее сканирование;delall:Отменить всё несохраненное";
        break;

      case SEL_CELL:
        definition = "cont:Продолжить;opis:Печать описи паллеты;later:Отложить;showtov:Показать общее кол-во;"
                + "showzak:Показать заказы на поставку;"
                + "delpal:Отменить сканирование паллеты;delall:Отменить всё несохраненное";
        break;

      default:
        definition = "cont:Назад;later:Отложить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.PRODPRI)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Приход с пр-ва", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
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
          Z_TS_PROD7 f = new Z_TS_PROD7();
          f.LGORT = lg;
          f.execute();
          if (f.isErr) {
            callSetErr(f.err, ctx);
            return htmlGet(true, ctx);
          }
          d.callSetLgort(lg, !f.LGNUM.isEmpty(), TaskState.TOV, ctx);
          callTaskNameChange(ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(false, ctx);
    } else if (menu.equals("no")) {
      callClearErr(ctx);
      callSetMsg("Операция отменена", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("cont")) {
      return htmlGet(false, ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("showtov")) {
      return htmlShowTov(ctx);
    } else if (menu.equals("showzak")) {
      return htmlShowZak(ctx);
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delall")) {
      callClearErrMsg(ctx);
      return htmlCnfDelAll(); // удалить всё несохраненное (запрос подтверждения)
    } else if (menu.equals("do_cnf_delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx);
    } else if (menu.equals("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPal(ctx); // отменить привязку к паллете (для продолжения сканирования товара)
    } else if (menu.equals("opis")) {
      callClearErrMsg(ctx);
      return htmlSelPrinter("opis", "Выберите принтер для печати описи паллеты:", ctx);
    } else if (menu.startsWith("opis_")) {
      callClearErrMsg(ctx);
      return handleMenuOpis(menu.substring(5), ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData htmlShowTov(TaskContext ctx) throws Exception {
    HashMap<String, ProdPriemkaTovData> pq = d.getPQ();

    int n = pq.size();

    if (n == 0) {
      callClearErr(ctx);
      callSetMsg("Несохраненных позиций нет (ничего не отсканировано)", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Общее кол-во";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Отсканированные партии на текущей паллете:</b>");
    p.addNewLine();
    ProdPriemkaTovData dt;
    RefChargStruct c;
    String charg;
    for (Entry<String, ProdPriemkaTovData> i : pq.entrySet()) {
      dt = i.getValue();
      charg = i.getKey();
      c = RefCharg.getNoNull(charg);
      p.addLine("<b><font color=blue>" + delDecZeros(dt.qty.toString()) + " ед:</font> "
              + c.matnr + "/" + charg + " <b> " + RefMat.getFullName(c.matnr) + "</b>");
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData htmlShowZak(TaskContext ctx) throws Exception {
    Z_TS_PROD4 f = new Z_TS_PROD4();
    f.LGORT = d.getLgort();
    f.IT_TOV = d.getScanData();

    f.execute();

    if (f.isErr) {
      callClearErrMsg(ctx);
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    int n = f.IT_EBELNS.length;

    if (n == 0) {
      callClearErr(ctx);
      callSetMsg("Несохраненных позиций нет (ничего не отсканировано)", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Заказы по партиям";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Заказы по партиям:</b>");
    p.addNewLine();

    String s = null;
    String ss;
    String charg;
    for (int i = 0; i < n; i++) {
      ss = delZeros(f.IT_EBELNS[i].EBELN) + " (" + delDecZeros(f.IT_EBELNS[i].QTY.toString()) + " ед)";
      if (f.IT_EBELNS[i].QTY.compareTo(f.IT_EBELNS[i].QTY_SCAN) < 0) {
        ss = "<font color=red>" + ss + "</font>";
      }
      if (s == null) {
        s = ss;
      } else {
        s = s + ", " + ss;
      }

      if ((i == (n - 1)) || !f.IT_EBELNS[i].CHARG.equals(f.IT_EBELNS[i + 1].CHARG)) {
        charg = delZeros(f.IT_EBELNS[i].CHARG);
        p.addLine("<font color=blue>" + delDecZeros(f.IT_EBELNS[i].QTY_SCAN.toString())
                + " ед " + RefCharg.getNoNull(charg).matnr + "/" + charg + ":</font> " + s);
        s = null;
      }
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    Z_TS_PROD6 f = new Z_TS_PROD6();
    f.LGORT = d.getLgort();
    f.IT_TOV = d.getScanData();

    int n = f.IT_TOV.length;
    if (n == 0) {
      callSetErr("Нет отсканированных позиций (для отмены)", ctx);
      return htmlGet(true, ctx);
    }

    f.CHARG = f.IT_TOV[n - 1].CHARG;
    String charg = delZeros(f.CHARG);
    BigDecimal q = f.IT_TOV[n - 1].QTY;

    f.execute();

    if (!f.isErr) {
      d.callSetEbelns(f.EBELNS, ctx);
      d.callDelLast(f.QTY_MAX, n == 1 ? TaskState.TOV : TaskState.TOV_PAL, ctx);
      String matnr = RefCharg.getNoNull(charg).matnr;
      String s = "Отменено: " + delDecZeros(q.toString())
              + " ед: " + matnr + "/" + charg + " "
              + RefMat.getFullName(matnr) + " (на паллете: "
              + delDecZeros(d.getQtyPal().toString()) + " ед; "
              + d.getNScan() + " скан)";
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelPal(TaskContext ctx) throws Exception {
    callSetMsg("Привязка товара к паллете " + d.getPal() + " отменена", ctx);
    callAddHist("Привязка к паллете " + d.getPal() + " отменена", ctx);
    callSetTaskState(TaskState.TOV_PAL, ctx);

    return htmlGet(true, ctx);
  }

  private FileData htmlCnfDelAll() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Приход с пр-ва",
            "Удалить все данные по текущей паллете?",
            "no:Нет;do_cnf_delall:Да", "no", null, null, false);
    return p.getPage();
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    d.callClearTovData(TaskState.TOV, ctx);
    callSetMsg("Все данные по текущей паллете удалены", ctx);
    callAddHist("Все данные по текущей паллете удалены", ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleMenuOpis(String printer, TaskContext ctx) throws Exception {
    // печать описи паллеты

    ctx.user.callSetPrinter(printer, ctx);

    Z_TS_PROD5 f = new Z_TS_PROD5();
    f.LGORT = d.getLgort();
    f.LENUM = d.getPal();
    f.USER_CODE = ctx.user.getUserSHK();
    f.TDDEST = printer.toUpperCase();
    f.EBELNS = d.getEbelns();
    f.IT_TOV = d.getScanData();

    f.execute();

    if (!f.isErr) {
      callSetMsg("Опись паллеты " + f.LENUM + " распечатана на принтере " + printer.toUpperCase(), ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  @Override
  public String procName() {
    return getProcType().text + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    return d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name;
  }
}

class ProdPriemkaScanData {  // отсканированные позиции

  public String charg;
  public BigDecimal qty;
  public String abc;

  public ProdPriemkaScanData(String charg, BigDecimal qty, String abc) {
    this.charg = charg;
    this.qty = qty;
    this.abc = abc;
  }
}

class ProdPriemkaTovData {  // общее и максимальное кол-во по партии

  public BigDecimal qty;
  public BigDecimal qtyMax;
  public String abc;

  public ProdPriemkaTovData(BigDecimal qty, BigDecimal qtyMax, String abc) {
    this.qty = qty;
    this.qtyMax = (qtyMax == null ? BigDecimal.ZERO : qtyMax);
    this.abc = abc;
  }
}

class ProdPriemkaData extends ProcData {

  private String lgort = ""; // склад
  private String pal = ""; // паллета
  private String cell = null; // ячейка (на всякий случай)
  private String vbeln = null; // созданнаЯ поставка (на всякий случай)
  private BigDecimal qtyPal = BigDecimal.ZERO; // общее кол-во на паллете
  private String ebelns = "-";
  private ArrayList<ProdPriemkaScanData> scanData
          = new ArrayList<ProdPriemkaScanData>(); // отсканированный товар (по текущей паллете; партия, кол-во)
  private HashMap<String, ProdPriemkaTovData> pq
          = new HashMap<String, ProdPriemkaTovData>(); // кол-во по партиям
  private boolean addrSkl = true; // признак адресного склада

  public boolean isAddrSkl() {
    return addrSkl;
  }

  public int getNScan() {
    return scanData.size();
  }

  public BigDecimal getQtyPal() {
    return qtyPal;
  }

  public String getLgort() {
    return lgort;
  }

  public String getPal() {
    return pal;
  }

  public String getCell() {
    return cell;
  }

  public String getVbeln() {
    return vbeln;
  }

  public String getEbelns() {
    return ebelns;
  }

  public HashMap<String, ProdPriemkaTovData> getPQ() {
    return pq;
  }

  public ZTS_PRT_QTY_S[] getScanData() {
    int n = scanData.size();
    ZTS_PRT_QTY_S[] ret = new ZTS_PRT_QTY_S[n];
    ProdPriemkaScanData sd;
    ZTS_PRT_QTY_S s;
    for (int i = 0; i < n; i++) {
      sd = scanData.get(i);
      s = new ZTS_PRT_QTY_S();
      s.CHARG = ProcessTask.fillZeros(sd.charg, 10);
      s.QTY = sd.qty;
      ret[i] = s;
    }
    return ret;
  }

  public ProdPriemkaTovData getPrtQty(String charg) {
    ProdPriemkaTovData ret = pq.get(charg);
    if (ret == null) {
      ret = new ProdPriemkaTovData(BigDecimal.ZERO, BigDecimal.ZERO, "");
    }
    return ret;
  }

  public ProdPriemkaTovData getPrtQtyNull(String charg) {
    return pq.get(charg);
  }

  public void callSetLgort(String lgort, boolean addrSkl, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
    }
    if (addrSkl != this.addrSkl) {
      dr.setB(FieldType.ADDR_SKL, addrSkl);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(pal, this.pal)) {
      dr.setS(FieldType.PAL, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetEbelns(String ebelns, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(ebelns, this.ebelns)) {
      dr.setS(FieldType.EBELNS, ebelns);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCellVbeln(String cell, String vbeln, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(cell, this.cell)) {
      dr.setS(FieldType.CELL, cell);
    }
    if (!strEq(vbeln, this.vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    dr.setI(FieldType.LOG, LogType.SET_CELL_VBELN.ordinal());
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVbeln(String vbeln, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(vbeln, this.vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
      dr.setI(FieldType.LOG, LogType.SET_VBELN.ordinal());
    }
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String charg, BigDecimal qty, BigDecimal qtyMax, String abc, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    dr.setS(FieldType.ABC, abc);
    if (qtyMax != null) {
      dr.setN(FieldType.QTY_MAX, qtyMax);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTovData(TaskState state, TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!scanData.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(BigDecimal qtyMax, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    dr.setN(FieldType.QTY_MAX, qtyMax);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdAddTov(DataRecord dr) {
    String abc = dr.getValStr(FieldType.ABC);
    if (abc == null) {
      abc = "";
    }

    ProdPriemkaScanData sd = new ProdPriemkaScanData(dr.getValStr(FieldType.CHARG),
            (BigDecimal) dr.getVal(FieldType.QTY), abc);
    BigDecimal qtyMax = (BigDecimal) dr.getVal(FieldType.QTY_MAX);

    if (sd.qty.signum() > 0) {
      scanData.add(sd);
      qtyPal = qtyPal.add(sd.qty);
      ProdPriemkaTovData r = pq.get(sd.charg);
      if (r == null) {
        r = new ProdPriemkaTovData(BigDecimal.ZERO, qtyMax, abc);
        pq.put(sd.charg, r);
      }
      r.qty = r.qty.add(sd.qty);
      if (qtyMax != null) {
        r.qtyMax = qtyMax;
      }
    }
  }

  private void hdDelLast(DataRecord dr) {
    BigDecimal qtyMax = (BigDecimal) dr.getVal(FieldType.QTY_MAX);
    int i = scanData.size() - 1;
    if (i < 0) {
      return;
    }
    ProdPriemkaScanData sd = scanData.get(i);
    scanData.remove(i);
    qtyPal = qtyPal.subtract(sd.qty);
    ProdPriemkaTovData r = pq.get(sd.charg);
    if (r != null) {
      r.qty = r.qty.subtract(sd.qty);
      r.qtyMax = qtyMax;
      if (r.qty.signum() <= 0) {
        pq.remove(sd.charg);
      }
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = (String) dr.getVal(FieldType.LGORT);
        }

        if (dr.haveVal(FieldType.CELL)) {
          cell = dr.getValStr(FieldType.CELL);
        }

        if (dr.haveVal(FieldType.PAL)) {
          pal = dr.getValStr(FieldType.PAL);
        }

        if (dr.haveVal(FieldType.VBELN)) {
          vbeln = dr.getValStr(FieldType.VBELN);
        }

        if (dr.haveVal(FieldType.ADDR_SKL)) {
          addrSkl = (Boolean) dr.getVal(FieldType.ADDR_SKL);
        }

        if (dr.haveVal(FieldType.EBELNS)) {
          ebelns = (String) dr.getVal(FieldType.EBELNS);
        }

        if (dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY)) {
          hdAddTov(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.DEL_LAST) && (scanData.size() > 0) && dr.haveVal(FieldType.QTY_MAX)) {
          hdDelLast(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          scanData.clear();
          pq.clear();
          qtyPal = BigDecimal.ZERO;
          ebelns = "-";
        }
        break;
    }
  }
}
