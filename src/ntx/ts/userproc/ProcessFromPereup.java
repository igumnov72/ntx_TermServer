package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.*;
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
import static ntx.ts.sysproc.ProcessUtil.delDecZeros;
import static ntx.ts.sysproc.ProcessUtil.fillZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanCharg;
import static ntx.ts.sysproc.ProcessUtil.getScanQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Отправка в переупаковку Состояния: START, SEL_PEREUP, TOV_PAL, SEL_CELL,
 * CNF_MERGE
 */
public class ProcessFromPereup extends ProcessTask {

  private final FromPereupData d = new FromPereupData();

  public ProcessFromPereup(long procId) throws Exception {
    super(ProcType.PEREUP2, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctxUser) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    TaskContext ctx = new TaskContext(ctxUser, this);

    if (getTaskState() == TaskState.START) {
      callSetTaskState(TaskState.SEL_PEREUP, ctx);
      return htmlGet(false, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), ctx);
    } else if (menu != null) {
      callClearErrMsg(ctx);
      return handleMenu(menu, ctx);
    }

    return htmlGet(false, ctx);
  }

  private FileData htmlGet(boolean playSound, TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case CNF_MERGE:
        return htmlCnfMerge();
      default:
        return htmlWork("Из переупаковки", playSound, ctx);
    }
  }

  private FileData htmlCnfMerge() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Из переупаковки",
            "Переместить товар на имеющуюся в ячейке паллету?",
            "no:Нет;yes:Да", "no", getLastErr(), getLastMsg(), false);
    return p.getPage();
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }

    switch (getTaskState()) {
      case SEL_PEREUP:
        return handleScanPereup(scan, ctx);

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

  private FileData handleScanPereup(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК исходной паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String pal = scan.substring(1);

    Z_TS_PEREUP21 f = new Z_TS_PEREUP21();
    f.PAL = pal;
    f.execute();

    if (!f.isErr) {
      String s = "Размещение товара с переупакованной паллеты " + pal + " (на склад " + f.LGORT2 + ")";
      callSetMsg(s, ctx);
      callAddHist(pal + " (-> " + f.LGORT2 + ")", ctx);
      d.callSetPal1(pal, TaskState.TOV_PAL, ctx);
      d.callSetNerazm(f.IT, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanTovPal(String scan, TaskContext ctx) throws Exception {
    if (isScanTov(scan)) {
      return handleScanTov(scan, ctx);
    } else if (isScanPal(scan)) {
      return handleScanPalDo(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать товар или размещаемую паллету (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    try {
      String charg = getScanCharg(scan);
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      return handleScanTovDo(c.matnr, charg, getScanQty(scan), ctx);
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = "ОШИБКА ! Сообщите разработчику";
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovDo(String matnr, String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    FromPereupScanData nr = d.getNerazm(matnr, charg);

    if (nr == null) {
      callSetErr("Этот материал/партия отсутствуют в этой переупаковке", ctx);
    } else if (nr.qty.compareTo(qty) < 0) {
      callSetErr("Этот материал/партия отсканирован в большем кол-ве, чем есть в этой переупаковке", ctx);
    } else {
      d.callAddTov(matnr, charg, qty, ctx);
      String s = delDecZeros(qty.toString()) + "ед " + matnr + " (" + charg + ") " + RefMat.getName(matnr);
      callAddHist(s, ctx);
      s += " (всего " + d.getQtyTotStr() + " ед)";
      callSetMsg(s, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanPalDo(String scan, TaskContext ctx) throws Exception {
    String pal = scan.substring(1);

    Z_TS_PEREUP22 f = new Z_TS_PEREUP22();
    f.PAL1 = d.getPal1();
    f.PAL2 = pal;

    int nn = d.getScanCount();
    FromPereupScanData sd;
    if (nn > 0) {
      f.IT_create(nn);
      for (int i = 0; i < nn; i++) {
        sd = d.getTov(i);
        f.IT[i].MATNR = fillZeros(sd.matnr, 18);
        f.IT[i].CHARG = fillZeros(sd.charg, 10);
        f.IT[i].QTY = sd.qty;
      }
    }

    f.execute();

    if (!f.isErr) {
      String s = "Паллета из переупаковки " + pal + ": " + d.getQtyTotStr() + " ед на склад " + f.LGORT2;
      callSetMsg(s, ctx);
      callAddHist(d.getQtyTotStr() + " ед -> " + pal + " скл " + f.LGORT2, ctx);
      d.callSetPal2(pal, TaskState.SEL_CELL, ctx);
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

    String cell = scan.substring(1);
    d.callSetCell(cell, null, ctx);

    return handleScanCellDo(false, ctx);
  }

  private FileData handleScanCellDo(boolean cnfDone, TaskContext ctx) throws Exception {
    Z_TS_PEREUP23 f = new Z_TS_PEREUP23();
    f.PAL1 = d.getPal1();
    f.PAL2 = d.getPal2();
    f.CELL = d.getCell();
    f.MERGE_CNF = cnfDone ? "X" : " ";
    f.TSD_USER = ctx.user.getUserSHK();

    int nn = d.getScanCount();
    FromPereupScanData sd;
    if (nn > 0) {
      f.IT_create(nn);
      for (int i = 0; i < nn; i++) {
        sd = d.getTov(i);
        f.IT[i].MATNR = fillZeros(sd.matnr, 18);
        f.IT[i].CHARG = fillZeros(sd.charg, 10);
        f.IT[i].QTY = sd.qty;
      }
    }

    f.execute();

    if (!f.isErr) {
      if (f.NEED_MERGE_CNF.equals("X") && !cnfDone) { // требуется подтверждение объединения товара
        callSetTaskState(TaskState.CNF_MERGE, ctx);
        callSetMsg("В ячейке " + f.CELL + " (склад " + f.LGORT2 + ") числится " + ProcessTask.delDecZeros(f.QTY0.toString()) + " ед товара", ctx);
      } else {
        callSetMsg("Паллета " + f.PAL2 + " (переупаковка " + f.PAL1 + ") размещена в " + f.CELL + " (склад " + f.LGORT2 + ")", ctx);
        callAddHist(f.PAL2 + " (" + f.PAL1 + ") -> " + f.CELL + " (" + f.LGORT2 + ")", ctx);
        callSetTaskState(TaskState.SEL_PEREUP, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlMenu() throws Exception {
    String definition;

    switch (getTaskState()) {
      case SEL_PEREUP:
        definition = "later:Отложить;fin:Завершить";
        break;
      case TOV_PAL:
        definition = "later:Отложить;del_last:Отменить последнее сканирование";
        if (d.getScanCount() > 0) {
          definition += ";del_all:Удалить весь товар";
        }
        break;
      case SEL_CELL:
        definition = "later:Отложить;del_last:Отменить последнее сканирование";
        break;
      default:
        definition = "later:Отложить";
        break;
    }

    definition = "cont:Назад;" + definition;

    if (RefInfo.haveInfo(ProcType.PLACEMENT)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Из переупаковки", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (getTaskState() == TaskState.CNF_MERGE) {
      // обработка подтверждения ячейки
      callClearErrMsg(ctx);
      return handleMenuCnfMerge(menu, ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("del_last")) {
      handleMenuDelLast(ctx);
    } else if (menu.equals("del_all")) {
      handleMenuDelAll(ctx);
    }

    return htmlGet(true, ctx);
  }

  private void handleMenuDelAll(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case TOV_PAL:
        int nn = d.getScanCount();
        if (nn > 0) { // отменяем сканирование товара
          FromPereupScanData t = d.getTov(nn - 1);
          String s = "Отменено " + d.getQtyTotStr() + " ед (весь товар)";
          d.callClearTov(null, ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else {
          callSetMsg("Товар не отсканирован, удалять нечего", ctx);
        }
        break;

      default:
        callSetErr("Неподходящее состояние (для отмены последнего сканирования): " + getTaskState().name(), ctx);
        break;
    }
  }

  private void handleMenuDelLast(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case TOV_PAL:
        int nn = d.getScanCount();
        if (nn > 0) { // отменяем сканирование товара
          FromPereupScanData t = d.getTov(nn - 1);
          String s = "Отменено " + delDecZeros(t.qty.toString()) + " ед мат " + t.matnr + " (" + t.charg + ")";
          d.callDelLast(ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else { // отменяем сканирование паллеты
          callSetTaskState(TaskState.SEL_PEREUP, ctx);
          callSetMsg("Размещение товара с переупакованной паллеты " + d.getPal1() + " отменено", ctx);
          callAddHist(d.getPal1() + " -X", ctx);
        }
        break;

      case SEL_CELL:
        callSetTaskState(TaskState.TOV_PAL, ctx);
        callSetMsg("Отменено сканирование новой паллеты (" + d.getPal2() + ")", ctx);
        callAddHist(d.getPal2() + " -X", ctx);
        break;

      default:
        callSetErr("Неподходящее состояние (для отмены последнего сканирования): " + getTaskState().name(), ctx);
        break;
    }
  }

  private FileData handleMenuCnfMerge(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("yes")) {
      return handleScanCellDo(true, ctx);
    } else if (menu.equals("no")) {
      callSetMsg("Размещение " + d.getPal2() + " в " + d.getCell() + " отменено", ctx);
      callAddHist(d.getPal2() + " -X " + d.getCell(), ctx);
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

class FromPereupScanData {  // отсканированные позиции

  public String matnr;
  public String charg;
  public BigDecimal qty;

  public FromPereupScanData(String matnr, String charg, BigDecimal qty) {
    this.matnr = matnr;
    this.charg = charg;
    this.qty = qty;
  }
}

class FromPereupData extends ProcData {

  private String pal1 = ""; // паллета на переупаковку
  private String pal2 = ""; // паллета с переупаковки
  private String cell = ""; // ячейка, куда помещается переупакованный товар
  private final ArrayList<FromPereupScanData> scanData
          = new ArrayList<FromPereupScanData>(); // отсканированный товар

  private FromPereupScanData[] nerazm = new FromPereupScanData[0]; // неразмещенный товар

  private void addNerazm(String matnr, String charg, BigDecimal qty) {
    for (FromPereupScanData nr : nerazm) {
      if ((nr.matnr.equals(matnr)) && (nr.charg.equals(charg))) {
        nr.qty = nr.qty.add(qty);
      }
    }
  }

  public FromPereupScanData getNerazm(String matnr, String charg) {
    for (FromPereupScanData nr : nerazm) {
      if ((nr.matnr.equals(matnr)) && (nr.charg.equals(charg))) {
        return nr;
      }
    }

    return null;
  }

  public BigDecimal getQtyTot() {
    int n = scanData.size();
    BigDecimal ret = BigDecimal.ZERO;
    for (int i = 0; i < n; i++) {
      ret = ret.add(scanData.get(i).qty);
    }
    return ret;
  }

  public String getQtyTotStr() {
    return delDecZeros(getQtyTot().toString());
  }

  public int getScanCount() {
    return scanData.size();
  }

  public FromPereupScanData getTov(int i) {
    return scanData.get(i);
  }

  public String getPal1() {
    return pal1;
  }

  public String getPal2() {
    return pal2;
  }

  public String getCell() {
    return cell;
  }

  public void callSetPal1(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.pal1, pal)) {
      dr.setS(FieldType.PAL, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    if (scanData.size() > 0) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    if (!pal2.isEmpty()) {
      dr.setS(FieldType.PAL2, "");
    }
    if (!cell.isEmpty()) {
      dr.setS(FieldType.CELL, "");
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPal2(String pal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.pal2, pal)) {
      dr.setS(FieldType.PAL2, pal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.SET_PAL.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell(String cell, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.cell, cell)) {
      dr.setS(FieldType.CELL, cell);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.SET_CELL.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTov(TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (scanData.size() > 0) {
      dr.setV(FieldType.DEL_LAST);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.QTY, qty);
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetNerazm(ZTS_MP_QTY_S[] it, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();

    dr.setV(FieldType.TAB1);
    String[] t1 = new String[it.length];
    String[] t2 = new String[it.length];
    BigDecimal[] t3 = new BigDecimal[it.length];
    for (int i = 0; i < it.length; i++) {
      t1[i] = ProcessTask.delZeros(it[i].MATNR);
      t2[i] = ProcessTask.delZeros(it[i].CHARG);
      t3[i] = it[i].QTY;
    }
    dr.setSa(FieldType.MATNRS, t1);
    dr.setSa(FieldType.CHARGS, t2);
    dr.setNa(FieldType.QTYS, t3);

    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdSetNerazm(DataRecord dr) {
    String[] t1 = (String[]) dr.getVal(FieldType.MATNRS);
    String[] t2 = (String[]) dr.getVal(FieldType.CHARGS);
    BigDecimal[] t3 = (BigDecimal[]) dr.getVal(FieldType.QTYS);
    int n = t1.length;
    nerazm = new FromPereupScanData[n];
    FromPereupScanData r;
    for (int i = 0; i < n; i++) {
      r = new FromPereupScanData(t1[i], t2[i], t3[i]);
      nerazm[i] = r;
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.PAL)) {
          pal1 = dr.getValStr(FieldType.PAL);
        }
        if (dr.haveVal(FieldType.PAL2)) {
          pal2 = dr.getValStr(FieldType.PAL2);
        }
        if (dr.haveVal(FieldType.CELL)) {
          cell = dr.getValStr(FieldType.CELL);
        }
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          for (FromPereupScanData d : scanData) {
            addNerazm(d.matnr, d.charg, d.qty);
          }
          scanData.clear();
        }
        if (dr.haveVal(FieldType.DEL_LAST)) {
          if (scanData.size() > 0) {
            FromPereupScanData d = scanData.get(scanData.size() - 1);
            scanData.remove(scanData.size() - 1);
            addNerazm(d.matnr, d.charg, d.qty);
          }
          scanData.clear();
        }
        if (dr.haveVal(FieldType.MATNR) && dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY)) {
          String charg = dr.getValStr(FieldType.CHARG);
          String matnr = dr.getValStr(FieldType.MATNR);
          BigDecimal qty = (BigDecimal) dr.getVal(FieldType.QTY);
          scanData.add(new FromPereupScanData(matnr, charg, qty));
          addNerazm(matnr, charg, qty.negate());
        }
        if (dr.haveVal(FieldType.TAB1)) {
          hdSetNerazm(dr); // логика перенесена в процедуру
        }
        break;
    }
  }
}
