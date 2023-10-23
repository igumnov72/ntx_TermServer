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
import ntx.sap.fm.Z_TS_IS_OPIS;
import ntx.sap.fm.Z_TS_SHKLIST1;
import ntx.sap.fm.Z_TS_SHKLIST2;
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
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.delZeros;
import static ntx.ts.sysproc.ProcessUtil.getScanQty;
import static ntx.ts.sysproc.ProcessUtil.getScanVbeln;
import static ntx.ts.sysproc.ProcessUtil.isScanMkPb;
import static ntx.ts.sysproc.ProcessUtil.isScanMkSn;
import static ntx.ts.sysproc.ProcessUtil.isScanPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccBox;
import static ntx.ts.sysproc.ProcessUtil.isScanSsccPal;
import static ntx.ts.sysproc.ProcessUtil.isScanSur;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import static ntx.ts.sysproc.ProcessUtil.isScanVbeln;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 *
 * @author amolchanov
 */
public class ProcessOpisSamteks extends ProcessTask {

  private final OpisSamteksData d = new OpisSamteksData();
//    public String zvv_opis;

  public ProcessOpisSamteks(long procId) throws Exception {
    super(ProcType.OPIS_SAMTEKS, procId);
  }
    
  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));    
    
    if (getTaskState() == TaskState.START && scan == null && menu == null) {
        callSetTaskState(TaskState.SEL_OPIS, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }    
    
//    if (scan != null) {
//      if (!scan.equals("00")) {
//        callClearErrMsg(ctx);
//      }
//      return handleScan(scan, false, ctx);
//    } else if (menu != null) {
//      return handleMenu(menu, ctx);
//    }
      return htmlWork("Опись Самтекс", false, ctx);
  }

  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (getTaskState() == TaskState.SEL_OPIS) {
    
//        if (isAllDigits(scan)) {
        if (isAllDigitsComma(scan)) {
            
          Z_TS_IS_OPIS ff = new Z_TS_IS_OPIS();
          ff.W_OPIS = scan;
          ff.execute();
          if (ff.isErr) {  
            callSetErr(ff.err, ctx);
//            callSetErr("Опись " + scan + " не найдена", ctx);
          }
          else {  
          callSetMsg("ОПИСЬ № " + scan, ctx); 
//          d.zvv_opis = scan;
          d.callSetOpis(scan, TaskState.SEL_SHK, this, ctx);
//          callSetTaskState(TaskState.SEL_SHK, ctx);

          }
        
          return htmlWork("Опись Самтекс", true, ctx);
        }
    }

    if (d.scanIsDouble(scan)) {
      callSetErr("ШК дублирован (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Опись Самтекс", true, ctx);
    }
    
    if (isScanMkSn(scan) || isScanMkPb(scan) || isScanSsccBox(scan) || 
        isScanSsccPal(scan) || isScanTov(scan) || isScanSur(scan) ||
        isScanPal(scan) ) {
//      callAddHist(scan, ctx);
//      //d.callAddNScan(this, ctx);
//      d.callAddScan(scan, this, ctx);
      if (isScanSur(scan)) {
//        Z_TS_SHKLIST2 f = new Z_TS_SHKLIST2();
        Z_TS_SHKLIST_IS_OPIS f = new Z_TS_SHKLIST_IS_OPIS();

        f.SHK = scan;
        f.W_OPIS = d.getOpis();
        f.execute();
        if (f.isErr) {
          callSetErr(f.err, ctx);
          return htmlWork("Опись Самтекс", true, ctx);
        }
        else {
          callAddHist(scan, ctx);
          //d.callAddNScan(this, ctx);
          d.callAddScan(scan, this, ctx);            
        }        

        BigDecimal d_qty = d.getSummaQty();       
        callSetMsg(f.INF + " " + "[" + d_qty.toString() + "/" + Integer.toString(d.getNScan()) + "]", ctx);
        
//        callSetMsg(f.INF, ctx);
      } else
        callSetMsg("В коробе " + Integer.toString(d.lastBoxScanCount()) + " СН" +
              " (" + d.lastBoxMatCount() + ") " + 
              Integer.toString(d.getBoxCount()) + "-" +
              Integer.toString(d.getGoodCount()), 
              ctx);
      callTaskNameChange(ctx);
      return htmlWork("Опись Самтекс", true, ctx);
    } else {
      callSetErr("Неизвестный тип ШК (сканирование " + scan + " не принято)", ctx);
      return htmlWork("Опись Самтекс", true, ctx);
    }
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;fin:Завершить";

    if (d.getNScan() > 0) {
//      definition = definition + ";save:Сохранить;del_last:Отменить последнее сканирование";
      definition = definition + ";check:Проверить;del_last:Отменить последнее сканирование";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Опись Самтекс",
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
    } else if (menu.equals("check")) {

//      String W_opis = d.getOpis();
        
// AM
//        if (!isScanVbeln(menu)) {
//          callSetErr("Требуется отсканировать ШК поставки (сканирование " + menu + " не принято)", ctx);
//        return htmlWork("Опись Самтекс", true, ctx);
// AM
        
        
//        Z_TS_SHKLIST1 f = new Z_TS_SHKLIST1();
        Z_TS_SHKLIST_OPIS_CHECK f = new Z_TS_SHKLIST_OPIS_CHECK();        
//        f.W_OPIS = W_opis; //d.zvv_opis;
        f.W_OPIS = d.getOpis();
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
          return htmlWork("Опись Самтекс", true, ctx);
        }
        else {
//        String s = "Сохранен список " + delZeros(f.SHKLIST);
          String s = "Опись " + f.W_OPIS + " проверена."; //d.zvv_opis + // + delZeros(f.SHKLIST);
          callAddHist(s, ctx);
          
          callAddHist("----------", ctx);

          String zinfo = "";
          for (int i = 0; i < f.IT_INFO.length; i++) {
             zinfo = f.IT_INFO[i].ZINFO;
             callAddHist(zinfo, ctx);
          }          
          callAddHist("----------", ctx);
          
          callSetMsg(s, ctx);
        }
        d.callClearScanData(this, ctx);
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
    
    return htmlWork("Опись Самтекс", false, ctx);
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

class OpisSamteksData extends ProcData {

  //private int nScan = 0;
  private final ArrayList<String> scanData
          = new ArrayList<String>(); // отсканированные ШК
  private final HashMap<String, Integer> mapMatCount
          = new HashMap<String, Integer>(); // кол-во сканов по ОЗМ
    private String vbeln;
//    String zvv_opis;
    private String opis;

  public BigDecimal getSummaQty() {
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

//            ret = ret.divide(new BigDecimal(1000));
//            qty = expParams.getBigDecimal("ret"); 
            s_qty = s_qty.add(ret);
//            s_qty = s_qty + ret;
            break;
          }

//        if (scan.startsWith("Z")) 
        
        }
    }
/*          switch (scan.charAt(5)) {
          case 'Z':
//            ret = ret.divide(new BigDecimal(1000));
            qty = 0.1; 
            s_qty = s_qty + qty;
            break;
          }    
  */  
    return s_qty;
  }    
    
  public String getOpis() {
//    if (opis.startsWith("I")) return "Приход";
//    else if (opis.startsWith("O")) return "Расход";
    //else return opis
     return opis;
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
  
/*  
  public void callAddNScan(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setI(FieldType.N_SCAN, nScan + 1);
    Track.saveProcessChange(dr, p, ctx);
  }
*/
  public void callSetOpis(String opis, TaskState state, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    if (!strEq(opis, this.opis)) {
      dr.setS(FieldType.OPIS, opis);
//      dr.setI(FieldType.LOG, LogType.OP.ordinal());
    }
    if ((state != null) && (state != p.getTaskState())) {

//callSetTaskState(TaskState.SEL_SHK, ctx);        
        
//      dr.setI(FieldType.OPIS, state.ordinal());
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
          /*
        if (dr.haveVal(FieldType.N_SCAN)) {
          nScan = (Integer) dr.getVal(FieldType.N_SCAN);
        }
          */

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
        if (dr.haveVal(FieldType.OPIS)) {
          opis = (String) dr.getVal(FieldType.OPIS);
        }        

        break;
    }
  }
 
    String getVbeln() {
    return vbeln;
    }

//    String getOpis() {
//    return zvv_opis;
//    }    
    
}
