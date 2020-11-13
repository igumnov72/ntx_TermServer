package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import ntx.sap.fm.Z_TS_DPDT1;
import ntx.sap.fm.Z_TS_DPDT2;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
import ntx.sap.refs.RefInfo;
import ntx.sap.refs.RefLgort;
import ntx.sap.refs.RefMat;
import ntx.sap.struct.ZTS_PRT_QTY_S;
import ntx.ts.html.HtmlPage;
import ntx.ts.html.HtmlPageMenu;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Отгрузка ДПДТ Состояния: START SEL_SKL TOV
 */
public class ProcessDpdt extends ProcessTask {

  private final DpdtData d = new DpdtData();

  public ProcessDpdt(long procId) throws Exception {
    super(ProcType.DPDT, procId);
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
        return htmlWork("Отгрузка ДПДТ", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      d.callSetLgort(lgorts[0], TaskState.TOV, ctx);
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

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    if (isScanTovMk(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovDo(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    //String charg = getScanCharg(scan);
    
    ScanChargQty scanInf; 
    scanInf = getScanChargQty(scan);
    if (!scanInf.err.isEmpty()) {
      callSetErr(scanInf.err + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
    
    String charg = scanInf.charg;
    RefChargStruct c = RefCharg.get(charg, null);
    if (c == null) {
      callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
    BigDecimal q = scanInf.qty;// getScanQty(scan);
    if (q.signum() <= 0) {
      callSetErr("Кол-во в штрих-коде должно быть больше нуля (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    DpdtTotData r = d.getPrtQtyNull(charg);
    if (r == null) {
      // партия еще не сканировалась, её нужно проверить
      Z_TS_DPDT1 f = new Z_TS_DPDT1();
      f.LGORT = d.getLgort();
      f.CHARG = fillZeros(charg, 10);

      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlGet(true, ctx);
      }
    }

    d.callAddTov(charg, q, ctx);
    r = d.getPrtQtyNull(charg);
    String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr)
            + ": " + delDecZeros(q.toString()) + " ед";
    callAddHist(s, ctx);
    s = s + " (по партии: " + delDecZeros(r.qty.toString()) + " ед, "
            + r.n + " скан; всего: " + delDecZeros(d.getQtyTot().toString()) + " ед, "
            + d.getNScan() + " скан)";
    callSetMsg(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;

    switch (getTaskState()) {
      case TOV:
        definition = "cont:Продолжить;later:Отложить;dellast:Отменить последнее сканирование;"
                + "delall:Отменить всё;showtov:Показать общее кол-во;fin:Завершить";
        break;

      default:
        definition = "cont:Назад;later:Отложить;fin:Завершить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.DPDT)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Отгрузка ДПДТ", "Выберите действие",
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
          d.callSetLgort(lg, TaskState.TOV, ctx);
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
      callClearErrMsg(ctx);
      return handleMenuFin(ctx);
    } else if (menu.equals("showtov")) {
      return htmlShowTov(ctx);
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delall")) {
      callClearErrMsg(ctx);
      return htmlCnfDelAll(); // удалить всё несохраненное (запрос подтверждения)
    } else if (menu.equals("do_cnf_delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuFin(TaskContext ctx) throws Exception {
    if (d.getNScan() > 0) {
      Z_TS_DPDT2 f = new Z_TS_DPDT2();
      f.LGORT = d.getLgort();
      f.USER_SHK = ctx.user.getUserSHK();
      f.IT_DONE = d.getScanData();

      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
      }
    }

    if (getLastErr() == null) {
      callTaskFinish(ctx);
      return null;
    } else {
      return htmlGet(true, ctx);
    }
  }

  private FileData htmlShowTov(TaskContext ctx) throws Exception {
    HashMap<String, DpdtTotData> pq = d.getPQ();

    int n = pq.size();

    if (n == 0) {
      callClearErr(ctx);
      callSetMsg("Ничего не отсканировано", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Общее кол-во";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Отсканированный товар:</b>");
    p.addNewLine();
    DpdtTotData dt;
    RefChargStruct c;
    String charg;
    for (Entry<String, DpdtTotData> i : pq.entrySet()) {
      dt = i.getValue();
      charg = i.getKey();
      c = RefCharg.getNoNull(charg);
      p.addLine("<b><font color=blue>" + delDecZeros(dt.qty.toString()) + " ед, "
              + dt.n + " скан:</font> "
              + c.matnr + "/" + charg + "</b> " + RefMat.getFullName(c.matnr));
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    DpdtScanData sd = d.getLastScan();
    if (sd == null) {
      callSetErr("Нет отсканированных позиций (для отмены)", ctx);
      return htmlGet(true, ctx);
    }

    d.callDelLast(ctx);
    String matnr = RefCharg.getNoNull(sd.charg).matnr;
    DpdtTotData r = d.getPrtQty(sd.charg);
    String s = "Отменено: " + delDecZeros(sd.qty.toString())
            + " ед: " + matnr + "/" + sd.charg + " "
            + RefMat.getFullName(matnr);
    callAddHist(s, ctx);
    s = s + " (по партии: " + delDecZeros(r.qty.toString()) + " ед, "
            + r.n + " скан; всего: " + delDecZeros(d.getQtyTot().toString()) + " ед, "
            + d.getNScan() + " скан)";
    callSetMsg(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData htmlCnfDelAll() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Отгрузка ДПДТ",
            "Удалить все отсканированные данные?",
            "no:Нет;do_cnf_delall:Да", "no", null, null, false);
    return p.getPage();
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    d.callClearTovData(ctx);
    callSetMsg("Все отсканированные данные удалены", ctx);
    callAddHist("Все отсканированные данные удалены", ctx);
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

class DpdtScanData {  // отсканированные позиции

  public String charg;
  public BigDecimal qty;

  public DpdtScanData(String charg, BigDecimal qty) {
    this.charg = charg;
    this.qty = qty;
  }
}

class DpdtTotData {  // данные сканирования товара по партиям

  public int n; // число сканирований
  public BigDecimal qty;

  public DpdtTotData(int n, BigDecimal qty) {
    this.n = n;
    this.qty = qty;
  }
}

class DpdtData extends ProcData {

  private String lgort = ""; // склад
  private final ArrayList<DpdtScanData> scanData
          = new ArrayList<DpdtScanData>(); // отсканированный товар (партия, кол-во)
  private final HashMap<String, DpdtTotData> pq
          = new HashMap<String, DpdtTotData>(); // кол-во и число сканирований по партиям
  private BigDecimal qtyTot = new BigDecimal(0);

  public int getNScan() {
    return scanData.size();
  }

  public BigDecimal getQtyTot() {
    return qtyTot;
  }

  public String getLgort() {
    return lgort;
  }

  public HashMap<String, DpdtTotData> getPQ() {
    return pq;
  }

  public DpdtScanData getLastScan() {
    int n = scanData.size();
    if (n == 0) {
      return null;
    } else {
      return scanData.get(n - 1);
    }
  }

  public ZTS_PRT_QTY_S[] getScanData() {
    int n = scanData.size();
    ZTS_PRT_QTY_S[] ret = new ZTS_PRT_QTY_S[n];
    DpdtScanData sd;
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

  public DpdtTotData getPrtQty(String charg) {
    DpdtTotData ret = pq.get(charg);
    if (ret == null) {
      ret = new DpdtTotData(0, BigDecimal.ZERO);
    }
    return ret;
  }

  public DpdtTotData getPrtQtyNull(String charg) {
    return pq.get(charg);
  }

  public void callSetLgort(String lgort, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTovData(TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!scanData.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdAddTov(DataRecord dr) {
    DpdtScanData sd = new DpdtScanData(dr.getValStr(FieldType.CHARG),
            (BigDecimal) dr.getVal(FieldType.QTY));

    if (sd.qty.signum() > 0) {
      scanData.add(sd);
      qtyTot = qtyTot.add(sd.qty);
      DpdtTotData r = pq.get(sd.charg);
      if (r == null) {
        r = new DpdtTotData(1, sd.qty);
        pq.put(sd.charg, r);
      } else {
        r.qty = r.qty.add(sd.qty);
        r.n++;
      }
    }
  }

  private void hdDelLast(DataRecord dr) {
    int i = scanData.size() - 1;
    if (i < 0) {
      return;
    }
    DpdtScanData sd = scanData.get(i);
    scanData.remove(i);
    qtyTot = qtyTot.subtract(sd.qty);
    DpdtTotData r = pq.get(sd.charg);
    if (r != null) {
      r.qty = r.qty.subtract(sd.qty);
      r.n--;
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

        if (dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY)) {
          hdAddTov(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.DEL_LAST) && (scanData.size() > 0)) {
          hdDelLast(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          scanData.clear();
          pq.clear();
          qtyTot = new BigDecimal(0);
        }
        break;
    }
  }
}
