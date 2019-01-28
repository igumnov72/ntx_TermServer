package ntx.ts.sysproc;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import ntx.sap.fm.Z_TS_EVENT_SME;
import ntx.sap.fm.Z_TS_EVENT_WOK;
import ntx.sap.fm.Z_TS_EVENT_WP;
import ntx.sap.fm.Z_TS_WP_STATE;
import ntx.sap.refs.RefDolgh;
import ntx.sap.refs.RefUser;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;

/**
 * Рабочее место (производственная линия)
 * Состояния:
 * START (только выбор WP)
 * EVENT
 * DOLGH
 * SOVM_DOLGH
 */
public class ProcessWP extends ProcessUtil {

  private final WPdata d = new WPdata();
  TermQuery tqLocal = null;

  @Override
  public String getAddTaskName(ProcessContext ctx) throws Exception {
    // дополнительное описание задачи
    return d.getWpName() + " / " + d.getCehName();
  }

  private FileData handleScanWP(String scan, TaskContext1 ctx) throws Exception {
    if (!isScanWP(scan)) {
      callSetErr("Требуется отсканировать ШК рабочего места (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    int wpNum = getScanWP(scan);

    if (wpNum != d.getWpNum()) {
      callSetErr("Рабочее место " + d.getWpNum() + " получило сканирование " + wpNum + " (сообщите разработчику)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_EVENT_WP f = new Z_TS_EVENT_WP();
    f.WP_ID = wpNum;
    f.WP_TYP = tqLocal.isUDP ? " " : "X";
    f.LAST_IP = tqLocal.fromAddr;
    f.LAST_DT_ID = Long.toString(tqLocal.terminal);
    f.LAST_DT_STR = df.format(new Date(tqLocal.terminal));

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s = "Планшет привязан к рабочему месту " + d.getWpName() + " (" + d.getWpNum() + ")";
    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    callSetTaskState(TaskState.EVENT, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanEvent(String scan, TaskContext1 ctx) throws Exception {
    if (isScanWP(scan)) {
      return handleScanWP(scan, ctx);
    } else if (isScanWPsme(scan)) {
      return handleScanEventSme(scan, ctx);
    } else if (isScanUser(scan)) {
      return handleScanEventUser(scan, ctx);
    } else {
      callSetErr("Сканирование " + scan + " не ожидается в этом состоянии", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanEventUser(String scan, TaskContext1 ctx) throws Exception {
    // тип ШК уже проверен

    if (!d.getIsSmena()) {
      callSetErr("Смена еще не начата (отсканируйте ШК начала смены)", ctx);
      return htmlGet(true, ctx);
    }

    String user = scan.substring(0, 10);
    String uName = RefUser.get(user).name;

    if (uName == null) {
      callSetErr("Сотрудника с кодом " + user + " не существует", ctx);
      return htmlGet(true, ctx);
    }

    String s = "Начало ввода данных по сотруднику " + uName + " (" + user + ")";

    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    d.callSetUser(user, TaskState.DOLGH, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanEventSme(String scan, TaskContext1 ctx) throws Exception {
    // тип ШК уже проверен

    boolean isSmena = getScanWPsme(scan);
    if (isSmena == d.getIsSmena()) {
      if (isSmena) {
        callSetMsg("Смена и так начата", ctx);
      } else {
        callSetMsg("Смена и так завершена", ctx);
      }
      return htmlGet(true, ctx);
    }

    Z_TS_EVENT_SME f = new Z_TS_EVENT_SME();
    f.WP_ID = d.getWpNum();
    f.EV_SME = isSmena ? "X" : " ";

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s = null;
    if (isSmena) {
      s = "Смена начата";
    } else {
      s = "Смена завершена";
    }

    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    d.callSetSme(isSmena, ctx);
    callSetTaskState(TaskState.EVENT, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanDolgh(String scan, TaskContext1 ctx) throws Exception {
    if (isScanWPdolgh(scan)) {
      return handleScanDolghDo(scan, ctx);
    } else if (isScanUser(scan)) {
      return handleScanUserFinish(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК должности или сотрудника (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanUserFinish(String scan, TaskContext1 ctx) throws Exception {
    // тип ШК уже проверен

    String user = scan.substring(0, 10);

    if (!strEq(user, d.getLastUser())) {
      callSetErr("Отсканированный код сотрудника " + user + " не совпадает с исходным " + d.getLastUser(), ctx);
      return htmlGet(true, ctx);
    }

    String uName = RefUser.get(user).name;
    String s = "";

    // сохраняем предыдущие данные (если есть)
    if (d.getHaveUserData()) {
      int lastDol = d.getLastDolgh();
      s = "Сохранено: " + RefDolgh.get(lastDol).name + " - 100%";
      d.callAddDolgh(lastDol, 100, null, ctx);
      callAddHist(s, ctx);
      s = s + "<br>";
    }

    // данные о занятости
    Map<Integer, Integer> tab = d.getDolghTab();
    if (tab.isEmpty()) {
      callSetMsg("Данные занятости не введены, сохранять нечего", ctx);
      callSetTaskState(TaskState.EVENT, ctx);
      return htmlGet(true, ctx);
    }

    // сохраняем всю таблицу в САПе
    Z_TS_EVENT_WOK f = new Z_TS_EVENT_WOK();
    f.WP_ID = d.getWpNum();
    f.SOTR = user;
    f.IT_WOK_create(tab.size());
    int i = 0;
    for (Entry<Integer, Integer> dol : tab.entrySet()) {
      f.IT_WOK[i].DOLGH_ID = dol.getKey();
      f.IT_WOK[i].SOVM = dol.getValue();
      i++;
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s2 = "Сохранены данные по сотруднику " + uName;
    callAddHist(s2, ctx);
    s = s + s2;
    callSetMsg(s, ctx);

    d.callClearDolghTab(TaskState.EVENT, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanDolghDo(String scan, TaskContext1 ctx) throws Exception {
    // тип ШК уже проверен

    int dolgh = getScanWPdolgh(scan);
    String dName = RefDolgh.get(dolgh).name;

    if (dName == null) {
      callSetErr("Должности с кодом " + dolgh + " не существует", ctx);
      return htmlGet(true, ctx);
    }

    // проверяем что эта должность еще не сканировалась
    if (d.haveDolghInTab(dolgh)) {
      callSetErr("Должность с кодом " + dolgh + " уже сканировалась", ctx);
      return htmlGet(true, ctx);
    }

    String s = "";

    // сохраняем предыдущие данные (если есть)
    if (d.getHaveUserData()) {
      int lastDol = d.getLastDolgh();
      s = "Сохранено: " + RefDolgh.get(lastDol).name + " - 100%";
      d.callAddDolgh(lastDol, 100, null, ctx);
      callAddHist(s, ctx);
      s = s + "<br>";
    }

    // сохраняем сканирование должности
    s = s + "Выбрано: " + dName;
    callSetMsg(s, ctx);
    d.callSetLastDolgh(dolgh, TaskState.SOVM_DOLGH, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanSovmDolgh(String scan, TaskContext1 ctx) throws Exception {
    if (isScanWPsovm(scan)) {
      return handleScanSovmDo(scan, ctx);
    } else if (isScanWPdolgh(scan)) {
      return handleScanDolghDo(scan, ctx);
    } else if (isScanUser(scan)) {
      return handleScanUserFinish(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК должности или сотрудника (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSovmDo(String scan, TaskContext1 ctx) throws Exception {
    // тип ШК уже проверен

    int sovm = getScanWPsovm(scan);

    if ((sovm < 1) || (sovm > 100)) {
      callSetErr("Процент совмещения должен быть от 1% до 100% (отсканировано " + sovm + "%)", ctx);
      return htmlGet(true, ctx);
    }

    int lastDol = d.getLastDolgh();
    String dName = RefDolgh.get(lastDol).name;

    if (dName == null) {
      callSetErr("Должности с кодом " + lastDol + " не существует", ctx);
      return htmlGet(true, ctx);
    }

    // сохраняем
    String s = "Сохранено: " + RefDolgh.get(lastDol).name + " - " + sovm + "%";
    callAddHist(s, ctx);
    callSetMsg(s, ctx);
    d.callAddDolgh(lastDol, sovm, TaskState.DOLGH, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScan(String scan, TaskContext1 ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu(ctx);
    }

    switch (getTaskState()) {
      case START:
      case EVENT:
        return handleScanEvent(scan, ctx);

      case DOLGH:
        return handleScanDolgh(scan, ctx);

      case SOVM_DOLGH:
        return handleScanSovmDolgh(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData htmlMenu(TaskContext1 ctx) throws Exception {
    return null;
  }

  public FileData handleMenu(String menu, TaskContext1 ctx) throws Exception {
    if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    }

    return null;
  }

  private FileData htmlGet(boolean playSound, TaskContext1 ctx) throws Exception {
    // рабочая страница не зависит от состояния
    return htmlWork("Производство", playSound, ctx);
  }

  public ProcessWP(long procId) throws Exception {
    super(ProcType.WP, procId);
  }

  public static ProcessWP createWP(int wpNum, ProcessContext ctx) throws Exception {
    // создание процесса с указанным номером

    if (wpNum == 0) {
      throw new Exception("Ошибка в программе: createWP: не указан wpNum");
    }

    ProcessWP wp1 = ctx.track.getWPbyNum(wpNum);
    if (wp1 != null) {
      return wp1;
    }

    Z_TS_WP_STATE f = new Z_TS_WP_STATE();
    f.WP_ID = wpNum;
    f.execute();
    if (f.isErr) {
      throw new Exception(f.err + "(ошибка при обращении к САПу)");
    }

    DataRecord dr = new DataRecord();
    dr.setI(FieldType.WP_NUM, wpNum);
    dr.setS(FieldType.WP_NAME, f.WP_NAME);
    dr.setI(FieldType.CEH_ID, f.CEH_ID);
    dr.setS(FieldType.CEH_NAME, f.CEH_NAME);
    dr.setB(FieldType.IS_SMENA, f.EV_SME.equalsIgnoreCase("X"));
    dr.setB(FieldType.IS_PLANSHET, f.WP_TYP.equalsIgnoreCase("X"));
    dr.setL(FieldType.TERM, Long.parseLong(f.LAST_DT_ID));
    dr.setS(FieldType.IP, f.LAST_IP);

    ProcessWP ret = (ProcessWP) Track.saveProcessNew(ProcType.WP, dr, new ProcessContext(ctx.track));
    return ret;
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.TERM)) {
          if (d.getLastTerminal() != 0) {
            ctx.track.tabTermWP.put(d.getLastTerminal(), this);
          }
        }
        if (dr.haveVal(FieldType.WP_NUM)) {
          if (d.getWpNum() != 0) {
            ctx.track.tabWP.put(d.getWpNum(), this);
          }
        }
        break;
    }
  }

  public FileData handleQuery(TermQuery tq, ProcessContext ctxProc) throws Exception {
    // обработка инф с терминала

    if (tq.terminal == 0) {
      tq.setNewTerminal();
    }
    if (tq.terminal != d.getLastTerminal()) {
      ProcessWP wp2 = ctxProc.track.getWPbyTerm(tq.terminal);
      if ((wp2 != null) && (wp2 != this)) {
        wp2.callAssignTerminal(0, ctxProc);
      }
      callAssignTerminal(tq.terminal, ctxProc);
    }

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));

    TaskContext1 ctx = new TaskContext1(ctxProc, this);
    tqLocal = tq;

//    if (getTaskState() == TaskState.START) {
//      return init(ctx);
//    } else
    if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    return htmlGet(false, ctx);
  }

  public void callAssignTerminal(long lastTerminal, ProcessContext ctx) throws Exception {
    d.callAssignTerminal(lastTerminal, new TaskContext1(ctx, this));
  }

  public static boolean isScanUser(String scan) {
    // проверка ШК сотрудника (0100000123A)
    int n = scan.length();
    if ((n == 11) && scan.substring(10).equalsIgnoreCase("A") && isAllDigits(scan.substring(0, 10))) {
      return true;
    }
    return false;
  }
}

class WPdata extends ProcData {

  private int wpNum = 0;
  private String wpName = "";
  private int cehId = 0;
  private String cehName = "";
  private boolean isSmena = false; // принак начатой смены
  private boolean isPlanshet = false; // тип оборудования (true - планшет)
  private long lastTerminal = 0;
  private String lastIP = "";
  private boolean canFinish = true;
  private String lastUser = "";
  private boolean haveUserData = false;
  private int lastDolgh = 0;
  private Map<Integer, Integer> dolghTab = Collections.synchronizedMap(new HashMap<Integer, Integer>(5));

  public void callClearDolghTab(TaskState state, TaskContext1 ctx) throws Exception {
    // очистка таблицы занятости
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.SAVED);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public boolean haveDolghInTab(int dolgh) {
    return dolghTab.containsKey(dolgh);
  }

  public Map<Integer, Integer> getDolghTab() {
    return dolghTab;
  }

  public void callAddDolgh(int dolgh, int sovm, TaskState state, TaskContext1 ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setI(FieldType.DOLGH, dolgh);
    dr.setI(FieldType.SOVM, sovm);
    dr.setI(FieldType.LAST_DOLGH, 0);
    dr.setB(FieldType.HAVE_USER_DATA, false);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLastDolgh(int lastDolgh, TaskState state, TaskContext1 ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (lastDolgh != this.lastDolgh) {
      dr.setI(FieldType.LAST_DOLGH, lastDolgh);
    }
    if (!haveUserData) {
      dr.setB(FieldType.HAVE_USER_DATA, true);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    // обработка записи трака (в т.ч. в реальном времени)

    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.WP_NUM)) {
          if (wpNum != 0) {
            ctx.track.tabWP.remove(wpNum);
          }
          wpNum = (Integer) dr.getVal(FieldType.WP_NUM);
        }
        if (dr.haveVal(FieldType.WP_NAME)) {
          wpName = (String) dr.getVal(FieldType.WP_NAME);
        }
        if (dr.haveVal(FieldType.CEH_ID)) {
          cehId = (Integer) dr.getVal(FieldType.CEH_ID);
        }
        if (dr.haveVal(FieldType.CEH_NAME)) {
          cehName = (String) dr.getVal(FieldType.CEH_NAME);
        }
        if (dr.haveVal(FieldType.IS_SMENA)) {
          isSmena = (Boolean) dr.getVal(FieldType.IS_SMENA);
        }
        if (dr.haveVal(FieldType.IS_PLANSHET)) {
          isPlanshet = (Boolean) dr.getVal(FieldType.IS_PLANSHET);
        }
        if (dr.haveVal(FieldType.TERM)) {
          if (lastTerminal != 0) {
            ctx.track.tabTermWP.remove(lastTerminal);
          }
          lastTerminal = (Long) dr.getVal(FieldType.TERM);
        }
        if (dr.haveVal(FieldType.IP)) {
          lastIP = (String) dr.getVal(FieldType.IP);
        }
        if (dr.haveVal(FieldType.CAN_FINISH)) {
          canFinish = (Boolean) dr.getVal(FieldType.CAN_FINISH);
        }
        if (dr.haveVal(FieldType.DOLGH) && dr.haveVal(FieldType.SOVM)) {
          dolghTab.put((Integer) dr.getVal(FieldType.DOLGH), (Integer) dr.getVal(FieldType.SOVM));
        }
        if (dr.haveVal(FieldType.LAST_DOLGH)) {
          lastDolgh = (Integer) dr.getVal(FieldType.LAST_DOLGH);
        }
        if (dr.haveVal(FieldType.HAVE_USER_DATA)) {
          haveUserData = (Boolean) dr.getVal(FieldType.HAVE_USER_DATA);
        }
        if (dr.haveVal(FieldType.SAVED)) {
          dolghTab.clear();
        }
        if (dr.haveVal(FieldType.USER)) {
          lastUser = (String) dr.getVal(FieldType.USER);
        }
        break;

      case 2:
        if (lastTerminal != 0) {
          ctx.track.tabTermWP.remove(lastTerminal);
        }
        if (wpNum != 0) {
          ctx.track.tabWP.remove(wpNum);
        }
        break;
    }
  }

  public int getWpNum() {
    return wpNum;
  }

  public String getWpName() {
    return wpName;
  }

  public int getCehId() {
    return cehId;
  }

  public String getCehName() {
    return cehName;
  }

  public boolean getIsSmena() {
    return isSmena;
  }

  public boolean getIsPlanshet() {
    return isPlanshet;
  }

  public long getLastTerminal() {
    return lastTerminal;
  }

  public String getLastIP() {
    return lastIP;
  }

  public boolean getCanFinish() {
    return canFinish;
  }

  public String getLastUser() {
    return lastUser;
  }

  public boolean getHaveUserData() {
    return haveUserData;
  }

  public int getLastDolgh() {
    return lastDolgh;
  }

  public void callAssignTerminal(long lastTerminal, TaskContext1 ctx) throws Exception {
    // присвоение терминала процессу
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (lastTerminal != this.lastTerminal) {
      dr.setL(FieldType.TERM, lastTerminal);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetSme(boolean isSmena, TaskContext1 ctx) throws Exception {
    // присвоение терминала процессу
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (isSmena != this.isSmena) {
      dr.setB(FieldType.IS_SMENA, isSmena);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetUser(String user, TaskState state, TaskContext1 ctx) throws Exception {
    // присвоение терминала процессу
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(user, this.lastUser)) {
      dr.setS(FieldType.USER, user);
    }
    if (haveUserData) {
      dr.setB(FieldType.HAVE_USER_DATA, false);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }
}
