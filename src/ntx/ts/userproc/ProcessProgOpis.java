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
      return handleScan(scan, false, ctx);
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

    if (getTaskState() == TaskState.SEL_SHK) {
        if (n == 13) {
          String mat = delZeros(scan.substring(0,8));
          String qty = scan.substring(8);
          RefMatStruct ref_mat = RefMat.get(mat);
          if (ref_mat != null && isAllDigits(qty)) {
            d.callSetShk(scan, TaskState.SEL_CELL, this, ctx);
            s = "Просканирован ШК остатка " + mat + " " + ref_mat.name + 
              " метраж " + qty.substring(0,4) + "." + qty.substring(4);
            callSetMsg(s, ctx);
            callAddHist(s, ctx);
          } else
          callSetErr("Просканирован не ШК остатка (сканирование " + scan + " не принято)", ctx);
        } else
          callSetErr("Просканирован не ШК остатка (сканирование " + scan + " не принято)", ctx);
    } else
    if (getTaskState() == TaskState.SEL_CELL) {
        if (isScanCell(scan)) {
          d.callAddScanData(scan, TaskState.SEL_SHK, this, ctx);
          s = "Добавлена запись по ячейке " + scan.substring(1) + ". Всего записей: " + 
                  Integer.toString(d.getScanDataCount());
          callSetMsg(s , ctx);
          callAddHist(s, ctx);
        } else {
          callSetErr("Просканирован не ШК ячейки (сканирование " + scan + " не принято)", ctx);
        }
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
        d.callSetOp("I", TaskState.SEL_SHK, this, ctx);
        return htmlWork("Прогресс опись", false, ctx);
    } else if (menu.equals("op_o")) {
        d.callSetOp("O", TaskState.SEL_SHK, this, ctx);
        return htmlWork("Прогресс опись", false, ctx);
    } else if (menu.equals("save")) {

        Z_TS_PROGOPIS1 f = new Z_TS_PROGOPIS1();
        //f.USER_SHK = ctx.user.getUserSHK();

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
  private String shk = "";
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
    shk = "";
    scanData.clear();
  }
  
  public void callClearScanData(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    if (TaskState.SEL_SHK != p.getTaskState()) {
      dr.setI(FieldType.TASK_STATE, TaskState.SEL_SHK.ordinal());
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

  public void callSetShk(String shk, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(shk, this.shk)) {
      dr.setS(FieldType.SHK, shk);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
    }
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
          String cell;
          cell = (String) dr.getVal(FieldType.CELL);
          ProgOpisScanData sd = new ProgOpisScanData(op, shk, cell);
          scanData.add(sd);
          shk = "";
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
