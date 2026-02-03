package ntx.ts.userproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import ntx.sap.fm.Z_TS_SPISZATR1;
import ntx.sap.fm.Z_TS_SPISZATR2;
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
 * Списание на затраты
 */
public class ProcessSpisZatr extends ProcessTask {

  private final SpisZatrData d = new SpisZatrData();

  public ProcessSpisZatr(long procId) throws Exception {
    super(ProcType.SPIS_ZATR, procId);
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
        HtmlPageMenu p;
        p = new HtmlPageMenu("Выбор МВЗ", "Выберите МВЗ:", 
                "mvz_mi:МИ;" +
                "mvz_kpb:КПБ;" + 
                "mvz_typ:Типография;" +
                "mvz_pvh:ПВХ;" +
                "mvz_ap:АП"
                ,
                "mvz_mi",
                null,
                null, 
                false);
        return p.getPage();
    } else
      return htmlWork("Списание на затраты", false, ctx);
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

    if (getTaskState() == TaskState.TOV) {
        if (((n == 6) || (n == 7)) && isAllDigits(scan)) {
          RefMatStruct ref_mat = RefMat.get(scan);
          if (ref_mat != null) {
            d.callSetShk(scan, TaskState.QTY, this, ctx);
          } else
          callSetErr("Просканирован не ШК материала (сканирование " + scan + " не принято)", ctx);
        } 
        /*
        else
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
            d.callSetShk(prog_scan, TaskState.QTY, this, ctx);
            s = "Просканирован ШК остатка " + delZeros(c.matnr) + " " +
              prog_mat_name + " метраж " + prog_qty;
            callSetMsg(s, ctx);
            callAddHist(s, ctx);
          }
        }
        */
        else
          callSetErr("Просканирован не ШК материала (сканирование " + scan + " не принято)", ctx);
    } else
    if ((getTaskState() == TaskState.QTY)) {

        Z_TS_SPISZATR1 f = new Z_TS_SPISZATR1();
        f.SHK = d.getShk();
        f.QTY = scan;

        f.execute();

        if (f.isErr) {
          callSetErr(f.err, ctx);
          callSetTaskState(TaskState.TOV, ctx);
          return htmlWork("Списание на затраты", true, ctx);
        }
        
        d.callAddScanData(scan, TaskState.TOV, this, ctx);
          s = d.getShk() + " / " + scan;
          //+ ". Всего записей: " + Integer.toString(d.getScanDataCount());
          callSetMsg(s , ctx);
          callAddHist(s, ctx);
    } else {
        callSetErr("Просканирован неизвестный ШК (сканирование " + scan + " не принято)", ctx);
    }
    
    return htmlWork("Списание на затраты", true, ctx);
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить";

    if (d.getNScan() > 0) {
      definition = definition + ";save:Создать проводку;del:Очистить данные";
    } else {
      definition = definition + ";fin:Завершить";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Списание на затраты",
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
      return htmlWork("Списание на затраты", true, ctx);
    } else if (menu.startsWith("mvz_")) {
        d.callSetMvz(menu.substring(4), TaskState.TOV, this, ctx);
        return htmlWork("Списание на затраты", false, ctx);
    } else if (menu.equals("save")) {

        Z_TS_SPISZATR2 f = new Z_TS_SPISZATR2();
        f.USER_SHK = ctx.user.getUserSHK();

        int nn = d.getScanDataCount();
        SpisZatrScanData sd;
        if (nn > 0) {
          f.IT_create(nn);
          for (int i = 0; i < nn; i++) {
            sd = d.getScanDataItem(i);
            f.IT[i].MVZ = sd.mvz;
            f.IT[i].SHK = sd.shk;
            f.IT[i].QTY = sd.qty;
          }
        }

        f.execute();

        String s = f.INF;

        if (f.isErr) {
          s = s + " " + f.err;
          callSetErr(s, ctx);
          return htmlWork("Списание на затраты", true, ctx);
        }

        d.callClearScanData(this, ctx);
        callAddHist(s, ctx);
        callSetMsg(s, ctx);
        callSetErr("", ctx);

    }
    
    return htmlWork("Списание на затраты", false, ctx);
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
    return getProcType().text + d.getMvzName() + " (" + d.getNScan() + ") " + 
            df2.format(new Date(getProcId()));
  }
}

class SpisZatrScanData {  // отсканированные позиции

  public String mvz;
  public String shk;
  public String qty;

  public SpisZatrScanData(String mvz, String shk, String qty) {
    this.mvz = mvz;
    this.shk = shk;
    this.qty = qty;
  }
}

class SpisZatrData extends ProcData {

  private String mvz = "";
  private String shk = "";
  private final ArrayList<SpisZatrScanData> scanData
          = new ArrayList<SpisZatrScanData>(); // отсканированные ШК

  public String getMvz() {
      return mvz;
  }
  
  public String getShk() {
      return shk;
  }
  
  public String getMvzName() {
         if (mvz.equals("mi")) return "МИ";
    else if (mvz.equals("kpb")) return "КПБ";
    else if (mvz.equals("typ")) return "Типография";
    else if (mvz.equals("pvh")) return "ПВХ";
    else if (mvz.equals("ap")) return "АП";
    else return "";
  }
  
  public int getNScan() {
    return scanData.size();
  }

  public int getScanDataCount() {
    return scanData.size();
  }

  public SpisZatrScanData getScanDataItem(int idx) {
    return scanData.get(idx);
  }

  public ArrayList<SpisZatrScanData> getScanData() {
    return scanData;
  }
  
  public void clearScanData() {
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

  public void callAddScanData(String qty, TaskState state, 
          ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.CELL, qty);
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callSetMvz(String mvz, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.OP, mvz);
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callSetShk(String shk, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.SHK, shk);
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
          shk = (String) dr.getVal(FieldType.SHK);
        }

        if (dr.haveVal(FieldType.CELL)) {
          String qty;
          qty = (String) dr.getVal(FieldType.CELL);
          SpisZatrScanData sd = new SpisZatrScanData(mvz, shk, qty);
          scanData.add(sd);
        }

        if (dr.haveVal(FieldType.OP)) {
          mvz = (String) dr.getVal(FieldType.OP);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          clearScanData();
        }

        break;
    }
  }
}
