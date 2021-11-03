package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.delDecZeros;
import static ntx.ts.sysproc.ProcessUtil.fillZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanCharg;
import static ntx.ts.sysproc.ProcessUtil.getScanQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Отправка в переупаковку Состояния: START, VBELN_VA, TOV, QTY
 */
public class ProcessOpisK extends ProcessTask {

  private final OpisKData d = new OpisKData();

  public ProcessOpisK(long procId) throws Exception {
    super(ProcType.OPISK, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctxUser) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    TaskContext ctx = new TaskContext(ctxUser, this);

    if (getTaskState() == TaskState.START) {
      callSetTaskState(TaskState.VBELN_VA, ctx);
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
      case QTY:
        return htmlWork("Покороб опись", playSound, delDecZeros(d.getLastQty().toString()), ctx);

      default:
        return htmlWork("Покороб опись", playSound, ctx);
    }
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu(ctx);
    }

    switch (getTaskState()) {
      case VBELN_VA:
        return handleScanVbelnVa(scan, ctx);

      case TOV:
        return handleScanTov(scan, ctx);

      case QTY:
        return handleScanQty(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanVbelnVa(String scan, TaskContext ctx) throws Exception {
    /*
    if (!isScanVbelnVa(scan) && !isScanEbeln(scan)) {
      callSetErr("Требуется ввести номер заказа (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }
    */

    Z_TS_OPISK1 f = new Z_TS_OPISK1();
    f.VBELN = fillZeros(scan, 10);
    f.execute();

    if (!f.isErr) {
      String s = "заказ " + scan + " дебитор " + f.KUNNR + " " + f.NAME1 + " (склад " + f.LGORT + ")";
      callSetMsg("Покоробочная опись: " + s, ctx);
      callAddHist(s, ctx);
      d.callSetVbelnVa(scan, f.LGORT, f.MARKED, TaskState.TOV, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    String charg;
    BigDecimal q;

    if (isScanTov(scan)) {
        /*
      if (d.isMarked()) {
        callSetErr("Корп ШК не принимаются по маркированному заказу", ctx);
        return htmlGet(true, ctx);
      }
        */
      charg = getScanCharg(scan);
      q = getScanQty(scan);
    } else if (isScanMkSn(scan)) {
        /*
      if (!d.isMarked()) {
        callSetErr("Маркированные ШК не принимаются по немаркированному заказу", ctx);
        return htmlGet(true, ctx);
      }
        */
      charg = delZeros(scan.substring(2, 9));
      q = new BigDecimal(1);
    } else if (isScanMkPb(scan)) {
        /*
      if (!d.isMarked()) {
        callSetErr("Маркированные ШК не принимаются по немаркированному заказу", ctx);
        return htmlGet(true, ctx);
      }
        */
      ZSHK_INFO f = new ZSHK_INFO();
      f.SHK = scan;
      f.execute();
      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlGet(true, ctx);
      }
      charg = f.CHARG;
      q = f.QTY;
    } else {
        callSetErr("ШК неизвестного типа (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
    }
    
    try {
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }

      boolean haveMat = getHaveMat(c.matnr, ctx);
      if ((getLastErr() != null) && !getLastErr().isEmpty()) {
        return htmlGet(true, ctx);
      } else if (!haveMat) {
        callSetErr("Этого материала нет в заказе (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }

/*
    if (ctx.user.getAskQtyCompl(d.getLgort())) {
        String s = c.matnr + " " + RefMat.getFullName(c.matnr);
        callSetMsg(s, ctx);
        d.callSetLastMatnr(c.matnr, getScanQty(scan), TaskState.QTY, ctx);
        return htmlGet(true, ctx);
      }
*/

      return handleScanTovDo(c.matnr, q, scan, ctx);
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = "ОШИБКА ! Сообщите разработчику";
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovDo(String matnr, BigDecimal qty, 
          String shk, TaskContext ctx) throws Exception {
    d.callAddTov(matnr, qty, shk, TaskState.TOV, ctx);
    String s = delDecZeros(qty.toString()) + "ед " + matnr + " " + RefMat.getName(matnr);
    callAddHist(s, ctx);
    s += " (всего " + d.getQtyTotStr() + " ед)";
    callSetMsg(s, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanQty(String scan, TaskContext ctx) throws Exception {
    if (scan.isEmpty() || isScanCell(scan) || isScanPal(scan)
            || isScanTov(scan) || !isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    callSetErr("Режим прямого ввода количества не реализован", ctx);
    return htmlGet(true, ctx);
    //return handleScanTovDo(d.getLastMatnr(), qty, ctx);
  }

  private boolean getHaveMat(String matnr, TaskContext ctx) throws Exception {
    Boolean haveMat = d.getHaveMat(matnr);

    if (haveMat == null) {
      Z_TS_OPISK2 f = new Z_TS_OPISK2();
      f.VBELN = fillZeros(d.getVbelnVa(), 10);
      f.MATNR = fillZeros(matnr, 18);
      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
        return false;
      }

      haveMat = (f.QTY.signum() > 0);
      d.callSetHaveMat(matnr, haveMat, ctx);
    }

    return haveMat;
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;

    String askQtyMenu = "";
    if (ctx.user.isAskQtyComplEnabled(d.getLgort())) {
      askQtyMenu = ctx.user.getAskQtyCompl(d.getLgort()) ? ";ask_qty_off:Выкл ввод кол-ва" : ";ask_qty_on:Вкл ввод кол-ва";
    }

    switch (getTaskState()) {
      case VBELN_VA:
        definition = "later:Отложить;fin:Завершить";
        break;
      case TOV:
        definition = "later:Отложить";
        if (d.getScanCount() > 0) {
          if (d.isMarked()) 
            definition += ";save:Сохранить;del_last:Отменить последнее сканирование;del_all:Удалить весь товар";
          else
            definition += ";opis:Печать описи;save:Сохранить без печати;del_last:Отменить последнее сканирование;del_all:Удалить весь товар";
        } else {
          definition += ";vbel:Другой заказ;fin:Завершить";
        }
        definition += askQtyMenu;
        break;
      default:
        definition = "later:Отложить";
        break;
    }

    definition = "cont:Назад;" + definition;

    if (RefInfo.haveInfo(ProcType.OPISK)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Покороб опись", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("del_last")) {
      handleMenuDelLast(ctx);
    } else if (menu.equals("del_all")) {
      handleMenuDelAll(ctx);
    } else if (menu.equals("ask_qty_on")) {
      callClearErrMsg(ctx);
      ctx.user.callSetAskQty(true, ctx);
    } else if (menu.equals("ask_qty_off")) {
      callClearErrMsg(ctx);
      ctx.user.callSetAskQty(false, ctx);
    } else if (menu.equals("vbel")) {
      handleMenuVbel(ctx);
    } else if (menu.equals("opis")) {
      callClearErrMsg(ctx);
      if (d.getScanCount() == 0) {
        callSetErr("Сохранять нечего", ctx);
      } else {
        return htmlSelPrinter("opis", "Выберите принтер для печати описи:", ctx);
      }
    } else if (menu.startsWith("opis_")) {
      callClearErrMsg(ctx);
      handleMenuOpis(menu.substring(5), ctx);
    } else if (menu.equals("save")) {
      handleMenuOpis("", ctx);
    }

    return htmlGet(true, ctx);
  }

  private void handleMenuOpis(String printer, TaskContext ctx) throws Exception {
    if (d.getScanCount() == 0) {
      callSetErr("Сохранять нечего", ctx);
      return;
    }

    ctx.user.callSetPrinter(printer, ctx);

    Z_TS_OPISK3 f = new Z_TS_OPISK3();
    f.VBELN = fillZeros(d.getVbelnVa(), 10);
    f.TDDEST = printer;

    int nn = d.getScanCount();
    OpisKScanData sd;
    if (nn > 0) {
      f.IT_create(nn);
      for (int i = 0; i < nn; i++) {
        sd = d.getTov(i);
        f.IT[i].MATNR = fillZeros(sd.matnr, 18);
        f.IT[i].QTY = sd.qty;
        f.IT[i].SHK = sd.shk;
      }
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return;
    }

    String s = "Сохранен короб " + delZeros(f.KOROB) + " по заказу " + d.getVbelnVa() + " (" + delDecZeros(f.QTY_TOT.toString()) + " ед)";
    callAddHist(s, ctx);
    callSetMsg(s, ctx);

    d.callSetKorob(delZeros(f.KOROB), ctx);

    if (!f.ERR2.isEmpty()) {
      callSetErr(f.ERR2, ctx);
      callAddHist(f.ERR2, ctx);
      return;
    }

    if (!printer.isEmpty()) {
      callAddHist("Опись отправлена на печать", ctx);
    }
  }

  private void handleMenuVbel(TaskContext ctx) throws Exception {
    callSetTaskState(TaskState.VBELN_VA, ctx);
  }

  private void handleMenuDelAll(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case TOV:
        int nn = d.getScanCount();
        if (nn > 0) { // отменяем сканирование товара
          String s = "Отменено " + d.getQtyTotStr() + " ед (весь товар)";
          d.callClearTov(ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else {
          callSetMsg("Товар не отсканирован, удалять нечего", ctx);
        }
        break;

      default:
        callSetErr("Неподходящее состояние (для отмены сканирования товара): " + getTaskState().name(), ctx);
        break;
    }
  }

  private void handleMenuDelLast(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case TOV:
        int nn = d.getScanCount();
        if (nn > 0) { // отменяем сканирование товара
          OpisKScanData t = d.getTov(nn - 1);
          String s = "Отменено " + delDecZeros(t.qty.toString()) + " ед мат " + t.matnr;
          d.callDelLast(ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        }
        break;

      default:
        callSetErr("Неподходящее состояние (для отмены последнего сканирования): " + getTaskState().name(), ctx);
        break;
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }
}

class OpisKScanData {  // отсканированные позиции

  public String matnr;
  public BigDecimal qty;
  public String shk;

  public OpisKScanData(String matnr, BigDecimal qty, String shk) {
    this.matnr = matnr;
    this.qty = qty;
    this.shk = shk;
  }
}

class OpisKData extends ProcData {

  private String vbelnVa = ""; // номер заказа
  private String lgort = ""; // склад (из заказа)
  private String lastMatnr = "";
  private String marked = "";
  private BigDecimal lastQty = BigDecimal.ZERO;
  private final ArrayList<OpisKScanData> scanData
          = new ArrayList<OpisKScanData>(); // отсканированный товар

  private final HashMap<String, Boolean> haveMat = new HashMap<String, Boolean>();

  public String getLastMatnr() {
    return lastMatnr;
  }

  public BigDecimal getLastQty() {
    return lastQty;
  }

  public String getVbelnVa() {
    return vbelnVa;
  }

  public String getLgort() {
    return lgort;
  }

  public boolean isMarked() {
    if (marked == null) return false;
    return !marked.isEmpty();
  }

  public ArrayList<OpisKScanData> getScanData() {
    return scanData;
  }

  public Boolean getHaveMat(String matnr) {
    matnr = ProcessTask.delZeros(matnr);
    return haveMat.get(matnr);
  }

  public String getQtyTotStr() {
    BigDecimal ret = BigDecimal.ZERO;
    for (OpisKScanData n : scanData) {
      ret = ret.add(n.qty);
    }
    return delDecZeros(ret.toString());
  }

  public int getScanCount() {
    return scanData.size();
  }

  public OpisKScanData getTov(int i) {
    return scanData.get(i);
  }

  public void callSetLastMatnr(String matnr, BigDecimal qty, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.lastMatnr, matnr)) {
      dr.setS(FieldType.LAST_MATNR, matnr);
    }
    if (this.lastQty.compareTo(qty) != 0) {
      dr.setN(FieldType.LAST_QTY, qty);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVbelnVa(String vbelnVa, String lgort, String marked,
          TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.VBELN, ProcessTask.delZeros(vbelnVa));
    dr.setS(FieldType.LGORT, lgort);
    dr.setS(FieldType.MARKED, marked);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetHaveMat(String matnr, boolean haveMat, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, ProcessTask.delZeros(matnr));
    dr.setB(FieldType.HAVE_MAT, haveMat);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTov(TaskContext ctx) throws Exception {
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

  public void callAddTov(String matnr, BigDecimal qty, String shk,
          TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setN(FieldType.QTY, qty);
    dr.setS(FieldType.SHK, shk);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    if (dr.isChanged()) {
      dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetKorob(String korob, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.KOROB, ProcessTask.delZeros(korob));
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.VBELN)) {
          String vbeln = dr.getValStr(FieldType.VBELN);
          if (!vbelnVa.equals(vbeln)) {
            haveMat.clear();
          }
          vbelnVa = vbeln;
          scanData.clear();
        }

        if (dr.haveVal(FieldType.LGORT)) {
          lgort = dr.getValStr(FieldType.LGORT);
        }

        if (dr.haveVal(FieldType.MARKED)) {
          marked = dr.getValStr(FieldType.MARKED);
        }

        if (dr.haveVal(FieldType.HAVE_MAT) && dr.haveVal(FieldType.MATNR)) {
          haveMat.put(dr.getValStr(FieldType.MATNR), (Boolean) dr.getVal(FieldType.HAVE_MAT));
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          scanData.clear();
        }

        if (dr.haveVal(FieldType.KOROB)) {
          scanData.clear();
        }

        if (dr.haveVal(FieldType.DEL_LAST)) {
          int n = scanData.size();
          if (n > 0) {
            scanData.remove(n - 1);
          }
        }

        if (dr.haveVal(FieldType.LAST_MATNR)) {
          lastMatnr = dr.getValStr(FieldType.LAST_MATNR);
        }

        if (dr.haveVal(FieldType.LAST_QTY)) {
          lastQty = (BigDecimal) dr.getVal(FieldType.LAST_QTY);
        }

        if (dr.haveVal(FieldType.QTY) && dr.haveVal(FieldType.MATNR) 
                && dr.haveVal(FieldType.SHK)) {
          String matnr = dr.getValStr(FieldType.MATNR);
          BigDecimal qty = (BigDecimal) dr.getVal(FieldType.QTY);
          String shk = dr.getValStr(FieldType.SHK);
          scanData.add(new OpisKScanData(matnr, qty, shk));
        }

        break;
    }
  }
}
