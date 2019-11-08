package ntx.ts.userproc;

import ntx.sap.fm.*;
import ntx.sap.refs.RefInfo;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import static ntx.ts.sysproc.ProcData.strEq;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Перемещение скомпл. товара
 *
 * Состояния: START, SGM_PAL, TO_PAL, SEL_ZONE
 */
public class ProcessComplMove extends ProcessTask {

  private final ComplMoveData d = new ComplMoveData();

  public ProcessComplMove(long procId) throws Exception {
    super(ProcType.COMPL_MOVE, procId);
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
      default:
        return htmlWork("Перемещение скомпл. товара", false, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    callSetTaskState(TaskState.SGM_PAL, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }

    switch (getTaskState()) {
      case SGM_PAL:
        return handleScanSgmPal(scan, ctx);

      case TO_PAL:
        return handleScanPalForSgm(scan, ctx);

      case SEL_ZONE:
        return handleScanZone(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPalForZone(scan, ctx);
    } else if (isScanSgm(scan)) {
      return handleScanSgm(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК перемещаемой скомплектованной паллеты или коробки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanPalForZone(String scan, TaskContext ctx) throws Exception {
    String pal = getScanPal(scan);

    Z_TS_CMOVE3 f = new Z_TS_CMOVE3();
    f.LENUM = pal;
    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s = "Перемещение паллеты " + pal + " в другую зону склада";
    d.callSetPal(pal, TaskState.SEL_ZONE, ctx);
    callSetMsg(s, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanZone(String scan, TaskContext ctx) throws Exception {
    if (!isScanZone(scan)) {
      callSetErr("Требуется отсканировать ШК зоны склада (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String zone = getScanZone(scan);

    Z_TS_CMOVE4 f = new Z_TS_CMOVE4();
    f.LENUM = d.getPal();
    f.PLACE = zone;
    f.USER_SHK = ctx.user.getUserSHK();
    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s1 = "Паллета " + d.getPal() + " помещена в зону \"" + f.PLACE_NAME + "\"";
    String s2 = "пал " + d.getPal() + " -> \"" + f.PLACE_NAME + "\"";

    callSetMsg2(s1, s2, ctx);
    d.callSetPal("", TaskState.SGM_PAL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanSgm(String scan, TaskContext ctx) throws Exception {
    int sgm = getScanSgm(scan);

    Z_TS_CMOVE1 f = new Z_TS_CMOVE1();
    f.SGM = sgm;
    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s = "Перемещение СГМ " + sgm + " на другую паллету";
    d.callSetSgm(sgm, TaskState.TO_PAL, ctx);
    callSetMsg(s, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanPalForSgm(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК паллеты, на которую помещен короб (СГМ) (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String pal = getScanPal(scan);

    Z_TS_CMOVE2 f = new Z_TS_CMOVE2();
    f.SGM = d.getSgm();
    f.LENUM = pal;
    f.USER_SHK = ctx.user.getUserSHK();
    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s1 = "СГМ " + d.getSgm() + " помещена на паллету " + pal;
    String s2 = "СГМ " + d.getSgm() + " -> пал " + pal;

    callSetMsg2(s1, s2, ctx);
    d.callSetSgm(0, TaskState.SGM_PAL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData htmlMenu() throws Exception {
    String definition;

    if (getTaskState() == TaskState.SGM_PAL) {
      definition = "cont:Назад;later:Отложить;fin:Завершить";
    } else {
      definition = "cont:Назад;cancel:Отменить операцию;later:Отложить";
    }

    if (RefInfo.haveInfo(ProcType.COMPL_MOVE)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Перемещение скомпл. товара", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("cancel")) {
      callClearErrMsg(ctx);
      d.callCancel(TaskState.SGM_PAL, ctx);
      callSetMsg("Операция отменена отменена", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    }

    return htmlGet(false, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }
}

class ComplMoveData extends ProcData {

  private String pal = ""; // паллета на переупаковку
  private int sgm; // перемещаемая СГМ (коробка)

  public String getPal() {
    return pal;
  }

  public int getSgm() {
    return sgm;
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

  public void callSetSgm(int sgm, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (sgm != this.sgm) {
      dr.setI(FieldType.SGM, sgm);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callCancel(TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!pal.isEmpty()) {
      dr.setS(FieldType.PAL, "");
    }
    if (sgm != 0) {
      dr.setI(FieldType.SGM, 0);
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
        if (dr.haveVal(FieldType.PAL)) {
          pal = dr.getValStr(FieldType.PAL);
        }
        if (dr.haveVal(FieldType.SGM)) {
          sgm = (Integer) dr.getVal(FieldType.SGM);
        }
        break;
    }
  }
}
