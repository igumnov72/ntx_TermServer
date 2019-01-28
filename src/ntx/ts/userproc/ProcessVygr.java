package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.Date;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.*;
import ntx.ts.sysproc.*;

/**
 * Выгрузка машин при приемке (информация вводится вручную) Состояния: START
 * SEL_SKL ENTER_M3_MACH ENTER_M3_GR READY STARTED
 */
public class ProcessVygr extends ProcessTask {

  private final VygrData d = new VygrData();

  public ProcessVygr(long procId) throws Exception {
    super(ProcType.VYGR, procId);
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
        return htmlWork("Выгрузка машины", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      d.callSetLgort(lgorts[0], TaskState.ENTER_M3_MACH, ctx);
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
      case ENTER_M3_MACH:
        return handleScanM3Mach(scan, ctx);

      case ENTER_M3_GR:
        return handleScanM3Gr(scan, ctx);

      case READY:
        return handleScanReady(scan, ctx);

      case STARTED:
        return handleScanStarted(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData htmlMenu() throws Exception {
    String def;

    switch (getTaskState()) {
//      case ENTER_M3_MACH:
//      case ENTER_M3_GR:
//      case READY:
      case STARTED:
        def = "cont:Назад;later:Отложить;fin:Завершить";
        break;

      default:
        def = "cont:Назад;later:Отложить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.OPIS)) {
      def = def + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Выгрузка машины", "Выберите действие",
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
          d.callSetLgort(lg, TaskState.ENTER_M3_MACH, ctx);
          callTaskNameChange(ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(false, ctx);
    } else if (menu.equals("fin")) {
      return handleFin(ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleScanM3Mach(String scan, TaskContext ctx) throws Exception {
    if (!isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    d.callSetM3Mach(qty, TaskState.ENTER_M3_GR, ctx);

    String s = "Кубатура машины: " + delDecZeros(qty.toString());
    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanM3Gr(String scan, TaskContext ctx) throws Exception {
    if (!isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    d.callSetM3Gr(qty, TaskState.READY, ctx);

    String s = "Кубатура груза: " + delDecZeros(qty.toString());
    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanReady(String scan, TaskContext ctx) throws Exception {
    if (!scan.equals("1")) {
      callSetErr("В момент начала выгрузки машины введите '1' и нажмите Enter (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    boolean isErr = saveData(ctx);

    if (!isErr) {
      d.callSetStarted(true, TaskState.STARTED, ctx);

      String s = "Выгрузка машины начата";
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanStarted(String scan, TaskContext ctx) throws Exception {
    callSetErr("Для индикации завершения выгрузки машины просто завершите задачу на терминале (ввод " + scan + " не принят)", ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleFin(TaskContext ctx) throws Exception {
    boolean isErr = saveData(ctx);

    if (isErr) {
      return htmlGet(true, ctx);
    } else {
      callTaskFinish(ctx); // Завершить
      return null;
    }
  }

  private boolean saveData(TaskContext ctx) throws Exception { // возвращает признак наличия ошибки
    Z_TS_VYGR1 f = new Z_TS_VYGR1();

    f.TASK_ID = String.valueOf(ctx.task.getProcId()).trim();
    f.LGORT = d.getLgort();
    f.M3_MACH = d.getM3Mach();
    f.M3_GR = d.getM3Gr();
    f.USER_CODE = ctx.user.getUserSHK();
    f.FINISHED = d.getStarted() ? "X" : " ";

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
    }

    return f.isErr;
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
    if (!d.getLgort().isEmpty()) {
      return d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name;
    } else {
      return null;
    }
  }
}

class VygrData extends ProcData {

  private String lgort = ""; // склад
  private boolean started = false; // признак того, что выгрузка начата
  private BigDecimal m3Mach = new BigDecimal(0); // кубатура машины
  private BigDecimal m3Gr = new BigDecimal(0); // кубатура груза

  public String getLgort() {
    return lgort;
  }

  public boolean getStarted() {
    return started;
  }

  public BigDecimal getM3Mach() {
    return m3Mach;
  }

  public BigDecimal getM3Gr() {
    return m3Gr;
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

  public void callSetStarted(boolean started, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (started != this.started) {
      dr.setB(FieldType.STARTED, started);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetM3Mach(BigDecimal m3Mach, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (m3Mach.compareTo(this.m3Mach) != 0) {
      dr.setN(FieldType.M3_MACH, m3Mach);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetM3Gr(BigDecimal m3Gr, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (m3Gr.compareTo(this.m3Gr) != 0) {
      dr.setN(FieldType.M3_GR, m3Gr);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
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
        if (dr.haveVal(FieldType.STARTED)) {
          started = (Boolean) dr.getVal(FieldType.STARTED);
        }
        if (dr.haveVal(FieldType.M3_MACH)) {
          m3Mach = (BigDecimal) dr.getVal(FieldType.M3_MACH);
        }
        if (dr.haveVal(FieldType.M3_GR)) {
          m3Gr = (BigDecimal) dr.getVal(FieldType.M3_GR);
        }
        break;
    }
  }
}
