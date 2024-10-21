package ntx.ts.userproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import ntx.sap.fm.Z_TS_PROGOPIS1;
import ntx.sap.fm.Z_TS_SHKLIST1;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
import ntx.sap.refs.RefInfo;
import ntx.sap.refs.RefMat;
import ntx.sap.refs.RefMatStruct;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import static ntx.ts.sysproc.ProcData.strEq;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.delZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanCharg;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import static ntx.ts.sysproc.ProcessUtil.getScanQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Прогресс опись
 */
public class ProcessProgOpis extends ProcessTask {

  private final ProgOpisData d = new ProgOpisData();

  public ProcessProgOpis(long procId) throws Exception {
    super(ProcType.PROGOPIS, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));

    if (getTaskState() == TaskState.START && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_OP, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    if (getTaskState() == TaskState.SEL_OP) {
        HtmlPageMenu p = new HtmlPageMenu("Выбор операции", "Выберите операцию:", 
                "op_i:Приход;op_o:Расход",
                "op_i",
                null,
                null, 
                false);
        return p.getPage();
    } else
      return htmlWork("Прогресс опись", false, ctx);
  }

  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    int n = scan.length();
    String s;
    String tov_qty;
    RefMatStruct prog_ref_mat;
    String prog_scan;
    String prog_mat_name;

    if ((getTaskState() == TaskState.TOV || getTaskState() == TaskState.TOV_CELL) 
            && isScanProgopis(scan)) {
        if (n == 13) {
          String mat = delZeros(scan.substring(0,8));
          String qty = scan.substring(8);
          RefMatStruct ref_mat = RefMat.get(mat);
          if (ref_mat != null && isAllDigits(qty)) {
            d.callAddShk(scan, TaskState.TOV_CELL, this, ctx);
            s = "Просканирован ШК остатка " + mat + " " + ref_mat.name + 
              " метраж " + qty.substring(0,4) + "." + qty.substring(4);
            callSetMsg(s, ctx);
            callAddHist(s, ctx);
          } else
          callSetErr("Просканирован не ШК остатка (сканирование " + scan + " не принято)", ctx);
        } else
        if ((n == 15) && isAllDigits(scan.substring(0, 7)) && isAllDigits(scan.substring(8)) && 
                ((scan.charAt(7) == 'D') || (scan.charAt(7) == 'C'))) {
          String tov_charg = delZeros(scan.substring(0, 7));
          tov_qty = scan.substring(8);
          RefChargStruct c = RefCharg.get(tov_charg, null);
          if (c == null) {
            callSetErr("Просканирован не ШК остатка (сканирование " + scan + " не принято)", ctx);
          } else {
            prog_scan = delZeros(c.matnr);
            prog_ref_mat = RefMat.get(prog_scan);
            prog_mat_name = "";
            if (prog_ref_mat != null) prog_mat_name = prog_ref_mat.name;
            if (prog_scan.length() == 6) prog_scan = "00" + prog_scan;
            if (prog_scan.length() == 7) prog_scan = "0" + prog_scan;
            String prog_qty = "";
            if (scan.charAt(7) == 'D') {
              prog_scan = prog_scan + tov_qty.substring(3) + "0";
              prog_qty = delZeros(tov_qty.substring(3)) + ".0";
            }
            if (scan.charAt(7) == 'C') {
              prog_scan = prog_scan + tov_qty.substring(2);
              prog_qty = delZeros(tov_qty.substring(2,6)) + "." + tov_qty.substring(6);
            }
            d.callAddShk(prog_scan, TaskState.TOV_CELL, this, ctx);
            s = "Просканирован ШК остатка " + delZeros(c.matnr) + " " +
              prog_mat_name + " метраж " + prog_qty;
            callSetMsg(s, ctx);
            callAddHist(s, ctx);
          }
        } else
        if ((n == 15) && isAllDigits(scan.substring(1)) && 
                (scan.charAt(0) == 'R')) {
            d.callAddShk(scan, TaskState.TOV_CELL, this, ctx);
            prog_scan = delZeros(scan.substring(1,8));
            prog_ref_mat = RefMat.get(prog_scan);
            prog_mat_name = "";
            if (prog_ref_mat != null) prog_mat_name = prog_ref_mat.name;
            s = "Просканирован ШК остатка " + delZeros(scan.substring(1,8)) + 
                prog_mat_name + 
                " метраж " + scan.substring(8,12) + "." + scan.substring(12);
            callSetMsg(s, ctx);
            callAddHist(s, ctx);
        }
        if (scan.length() > 10 && scan.charAt(7) == ':' && isAllDigits(scan.substring(1,7))) {

            prog_ref_mat = RefMat.get(scan.substring(1, 7));
            if (prog_ref_mat == null) {
                callSetErr("ОЗМ " + scan.substring(1, 7) + " не найден", ctx);
            }
            else 
            {
              int i_scan = prog_ref_mat.name.length() + 10;
              tov_qty = "";
              boolean tov_qty_err = false;
              while (scan.charAt(i_scan) != ' ' && i_scan < scan.length()) {
                  tov_qty = tov_qty + scan.charAt(i_scan);
                  if ((scan.charAt(i_scan) < '0' || scan.charAt(i_scan) > '9') 
                          && scan.charAt(i_scan) != ',') 
                      tov_qty_err = true;
                  i_scan++;
              }
              if (tov_qty.length() > 0 && !tov_qty_err) {
                prog_scan = 'P' + scan.substring(1, 7) + tov_qty;
                d.callAddShk(prog_scan, TaskState.TOV_CELL, this, ctx);
                s = "Просканирован ШК остатка " + scan.substring(1, 7) + " " +
                  prog_ref_mat.name + " метраж " + tov_qty;
                callSetMsg(s, ctx);
                callAddHist(s, ctx);
              } else
                  callSetErr("Некорректное количество: " + tov_qty, ctx);
            }
        }
        else
          callSetErr("Просканирован не ШК остатка (сканирование " + scan + " не принято)", ctx);
    } else
    if ((getTaskState() == TaskState.TOV_CELL) && isScanCell(scan)) {
          d.callAddScanData(scan.substring(1), TaskState.TOV, this, ctx);
          s = "Добавлены записи по ячейке " + scan.substring(1) + ". Всего записей: " + 
                  Integer.toString(d.getScanDataCount());
          callSetMsg(s , ctx);
          callAddHist(s, ctx);
    } else {
        callSetErr("Просканирован неизвестный ШК (сканирование " + scan + " не принято)", ctx);
    }
    
    return htmlWork("Прогресс опись", true, ctx);
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить";

    if (d.getNScan() > 0) {
      definition = definition + ";save:Сохранить;del:Очистить данные";
    } else {
      definition = definition + ";fin:Завершить";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Прогресс опись",
            definition, null, null, null);
    return p.getPage();
  }

  public FileData handleMenu(String menu, UserContext ctx) throws Exception {
    if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("del")) {
      d.callClearScanData(this, ctx);
      return htmlWork("Прогресс опись", true, ctx);
    } else if (menu.equals("op_i")) {
        d.callSetOp("I", TaskState.TOV, this, ctx);
        return htmlWork("Прогресс опись", false, ctx);
    } else if (menu.equals("op_o")) {
        d.callSetOp("O", TaskState.TOV, this, ctx);
        return htmlWork("Прогресс опись", false, ctx);
    } else if (menu.equals("save")) {

        Z_TS_PROGOPIS1 f = new Z_TS_PROGOPIS1();
        f.USER_SHK = ctx.user.getUserSHK();

        int nn = d.getScanDataCount();
        ProgOpisScanData sd;
        if (nn > 0) {
          f.IT_create(nn);
          for (int i = 0; i < nn; i++) {
            sd = d.getScanDataItem(i);
            f.IT[i].IO = sd.io;
            f.IT[i].SHK = sd.shk;
            f.IT[i].CELL = sd.cell;
          }
        }

        f.execute();

        if (f.isErr) {
          callSetErr(f.err, ctx);
          return htmlWork("Прогресс опись", true, ctx);
        }

        d.callClearScanData(this, ctx);
        String s = "Данные сохранены";
        callAddHist(s, ctx);
        callSetMsg(s, ctx);

    }
    
    return htmlWork("Прогресс опись", false, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    if (d.getNScan() == 0) {
      return super.procName();
    }
    return getProcType().text + d.getOp() + " (" + d.getNScan() + ") " + df2.format(new Date(getProcId()));
  }
}

class ProgOpisScanData {  // отсканированные позиции

  public String io;
  public String shk;
  public String cell;

  public ProgOpisScanData(String io, String shk, String cell) {
    this.io = io;
    this.shk = shk;
    this.cell = cell;
  }
}

class ProgOpisData extends ProcData {

  private String op = "";
  //private String shk = "";
  private final ArrayList<String> shkList
          = new ArrayList<String>();
  private final ArrayList<ProgOpisScanData> scanData
          = new ArrayList<ProgOpisScanData>(); // отсканированные ШК

  public String getOp() {
    if (op.startsWith("I")) return "Приход";
    else if (op.startsWith("O")) return "Расход";
    else return "";
  }
  
  public int getNScan() {
    //return nScan;
    return scanData.size();
  }

  public int getScanDataCount() {
    return scanData.size();
  }

  public ProgOpisScanData getScanDataItem(int idx) {
    return scanData.get(idx);
  }

  public ArrayList<ProgOpisScanData> getScanData() {
    return scanData;
  }
  
  public void clearScanData() {
  //  op = "";
  //  shk = "";
    shkList.clear();
    scanData.clear();
  }
  
  public void callClearScanData(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    if (TaskState.TOV != p.getTaskState()) {
      dr.setI(FieldType.TASK_STATE, TaskState.TOV.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callAddScanData(String cell, TaskState state, 
          ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.CELL, cell);
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callSetOp(String op, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(op, this.op)) {
      dr.setS(FieldType.OP, op);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
    }
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callAddShk(String shk, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
//    if (!strEq(shk, this.shk)) {
      dr.setS(FieldType.SHK, shk);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
//    }
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }
  
  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
          /*
        if (dr.haveVal(FieldType.N_SCAN)) {
          nScan = (Integer) dr.getVal(FieldType.N_SCAN);
        }
          */

        if (dr.haveVal(FieldType.SHK)) {
          String shk = (String) dr.getVal(FieldType.SHK);
          shkList.add(shk);
        }

        if (dr.haveVal(FieldType.CELL)) {
          String cell;
          cell = (String) dr.getVal(FieldType.CELL);
          int n = shkList.size();
          for (int i = 0; i < n; i++) {
            ProgOpisScanData sd = new ProgOpisScanData(op, shkList.get(i), cell);
            scanData.add(sd);
          }
          shkList.clear();
        }

        if (dr.haveVal(FieldType.OP)) {
          op = (String) dr.getVal(FieldType.OP);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          clearScanData();
        }

        break;
    }
  }
}
