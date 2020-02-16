package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_SKOROB_SHK_S;
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
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Сборный короб Состояния: START, VBELN_VA, KOROB, KOROB_PAL
 */
public class ProcessSKorob extends ProcessTask {

  private final SKorobData d = new SKorobData();

  public ProcessSKorob(long procId) throws Exception {
    super(ProcType.SKOROB, procId);
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
      default:
        return htmlWork("Сборный короб", playSound, ctx);
    }
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu(ctx);
    }

    switch (getTaskState()) {
      case VBELN_VA:
        return handleScanVbelnVa(scan, ctx);

      case KOROB:
        return handleScanKorob(scan, ctx);

      case KOROB_PAL:
        return handleScanKorobPal(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData handleScanVbelnVa(String scan, TaskContext ctx) throws Exception {
    if (!isScanVbelnVa(scan) && !isScanEbeln(scan)) {
      callSetErr("Требуется ввести номер заказа (ввод " + scan + " не принят)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_SKOROB1 f = new Z_TS_SKOROB1();
    f.ZV = fillZeros(scan, 10);
    f.execute();

    if (!f.isErr) {
      String s = "заказ " + scan + " дебитор " + f.NAME1 + " (склад " + f.LGORT + ")";
      callSetMsg("Сборный короб: " + s, ctx);
      callAddHist(s, ctx);
      d.callSetVbelnVa(scan, f.LGORT, f.IT, TaskState.KOROB, ctx);

      callTaskNameChange(ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanKorob(String scan, TaskContext ctx) throws Exception {
    if (!isAllDigits(scan)) {
      callSetErr("Требуется отсканировать ШК короба (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    SKorobShk zak = d.getZakData().get(scan);
    if (zak == null) {
      callSetErr("Это не ШК существующего в указанном заказе короба (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    } else if (zak.n >= zak.nKor) {
      callSetErr("Превышено допустимое число коробов с таким ШК: " + zak.nKor + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String s = "короб " + scan + " (" + (zak.n + 1) + " из " + zak.nKor + ")";
    callSetMsg("Отсканирован " + s, ctx);
//    callAddHist(s, ctx);
    d.callSetKorob(scan, TaskState.KOROB_PAL, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanKorobPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPal(getScanPal(scan), ctx);
    } else {
      return handleScanKorob(scan, ctx);
    }
  }

  private FileData handleScanPal(String pal, TaskContext ctx) throws Exception {
    // тип ШК уже проверен

    Z_TS_SKOROB2 f = new Z_TS_SKOROB2();
    f.ZV = d.getVbelnVa();
    f.LENUM = fillZeros(pal, 20);
    f.IT = d.getScanData();

    if (f.IT == null) {
      callSetErr("Нет отсканированных коробов, сохранять на паллету нечего (сканирование номера паллеты не принято)", ctx);
      return htmlGet(true, ctx);
    }

    f.execute();

    if (!f.isErr) {
      int nn = d.getNKorob();
      String s = "паллета " + pal + " (" + nn + " коробов)";
      callSetMsg("Сохранена " + s, ctx);
      callAddHist(s, ctx);
      d.callClearTov(ctx);

      Z_TS_SKOROB1 f1 = new Z_TS_SKOROB1();
      f1.ZV = d.getVbelnVa();
      f1.execute();
      if (!f1.isErr) {
        d.callSetVbelnVa(f1.ZV, f1.LGORT, f1.IT, TaskState.KOROB, ctx);
      } else {
        callSetTaskState(TaskState.KOROB, ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition = "cont:Назад;later:Отложить";

    switch (getTaskState()) {
      case VBELN_VA:
        definition += ";fin:Завершить";
        break;

      case KOROB:
        definition += ";zak:Другой заказ;fin:Завершить";
        break;

      case KOROB_PAL:
        definition += ";del_all:Удалить все короба";
        break;
    }

    if (RefInfo.haveInfo(ProcType.SKOROB)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Сборный короб", "Выберите действие",
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
    } else if (menu.equals("del_all")) {
      handleMenuDelAll(ctx);
    } else if (menu.equals("zak")) {
      handleMenuZak(ctx);
    }

    return htmlGet(true, ctx);
  }

  private void handleMenuDelAll(TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case KOROB_PAL:
        int nn = d.getNKorob();
        if (nn > 0) { // отменяем сканирование товара
          String s = "Отменено " + d.getNKorob() + " коробов (все отсканированные на паллету)";
          d.callClearTov(ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else {
          callSetMsg("Короба не отсканированы, удалять нечего", ctx);
        }
        break;

      default:
        callSetErr("Неподходящее состояние (для отмены сканирования коробов): " + getTaskState().name(), ctx);
        break;
    }
  }

  private void handleMenuZak(TaskContext ctx) throws Exception {
    callSetTaskState(TaskState.VBELN_VA, ctx);
  }

  @Override
  public String procName() {
    if (d.getVbelnVa().isEmpty()) {
      return getProcType().text + " " + df2.format(new Date(getProcId()));
    } else {
      return getProcType().text + " зак " + d.getVbelnVa() + " " + df2.format(new Date(getProcId()));
    }
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    if (d.getVbelnVa().isEmpty()) {
      return "";
    } else {
      return d.getLgort() + ", зак " + d.getVbelnVa();
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }
}

class SKorobShk {  // данные по ШК короба

  public String shkKor;
  public int nKor;
  public int n;

  public SKorobShk(String shkKor, int nKor) {
    this.shkKor = shkKor;
    this.nKor = nKor;
    this.n = 0;
  }

  public static void clearN(HashMap<String, SKorobShk> ar) {
    for (Map.Entry<String, SKorobShk> i : ar.entrySet()) {
      i.getValue().n = 0;
    }
  }
}

class SKorobData extends ProcData {

  private String vbelnVa = ""; // номер заказа
  private String lgort = ""; // склад (из заказа)
  private final HashMap<String, SKorobShk> zakData
          = new HashMap<String, SKorobShk>(); // короба по заказу

  public String getVbelnVa() {
    return vbelnVa;
  }

  public String getLgort() {
    return lgort;
  }

  public HashMap<String, SKorobShk> getZakData() {
    return zakData;
  }

  public ZTS_SKOROB_SHK_S[] getScanData() {
    int n = 0;
    for (Map.Entry<String, SKorobShk> i : zakData.entrySet()) {
      if (i.getValue().n > 0) {
        n++;
      }
    }

    if (n == 0) {
      return null;
    }

    ZTS_SKOROB_SHK_S[] ret = new ZTS_SKOROB_SHK_S[n];
    int j = 0;
    ZTS_SKOROB_SHK_S r;
    SKorobShk v;
    for (Map.Entry<String, SKorobShk> i : zakData.entrySet()) {
      v = i.getValue();
      if (v.n > 0) {
        r = new ZTS_SKOROB_SHK_S();
        ret[j] = r;
        r.SHK_KOR = v.shkKor;
        r.N_KOR = v.n;

        j++;
      }
    }

    return ret;
  }

  public int getNKorob() {
    int ret = 0;
    for (Map.Entry<String, SKorobShk> i : zakData.entrySet()) {
      ret += i.getValue().n;
    }
    return ret;
  }

  public void callSetVbelnVa(String vbelnVa, String lgort, ZTS_SKOROB_SHK_S[] it, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.VBELN, ProcessTask.delZeros(vbelnVa));
    dr.setS(FieldType.LGORT, lgort);

    String[] t1 = new String[it.length];
    int[] t2 = new int[it.length];
    for (int i = 0; i < it.length; i++) {
      t1[i] = it[i].SHK_KOR;
      t2[i] = it[i].N_KOR;
    }
    dr.setSa(FieldType.KOR_SHK_S, t1);
    dr.setIa(FieldType.KOR_N_S, t2);

    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTov(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetKorob(String korob, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.KOROB, ProcessTask.delZeros(korob));
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdSetVbeln(DataRecord dr) {
    zakData.clear();
    vbelnVa = dr.getValStr(FieldType.VBELN);

    String[] t1 = (String[]) dr.getVal(FieldType.KOR_SHK_S);
    int[] t2 = (int[]) dr.getVal(FieldType.KOR_N_S);
    int n = t1.length;
    SKorobShk r;
    for (int i = 0; i < n; i++) {
      r = new SKorobShk(t1[i], t2[i]);
      zakData.put(t1[i], r);
    }
  }

  private void hdSetKorob(DataRecord dr) {
    zakData.clear();
    String kor = dr.getValStr(FieldType.KOROB);

    SKorobShk zak = zakData.get(kor);
    if (zak == null) {
      zak = new SKorobShk(kor, 0);
      zakData.put(kor, zak);
    }

    zak.n++;
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.VBELN)) {
          hdSetVbeln(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.LGORT)) {
          lgort = dr.getValStr(FieldType.LGORT);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          SKorobShk.clearN(zakData);
        }

        if (dr.haveVal(FieldType.KOROB)) {
          hdSetKorob(dr); // логика перенесена в процедуру
        }

        break;
    }
  }
}
