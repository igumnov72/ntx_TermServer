package ntx.ts.userproc;

import java.util.Date;
import ntx.sap.fm.Z_TS_DESCR;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
import ntx.sap.refs.RefInfo;
import ntx.sap.refs.RefMat;
import ntx.sap.refs.RefMatStruct;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanCharg;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * тестовый процесс на терминале
 */
public class ProcessTest extends ProcessTask {

  private final TestData d = new TestData();

  public ProcessTest(long procId) throws Exception {
    super(ProcType.TEST, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    String htext1 = (tq.params == null ? null : tq.params.getParNull("htext1"));

    if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan, false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    return htmlWork("Тестовая задача", false, ctx);
  }

  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (scan.length() == 6) {
      return htmlMatFoto(scan);
    }
    
    String extra_inf = "";
    int char_code;
    if (scan.length() > 40) {
        for (int i=0; i < scan.length(); i++) {
          char_code = (int) scan.charAt(i);
          extra_inf = extra_inf + " " + String.valueOf(char_code);
        }
        callSetErr(extra_inf, ctx);
        return htmlWork("Тестовая задача", true, ctx);
        /*
      char_code = (int) scan.charAt(7);
      extra_inf = extra_inf + "+" + String.valueOf(char_code) + "- ";
      if (scan.charAt(7) == ':') {
        RefMatStruct mat = RefMat.get(scan.substring(1, 7));
        if (mat == null) extra_inf = extra_inf + "+mat:" + scan.substring(1, 7) + "-";
        else 
        {
          int i_scan = mat.name.length() + 10;
          String qty = "";
          while (scan.charAt(i_scan) != ' ' && i_scan < scan.length()) {
              qty = qty + scan.charAt(i_scan);
              i_scan++;
          }
          extra_inf = extra_inf + "+qty:" + qty + "-";
          //String.valueOf(qty);
        }
    } else
          extra_inf = "+ss:" + scan.charAt(7) + "-";
*/
    }

    Z_TS_DESCR f = new Z_TS_DESCR();
    f.SHK = scan;
    f.execute();

    if (!f.isErr && f.DESCR.startsWith("?")) {
      callSetErr("Неизвестный штрих-код: " + scan + extra_inf, ctx);
    } else if (!f.isErr) {
      callAddHist(f.DESCR, ctx);
      callSetMsg(f.DESCR, ctx);
      d.callAddNScan(this, ctx);
      if (isScanTovMk(scan)) {

        ScanChargQty scanInf; 
        scanInf = getScanChargQty(scan);
        if (!scanInf.err.isEmpty()) {
          callSetErr(scanInf.err + " (сканирование " + scan + " не принято)", ctx);
          return htmlWork("Тестовая задача", true, ctx);
        }

        String charg = scanInf.charg;// getScanCharg(scan);
        RefChargStruct c = RefCharg.get(charg, null);
        if (c != null) d.callSetMatnr(c.matnr, this, ctx);
      }
      callTaskNameChange(ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlWork("Тестовая задача", true, ctx);
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;fin:Завершить";

    if (RefInfo.haveInfo(ProcType.TEST)) {
      definition = definition + ";manuals:Инструкции";
    }

    String m = d.getMatnr();
    if (!m.isEmpty() && isAllDigits(m)) {
      definition = definition + ";foto:Фото";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Тестовая задача",
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
    } else if (menu.equals("foto")) {
      return htmlMatFoto(d.getMatnr());
    }
    return htmlWork("Тестовая задача", false, ctx);
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

class TestData extends ProcData {

  private int nScan = 0;
  private String matnr = "";

  public int getNScan() {
    return nScan;
  }

  public String getMatnr() {
    return matnr;
  }

  public void callAddNScan(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setI(FieldType.N_SCAN, nScan + 1);
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callSetMatnr(String matnr, ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    Track.saveProcessChange(dr, p, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.N_SCAN)) {
          nScan = (Integer) dr.getVal(FieldType.N_SCAN);
        }
        if (dr.haveVal(FieldType.MATNR)) {
          matnr = (String) dr.getVal(FieldType.MATNR);
        }
        break;
    }
  }
}
