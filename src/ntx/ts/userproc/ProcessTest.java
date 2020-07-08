package ntx.ts.userproc;

import java.util.Date;
import ntx.sap.fm.Z_TS_DESCR;
import ntx.sap.refs.RefCharg;
import ntx.sap.refs.RefChargStruct;
import ntx.sap.refs.RefInfo;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanCharg;
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

    Z_TS_DESCR f = new Z_TS_DESCR();
    f.SHK = scan;
    f.execute();

    if (!f.isErr && f.DESCR.startsWith("?")) {
      callSetErr("Неизвестный штрих-код: " + scan, ctx);
    } else if (!f.isErr) {
      callAddHist(f.DESCR, ctx);
      callSetMsg(f.DESCR, ctx);
      d.callAddNScan(this, ctx);
      if (isScanTov(scan)) {
        String charg = getScanCharg(scan);
        RefChargStruct c = RefCharg.get(charg, null);
        d.callSetMatnr(c.matnr, this, ctx);
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
