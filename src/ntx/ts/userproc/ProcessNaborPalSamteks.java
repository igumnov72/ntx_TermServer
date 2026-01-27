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
import ntx.sap.fm.Z_TS_IS_SUPPLY;
import ntx.sap.fm.Z_TS_SHKLIST2_IS_CHARG;
import ntx.sap.fm.Z_TS_SHKLIST2_IS_PAL;
import ntx.sap.fm.Z_TS_SHKLIST_GET_SUPPLY;
import ntx.sap.fm.Z_TS_SHKLIST_NEW_CHARG;
import ntx.sap.fm.Z_TS_SHKLIST_NEW_CHARGS;
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
import static ntx.ts.sysproc.ProcessUtil.isAllDigits;
import static ntx.ts.sysproc.ProcessUtil.isScanMkPb;
import static ntx.ts.sysproc.ProcessUtil.isScanMkSn;
import static ntx.ts.sysproc.ProcessUtil.isScanPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccBox;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSur;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import ntx.ts.sysproc.UserContext;

public class ProcessNaborPalSamteks extends ProcessTask {

  private final NaborPalSamteksData d = new NaborPalSamteksData();

  public ProcessNaborPalSamteks(long procId) throws Exception {
    super(ProcType.NABOR_PAL_SAMTEKS, procId);
  }
    
  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));    
    
    if (getTaskState() == TaskState.START && scan == null && menu == null) {
        callSetTaskState(TaskState.KRATN, ctx);    
    } else if (getTaskState() == TaskState.KRATN && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_CHARG, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }    
    
      return htmlWork("Набор паллет Самтекс", false, ctx);
  }

  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (getTaskState() == TaskState.KRATN) {
        if (isAllDigits(scan)) {
            
//          if (scan == "0") {
//            callSetErr("Кратность не должна быть 0", ctx); 
//          }
              callDelHist(ctx); 
              callSetMsg("Кратность:" + scan, ctx); 
          d.callSetKratn(scan, TaskState.SEL_CHARG, this, ctx);   
        
          return htmlWork("Набор паллет Самтекс", true, ctx);
        }
    }
    
    if (getTaskState() == TaskState.SEL_CHARG) {

          Z_TS_IS_CHARG_PU ff1 = new Z_TS_IS_CHARG_PU();
          ff1.W_CHARG_PU = scan;
          ff1.execute();
          if (ff1.isErr) {  
            callSetErr(ff1.err, ctx);
          }
          else {                  
            callSetMsg("Номер партии ПУ № " + ff1.W_NEW_CHARG_PU, ctx);         
            d.callSetChargPU(ff1.W_NEW_CHARG_PU, TaskState.SEL_SHK, this, ctx);   
          }
          return htmlWork("Набор паллет Самтекс", true, ctx);    
    }
   
    
    if (d.scanIsDouble(scan)) {
      callSetErr("ШК дублирован (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Набор паллет Самтекс", true, ctx);
    }
    
    if (isScanMkSn(scan) || isScanMkPb(scan) || isScanSsccBox(scan) || 
        isScanSsccPal(scan) || isScanTov(scan) || isScanSur(scan) ||
        isScanPal(scan) || 
        isScanPallet(scan) ) {
      if (isScanPallet(scan)) { // (isScanSur(scan)) {
        Z_TS_SHKLIST2_IS_PAL f = new Z_TS_SHKLIST2_IS_PAL();

//        f.SUPPLY = d.getZavoz();
        f.SHK = scan;
        f.execute();
        if (f.isErr) {
          callSetErr(f.err, ctx);
          return htmlWork("Набор паллет Самтекс", true, ctx);
        }
        else {
          callAddHist(f.INF, ctx); // scan
          d.callAddScan(scan, this, ctx);            
        }        
        
        BigDecimal d_qty = d.getSummaQty();       
        callSetMsg(f.INF + " " + "[" + d_qty.toString() + "/" + Integer.toString(d.getNScan()) + "]", ctx);
      } //else
        //callSetMsg("В коробе " + Integer.toString(d.lastBoxScanCount()) + " СН" +
        //      " (" + d.lastBoxMatCount() + ") " + 
        //      Integer.toString(d.getBoxCount()) + "-" +
        //      Integer.toString(d.getGoodCount()), 
        //      ctx);
      //callTaskNameChange(ctx);
      //return htmlWork("Набор паллет Самтекс", true, ctx);
     else {
      callSetErr("Неизвестный тип ШК (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Набор паллет Самтекс", true, ctx);
    }
  }
      return htmlWork("Набор паллет Самтекс", true, ctx);
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;fin:Завершить";

    if (d.getNScan() > 0) {
      definition = definition + ";test:Тестировать;create:Создать Партии;del_last:Отменить последнее сканирование";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Набор паллет Самтекс",
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

    } else if (menu.equals("test")) {
        Z_TS_SHKLIST_NEW_CHARGS f = new Z_TS_SHKLIST_NEW_CHARGS();        
        f.W_KRATN = d.getKratn();
        f.W_CHARG_PU = d.getChargPU();
        f.USER_SHK = ctx.user.getUserSHK();
        f.W_TEST = "X";

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
          return htmlWork("Набор паллет Самтекс", true, ctx);
        }
        else {
//          BigDecimal d_qty = d.getSummaQty();       
          callClearErr(ctx);
//          String s =  " Создана партия: " + f.W_CHARG_PU + "[" + d_qty.toString() + "/" + Integer.toString(d.getNScan()) + "]";
//          callAddHist(s, ctx);
//          callSetMsg(s, ctx);

      
          callAddHist("----------", ctx);
          String zinfo = "";
          for (int i = 0; i < f.IT_INFO.length; i++) {
             zinfo = f.IT_INFO[i].ZINFO;
             callAddHist(zinfo, ctx);
          }          
          callAddHist("Будут созданы партии:", ctx);
          callAddHist("----------", ctx);      

//          d.callClearScanData(this, ctx);
          callSetTaskState(TaskState.SEL_SHK, ctx); 
//          menu = null;
          
          return htmlWork("Набор паллет Самтекс", false, ctx);
        }                   
    } else if (menu.equals("create")) {
        Z_TS_SHKLIST_NEW_CHARGS f = new Z_TS_SHKLIST_NEW_CHARGS();        
        f.W_KRATN = d.getKratn();
        f.W_CHARG_PU = d.getChargPU();
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
          return htmlWork("Набор паллет Самтекс", true, ctx);
        }
        else {
          BigDecimal d_qty = d.getSummaQty();       
          callClearErr(ctx);
  //        String s =  " Создана партия: " + f.W_CHARG_PU + "[" + d_qty.toString() + "/" + Integer.toString(d.getNScan()) + "]";
          
          callAddHist("----------", ctx);
          String zinfo = "";
          for (int j = 0; j < f.IT_INFO.length; j++) {
             zinfo = f.IT_INFO[j].ZINFO;
             callAddHist(zinfo, ctx);
          }          
          callAddHist("СОЗДАНЫ ПАРТИИ:", ctx);
          callAddHist("----------", ctx);             
          
   //       callAddHist(s, ctx);
   //       callSetMsg(s, ctx);
          d.callClearScanData(this, ctx);
          callSetTaskState(TaskState.KRATN, ctx); 
          menu = null;
          
          return htmlWork("Набор паллет Самтекс", false, ctx);
        }
    } 
    else if (menu.equals("del_last")) {

        int nn = d.getScanDataCount();
        if (nn > 0) { // отменяем сканирование товара
          String shk = d.getScanDataItem(nn-1);
          String s = "Отменено сканирование ШК " + shk;
          d.callDelLast(this, ctx);
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        }      
    }
    
    return htmlWork("Набор паллет Самтекс", false, ctx);

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

class NaborPalSamteksData extends ProcData {

  //private int nScan = 0;
  private final ArrayList<String> scanData
          = new ArrayList<String>(); // отсканированные ШК
  private final HashMap<String, Integer> mapMatCount
          = new HashMap<String, Integer>(); // кол-во сканов по ОЗМ
    private String Kratn;
    private String Zavoz;
    private String Charg_PU;

 /* public BigDecimal getSummaQty() {
    BigDecimal qty = new BigDecimal(0);
    BigDecimal s_qty = new BigDecimal(0);
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);
        if (scan.length() == 15) {

          switch (scan.charAt(4)) {
          case 'Z':
            BigDecimal ret = new BigDecimal(Long.parseLong(scan.substring(0,4)));
            ret = ret.setScale(3);
            ret = ret.divide(new BigDecimal(10));
            s_qty = s_qty.add(ret);
            break;
          }
       
        }
    }
    return s_qty;*/
  
  public BigDecimal getSummaQty() {
    BigDecimal qty = new BigDecimal(0);
    BigDecimal s_qty = new BigDecimal(0);
    int n = scanData.size() - 1;
    String scan;
    for (int i = n; i >= 0; i--) {
        scan = scanData.get(i);

        Z_TS_SHKLIST2_IS_PAL f = new Z_TS_SHKLIST2_IS_PAL();
        
        f.SHK = scan;
        f.execute();        
        
/*        Z_TS_SHKLIST3_STELL f = new Z_TS_SHKLIST3_STELL();
        f.SHK = scan;
        f.W_CHARG_PU = Charg_PU; //d.getCharg_PU();
        f.execute();
*/        
        if (!f.isErr) {              
            qty = f.QTY;
            s_qty = s_qty.add(qty);
        }
//        else {
//            callSetErr(f.err, ctx);
//            return htmlWork("Опись суровья", true, ctx);
//        }  
    }
    return s_qty;  
  
  }
    
   
    public String getChargPU() {
     return Charg_PU;
  }    

    public String getZavoz() {
     return Zavoz;
  }    

    public String getKratn() {
     return Kratn;
  }    
    
  public int getNScan() {
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
  
  public void callSetZavoz(String Zavoz, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(Zavoz, this.Zavoz)) {
      dr.setS(FieldType.ZAVOZ, Zavoz);

//    if (!strEq(Zavoz, this.Zavoz)) {
//      dr.setS(FieldType.ZAVOZ, Zavoz);
    }
    if ((state != null) && (state != p.getTaskState())) {

      dr.setI(FieldType.TASK_STATE, state.ordinal());
      
    }
    Track.saveProcessChange(dr, p, ctx);
  }    

  
  public void callSetKratn(String Kratn, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(Kratn, this.Kratn)) {
      dr.setS(FieldType.KRATN, Kratn);

//    if (!strEq(Zavoz, this.Zavoz)) {
//      dr.setS(FieldType.ZAVOZ, Zavoz);
    }
    if ((state != null) && (state != p.getTaskState())) {

      dr.setI(FieldType.TASK_STATE, state.ordinal());
      
    }
    Track.saveProcessChange(dr, p, ctx);
  }    
  
  public void callSetChargPU(String Charg_PU, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(Charg_PU, this.Charg_PU)) {
      dr.setS(FieldType.CHARG_PU, Charg_PU);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
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
        if (dr.haveVal(FieldType.ZAVOZ)) {
          Zavoz = (String) dr.getVal(FieldType.ZAVOZ);
        }        

        if (dr.haveVal(FieldType.CHARG_PU)) {
          Charg_PU = (String) dr.getVal(FieldType.CHARG_PU);
        }        

        if (dr.haveVal(FieldType.KRATN)) {
          Kratn = (String) dr.getVal(FieldType.KRATN);
        }        

        break;
    }
  }

    
}
