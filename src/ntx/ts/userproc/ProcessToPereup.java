package ntx.ts.userproc;

import ntx.sap.fm.*;
import ntx.sap.refs.RefInfo;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
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
 * Отправка в переупаковку Состояния: START, SEL_SKL, SEL_CELL, SEL_PAL
 */
public class ProcessToPereup extends ProcessTask {

  private final ToPereupData d = new ToPereupData();

  public ProcessToPereup(long procId) throws Exception {
    super(ProcType.PEREUP1, procId);
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
        return htmlWork("В переупаковку", false, ctx);
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
      return htmlMenu();
    }

    switch (getTaskState()) {
      case SEL_PAL:
        return handleScanPal(scan, ctx);

      case SEL_CELL:
        return handleScanCell(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String cell = scan.substring(1);

    Z_TS_PEREUP1 f = new Z_TS_PEREUP1();
    f.LGORT = d.getLgort();
    f.CELL = cell;
    f.execute();

    if (!f.isErr) {
      String s = "Выбрана ячейка " + cell;
      callSetMsg(s, ctx);
      d.callSetCell(cell, TaskState.SEL_PAL, ctx);
      return htmlGet(false, ctx);
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

    String pal = scan.substring(1);

    Z_TS_PEREUP2 f = new Z_TS_PEREUP2();
    f.LGORT = d.getLgort();
    f.CELL = d.getCell();
    f.PAL = pal;
    f.TSD_USER = ctx.user.getUserSHK();
    f.execute();

    if (!f.isErr) {
      String s = "Паллета " + pal + " из ячейки " + d.getCell() + " отправлена в переупаковку";
      callSetMsg(s, ctx);
      callAddHist(d.getCell() + ": " + pal, ctx);
      d.callSetPal(pal, TaskState.SEL_CELL, ctx);
    } else {
      callSetErr(f.err, ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlMenu() throws Exception {
    String definition;

    if (getTaskState() == TaskState.SEL_PAL) {
      definition = "cont:Назад;cancel:Отменить ячейку;later:Отложить";
    } else {
      definition = "cont:Назад;later:Отложить;fin:Завершить";
    }

    if (RefInfo.haveInfo(ProcType.PLACEMENT)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("В переупаковку", "Выберите действие",
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
      callSetMsg("Отправка из ячейки " + d.getCell() + " отменена", ctx);
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

class ToPereupData extends ProcData {

  private String lgort = ""; // склад
  private String cell = ""; // ячейка, из которой берем на переупаковку
  private String pal = ""; // паллета на переупаковку

  public String getPal() {
    return pal;
  }

  public String getLgort() {
    return lgort;
  }

  public String getCell() {
    return cell;
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

  public void callSetCell(String cell, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.cell, cell)) {
      dr.setS(FieldType.CELL, cell);
      dr.setI(FieldType.LOG, LogType.SET_CELL.ordinal());
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.pal, pal)) {
      dr.setS(FieldType.PAL, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.SET_PAL.ordinal());
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
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = dr.getValStr(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.CELL)) {
          cell = dr.getValStr(FieldType.CELL);
        }
        break;
    }
  }
}
