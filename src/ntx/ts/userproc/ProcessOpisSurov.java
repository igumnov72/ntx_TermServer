/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import ntx.sap.fm.Z_TS_IS_CHARG_PU;
import ntx.sap.fm.Z_TS_IS_OPIS;
import ntx.sap.fm.Z_TS_IS_STELL_ZNS;
import ntx.sap.fm.Z_TS_OPISSUROV_LGORT;
import ntx.sap.fm.Z_TS_OPISSUROV_OPIS;
import ntx.sap.fm.Z_TS_OPISSUROV_SHK;
import ntx.sap.fm.Z_TS_OPISSUROV_SHKLIST_SAVE;
import ntx.sap.fm.Z_TS_SHKLIST3_CHECK_STELL;
import ntx.sap.fm.Z_TS_SHKLIST3_IS_CHARG_PU;
import ntx.sap.fm.Z_TS_SHKLIST3_SET_STELL;
import ntx.sap.fm.Z_TS_SHKLIST3_STELL;
import ntx.sap.fm.Z_TS_SHKLIST_IS_OPIS;
import ntx.sap.fm.Z_TS_SHKLIST_OPIS_CHECK;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
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
import static ntx.ts.sysproc.ProcessUtil.delZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanQty;
import static ntx.ts.sysproc.ProcessUtil.isAllDigitsComma;
import static ntx.ts.sysproc.ProcessUtil.isScanMkPb;
import static ntx.ts.sysproc.ProcessUtil.isScanMkSn;
import static ntx.ts.sysproc.ProcessUtil.isScanPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccBox;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSur;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import ntx.ts.sysproc.UserContext;

/**
 *
 * @author amolchanov
 */
public class ProcessOpisSurov extends ProcessTask {
    
  private final OpisSurovData d = new OpisSurovData();

  public ProcessOpisSurov(long procId) throws Exception {
    super(ProcType.OPIS_SUROV, procId);
  }
    
    
  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));    
    
    if (getTaskState() == TaskState.START && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_SKL, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }    
      return htmlWork("Опись суровья", false, ctx);
  }

  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (getTaskState() == TaskState.SEL_SKL) { //SEL_STELL) {   
        if (isAllDigitsComma(scan)) {            
// Проверка склада - Z_TS_OPISSUROV_LGORT
          Z_TS_OPISSUROV_LGORT ff = new Z_TS_OPISSUROV_LGORT();
          ff.W_LGORT = scan;
          ff.execute();
          if (ff.isErr) {  
            callSetErr(ff.err, ctx);
          }
          else {  
          callSetMsg("Склад:" + scan, ctx); 
          d.callSetLGORT(scan, TaskState.SEL_OPIS, this, ctx); 
          }        
          return htmlWork("Опись суровья", true, ctx);
        }
    }
 
    
    if (getTaskState() == TaskState.SEL_OPIS) {

          Z_TS_OPISSUROV_OPIS ff1 = new Z_TS_OPISSUROV_OPIS();
          ff1.W_OPIS = scan;
          ff1.execute();
          if (ff1.isErr) {  
            callSetErr(ff1.err, ctx);
          }
          else {                  
            callSetMsg("Склад:" + d.getLGORT() + "Опись:" + scan, ctx);         
            d.callSetOPIS(scan, TaskState.SEL_SHK, this, ctx);  
    }
//          callSetMsg("Опись: " + scan, ctx); 
//          d.callSetChargPU(scan, TaskState.SEL_SHK, this, ctx);  

          return htmlWork("Опись суровья", true, ctx);    
    }    
  

    if (d.scanIsDouble(scan)) {
      callSetErr("ШК дублирован (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Опись суровья", true, ctx);
    }
    
//    if (isAllDigitsComma(scan)) {
    if (isScanSur(scan)) {
//       d.callAddScan(scan, this, ctx);                    
//       String s = scan;
//       callAddHist(s, ctx);

        
        Z_TS_OPISSUROV_SHK f = new Z_TS_OPISSUROV_SHK();
        
        f.W_SHK = scan;
        f.W_OPIS = d.getOPIS();
        f.W_LGORT = d.getLGORT();
        f.execute();
        
        if (f.isErr) {
          callSetErr(f.err, ctx);
        }
        else {        
          BigDecimal w_qty = f.QTY;
//          BigDecimal w_qty = d.getSummaQty();
          
//          String s = scan;
          String s = scan + "/" + w_qty.toString();          
          callAddHist(s, ctx);

          String sInfo = f.INFO;
                   
          d.callAddScan(scan, this, ctx);                    
          callSetMsg("Склад:" + d.getLGORT() + "; Опись:" + d.getOPIS() + "; " + sInfo + "  " + d.getSummaQty() + "[" + Integer.toString(d.getNScan()) + "]", ctx);       

//          callSetMsg("Склад:" + d.getLGORT() + "; Опись:" + d.getOPIS() + "; " + f.INFO + "[" + Integer.toString(d.getNScan()) + "]", ctx);       
     
        }
    }
    else {
      callSetErr("Неизвестный тип ШК (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Опись суровья", true, ctx);        
    }
  
/*           
    if (isScanMkSn(scan) || isScanMkPb(scan) || isScanSsccBox(scan) || 
        isScanSsccPal(scan) || isScanTov(scan) || isScanSur(scan) ||
        isScanPal(scan) ) {
      if (isScanSur(scan)) {
//        Z_TS_SHKLIST_IS_OPIS f = new Z_TS_SHKLIST_IS_OPIS();
//
//        f.SHK = scan;
//        f.W_OPIS = d.getOpis();
//        f.execute();

//        if (f.isErr) {
//          callSetErr(f.err, ctx);
//          return htmlWork("Опись суровья", true, ctx);
//        }
//        else {
          callAddHist(scan, ctx);
          d.callAddScan(scan, this, ctx);            
//        }        

//        BigDecimal d_qty = d.getSummaQty();       
//        callSetMsg(f.INF + " " + "[" + d_qty.toString() + "/" + Integer.toString(d.getNScan()) + "]", ctx);       
      } else
        callSetMsg("В коробе " + Integer.toString(d.lastBoxScanCount()) + " СН" +
              " (" + d.lastBoxMatCount() + ") " + 
              Integer.toString(d.getBoxCount()) + "-" +
              Integer.toString(d.getGoodCount()), 
              ctx);
      callTaskNameChange(ctx);
      return htmlWork("Опись суровья", true, ctx);
    } else {
      callSetErr("Неизвестный тип ШК (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Опись суровья", true, ctx);
    }*/

  return htmlWork("Опись суровья", false, ctx);  
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;fin:Завершить";

    if (d.getNScan() > 0) {
      definition = definition + ";save:Сохранить;del_last:Отменить последнее сканирование";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Опись суровья",
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
    } else if (menu.equals("save")) {       

/*        callSetTaskState(TaskState.SEL_OPIS, ctx);             
        menu = null; 
        callDelHist(ctx);
        d.callClearScanData(this, ctx);
*/        

        Z_TS_OPISSUROV_SHKLIST_SAVE f = new Z_TS_OPISSUROV_SHKLIST_SAVE();        
        f.W_LGORT = d.getLGORT();
        f.W_OPIS = d.getOPIS();
        f.USER_SHK = ctx.user.getUserSHK();
        int nn = d.getScanDataCount();
        String sd;
        if (nn > 0) {
          f.IT_create(nn);
          for (int i = 0; i < nn; i++) {
            sd = d.getScanDataItem(i);
            f.IT[i].SHK = sd;
          }
        }
        f.execute();
        if (f.isErr) {
          callSetErr(f.err, ctx);
          return htmlWork("Опись суровья", true, ctx);
        }
        else {
// Сохранение стеллажа
            callSetMsg("Создана Опись №" + f.OPIS, ctx);       

            callSetTaskState(TaskState.SEL_STELL, ctx);             
            menu = null; 
            callDelHist(ctx);
            d.callClearScanData(this, ctx);
            return htmlWork("Опись суровья", false, ctx);

/*          Z_TS_SHKLIST3_SET_STELL f1 = new Z_TS_SHKLIST3_SET_STELL();
          f1.W_STELL = d.getStell();
          f1.W_CHARG_PU = d.getCharg_PU();
          f1.USER_SHK = ctx.user.getUserSHK();
          int nn1 = d.getScanDataCount();
          String sd1;
          if (nn1 > 0) {
            f1.IT_create(nn1);
            for (int i = 0; i < nn1; i++) {
              sd1 = d.getScanDataItem(i);
              f1.IT[i].SHK = sd1;
            }
          }
          f1.execute();
          if (f1.isErr) {
            callSetErr(f1.err, ctx);
            return htmlWork("Опись суровья", true, ctx);
          }
          else {
              callSetMsg("Стеллаж №" + d.getStell() + " установлен для партии:" + d.getCharg_PU() + "   [" + Integer.toString(d.getNScan()) + "]", ctx);       

            callSetTaskState(TaskState.SEL_STELL, ctx);             
            menu = null; 
            callDelHist(ctx);
            d.callClearScanData(this, ctx);
            return htmlWork("Опись суровья", false, ctx);
          }
*/
    }
         
    } else if (menu.equals("del_last")) {

        int nn = d.getScanDataCount();
        if (nn > 0) { // отменяем сканирование товара
          String shk = d.getScanDataItem(nn-1);
          String s = "Отменено сканирование ШК " + shk;
          d.callDelLast(this, ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        }      
    }
    
    return htmlWork("Опись суровья", false, ctx);
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

//AM 12.01.2024
class OpisSurovData extends ProcData {

  private final ArrayList<String> scanData
          = new ArrayList<String>(); // отсканированные ШК
  private final HashMap<String, Integer> mapMatCount
          = new HashMap<String, Integer>(); // кол-во сканов по ОЗМ
    private String vbeln;
    private String LGORT;
    private String OPIS; //Charg_PU;

  public BigDecimal getSummaQty() {
    BigDecimal qty = new BigDecimal(0);
    BigDecimal s_qty = new BigDecimal(0);
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);

        Z_TS_OPISSUROV_SHK f = new Z_TS_OPISSUROV_SHK();
        
        f.W_SHK = scan;
        f.W_OPIS = OPIS;
        f.execute();        
        
/*        Z_TS_SHKLIST3_STELL f = new Z_TS_SHKLIST3_STELL();
        f.SHK = scan;
        f.W_CHARG_PU = Charg_PU; //d.getCharg_PU();
        f.execute();
*/        
        if (f.isErr) {
//          callSetErr(f.err, ctx);
//          return htmlWork("Опись суровья", true, ctx);
        }
        else {  
            
            qty = f.QTY;
            s_qty = s_qty.add(qty);
        }
  
    }
    return s_qty;
  }    
    
  public String getLGORT() {
     return LGORT;
  }    

  public String getOPIS() {
     return OPIS; //Charg_PU;
  }   
  
  public int getNScan() {
    //return nScan;
    return scanData.size();
  }

  public int getScanDataCount() {
    return scanData.size();
  }

  public String getScanDataItem(int idx) {
    return scanData.get(idx);
  }

  public String[] getScanData() {
    int n = scanData.size();
    String[] ret = new String[n];
    for (int i = 0; i < n; i++) ret[i] = scanData.get(i);
    return ret;
  }
  
  public void clearScanData() {
    //nScan = 0;
    scanData.clear();
  }
  
  public void callClearScanData(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, p, ctx);
  }

  public boolean scanIsDouble(String scan) {
    int n = scanData.size();
    for (int i = 0; i < n; i++) 
        if (scanData.get(i).equals(scan) && !isScanTov(scan)) return true;
    return false;
  }

  public int getGoodCount() {
    int ret = 0;
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);
        if (scan.startsWith("S")) 
            ret++; 
        else if (isScanTov(scan)) 
            ret = ret + getScanQty(scan).intValue();
    }
    return ret;
  }

  public int getBoxCount() {
    int ret = 0;
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);
        if (scan.startsWith("YN") || scan.startsWith("046")) 
            ret++; 
        else if (isScanTov(scan)) {
            if (getScanQty(scan).intValue() > 1) 
                ret++;
        }
    }
    return ret;
  }

  public int lastBoxScanCount() {
    int ret = 0;
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);
        if (scan.startsWith("S")) ret++; else return ret;
    }
    return ret;
  }

  public String lastBoxMatCount() {
    String ret = "";
    int n = scanData.size() - 1;
    String scan;
    String charg;
    mapMatCount.clear();
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);
        if (scan.startsWith("S")) {
            charg = delZeros(scan.substring(2, 9));
            RefChargStruct c = null;
            try { c = RefCharg.get(charg, null); } catch (Exception e) {}
            if (c == null) continue;
            Integer matCount = mapMatCount.get(c.matnr);
            Integer matCountNew;
            if (matCount == null) matCountNew = 1;
            else matCountNew = matCount + 1;
            mapMatCount.put(c.matnr, matCountNew);
        }
        else 
            break;
    }
    for (String matnr : mapMatCount.keySet()) {
        if (ret.isEmpty()) ret = mapMatCount.get(matnr).toString(); 
        else ret = ret + "-" + mapMatCount.get(matnr).toString(); 
    }
    return ret;
  }
  
  public void callSetLGORT(String LGORT, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(LGORT, this.LGORT)) {
      dr.setS(FieldType.LGORT, LGORT);
    }
    if ((state != null) && (state != p.getTaskState())) {

      dr.setI(FieldType.TASK_STATE, state.ordinal());
      
    }
    Track.saveProcessChange(dr, p, ctx);
  }    

  public void callSetOPIS(String Opis, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(Opis, this.OPIS)) {
      dr.setS(FieldType.OPIS, Opis);
    }
    if ((state != null) && (state != p.getTaskState())) {

      dr.setI(FieldType.TASK_STATE, state.ordinal());
      
    }
    Track.saveProcessChange(dr, p, ctx);
  }     
  
  public void callAddScan(String scan, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.SHK, scan);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, p, ctx);
  }
  
  public void callDelLast(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (scanData.size() > 0) {
      dr.setV(FieldType.DEL_LAST);
    }
    Track.saveProcessChange(dr, p, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.SHK)) {
          String shk;
          shk = (String) dr.getVal(FieldType.SHK);
          scanData.add(shk);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          clearScanData();
        }

        if (dr.haveVal(FieldType.DEL_LAST)) {
          int n = scanData.size();
          if (n > 0) {
            scanData.remove(n - 1);
          }
        }
        if (dr.haveVal(FieldType.LGORT)) {
          LGORT = (String) dr.getVal(FieldType.LGORT);
        }        

        if (dr.haveVal(FieldType.OPIS)) {
          OPIS = (String) dr.getVal(FieldType.OPIS);
        }        

        break;
    }
  }
 
//    String getVbeln() {
//    return vbeln;
//    }
    
}

