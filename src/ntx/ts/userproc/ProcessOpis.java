package ntx.ts.userproc;

import java.util.ArrayList;
import java.util.Date;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_SGM_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.*;
import ntx.ts.sysproc.*;

/**
 * Опись паллет при отгрузке (сканируются СГМ и привязываются к паллете)
 * Состояния: START SEL_SKL SGM SGM_PAL
 */
public class ProcessOpis extends ProcessTask {

  private final OpisData d = new OpisData();

  public ProcessOpis(long procId) throws Exception {
    super(ProcType.OPIS, procId);
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
        return htmlWork("Возврат от клиента", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      d.callSetLgort(lgorts[0], TaskState.SGM, ctx);
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
      case SGM:
        return handleScanSgm(scan, ctx);

      case SGM_PAL:
        return handleScanSgmPal(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData htmlMenu() throws Exception {
    String def;

    switch (getTaskState()) {
      case SGM:
        def = "cont:Назад;later:Отложить;fin:Завершить";
        break;

      case SGM_PAL:
        def = "cont:Назад;dellast:Отменить последнее сканирование;delall:Отменить всё по текущей паллете;later:Отложить";
        break;

      default:
        def = "cont:Назад;later:Отложить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.OPIS)) {
      def = def + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Опись отгрузки", "Выберите действие",
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
          d.callSetLgort(lg, TaskState.SGM, ctx);
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
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx); // Завершить
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    // Отменить всё по текущей паллете
    d.callClearSgms(TaskState.SGM, ctx);
    callSetMsg("Сканирование СГМ по текущей паллете отменено", ctx);
    callAddHist("Сканирование СГМ по текущей паллете отменено", ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    // Отменить последнее сканирование
    if (d.getNScan() == 0) {
      callSetErr("Нет последнего сканирования", ctx);
      return htmlGet(true, ctx);
    }

    callSetMsg("Отменено сканирование СГМ " + d.getLast(), ctx);
    callAddHist("Отменено " + d.getLast(), ctx);
    d.callDelLast(ctx);
    if (d.getNScan() == 0) {
      callSetTaskState(TaskState.SGM, ctx);
    } else {
      callSetTaskState(TaskState.SGM_PAL, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanSgm(String scan, TaskContext ctx) throws Exception {
    if (isScanSgm(scan)) {
      return handleScanSgmDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать СГМ (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (isScanSgm(scan)) {
      return handleScanSgmDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК СГМ или паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmDo(String scanSgm, TaskContext ctx) throws Exception {
    try {
      int sgm = getScanSgm(scanSgm);
      if (sgm <= 0) {
        callSetErr("Неверный номер СГМ: " + sgm + " (сканирование " + scanSgm + " не принято)", ctx);
        return htmlGet(true, ctx);
      }

      boolean done = d.callAddSgm(sgm, ctx);

      if (!done) {
        callSetErr("СГМ " + sgm + " уже сканировалась на этой паллете", ctx);
      } else {
        String s = "СГМ " + sgm + " (всего " + d.getNScan() + " кор)";
        callAddHist(s, ctx);
        callSetMsg(s, ctx);
        callSetTaskState(TaskState.SGM_PAL, ctx);
      }
    } catch (Exception e) {
      callSetErr(e.getMessage(), ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана
    int n = d.getNScan();
    if (n == 0) {
      callSetErr("СГМ не отсканирован, привязывать к паллете нечего", ctx);
      return htmlGet(true, ctx);
    }

    String pal = getScanPal(scan);

    // сохранение данных по паллете
    Z_TS_OPIS1 f = new Z_TS_OPIS1();
    f.LGORT = d.getLgort();
    f.USER_SHK = ctx.user.getUserSHK();
    f.LENUM = pal;
    f.IT = d.getSgmTab();

    f.execute();
    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    d.callClearSgms(TaskState.SGM, ctx);

    String addStr = "";
    if (!f.IS_NEW.equals("X")) {
      addStr = " (старая привязка заменена)";
    }

    callSetMsg("Сохранены данные по паллете " + pal + " (" + n + " кор)" + addStr, ctx);
    callAddHist("Сохр. паллета " + pal + " (" + n + " кор)" + addStr, ctx);

    return htmlGet(true, ctx);
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

class OpisData extends ProcData {

  private String lgort = ""; // склад
  private ArrayList<Integer> sgms = new ArrayList<Integer>(); // СГМ на текущей паллете

  public int getNScan() {
    return sgms.size();
  }

  public String getLgort() {
    return lgort;
  }

  public ZTS_SGM_S[] getSgmTab() {
    ZTS_SGM_S[] ret = new ZTS_SGM_S[sgms.size()];
    int i = 0;
    ZTS_SGM_S wa;
    for (Integer s : sgms) {
      wa = new ZTS_SGM_S();
      wa.SGM = s;
      ret[i] = wa;
      i++;
    }
    return ret;
  }

  public int getLast() {
    return sgms.isEmpty() ? 0 : sgms.get(sgms.size() - 1);
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

  public void callClearSgms(TaskState state, TaskContext ctx) throws Exception {
    // удаление данных на текущей паллете
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!sgms.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public boolean callAddSgm(int sgm, TaskContext ctx) throws Exception {
    int j;
    for (Integer i : sgms) {
      j = i;
      if (sgm == j) {
        return false;
      }
    }

    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setI(FieldType.SGM, sgm);
    dr.setI(FieldType.LOG, LogType.ADD_SGM.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
    return true;
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
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
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          sgms.clear();
        }
        if (dr.haveVal(FieldType.SGM)) {
          int sgm = (Integer) dr.getVal(FieldType.SGM);
          sgms.add(sgm);
        }
        if (dr.haveVal(FieldType.DEL_LAST)) {
          if (!sgms.isEmpty()) {
            sgms.remove(sgms.size() - 1);
          }
        }
        break;
    }
  }
}
