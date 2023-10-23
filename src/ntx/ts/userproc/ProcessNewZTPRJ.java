/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import ntx.sap.fm.Z_TS_CREATE_ZTPRJ;
import ntx.sap.fm.Z_TS_IS_EQ;
import ntx.sap.fm.Z_TS_IS_NAVOI;
import ntx.sap.fm.Z_TS_SHKLIST_NEW_CHARG;
import ntx.ts.html.HtmlPageMenu;
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
import ntx.ts.sysproc.UserContext;

/**
 *
 * @author amolchanov
 */

public class ProcessNewZTPRJ extends ProcessTask {

  private final NewZTPRJData d = new NewZTPRJData();
//    public String zvv_opis;
//    private String SHK_EQ;

  public ProcessNewZTPRJ(long procId) throws Exception {
    super(ProcType.NEW_ZTPRJ, procId);
  }

    @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));    
    
    if (getTaskState() == TaskState.START && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_ZEQ, ctx);    
    } else if (getTaskState() == TaskState.SEL_ZEQ && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_NAVOI, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }    
      return htmlWork("Создать Наряд", false, ctx);
  }
  
    public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {   
        if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (getTaskState() == TaskState.SEL_ZEQ) {
          Z_TS_IS_EQ ff = new Z_TS_IS_EQ();
          ff.W_SHK_ZEQ = scan;
          ff.execute();
          if (ff.isErr) {  
            callSetErr(ff.err, ctx);
          }
          else {  
          callSetMsg("ШК станка:" + scan, ctx); 
          d.callSetSHK_ZEQ(scan, TaskState.SEL_NAVOI, this, ctx); 
          }       
          return htmlWork("Создать Наряд", true, ctx);
    }
    
    if (getTaskState() == TaskState.SEL_NAVOI) {
          Z_TS_IS_NAVOI ff1 = new Z_TS_IS_NAVOI();
          ff1.W_SHK_NAVOI = scan;
          ff1.execute();
          if (ff1.isErr) {  
            callSetErr(ff1.err, ctx);
          }
          else {                  
            callSetMsg("ШК Навоя:" + ff1.W_SHK_NAVOI, ctx);         
// Все данные есть - создаем Наряд
            Z_TS_CREATE_ZTPRJ ff2 = new Z_TS_CREATE_ZTPRJ();
            ff2.W_SHK_ZEQ = d.getSHK_EQ(); 
            ff2.W_SHK_CHARG = ff1.W_SHK_NAVOI; 
            ff2.execute();
            if (ff2.isErr) { callSetErr(ff2.err, ctx); }
            else { 
                callSetMsg("Создан(ы) Наряд(ы):" + ff2.ZTPRJ, ctx);  
            }
          }
          return htmlWork("Создать Наряд", false, ctx);    
    }
    else {
      return htmlWork("Создать Наряд", false, ctx);    
    }
    }
    
    
  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;fin:Завершить";

//    if (d.getNScan() > 0) {
////      definition = definition + ";save:Сохранить;del_last:Отменить последнее сканирование";
//      definition = definition + ";save:Сохранить ШК;del_last:Отменить последнее сканирование";
//    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Создать Наряд",
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
//    } else if (menu.equals("save")) {
    }    
    
    return htmlWork("Создать Наряд", false, ctx);
  }
   
    
//    if (getTaskState() == TaskState.SEL_CHARG) {
//        if (isAllDigits(scan)) {
//          callSetMsg("Партия ПУ № " + scan, ctx); 
//          d.callSetOpis(scan, TaskState.SEL_SHK, this, ctx);
//        }
//    }
    
//    }

//    if (d.scanIsDouble(scan)) {
//      callSetErr("ШК дублирован (сканирование " + scan + " не принято)", ctx);
//      return htmlWork("Набор Самтекс", true, ctx);
//    }

 
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
  
  
  class NewZTPRJData extends ProcData {

        private final ArrayList<String> scanData
          = new ArrayList<String>(); // отсканированные ШК
      
    private String SHK_EQ;
    private String SHK_CHARG;
    
  public void callSetSHK_ZEQ(String SHK_EQ, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(SHK_EQ, this.SHK_EQ)) {
      dr.setS(FieldType.SHK_ZEQ, SHK_EQ);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
    }
    if ((state != null) && (state != p.getTaskState())) {

      dr.setI(FieldType.TASK_STATE, state.ordinal());
      
    }
    Track.saveProcessChange(dr, p, ctx);
  }    
  
  public void callSetSHK_CHARG(String SHK_CHARG, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(SHK_CHARG, this.SHK_CHARG)) {
      dr.setS(FieldType.SHK_CHARG, SHK_CHARG);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
    }
//    if ((state != null) && (state != p.getTaskState())) {
//
//      dr.setI(FieldType.TASK_STATE, state.ordinal());
//      
//    }
    Track.saveProcessChange(dr, p, ctx);
  }    
    
    public String getSHK_EQ() {
     return SHK_EQ;
  } 

  public String getSHK_CHARG() {
     return SHK_CHARG;
  } 
  
  public int getNScan() {
    //return nScan;
    return scanData.size();
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

//        if (dr.haveVal(FieldType.SHK)) {
//          String shk;
//          shk = (String) dr.getVal(FieldType.SHK);
//          scanData.add(shk);
//        }

//        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
//          clearScanData();
//        }

//        if (dr.haveVal(FieldType.DEL_LAST)) {
//          int n = scanData.size();
//          if (n > 0) {
//            scanData.remove(n - 1);
//          }
//        }
        if (dr.haveVal(FieldType.SHK_ZEQ)) {
          SHK_EQ = (String) dr.getVal(FieldType.SHK_ZEQ);
        }        

        if (dr.haveVal(FieldType.SHK_CHARG)) {
          SHK_CHARG = (String) dr.getVal(FieldType.SHK_CHARG);
        }        

        break;
    }
  }
     
    
  }
  
