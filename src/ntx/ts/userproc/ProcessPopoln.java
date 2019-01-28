package ntx.ts.userproc;

import java.util.Date;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_CELLS_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Пополнение зоны комплектации Состояния: START SEL_SKL SEL_CELL SEL_PAL
 * SEL_CELL2
 */
public class ProcessPopoln extends ProcessTask {

  private final PopolnData d = new PopolnData();

  public ProcessPopoln(long procId) throws Exception {
    super(ProcType.POPOLN, procId);
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
        return htmlWork("Пополнение", playSound, ctx);
    }
  }

  private String getPopTimeMessage() {
    // получение сообщения о том, сколько времени назад сделан отчет о пополнении

    Date dt = parseDateYYYYMMDD_HHMMSS(d.getDT());
    if (dt == null) {
      return "Отчета о пополнении нет";
    }

    Date n = new Date();

    if (n.before(dt)) {
      return "??? Ошибка в дате отчета";
    }

    long n1 = dt.getTime() / 60000;
    long n2 = n.getTime() / 60000;
    long nn = n2 - n1;

    long d1 = nn / (60 * 24);
    long d2 = (nn - d1 * 60 * 24) / 60;
    long d3 = nn - d1 * 60 * 24 - d2 * 60;

    String s = "";
    if ((d1 == 0) && (d2 == 0) && (d3 == 0)) {
      s = " 0м";
    } else {
      if (d1 > 0) {
        s = s + " " + d1 + "д";
      }
      if ((d2 > 0) || (d1 > 0)) {
        s = s + " " + d2 + "ч";
      }
      if ((d3 > 0) || (d2 > 0) || (d1 > 0)) {
        s = s + " " + d3 + "м";
      }
    }

    return "Отчет" + s + " назад";
  }

  private FileData setLgort(String lgort, TaskContext ctx) throws Exception {
    // установка склада (с проверкой наличия отчета о пополнении)

    Z_TS_POP1 f = new Z_TS_POP1();
    f.LGORT = lgort;

    f.execute();

    if (!f.isErr) {
      d.callSetLgort(lgort, f.LGTYP1, f.DT, f.LGNUM, TaskState.SEL_CELL, ctx);

      String s = "";
      if (!f.NEXT_CELL.isEmpty()) {
        s = "</b></div>\r\n<div style=\"color:green\"><b>Следующая ячейка: " + f.NEXT_CELL;
      }

      callSetMsg(getPopTimeMessage() + s, ctx);
      callTaskNameChange(ctx);
      return htmlGet(false, ctx);
    } else {
      callSetTaskState(TaskState.SEL_SKL, ctx);
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      return setLgort(lgorts[0], ctx);
    } else {
      callSetTaskState(TaskState.SEL_SKL, ctx);
      return htmlGet(false, ctx);
    }
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

      case SEL_CELL2:
        return handleScanCell2(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if ((getTaskState() == TaskState.SEL_SKL) && !menu.equals("fin")) {
      // обработка выбора склада
      callClearErrMsg(ctx);
      if ((menu.length() == 12) && menu.startsWith("sellgort")) {
        String lg = menu.substring(8);
        if (!rightsLgort(lg, ctx)) {
          callSetErr("Нет прав по складу " + lg, ctx);
          return htmlGet(true, ctx);
        } else {
          return setLgort(lg, ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
    } else if (menu.equals("cancel")) {
      callClearErrMsg(ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      callSetMsg("Пополнение из " + d.getCell1() + " отменено", ctx);
      callAddHist("! Пополнение из " + d.getCell1() + " отменено", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("show")) {
      callClearErrMsg(ctx);
      return handleMenuShow(ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuShow(TaskContext ctx) throws Exception {
    HtmlPage p = new HtmlPage();
    p.title = "Расхождения";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    Z_TS_POP2 f = new Z_TS_POP2();
    f.LGORT = d.getLgort();

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    if (f.IT.length == 0) {
      callSetMsg("Нет ячеек для пополнения", ctx);
      return htmlGet(false, ctx);
    }

    d.callSetDT(f.DT, ctx);

    // получение числа ячеек с адресатом и без
    int nn = f.IT.length;
    int n1 = 0;
    int n2 = 0;
    for (int i = 0; i < nn; i++) {
      if (f.IT[i].LGTYP.isEmpty()) {
        n1++;
      } else {
        n2++;
      }
    }

    p.addLine("<font color=blue>" + getPopTimeMessage() + "</font>");
    p.addNewLine();

    ZTS_CELLS_S wa;
    boolean is1st = true;
    if (n1 > 0) {
      p.addLine("<b>Список ячеек с ячейкой-получателем:</b>");
      p.addText("<div style=\"font-size:" + TSparams.fontSize3
              + "pt\">\r\n");

      for (int i = 0; i < nn; i++) {
        wa = f.IT[i];
        if (wa.LGTYP.isEmpty()) {
          if (!is1st) {
            p.addText(",\r\n");
          }
          p.addText(wa.LGPLA);
          is1st = false;
        }
      }
      p.addText("\r\n</div>\r\n");
    }

    if ((n1 > 0) && (n2 > 0)) {
      p.addText("<hr>\r\n");
    }

    is1st = true;
    if (n2 > 0) {
      p.addLine("<b>Список ячеек БЕЗ ячейки-получателя:</b>");
      p.addText("<div style=\"font-size:" + TSparams.fontSize3
              + "pt\">\r\n");

      for (int i = 0; i < nn; i++) {
        wa = f.IT[i];
        if (!wa.LGTYP.isEmpty()) {
          if (!is1st) {
            p.addText(",\r\n");
          }
          p.addText(wa.LGPLA);
          is1st = false;
        }
      }
      p.addText("\r\n</div>\r\n");
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_POP3 f = new Z_TS_POP3();
    f.LGPLA = scan.substring(1);
    f.LGORT = d.getLgort();

    f.execute();

    if (!f.isErr) {
      d.callSetCell1(f.LGPLA, f.LGPLA2, f.LGTYP1, f.DT, f.LGNUM, TaskState.SEL_PAL, ctx);
      String s = "Пополнение из " + f.LGPLA;
      if (f.LGPLA2.isEmpty()) {
        s = s + " (ячейка-получатель не указана)";
      } else {
        s = s + " (поместить в " + f.LGPLA2 + ")";
      }
      s = s + "<br>" + getPopTimeMessage();
      callSetMsg(s, ctx);
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

    Z_TS_POP4 f = new Z_TS_POP4();
    f.DT = d.getDT();
    f.LGPLA = d.getCell1();
    f.LGNUM = d.getLgnum();
    f.LGTYP = d.getLgtyp1();
    f.PAL = scan.substring(1);

    f.execute();

    if (!f.isErr) {
      d.callSetPal(f.PAL, TaskState.SEL_CELL2, ctx);
      String s = "Пополнение из " + f.LGPLA + " " + f.PAL;
      String cell2 = d.getCell2();
      if (cell2.isEmpty()) {
        s = s + " (ячейка-получатель не указана)";
      } else {
        s = s + " (поместить в " + cell2 + ")";
      }
      callSetMsg(s, ctx);
      s = "Источник: " + f.LGPLA + " " + f.PAL;
      if (cell2.isEmpty()) {
        s = s + " (->X)";
      } else {
        s = s + " (->" + cell2 + ")";
      }
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanCell2(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ячейку-получателя (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_POP5 f = new Z_TS_POP5();
    f.DT = d.getDT();
    f.LGPLA = scan.substring(1);
    f.LGPLA1 = d.getCell1();
    f.LGPLA2 = d.getCell2();
    f.LGNUM = d.getLgnum();
    f.LGTYP1 = d.getLgtyp1();
    f.PAL = d.getPal();
    f.LGORT = d.getLgort();
    f.TSD_USER = ctx.user.getUserSHK();

    f.execute();

    if (!f.isErr) {
      String s = "";
      if (!f.NEXT_CELL.isEmpty()) {
        s = "</b></div>\r\n<div style=\"color:green\"><b>Следующая ячейка: " + f.NEXT_CELL;
      }

      callSetMsg("Выполнено пополнение из " + f.LGPLA1 + " " + f.PAL + " в " + f.LGPLA + s, ctx);
      callAddHist("Выполнено: " + f.LGPLA1 + " " + f.PAL + " -> " + f.LGPLA, ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
    } else if (!f.isSapErr) {
      if (f.TR_ZAK_CREATED.isEmpty()) {
        callSetErrMsg(f.err, "Возможно, следует вернуть товар в "
                + d.getCell1() + " и отменить пополнение", ctx);
        callAddHist("ОШИБКА при пополнении: " + f.LGPLA1 + " " + f.PAL + " -> " + f.LGPLA, ctx);
      } else {
        callSetErrMsg(f.err, "Пополнение завершено, следует разобраться с ошибкой в ztsd20", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        callAddHist("Пополнение " + f.LGPLA1 + " " + f.PAL + " -> " + f.LGPLA
                + " завершено, следует разобраться с ошибкой в ztsd20 строка " + f.KEY1, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;

    switch (getTaskState()) {
      case SEL_CELL:
        definition = "cont:Продолжить;later:Отложить;show:Показать ячейки";
        break;

      default:
        definition = "cont:Назад;cancel:Отменить пополнение;later:Отложить";
        break;
    }

    if (getTaskState() == TaskState.SEL_CELL) {
      definition = definition + ";fin:Завершить";
    }

    if (RefInfo.haveInfo(ProcType.SKL_MOVE)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Пополнение", "Выберите действие",
            definition, null, null, null);

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
    if (!d.getLgort().isEmpty()) {
      return d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name;
    } else {
      return "";
    }
  }
}

class PopolnData extends ProcData {

  private String dt = ""; // дата-время последней генерации отчета о пополнении в формате САП
  private String cell1 = ""; // ячейка, из которой берем
  private String lgtyp1 = "";
  private String pal = ""; // паллета, с которой берем
  private String cell2 = ""; // ячейка, в которую помещаем
  private String lgort = ""; // склад
  private String lgnum = "";

  public String getDT() {
    return dt;
  }

  public String getCell1() {
    return cell1;
  }

  public String getLgtyp1() {
    return lgtyp1;
  }

  public String getPal() {
    return pal;
  }

  public String getCell2() {
    return cell2;
  }

  public String getLgort() {
    return lgort;
  }

  public String getLgnum() {
    return lgnum;
  }

  public void callSetLgort(String lgort, String lgtyp1, String dt, String lgnum,
          TaskState state, TaskContext ctx) throws Exception {

    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
    }
    if (!strEq(lgtyp1, this.lgtyp1)) {
      dr.setS(FieldType.LGTYP1, lgtyp1);
    }
    if (!strEq(dt, this.dt)) {
      dr.setS(FieldType.DT, dt);
    }
    if (!strEq(lgnum, this.lgnum)) {
      dr.setS(FieldType.LGNUM, lgnum);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetDT(String dt, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(dt, this.dt)) {
      dr.setS(FieldType.DT, dt);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell1(String cell1, String cell2, String lgtyp1,
          String dt, String lgnum, TaskState state, TaskContext ctx) throws Exception {

    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(cell1, this.cell1)) {
      dr.setS(FieldType.CELL, cell1);
    }
    if (!strEq(cell2, this.cell2)) {
      dr.setS(FieldType.CELL2, cell2);
    }
    if (!strEq(lgtyp1, this.lgtyp1)) {
      dr.setS(FieldType.LGTYP1, lgtyp1);
    }
    if (!strEq(dt, this.dt)) {
      dr.setS(FieldType.DT, dt);
    }
    if (!strEq(lgnum, this.lgnum)) {
      dr.setS(FieldType.LGNUM, lgnum);
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
          cell1 = (String) dr.getVal(FieldType.CELL);
        }
        if (dr.haveVal(FieldType.LGTYP1)) {
          lgtyp1 = (String) dr.getVal(FieldType.LGTYP1);
        }
        if (dr.haveVal(FieldType.PAL)) {
          pal = (String) dr.getVal(FieldType.PAL);
        }
        if (dr.haveVal(FieldType.CELL2)) {
          cell2 = (String) dr.getVal(FieldType.CELL2);
        }
        if (dr.haveVal(FieldType.DT)) {
          dt = (String) dr.getVal(FieldType.DT);
        }
        break;
    }
  }
}
