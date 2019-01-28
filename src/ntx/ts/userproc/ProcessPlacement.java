package ntx.ts.userproc;

import ntx.sap.fm.*;
import ntx.sap.refs.RefInfo;
import ntx.sap.struct.*;
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
 * Размещение паллет в процессе приемки Состояния: START, SEL_PAL, SEL_CELL,
 * CNF_CELL, CNF_MERGE, CNF_PM
 */
public class ProcessPlacement extends ProcessTask {

  private final PlacementData d = new PlacementData();

  public ProcessPlacement(long procId) throws Exception {
    super(ProcType.PLACEMENT, procId);
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
      case CNF_CELL:
        return htmlCnfPlc();
      case CNF_MERGE:
        return htmlCnfMerge();
      case CNF_PM:
        return htmlCnfPM();
      default:
        return htmlWork("Размещение", false, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    callSetTaskState(TaskState.SEL_PAL, ctx);
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

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_PLC1 f = new Z_TS_PLC1();
    f.PAL = scan.substring(1);
    f.execute();

    if (!f.isErr) {
      if (!rightsLgort(f.LGORT, ctx)) {
        callSetErr("Нет прав по складу " + f.LGORT, ctx);
      } else {
        d.callSetPalVbelnLgort(f.PAL, delZeros(f.VBELN), f.LGORT, ctx);
        String s1 = "";
        if (!f.LGPLA.isEmpty()) {
          s1 = "<br>\r\n(только в " + f.LGPLA + ")";
        }
        callSetMsg("Размещение паллеты " + d.getPal() + " на складе " + f.LGORT
                + s1 + "<br>\r\nВсего паллет к размещению по " + d.getVbeln() + ": " + f.NERAZM, ctx);
        d.callSetDoPM(false, false, ctx);
        if (f.ASK_CNF_PM.equals("X")) {
          callSetTaskState(TaskState.CNF_PM, ctx);
        } else {
          callSetTaskState(TaskState.SEL_CELL, ctx);
        }
      }
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    if (!isScanCell(scan)) {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanCellDo(scan.substring(1), ctx, false);
  }

  private FileData handleScanCellDo(String cell, TaskContext ctx, boolean haveMergeCnf) throws Exception {
    Z_TS_PLC2 f = new Z_TS_PLC2();
    f.PAL = d.getPal();
    f.CELL = cell;
    f.USER_CODE = ctx.user.getUserSHK();
    f.DO_CNF = TSparams.cnfPlc ? " " : "X";
    f.MERGE_CNF = haveMergeCnf ? "X" : " ";
    f.DO_PM = d.getDoPM() ? "X" : " ";
    f.DO_PM_MAIL = d.getDoPMmail() ? "X" : " ";
    f.execute();

    if (!f.isErr) {
      d.callSetCell(f.LGPLA, ctx);
      d.callSetTanums(f.TANUMS, ctx);
      if (TSparams.cnfPlc) {
        callSetTaskState(TaskState.CNF_CELL, ctx);
        d.callSetNerazm(f.NERAZM, ctx);
        return htmlCnfPlc();
      } else if (!haveMergeCnf && f.NEED_MERGE_CNF.equals("X")) {
        callSetTaskState(TaskState.CNF_MERGE, ctx);
        callSetMsg("В ячейке " + d.getCell() + " числится " + ProcessTask.delDecZeros(f.QTY0.toString()) + " ед товара", ctx);
        return htmlCnfMerge();
      } else {
        String s = "Паллета " + d.getPal() + " размещена в " + d.getCell()
                + "<br>\r\nОсталось паллет к размещению по " + d.getVbeln() + ": " + f.NERAZM;
        if (f.OLD_PAL.equals("X")) {
          s += "<br>\r\n(по САПу товар размещен на старую паллету)";
        }
        callSetMsg(s, ctx);
        callAddHist(d.getPal() + " -> " + d.getCell(), ctx);
        callSetTaskState(TaskState.SEL_PAL, ctx);
        return htmlGet(false, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlCnfPlc() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Размещение",
            "Паллета " + d.getPal() + " помещена в " + d.getCell() + "?",
            "no:Нет;yes:Да", "no", getLastErr(), getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlCnfMerge() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Размещение",
            "Переместить товар на размещаемую паллету?",
            "no:Нет;yes:Да", "no", getLastErr(), getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlCnfPM() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Размещение",
            "Создать документ поступления материала после размещения этой паллеты?",
            "no:Нет;yes:Да", "no", getLastErr(), getLastMsg(), false);
    return p.getPage();
  }

  private FileData htmlMenu() throws Exception {
    String definition;

    String nerazm = "";
    if (!d.getVbeln().isEmpty()) {
      nerazm = ";nerazm:Неразмещенные паллеты";
    }

    if (getTaskState() == TaskState.SEL_CELL) {
      definition = "cont:Назад;cancel:Отменить разм. паллеты" + nerazm + ";later:Отложить";
    } else {
      definition = "cont:Назад;later:Отложить" + nerazm;
    }

    if (getTaskState() == TaskState.SEL_PAL) {
      definition = definition + ";fin:Завершить";
    }

    if (RefInfo.haveInfo(ProcType.PLACEMENT)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Размещение", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (getTaskState() == TaskState.CNF_CELL) {
      // обработка подтверждения ячейки
      callClearErrMsg(ctx);
      return handleMenuCnf(menu, ctx);
    } else if (getTaskState() == TaskState.CNF_MERGE) {
      // обработка подтверждения объединения товара
      callClearErrMsg(ctx);
      return handleMenuMerge(menu, ctx);
    } else if (getTaskState() == TaskState.CNF_PM) {
      // обработка подтверждения создания ПМ
      callClearErrMsg(ctx);
      return handleMenuPM(menu, ctx);
    } else if (menu.equals("cancel")) {
      callClearErrMsg(ctx);
      callSetTaskState(TaskState.SEL_PAL, ctx);
      callSetMsg("Размещение паллеты " + d.getPal() + " отменено", ctx);
      callAddHist(d.getPal() + " X", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("nerazm")) {
      callClearErrMsg(ctx);
      return handleMenuNerazm(ctx);
    }

    return htmlGet(false, ctx);
  }

  private FileData handleMenuNerazm(TaskContext ctx) throws Exception {
    Z_TS_NERAZM_PAL f = new Z_TS_NERAZM_PAL();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.LGORT = d.getLgort();
    f.NO_AUTO_MAT = "X";

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }
    if (f.IT_NERAZM.length == 0) {
      callSetMsg("По поставке " + d.getVbeln() + " нет неразмещенных паллет", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Неразмещенные паллеты";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize;
    p.scrollToTop = true;
    p.addLine("Неразмещенные паллеты по поставке <b>" + d.getVbeln() + "<b>:");
    for (int i = 0; i < f.IT_NERAZM.length; i++) {
      if (f.IT_NERAZM[i].LENUM.length() == 20) {
        f.IT_NERAZM[i].LENUM = f.IT_NERAZM[i].LENUM.substring(10);
      }
      p.addLine("<b>" + f.IT_NERAZM[i].LENUM + "</b>");
    }
    p.addNewLine();
    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();
    return p.getPage();
  }

  private FileData handleMenuCnf(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("yes")) {
      Z_TS_PLC3 f = new Z_TS_PLC3();
      f.DO_STORNO = "";
      f.LGORT = d.getLgort();
      f.PAL = d.getPal();
      f.VBELN = fillZeros(d.getVbeln(), 10);
      f.LGPLA = d.getCell();
      f.USER_CODE = ctx.user.getUserSHK();
      f.DO_PM = d.getDoPM() ? "X" : " ";
      f.DO_PM_MAIL = d.getDoPMmail() ? "X" : " ";
      String[] t1 = d.getTanum1();
      String[] t2 = d.getTanum2();
      f.TANUMS_create(t1.length);
      for (int i = 0; i < t1.length; i++) {
        f.TANUMS[i].LGNUM = t1[i];
        f.TANUMS[i].TANUM = t2[i];
      }

      f.execute();

      if (!f.isErr) {
        callSetMsg("Паллета " + d.getPal() + " размещена в " + d.getCell()
                + "<br>\r\nОсталось паллет к размещению по "
                + d.getVbeln() + ": " + d.getNerazm(), ctx);
        callAddHist(d.getPal() + " -> " + d.getCell(), ctx);
        callSetTaskState(TaskState.SEL_PAL, ctx);
        return htmlGet(false, ctx);
      } else {
        callSetErr(f.err, ctx);
        callSetTaskState(TaskState.CNF_CELL, ctx);
      }

      return htmlCnfPlc();
    } else if (menu.equals("no")) {
      Z_TS_PLC3 f = new Z_TS_PLC3();
      f.DO_STORNO = "X";
      f.LGORT = d.getLgort();
      f.PAL = d.getPal();
      f.VBELN = fillZeros(d.getVbeln(), 10);
      f.LGPLA = d.getCell();
      f.USER_CODE = ctx.user.getUserSHK();
      f.DO_PM = d.getDoPM() ? "X" : " ";
      f.DO_PM_MAIL = d.getDoPMmail() ? "X" : " ";
      String[] t1 = d.getTanum1();
      String[] t2 = d.getTanum2();
      f.TANUMS_create(t1.length);
      for (int i = 0; i < t1.length; i++) {
        f.TANUMS[i].LGNUM = t1[i];
        f.TANUMS[i].TANUM = t2[i];
      }

      f.execute();

      if (!f.isErr) {
        callSetMsg("Размещение " + d.getPal() + " в " + d.getCell() + " отменено", ctx);
        callAddHist(d.getPal() + " -X " + d.getCell(), ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        return htmlGet(false, ctx);
      } else {
        callSetErr(f.err, ctx);
      }

      return htmlCnfPlc();
    } else {
      callSetErr("Ошибка программы: неизвестный ответ " + menu, ctx);
      return htmlCnfPlc();
    }
  }

  private FileData handleMenuPM(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("yes")) {
      d.callSetDoPM(true, false, ctx);
      callSetMsg("После размещения паллеты будет создан документ ПМ", ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("no")) {
      d.callSetDoPM(false, true, ctx);
      callSetMsg("Документ ПМ создаваться не будет", ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      return htmlGet(false, ctx);
    } else {
      callSetErr("Ошибка программы: неизвестный ответ " + menu, ctx);
      return htmlCnfPlc();
    }
  }

  private FileData handleMenuMerge(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("yes")) {
      return handleScanCellDo(d.getCell(), ctx, true);
    } else if (menu.equals("no")) {
      callSetMsg("Размещение " + d.getPal() + " в " + d.getCell() + " отменено", ctx);
      callAddHist(d.getPal() + " -X " + d.getCell(), ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      return htmlGet(false, ctx);
    } else {
      callSetErr("Ошибка программы: неизвестный ответ " + menu, ctx);
      return htmlCnfMerge();
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }
}

class PlacementData extends ProcData {

  private String pal = ""; // размещаемая паллета
  private String vbeln = ""; // поставка (или ПНП)
  private String lgort = ""; // склад
  private String cell = ""; // ячейка, в которую размещаем
  private boolean doPM = false; // создать ПМ после размещения в ячейку
  private boolean doPMmail = false; // отправить письмо о создании ПМ после размещения в ячейку
  private String[] tanum1 = new String[0];
  private String[] tanum2 = new String[0];
  private int nerazm = 0; // число неразмещенных паллет (сохраняется только если требуется подтверждение размещения)

  public int getNerazm() {
    return nerazm;
  }

  public String getPal() {
    return pal;
  }

  public String getVbeln() {
    return vbeln;
  }

  public String getLgort() {
    return lgort;
  }

  public String getCell() {
    return cell;
  }

  public String[] getTanum1() {
    return tanum1;
  }

  public String[] getTanum2() {
    return tanum2;
  }

  public boolean getDoPM() {
    return doPM;
  }

  public boolean getDoPMmail() {
    return doPMmail;
  }

  public void callSetPalVbelnLgort(String pal, String vbeln, String lgort, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.pal, pal)) {
      dr.setS(FieldType.PAL, pal);
    }
    if (!strEq(this.vbeln, vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
    }
    if (!strEq(this.lgort, lgort)) {
      dr.setS(FieldType.LGORT, lgort);
    }
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.SET_PAL_VBELN_LGORT.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetDoPM(boolean doPM, boolean doPMmail, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (doPM != this.doPM) {
      dr.setB(FieldType.DO_PM, doPM);
    }
    if (doPMmail != this.doPMmail) {
      dr.setB(FieldType.DO_PM_MAIL, doPM);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell(String cell, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.cell, cell)) {
      dr.setS(FieldType.CELL, cell);
      dr.setI(FieldType.LOG, LogType.SET_CELL.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetNerazm(int nerazm, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (this.nerazm != nerazm) {
      dr.setI(FieldType.NERAZM, nerazm);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetTanums(ZTS_TANUM_S[] tt, TaskContext ctx) throws Exception {
    // запоминание массива номеров трансп заказов

    if (tt.length == 0) {
      return;
    }

    String[] t1, t2;
    if (tt == null) {
      t1 = new String[0];
      t2 = new String[0];
    } else {
      t1 = new String[tt.length];
      t2 = new String[tt.length];
    }
    for (int i = 0; i < tt.length; i++) {
      t1[i] = tt[i].LGNUM;
      t2[i] = tt[i].TANUM;
    }

    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setSa(FieldType.LGNUMS, t1);
    dr.setSa(FieldType.TANUMS, t2);
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
        if (dr.haveVal(FieldType.VBELN)) {
          vbeln = dr.getValStr(FieldType.VBELN);
        }
        if (dr.haveVal(FieldType.CELL)) {
          cell = dr.getValStr(FieldType.CELL);
        }
        if (dr.haveVal(FieldType.LGNUMS)) {
          tanum1 = (String[]) dr.getVal(FieldType.LGNUMS);
        }
        if (dr.haveVal(FieldType.TANUMS)) {
          tanum2 = (String[]) dr.getVal(FieldType.TANUMS);
        }
        if (dr.haveVal(FieldType.NERAZM)) {
          nerazm = (Integer) dr.getVal(FieldType.NERAZM);
        }
        if (dr.haveVal(FieldType.DO_PM)) {
          doPM = (Boolean) dr.getVal(FieldType.DO_PM);
        }
        if (dr.haveVal(FieldType.DO_PM_MAIL)) {
          doPMmail = (Boolean) dr.getVal(FieldType.DO_PM_MAIL);
        }
        break;
    }
  }
}
