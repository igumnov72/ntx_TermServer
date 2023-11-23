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
 * Комплектация перемещения комплексной поставки
 * K_VBELN SEL_CELL TOV_CELL
 */
public class ProcessCDlvMove extends ProcessTask {

  private final CDlvMoveData d = new CDlvMoveData();

  public ProcessCDlvMove(long procId) throws Exception {
    super(ProcType.CDLVMOVE, procId);
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
        CDlvMoveScanData sd;
        if (nn > 0) {
          f.IT_create(nn);
          for (int i = 0; i < nn; i++) {
            sd = d.getScanDataItem(i);
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
    return getProcType().text + " (" + d.getNScan() + ") " + df2.format(new Date(getProcId()));
  }
}

class CDlvMoveScanData {  // отсканированные позиции

  public String shk;
  public String cell;

  public CDlvMoveScanData(String shk, String cell) {
    this.shk = shk;
    this.cell = cell;
  }
}

class CDlvMoveData extends ProcData {

  private final ArrayList<String> shkList
          = new ArrayList<String>();
  private final ArrayList<CDlvMoveScanData> scanData
          = new ArrayList<CDlvMoveScanData>(); // отсканированные ШК

  public int getNScan() {
    //return nScan;
    return scanData.size();
  }

  public int getScanDataCount() {
    return scanData.size();
  }

  public CDlvMoveScanData getScanDataItem(int idx) {
    return scanData.get(idx);
  }

  public ArrayList<CDlvMoveScanData> getScanData() {
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
            CDlvMoveScanData sd = new CDlvMoveScanData(shkList.get(i), cell);
            scanData.add(sd);
          }
          shkList.clear();
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          clearScanData();
        }

        break;
    }
  }
}
