package ntx.ts.userproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import ntx.sap.struct.*;
import ntx.sap.fm.Z_TS_CVL1;
import ntx.sap.fm.Z_TS_CVL2;
import ntx.sap.fm.Z_TS_CVL3;
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
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import static ntx.ts.sysproc.ProcData.strEq;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.delDecZeros;
import static ntx.ts.sysproc.ProcessUtil.delZeros;
import static ntx.ts.sysproc.ProcessUtil.fillZeros;
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
        callSetTaskState(TaskState.K_VBELN, ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), false, ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    return htmlWork("Комплектация КП", false, ctx);
  }

  private FileData htmlShowCompl(UserContext ctx, String cell) throws Exception {
    // отображение нескомплектованных позиций

    Z_TS_CVL3 f = new Z_TS_CVL3();

    f.CVL = d.getVbeln();
    f.LGPLA = cell;

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlWork("Комплектация КП", true, ctx);
    }

    int n = f.IT.length;
    if (n == 0) {
      callSetMsg("Нескомплектованных позиций нет", ctx);
      return htmlWork("Комплектация КП", true, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Нескомплектованные позиции";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Нескомплектованные позиции:</b>");
    p.addNewLine();
    String s;
    ZCVL_MV r;
    for (int i = 0; i < n; i++) {
      r = f.IT[i];
      r.MATNR = delZeros(r.MATNR);
      r.CHARG = delZeros(r.CHARG);

      s = "<b><font color=red>" + r.LGPLA + "</font>";
      s = s + " " + r.MATNR;
      s = s + " / " + r.CHARG;
      s = s + "</b> " + r.MAKTX + " <b>"
              + delDecZeros(r.TO_PICK.toString()) + " ед</b>";
      p.addLine(s);
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }
  
  public FileData handleScan(String scan, boolean isHtext, UserContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }
    
    if (getTaskState() == TaskState.K_VBELN) {
      Z_TS_CVL1 f = new Z_TS_CVL1();
      f.CVL = scan;
      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlWork("Комплектация КП", true, ctx);
      }

      d.callSetVbeln(scan, TaskState.SEL_CELL, this, ctx);
      String s = "Выбрана КП " + scan;
      callAddHist(s, ctx);
      callSetMsg(s, ctx);
    } else
    if ((getTaskState() == TaskState.SEL_CELL) || 
        (getTaskState() == TaskState.TOV_CELL && isScanCell(scan))){
      if (!isScanCell(scan)){
        callSetErr("Скан " + scan + " не является ШК ячейки", ctx);
        return htmlWork("Комплектация КП", true, ctx);
      }
      String cell = scan.substring(1);
      if (!d.getCell().equals(cell) && d.getShkCount() > 0) {

        Z_TS_CVL2 f = new Z_TS_CVL2();
        f.CVL = d.getVbeln();
        f.LGPLA = d.getCell();
        f.SAVE = "X";

        int nn = d.getShkCount();
        if (nn > 0) {
          f.IT_create(nn);
          for (int i = 0; i < nn; i++) {
            f.IT[i].SHK = d.getShk(i);
          }
        }

        f.execute();

        if (f.isErr) {
          callSetErr(f.err, ctx);
          return htmlWork("Комплектация КП", true, ctx);
        }

        String s = "Сохранено " + d.getShkCount() + " ШК";
        d.callClearShkList(this, ctx);
        callAddHist(s, ctx);
        callSetMsg(s, ctx);
          
      }
      d.callSetCell(cell, TaskState.TOV_CELL, this, ctx);
      String s = "Выбрана ячейка " + cell;
      callAddHist(s, ctx);
      callSetMsg(s, ctx);
    } else
    if (getTaskState() == TaskState.TOV_CELL) {
      if (!isScanTovMk(scan)){
        callSetErr("Скан " + scan + " не является ШК товара", ctx);
        return htmlWork("Комплектация КП", true, ctx);
      }
        
      Z_TS_CVL2 f = new Z_TS_CVL2();
      f.CVL = d.getVbeln();
      f.LGPLA = d.getCell();
      f.SHK = scan;

      int nn = d.getShkCount();
      if (nn > 0) {
        f.IT_create(nn);
        for (int i = 0; i < nn; i++) {
          f.IT[i].SHK = d.getShk(i);
        }
      }

      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlWork("Комплектация КП", true, ctx);
      }

      d.callAddShk(scan, TaskState.TOV_CELL, this, ctx);
      String s = "Добавлен товар " + scan;
      callAddHist(s, ctx);
      callSetMsg(s, ctx);
    } 
    
    return htmlWork("Комплектация КП", true, ctx);
  }

  public FileData htmlMenu() throws Exception {
    String definition = "cont:Назад;later:Отложить;vedall:Ведомость комплектации";
    
    if (d.getCell().length() > 0) {
      definition = definition + ";vedcell:Ведомость по ячейке";
    }

    if (d.getShkCount() > 0) {
      definition = definition + 
        ";dellast:Отменить последнее сканирование товара" + 
        ";delall:Отменить всё несохраненное (по ячейке и поставке)" ;
    } else {
      definition = definition + ";fin:Завершить";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", "Комплектация КП",
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
    } else if (menu.equals("dellast")) {
      callAddHist("Удален последний скан", ctx);
      d.callClearLastShk(this, ctx);
      return htmlWork("Прогресс опись", true, ctx);
    } else if (menu.equals("delall")) {
      callAddHist("Удалены все несохраненные сканы", ctx);
      d.callClearShkList(this, ctx);
      return htmlWork("Прогресс опись", true, ctx);
    } else if (menu.equals("vedall")) {
      return htmlShowCompl(ctx, "");
    } else if (menu.equals("vedcell")) {
      return htmlShowCompl(ctx, d.getCell());
    }
    
    return htmlWork("Комплектация КП", false, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    if (d.getShkCount() == 0) {
      return super.procName();
    }
    return getProcType().text + " (" + d.getShkCount() + ") " + df2.format(new Date(getProcId()));
  }
}

class CDlvMoveData extends ProcData {

  private String vbeln = "";
  private String cell = "";
  private final ArrayList<String> shkList
          = new ArrayList<String>(); // отсканированные ШК

  public int getShkCount() {
    return shkList.size();
  }

  public String getShk(int idx) {
    return shkList.get(idx);
  }

  public String getVbeln() {
    return vbeln;
  }

  public String getCell() {
    return cell;
  }

  public ArrayList<String> getShkList() {
    return shkList;
  }
  
  public void clearShkList() {
    shkList.clear();
  }
  
  public void callClearLastShk(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callClearShkList(ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setV(FieldType.CLEAR_TOV_DATA);
    Track.saveProcessChange(dr, p, ctx);
  }

  public void callAddShk(String shk, TaskState state, 
          ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.SHK, shk);
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

    public void callSetCell(String cell, TaskState state, 
          ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.CELL, cell);
    if ((state != null) && (state != p.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, p, ctx);
  }

    public void callSetVbeln(String vbeln, TaskState state, 
          ProcessTask p, UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = p.getProcId();
    dr.setS(FieldType.VBELN, vbeln);
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
        if (dr.haveVal(FieldType.SHK)) {
          String shk = (String) dr.getVal(FieldType.SHK);
          shkList.add(shk);
        }

        if (dr.haveVal(FieldType.CELL)) {
          cell = (String) dr.getVal(FieldType.CELL);
        }

        if (dr.haveVal(FieldType.VBELN)) {
          vbeln = (String) dr.getVal(FieldType.VBELN);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          clearShkList();
        }

        if (dr.haveVal(FieldType.DEL_LAST)) {
          int n = shkList.size();
          if (n > 0) {
            shkList.remove(n - 1);
          }
        }

        break;
    }
  }
}
