package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_VOZ1_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Выполнение возврата товара от клиента на склад Состояния: START SEL_SKL
 * TOV_JVOZ TOV TOV_PAL FIN_MSG CHECK_PAL
 */
public class ProcessVozvrat extends ProcessTask {

  private final VozvratData d = new VozvratData();

  public ProcessVozvrat(long procId) throws Exception {
    super(ProcType.VOZVRAT, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctxUser) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    TaskContext ctx = new TaskContext(ctxUser, this);

    if (getTaskState() == TaskState.START) {
      return init(ctx);
//    } else if ((getTaskState() == TaskState.FIN_MSG) && (menu != null) && !menu.equals("fin")) {
    } else if (getTaskState() == TaskState.FIN_MSG) {
      return handleFinMsg(tq.params == null ? null : tq.params.getParNull("htext1"), ctx);
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

      case FIN_MSG:
        return (new HtmlPageMessage(getLastErr(), getLastMsg(), "fin_msg", null)).getPage();

      default:
        return htmlWork("Возврат от клиента", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      d.callSetLgort(lgorts[0], TaskState.TOV_JVOZ, ctx);
      callTaskNameChange(ctx);
    } else {
      callSetTaskState(TaskState.SEL_SKL, ctx);
    }
    return htmlGet(false, ctx);
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }

    switch (getTaskState()) {
      case TOV_JVOZ:
        return handleScanTovJvoz(scan, ctx);

      case TOV:
        return handleScanTov(scan, ctx);

      case TOV_PAL:
        return handleScanTovPal(scan, ctx);

      case CHECK_PAL:
        return handleScanCheckPal(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData htmlMenu() throws Exception {
    String def;

    switch (getTaskState()) {
      case TOV_JVOZ:
        def = "cont:Назад;later:Отложить;fin:Завершить";
        break;

      case TOV:
        def = "cont:Назад;delpal:Удалить паллету;check_pal:Проверка паллет;later:Отложить;fin:Завершить";
        break;

      case TOV_PAL:
        def = "cont:Назад;dellast:Отменить последнее сканирование;delall:Отменить всё по текущей паллете;later:Отложить";
        break;

      case CHECK_PAL:
        def = "cont:Назад;exit_check_pal:Выход из проверки паллет;later:Отложить";
        break;

      case FIN_MSG:
        return null;

      default:
        def = "cont:Назад;later:Отложить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.VOZVRAT)) {
      def = def + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Возврат от клиента", "Выберите действие",
            def, null, null, null);

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
          d.callSetLgort(lg, TaskState.TOV_JVOZ, ctx);
          callTaskNameChange(ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(false, ctx);
    } else if (menu.equals("delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx); // Отменить всё по текущей паллете
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPal(ctx); // Удалить паллету
    } else if (menu.startsWith("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPalDo(menu.substring(6), ctx); // Удалить паллету
    } else if (menu.equals("fin")) {
      callClearErrMsg(ctx);
      return handleMenuFin(ctx); // Завершить
    } else if (menu.equals("exit_check_pal")) {
      callClearErrMsg(ctx);
      return handleExitCheckPal(ctx);
    } else if (menu.equals("check_pal")) {
      callClearErrMsg(ctx);
      return handleCheckPal(ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    // Отменить всё по текущей паллете
    d.callClearTov(ctx);
    callSetTaskState(TaskState.TOV, ctx);
    callSetMsg("Сканирование товара по текущей паллете отменено", ctx);
    callAddHist("Сканирование товара по текущей паллете отменено", ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    // Отменить последнее сканирование
    ArrayList<VozvratTovData> tov = d.getTovCur();

    if (tov.isEmpty()) {
      callSetErr("Нет последнего сканирования", ctx);
      return htmlGet(true, ctx);
    }

//    try {
    VozvratTovData td = tov.get(tov.size() - 1);
    String s = "Отменено: " + delDecZeros(td.qty.toString())
            + " ед: " + td.matnr + "/" + td.charg + " "
            + RefMat.getName(td.matnr);

    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    d.callDelLast(ctx);
    if (d.getTovCur().isEmpty()) {
      callSetTaskState(TaskState.TOV, ctx);
      d.callClearTov(ctx);
    } else {
      callSetTaskState(TaskState.TOV_PAL, ctx);
    }
//    } catch (Exception e) {
//      callSetErr(e.getMessage());
//    }
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelPal(TaskContext ctx) throws Exception {
    // Удалить паллету (отображение списка паллет для выбора)

    HashMap<String, VozvratPalData> pals = d.getPals();

    if (pals.isEmpty()) {
      callSetErr("Не просканировано ни одной паллеты", ctx);
      return htmlGet(true, ctx);
    }

    // сортируем список паллет
    ConcurrentSkipListMap<String, VozvratPalData> pp = new ConcurrentSkipListMap<String, VozvratPalData>();
    for (Entry<String, VozvratPalData> i : pals.entrySet()) {
      pp.put(i.getKey(), i.getValue());
    }

    String def = "cont:Назад";
    for (Entry<String, VozvratPalData> i : pp.entrySet()) {
      def = def + ";" + "delpal" + i.getKey() + ":" + i.getKey() + " (" + delDecZeros(i.getValue().qtyPal.toString()) + "ед)";
    }
    return (new HtmlPageMenu("Удаление паллеты", "Удаление раскладки по паллете:",
            def, null, null, null)).getPage();
  }

  private FileData handleMenuDelPalDo(String pal, TaskContext ctx) throws Exception {
    // Удалить паллету

    VozvratPalData p = d.getPals().get(pal);

    if (p == null) {
      callSetErr("Паллета " + pal + " не была просканирована или уже удалена", ctx);
      return htmlGet(true, ctx);
    }

    d.callDelPal(pal, TaskState.TOV, ctx);

    String s = "Удалена раскладка по паллете  " + pal
            + "\r\n<br>КОЛ-ВО скан всего: " + delDecZeros(d.getQtyTot().toString());
    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleMenuFin(TaskContext ctx) throws Exception {
    // Завершить

    BigDecimal qty = d.getQtyTot();
    if (qty.signum() != 1) {
      callSetMsg("Завершение возврата от клиента, товар не просканирован", ctx);
      callSetTaskState(TaskState.FIN_MSG, ctx);
      return htmlGet(false, ctx);
    }

    Z_TS_VOZ1 f = new Z_TS_VOZ1();
    f.LGORT = d.getLgort();
    f.TSD_USER = ctx.user.getUserSHK();
    f.ID = d.getJvoz();

    HashMap<String, VozvratPalData> pals = d.getPals();
    int nLines = 0;
    for (Entry<String, VozvratPalData> i : pals.entrySet()) {
      nLines += i.getValue().tov.size();
    }
    f.IT_TOV_create(nLines);
    int counter = 0;
    for (Entry<String, VozvratPalData> i : pals.entrySet()) {
      for (VozvratTovData j : i.getValue().tov) {
        f.IT_TOV[counter].LENUM = i.getKey();
        f.IT_TOV[counter].MATNR = fillZeros(j.matnr, 18);
        f.IT_TOV[counter].CHARG = fillZeros(j.charg, 10);
        f.IT_TOV[counter].QTY = j.qty;
        f.IT_TOV[counter].N_POS = j.nScan;
        counter++;
      }
    }

    f.execute();

    if (!f.isErr) {
      String s = "Сканирование возврата от клиента завершено ("
              + f.N_PAL + " паллет, " + delDecZeros(f.QTY_TOT.toString())
              + " ед товара); номер строки журнала ztsd26: " + f.ID;
      callSetMsg(s, ctx);
      callSetTaskState(TaskState.FIN_MSG, ctx);
      return htmlGet(false, ctx);
    } else {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleCheckPal(TaskContext ctx) throws Exception {
    callSetMsg("Включен режим проверки паллет", ctx);
    callSetTaskState(TaskState.CHECK_PAL, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleExitCheckPal(TaskContext ctx) throws Exception {
    callSetMsg("Проверка паллет завершена", ctx);
    callSetTaskState(TaskState.TOV, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleScanTovJvoz(String scan, TaskContext ctx) throws Exception {
    if (isScanJvoz(scan)) {
      return handleScanJvoz(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    if (isScanTovMk(scan)) {
      d.callClearTov(ctx);
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать товар (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTovDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanCheckPal(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК проверяемой паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String pal = getScanPal(scan);
    VozvratPalData p = d.getPals().get(pal);

    if (p == null) {
      callSetErr("Паллета " + pal + " не сканировалась", ctx);
    } else {
      callSetMsg("Паллета " + pal + " просканирована, общее кол-во: " + delDecZeros(p.qtyPal.toString()), ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanTovDo(String scanTov, TaskContext ctx) throws Exception {
    try {
      ScanChargQty scanInf; 
      scanInf = getScanChargQty(scanTov);
      if (!scanInf.err.isEmpty()) {
        callSetErr(scanInf.err + " (сканирование " + scanTov + " не принято)", ctx);
        return htmlGet(true, ctx);
      }

      String charg = scanInf.charg;//getScanCharg(scanTov);
      RefChargStruct c = RefCharg.get(charg);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scanTov + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      BigDecimal qty = scanInf.qty;//getScanQty(scanTov);
      d.callAddTov(c.matnr, charg, qty, 1, ctx);
      String s = delDecZeros(qty.toString()) + " ед: " + c.matnr + "/" + charg + " " + RefMat.getName(c.matnr);
      s = s + " (на паллете: " + d.getTovMcur().size() + " мат; "
              + delDecZeros(d.getQtyPalCur().toString()) + " ед; "
              + d.getNScan() + " скан; ВСЕГО: " + delDecZeros(d.getQtyTot().toString()) + " ед)";
      callAddHist(s, ctx);
      callSetMsg(s, ctx);
      callSetTaskState(TaskState.TOV_PAL, ctx);
    } catch (Exception e) {
      callSetErr(e.getMessage(), ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана
    ArrayList<VozvratTovData> tov = d.getTovCur();
    if (tov.isEmpty()) {
      callSetErr("Товар не отсканирован, нечего привязывать к паллете", ctx);
      return htmlGet(true, ctx);
    }

    String pal = getScanPal(scan);
    BigDecimal qty = d.getQtyPalCur();

    // проверка что такой номер паллеты еще не используется
    Z_TS_PROD2 f1 = new Z_TS_PROD2();
    f1.PAL = pal;
    f1.execute();
    if (f1.isErr) {
      callSetErr(f1.err, ctx);
      return htmlGet(true, ctx);
    }

    VozvratPalData p = d.getPals().get(pal);

    d.callSetPal(pal, TaskState.TOV, ctx);

    String s = "Товар привязан к паллете " + pal;
    if (p != null) {
      s = s + " (предыдущие данные по паллете удалены)";
    }

    s = s + "\r\n<br>КОЛ-ВО скан: " + delDecZeros(qty.toString());
    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanJvoz(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана
    int jVoz = getScanJvoz(scan);

    // проверка номера возврата и получение старых данных возврата
    Z_TS_VOZ2 f = new Z_TS_VOZ2();
    f.ID = jVoz;
    f.LGORT = d.getLgort();

    f.execute();
    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    d.callSetVoz(jVoz, f.IT_TOV, TaskState.TOV, ctx);

    String s = "Исправление возврата " + jVoz;
    s = s + "\r\n<br>Число паллет: " + d.getPals().size();
    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleFinMsg(String htext1, TaskContext ctx) throws Exception {
    if ((htext1 != null) && htext1.equals("fin_msg")) {
      callTaskFinish(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    int jVoz = d.getJvoz();
    if (jVoz == 0) {
      return getProcType().text + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
    } else {
      return getProcType().text + " " + d.getLgort() + " (" + jVoz + ") " + df2.format(new Date(getProcId()));
    }
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    String s1 = "", s2 = "";
    int jVoz = d.getJvoz();

    if (!d.getLgort().isEmpty()) {
      s1 = d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name;
    }

    if (jVoz != 0) {
      s2 = "воз " + jVoz;
    }

    if (s1.isEmpty()) {
      s1 = s2;
      s2 = "";
    }

    if (!s2.isEmpty()) {
      s1 = s1 + "; " + s2;
    }

    return s1.isEmpty() ? null : s1;
  }
}

class VozvratTovData {

  public String matnr;
  public String charg;
  public BigDecimal qty;
  public int nScan;

  public VozvratTovData(String matnr, String charg, BigDecimal qty, int nScan) {
    this.matnr = matnr;
    this.charg = charg;
    this.qty = qty;
    this.nScan = nScan;
  }
}

class VozvratPalData {

  public ArrayList<VozvratTovData> tov; // товар на паллете
  public HashMap<String, BigDecimal> tovM; // товар на паллете по материалам
  public BigDecimal qtyPal; // кол-во на паллете

  public VozvratPalData(ArrayList<VozvratTovData> tov, HashMap<String, BigDecimal> tovM, BigDecimal qtyPal) {
    this.tov = tov;
    this.tovM = tovM;
    this.qtyPal = qtyPal;
  }
}

class VozvratData extends ProcData {

  private String lgort = ""; // склад
  private int jVoz = 0; // номер возврата (если исправляется существующий)
  private ArrayList<VozvratTovData> tovCur = new ArrayList<VozvratTovData>(); // товар на текущей паллете
  private BigDecimal qtyPalCur = BigDecimal.ZERO; // кол-во на текущей паллете
  private BigDecimal qtyTot = BigDecimal.ZERO; // общее кол-во
  private HashMap<String, BigDecimal> tovMcur = new HashMap<String, BigDecimal>(); // товар на текущей паллете по материалам
  private HashMap<String, VozvratPalData> pals = new HashMap<String, VozvratPalData>(); // отсканированные паллеты

  public int getNScan() {
    int ret = 0;
    for (VozvratTovData d : tovCur) {
      ret += d.nScan;
    }
    return ret;
  }

  public int getJvoz() {
    return jVoz;
  }

  public BigDecimal getQtyPalCur() {
    return qtyPalCur;
  }

  public BigDecimal getQtyTot() {
    return qtyTot;
  }

  public String getLgort() {
    return lgort;
  }

  public ArrayList<VozvratTovData> getTovCur() {
    return tovCur;
  }

  public HashMap<String, VozvratPalData> getPals() {
    return pals;
  }

  public HashMap<String, BigDecimal> getTovMcur() {
    return tovMcur;
  }

  public void callSetPal(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (qtyPalCur.signum() == 1) {
      dr.setS(FieldType.PAL, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelPal(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.DEL_PAL, pal);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
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

  public void callClearTov(TaskContext ctx) throws Exception {
    // удаление данных о товаре на текущей паллете
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!tovCur.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, BigDecimal qty, int nScan, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    dr.setI(FieldType.N_SCAN, nScan);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVoz(int jVoz, ZTS_VOZ1_S[] it, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();

    dr.setI(FieldType.KEY1, jVoz);

    String[] t1 = new String[it.length];
    String[] t2 = new String[it.length];
    String[] t3 = new String[it.length];
    BigDecimal[] t4 = new BigDecimal[it.length];
    int[] t5 = new int[it.length];
    for (int i = 0; i < it.length; i++) {
      t1[i] = it[i].LENUM.substring(10);
      t2[i] = ProcessTask.delZeros(it[i].MATNR);
      t3[i] = ProcessTask.delZeros(it[i].CHARG);
      t4[i] = it[i].QTY;
      t5[i] = it[i].N_POS;
    }
    dr.setSa(FieldType.LENUMS, t1);
    dr.setSa(FieldType.MATNRS, t2);
    dr.setSa(FieldType.CHARGS, t3);
    dr.setNa(FieldType.QTYS, t4);
    dr.setIa(FieldType.POS_COUNTS, t5);

    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdJvoz(DataRecord dr) {
    jVoz = (Integer) dr.getVal(FieldType.KEY1);

    tovCur.clear();
    qtyPalCur = BigDecimal.ZERO;
    qtyTot = BigDecimal.ZERO;
    tovMcur.clear();
    pals.clear();

    String[] ll = (String[]) dr.getVal(FieldType.LENUMS);
    int n = ll.length;
    if (n == 0) {
      return;
    }

    String[] mm = (String[]) dr.getVal(FieldType.MATNRS);
    String[] cc = (String[]) dr.getVal(FieldType.CHARGS);
    BigDecimal[] qq = (BigDecimal[]) dr.getVal(FieldType.QTYS);
    int[] pp = (int[]) dr.getVal(FieldType.POS_COUNTS);

    String lastPal = ll[0];
    for (int i = 0; i < n; i++) {
      if (!lastPal.equals(ll[i])) {
        hdPal(lastPal);
        lastPal = ll[i];
      }

      hdTov(mm[i], cc[i], qq[i], pp[i]);
    }
    hdPal(lastPal);
  }

  private void hdTov(String matnr, String charg, BigDecimal nn, int nScan) {
    VozvratTovData td = new VozvratTovData(matnr, charg, nn, nScan);
    tovCur.add(td);
    qtyPalCur = qtyPalCur.add(nn);
    qtyTot = qtyTot.add(nn);

    BigDecimal nn0 = tovMcur.get(matnr);
    if (nn0 != null) {
      nn = nn.add(nn0);
    }
    tovMcur.put(matnr, nn);
  }

  private void hdPal(String pal1) {
    VozvratPalData pal2 = pals.get(pal1);
    if (pal2 != null) {
      qtyTot = qtyTot.subtract(pal2.qtyPal);
    }
    pals.put(pal1, new VozvratPalData(tovCur, tovMcur, qtyPalCur));
    tovCur = new ArrayList<VozvratTovData>();
    qtyPalCur = BigDecimal.ZERO;
    tovMcur = new HashMap<String, BigDecimal>();
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = (String) dr.getVal(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          tovCur.clear();
          tovMcur.clear();
          qtyTot = qtyTot.subtract(qtyPalCur);
          qtyPalCur = BigDecimal.ZERO;
        }
        if (dr.haveVal(FieldType.MATNR) && dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY)) {
          String matnr = dr.getValStr(FieldType.MATNR);
          String charg = dr.getValStr(FieldType.CHARG);
          BigDecimal nn = (BigDecimal) dr.getVal(FieldType.QTY);
          int nScan;
          if (dr.haveVal(FieldType.N_SCAN)) {
            nScan = (Integer) dr.getVal(FieldType.N_SCAN);
          } else {
            nScan = 1;
          }
          hdTov(matnr, charg, nn, nScan);
        }
        if (dr.haveVal(FieldType.DEL_LAST)) {
          if (tovCur.isEmpty()) {
            tovMcur.clear();
            qtyTot = qtyTot.subtract(qtyPalCur);
            qtyPalCur = BigDecimal.ZERO;
          } else {
            int i = tovCur.size() - 1;
            VozvratTovData td = tovCur.get(i);
            tovCur.remove(i);
            qtyPalCur = qtyPalCur.subtract(td.qty);
            qtyTot = qtyTot.subtract(td.qty);

            BigDecimal nn = td.qty.negate();
            BigDecimal nn0 = tovMcur.get(td.matnr);
            if (nn0 != null) {
              nn = nn.add(nn0);
            }
            if (nn.signum() == 0) {
              tovMcur.remove(td.matnr);
            } else {
              tovMcur.put(td.matnr, nn);
            }
          }
        }
        if (dr.haveVal(FieldType.PAL)) {
          String pal1 = dr.getValStr(FieldType.PAL);
          hdPal(pal1);
        }
        if (dr.haveVal(FieldType.DEL_PAL)) {
          String pal1 = dr.getValStr(FieldType.DEL_PAL);
          VozvratPalData pal2 = pals.get(pal1);
          if (pal2 != null) {
            pals.remove(pal1);
            qtyTot = qtyTot.subtract(pal2.qtyPal);
          }
        }
        if (dr.haveVal(FieldType.KEY1)) {
          hdJvoz(dr);
        }
        break;
    }
  }
}
