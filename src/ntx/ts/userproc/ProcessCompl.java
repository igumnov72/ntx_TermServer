package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.*;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Комплектация поставки Состояния: START SEL_SKL SEL_SGM VBELN CELL_VBELN
 * CNF_CROSSDOC VBELN_CELL SEL_CELL FROM_PAL_CELL TOV_CELL QTY SGM FIN_MSG
 * COMPL_TO_PAL SEL_ZONE
 */
public class ProcessCompl extends ProcessTask {

  private final ComplData d = new ComplData();

  public ProcessCompl(long procId) throws Exception {
    super(ProcType.COMPL, procId);
  }

  @Override
  public FileData handleQuery(TermQuery tq, UserContext ctxUser) throws Exception {
    // обработка инф с терминала

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    TaskContext ctx = new TaskContext(ctxUser, this);

    if (getTaskState() == TaskState.START) {
      return init(ctx);
    } else if (getTaskState() == TaskState.FIN_MSG) {
      return handleFinMsg(tq.params == null ? null : tq.params.getParNull("htext1"), ctx);
    } else if (scan != null) {
      if (!scan.equals("00")) {
        callClearErrMsg(ctx);
      }
      return handleScan(scan.toUpperCase(), ctx);
    } else if (menu != null) {
      return handleMenu(menu, ctx);
    }

    return htmlGet(false, ctx);
  }

  private FileData htmlGet(boolean playSound, TaskContext ctx) throws Exception {
    switch (getTaskState()) {
      case SEL_SKL:
        try {
          return htmlSelLgort(null, playSound, ctx);
        } catch (Exception e) {
          callSetErr(e.getMessage(), ctx);
          return (new HtmlPageMessage(getLastErr(), null, null, null)).getPage();
        }

      case ASK_SEL_SGM:
        try {
          return htmlSelIsSgm(playSound, ctx);
        } catch (Exception e) {
          callSetErr(e.getMessage(), ctx);
          return (new HtmlPageMessage(getLastErr(), null, null, null)).getPage();
        }

      case CNF_CROSSDOC:
        try {
          return htmlCnfCrossDoc(ctx);
        } catch (Exception e) {
          callSetErr(e.getMessage(), ctx);
          return (new HtmlPageMessage(getLastErr(), null, null, null)).getPage();
        }

      case QTY:
        return htmlWork("Комплектация", playSound, delDecZeros(d.getLastQty().toString()), ctx);

      case FIN_MSG:
        return (new HtmlPageMessage(getLastErr(), getLastMsg(), "fin_msg", null)).getPage();

      default:
        return htmlWork("Комплектация", playSound, ctx);
    }
  }

  public FileData htmlCnfCrossDoc(UserContext ctx) throws Exception {
    // подтверждение кроссдокинга
    HtmlPageMenu p = new HtmlPageMenu("Подтверждение кроссдокинга", "Эта поставка помечена как кроссдокинговая, подтверждаете?",
            "cnfcrossdoc0:нет, не кроссдокинг;cnfcrossdoc1:да, кроссдокинг", "cnfcrossdoc1", getLastErr(), getLastMsg(), true);
    return p.getPage();
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    boolean playSnd = false;
    String[] lgorts = getLgorts(ctx);
    if (lgorts.length == 1) {
      // единственные склад
      // сначала проверяем необходимость сканирования СГМ
      Z_TS_COMPL15 f = new Z_TS_COMPL15();
      f.LGORT = d.getLgort();
      f.execute();

      if (f.isErr) {
        HtmlPageMessage p = new HtmlPageMessage("Ошибка Z_TS_COMPL15: " + f.err, "устраните ошибку", null, null);
        return p.getPage();
      }

      if (!f.SGM.equalsIgnoreCase("X")) {
        d.callSetLgort(lgorts[0], f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.VBELN, ctx);
        callTaskNameChange(ctx);
      } else if (f.SGM_ASK.equalsIgnoreCase("X")) {
        d.callSetLgort(lgorts[0], f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.ASK_SEL_SGM, ctx);
        callTaskNameChange(ctx);
      } else {
        d.callSetLgort(lgorts[0], f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.VBELN, ctx);
        d.callSetIsSGM(true, TaskState.VBELN, ctx);
        callTaskNameChange(ctx);
      }
    } else {
      callSetTaskState(TaskState.SEL_SKL, ctx);
    }
    return htmlGet(playSnd, ctx);
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu(ctx);
    }

    switch (getTaskState()) {
      case VBELN:
        return handleScanVbeln(scan, ctx);

      case CELL_VBELN:
        return handleScanCellVbeln(scan, ctx);

      case SEL_CELL:
        return handleScanCell0(scan, ctx);

      case VBELN_CELL:
        return handleScanVbelnCell(scan, ctx);

      case FROM_PAL_CELL:
        return handleScanPalCell(scan, ctx);

      case TOV_CELL:
        return handleScanTovCell(scan, ctx);

      case QTY:
        return handleScanQty(scan, ctx);

      case SGM:
        return handleScanSgm(scan, ctx);

      case TO_MOD_SGM:
        return handleScanSgmMod(scan, ctx);

      case MOD_SGM:
        return handleScanSgmModTov(scan, ctx);

      case MOD_SGM_QTY:
        return handleScanSgmModQty(scan, ctx);

      case COMPL_TO_PAL:
        return handleScanToPal(scan, ctx);

      case SEL_ZONE:
        return handleScanZone(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  @Override
  public String getActionText() {
    TaskState st = getTaskState();
    switch (st) {
      case FROM_PAL_CELL:
        if (canScanVbeln()) {
          return "С паллеты (или поставка, или ячейка):";
        } else {
          return "С паллеты (или ячейка):";
        }
      case TOV_CELL:
        boolean sVbel = canScanVbeln();
        boolean sPal = canScanPal();
        if (sVbel && sPal) {
          return "Товар (или паллета, или поставка, или ячейка):";
        } else if (sVbel) {
          return "Товар (или поставка, или ячейка):";
        } else if (sPal) {
          return "Товар (или паллета, или ячейка):";
        } else {
          return "Товар (или ячейка):";
        }
      default:
        return st.actionText;
    }
  }

  private ComplRetErrCrossdoc loadVedCompl(String a_vbeln, String a_cell, TaskContext ctx) throws Exception {
    // загружаем ведомость комплектации по ячейке и поставке

    Z_TS_COMPL5 f = new Z_TS_COMPL5();

    f.LGORT = d.getLgort();
    f.VBELN = fillZeros(a_vbeln, 10);
    f.LGPLA = a_cell;
    f.INF_COMPL = d.getInfCompl();

    f.execute();

    if (!f.isErr) {
      if (f.IT.length == 0) {
        d.callClearVed(ctx);
        return new ComplRetErrCrossdoc("По ячейке " + a_cell + " поставке " + a_vbeln + " комплектовать нечего", f.ZCOMP_CLIENT.equals("X"));
      }

      d.callSetVed(f.IT, f.IT_STRICT_PRT, f.IT_ALL_PRT, ctx);
      return new ComplRetErrCrossdoc(null, f.ZCOMP_CLIENT.equals("X"));
    } else {
      d.callClearVed(ctx);
      return new ComplRetErrCrossdoc(f.err, f.ZCOMP_CLIENT.equals("X"));
    }
  }

  private ComplRetErrCrossdoc loadVedComplSK(String a_vbeln, String a_cell, TaskContext ctx) throws Exception {
    // загружаем ведомость комплектации по ячейке и поставке (свободная комплектация)

    Z_TS_COMPL14 f = new Z_TS_COMPL14();

    f.LGORT = d.getLgort();
    f.VBELN = fillZeros(a_vbeln, 10);
    f.LGPLA = a_cell;

    f.execute();

    if (!f.isErr) {
      if (f.IT.length == 0) {
        d.callClearVed(ctx);
        return new ComplRetErrCrossdoc("По ячейке " + a_cell + " поставке " + a_vbeln + " комплектовать нечего", f.ZCOMP_CLIENT.equals("X"));
      }

      d.callSetCFTT(f.COMPL_FROM, f.TANUM1, f.TANUM2, a_cell, ctx);
      d.callSetVed(f.IT, f.IT_STRICT_PRT, f.IT_ALL_PRT, ctx);
//      d.callSetVedSK(f.IT, f.COMPL_FROM, a_cell, ctx);
      return new ComplRetErrCrossdoc(null, f.ZCOMP_CLIENT.equals("X"));
    } else {
      d.callClearVed(ctx);
      return new ComplRetErrCrossdoc(f.err, f.ZCOMP_CLIENT.equals("X"));
    }
  }

  private boolean canScanVbeln() {
    return !d.getIs1vbeln();
  }

  private boolean canScanPal() {
    return d.getFP();
  }

  private FileData handleScanVbeln(String scan, TaskContext ctx) throws Exception {
    // первоначальное сканирование поставки (выбор комплектуемых поставок)
    if (!isScanVbeln(scan)) {
      callSetErr("Требуется отсканировать ШК поставки в ведомости на комплектацию (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String vbeln = getScanVbeln(scan);
    if (d.haveVbeln(vbeln)) {
      callSetErr("Поставка " + vbeln + " уже выбрана для комплектации", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_COMPL4 f = new Z_TS_COMPL4();
    f.LGORT = d.getLgort();
    f.VBELN = fillZeros(vbeln, 10);
    f.INF_COMPL1 = d.getInfCompl();
    f.CHECK_COMPL1 = d.getCheckCompl();

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    boolean crossDoc = f.ZCOMP_CLIENT.equals("X");

    d.callAddVbeln(vbeln, f.IT, f.IT_FP, crossDoc ? TaskState.CNF_CROSSDOC : TaskState.CELL_VBELN, ctx);
    d.callSetInfCompl(f.INF_COMPL, ctx);
    d.callSetCheckCompl(f.CHECK_COMPL, ctx);

    String s = "Поставка " + vbeln + " выбрана для комплектации; кол-во "
            + delDecZeros(d.getVbelnQty(vbeln).toString());
    if (crossDoc) {
      s += "; ПОДТВЕРДИТЕ КРОССДОКИНГ";
      callSetMsg2(s, "", ctx);
    } else {
      callSetMsg2(s, "", ctx);
      callAddHist(s, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData cnfCrossDocDone(boolean crossDoc, TaskContext ctx) throws Exception {
    String s = "Поставка " + d.getLastVbeln() + " выбрана для комплектации; кол-во "
            + delDecZeros(d.getVbelnQty(d.getLastVbeln()).toString());
    if (crossDoc) {
      s += "; кроссдокинг подтвержден";
    } else {
      Z_TS_COMPL21 f = new Z_TS_COMPL21();
      f.VBELN = fillZeros(d.getLastVbeln(), 10);
      f.execute();
      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlGet(true, ctx);
      }

      s += "; кроссдокинг ОТМЕНЕН";
    }

    callSetMsg2(s, "", ctx);
    callAddHist(s, ctx);
    callSetTaskState(TaskState.CELL_VBELN, ctx);

    return htmlGet(false, ctx);
  }

  private FileData handleScanCellVbeln(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCell(scan, ctx, false);
    } else if (isScanVbeln(scan)) {
      return handleScanVbeln(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК ячейки или поставки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData htmlRestQtyCnf(String restQty) throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Комплектация",
            "Имеются недокомплектованные позиции по ячейке/поставке (" + restQty + "), всё равно сохранить?",
            "no:Отмена;showcell:Нет, показать недокомпл. позиции;restqty_cnf:Да, сохранить недокомпл. кол-во", "no", null, null, true);
    p.sound = "err.wav";
    return p.getPage();
  }

  private FileData handleScanCell(String scan, TaskContext ctx, boolean restQtyCnf) throws Exception {
    if (d.isFreeCompl()) {
      return handleScanCell2(scan, ctx);
    } else {
      return handleScanCell1(scan, ctx, restQtyCnf);
    }
  }

  private FileData handleScanCell1(String scan, TaskContext ctx, boolean restQtyCnf) throws Exception {
    // обработка сканирования ячейки в обычном режиме (не своб клмпл)
    // тип ШК уже проверен

    String cell = scan.substring(1);

    if (d.getScanDataSize() > 0) {
      // имеются несохраненные данные сканирования товара
      ComplRetSave sv = saveComplData(scan, "", ctx, restQtyCnf);
      if (!sv.ok) {
        if (!restQtyCnf && (sv.restQty != null)) {
          return htmlRestQtyCnf(sv.restQty);
        } else if (sv.err != null) {
          callSetErrMsg(sv.err, "Сканирование ячейки " + cell + " не принято", ctx);
          return htmlGet(true, ctx);
        }
      }

      if (!canScanVbeln() && d.getCell().equals(cell) && getTaskState() != TaskState.SEL_CELL) {
        // отсканирована та же ячейка - чисто для сохранения
        // текущей ячейки нет
        callSetStateText("", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        return htmlGet(true, ctx);
      }
    }

    String vs = d.getCellVbelns(cell); // поставки, которые нужно компл из этой ячейки, либо null
    if (vs == null) {
      callSetErr("Ячейка " + cell + " отсутствует в ведомости на комплектацию", ctx);
      callSetStateText("", ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      return htmlGet(true, ctx);
    }

    // выбрана ячейка
    String addMsg = getLastMsg();
    if (addMsg == null) {
      addMsg = "";
    } else {
      addMsg = addMsg + "<br>\r\n";
    }
    String addMsg2 = "";

    if (canScanVbeln()) {
      if (d.getFP(cell)) {
        addMsg2 = " (со сканированием ШК паллеты)";
      }
      callSetMsg2(addMsg + "Выбрана ячейка " + cell + "; поставки: " + vs + addMsg2, cell, ctx);
      callAddHist("Выбрана " + cell + "; поставки: " + vs + addMsg2, ctx);
      d.callSetCell(cell, TaskState.VBELN_CELL, ctx);
    } else {
      // загружаем ведомость комплектации по ячейке и поставке
      ComplRetErrCrossdoc ret = loadVedCompl(d.getVbeln(), cell, ctx);
      if (ret.err != null) {
        // ошибка или комплектовать нечего
        callSetErr(ret.err, ctx);
        callSetStateText("", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        return htmlGet(true, ctx);
      }

      if (d.getFP(cell)) {
        addMsg2 = " (паллеты: " + d.getPals() + ")";
      }

      callSetMsg2(addMsg + "Выбрана ячейка " + cell + " " + vs + addMsg2, cell, ctx);
      callAddHist("Выбрана " + cell + " " + vs + addMsg2, ctx);
      d.callSetCell(cell, d.getFP(cell) ? TaskState.FROM_PAL_CELL : TaskState.TOV_CELL, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanCell2(String scan, TaskContext ctx) throws Exception {
    // обработка сканирования ячейки в режиме своб клмпл
    // тип ШК уже проверен

    String cell = scan.substring(1);

//    if (d.getScanDataSize() > 0) {
    // сохранение делаем в любом случае - для сторнирования тр заказа на своб компл
    ComplRetSave sv = saveComplData(scan, "", ctx, true);
    if (sv.err != null) {
      callSetErrMsg(sv.err, "Сканирование ячейки " + cell + " не принято", ctx);
      return htmlGet(true, ctx);
    }
//      saveComplDataSK(ctx);

    // имеются несохраненные данные сканирования товара
    if (!canScanVbeln() && d.getCell().equals(cell) && getTaskState() != TaskState.SEL_CELL) {
      // отсканирована та же ячейка - чисто для сохранения
      // текущей ячейки нет
      callSetStateText("", ctx);
      callSetTaskState(TaskState.SEL_CELL, ctx);
      return htmlGet(true, ctx);
    }
//    }

    // выбрана ячейка
    String addMsg = getLastMsg();
    if (addMsg == null) {
      addMsg = "";
    } else {
      addMsg = addMsg + "<br>\r\n";
    }
    String addMsg2 = "";

    if (canScanVbeln()) {
      if (d.getFP(cell)) {
        addMsg2 = " (со сканированием ШК паллеты)";
      }
      callSetMsg2(addMsg + "Выбрана ячейка " + cell + addMsg2, cell, ctx);
      callAddHist("Выбрана " + cell + addMsg2, ctx);
      d.callSetCell(cell, TaskState.VBELN_CELL, ctx);
    } else {
      // загружаем ведомость комплектации по ячейке и поставке
      ComplRetErrCrossdoc ret = loadVedComplSK(d.getVbeln(), cell, ctx);
      if (ret.err != null) {
        // ошибка или комплектовать нечего
        callSetErr(ret.err, ctx);
        callSetStateText("", ctx);
        callSetTaskState(TaskState.SEL_CELL, ctx);
        return htmlGet(true, ctx);
      }

      if (d.getFP(cell)) {
        addMsg2 = " (паллеты: " + d.getPals() + ")";
      }

      callSetMsg2(addMsg + "Выбрана ячейка " + cell + addMsg2, cell, ctx);
      callAddHist("Выбрана " + cell + addMsg2, ctx);
      d.callSetCell(cell, d.getFP(cell) ? TaskState.FROM_PAL_CELL : TaskState.TOV_CELL, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен

    String pal = getScanPal(scan);
    String vq = d.getVbelnCellPalQty(pal);
    if ((vq == null) || vq.isEmpty()) {
      if (canScanVbeln()) {
        callSetErr("По поставке " + d.getVbeln() + " из ячейки " + d.getCell()
                + " с паллеты " + pal + " комплектовать нечего", ctx);
      } else {
        callSetErr("Из ячейки " + d.getCell() + " с паллеты "
                + pal + " комплектовать нечего", ctx);
      }
      return htmlGet(true, ctx);
    }

    // паллета проверена
    String st;
    if (canScanVbeln()) {
      st = d.getCell() + ", " + d.getVbeln() + ", " + pal;
    } else {
      st = d.getCell() + ", " + pal;
    }
    callSetMsg2("Выбрана паллета " + pal + " " + vq, st, ctx);
    callAddHist("Паллета " + pal + " " + vq, ctx);
    d.callSetLenum(pal, TaskState.TOV_CELL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanVbelnCell(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCell(scan, ctx, false);
    } else if (isScanVbeln(scan)) {
      return handleScanVbeln2(scan, ctx, false);
    } else {
      callSetErr("Требуется отсканировать ШК ячейки или поставки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanCell0(String scan, TaskContext ctx) throws Exception {
    if (isScanCell(scan)) {
      return handleScanCell(scan, ctx, false);
    } else {
      callSetErr("Требуется отсканировать ШК ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanVbeln2(String scan, TaskContext ctx, boolean restQtyCnf) throws Exception {
    if (d.isFreeCompl()) {
      return handleScanVbeln2_2(scan, ctx, restQtyCnf);
    } else {
      return handleScanVbeln2_1(scan, ctx, restQtyCnf);
    }
  }

  private FileData handleScanVbeln2_1(String scan, TaskContext ctx, boolean restQtyCnf) throws Exception {
    // обработка сканирования номера поставки при обычной комплектации
    // тип ШК уже проверен

    String vbeln = getScanVbeln(scan);

    if (d.getScanDataSize() > 0) {
      // имеются несохраненные данные сканирования товара
      ComplRetSave sv = saveComplData("", scan, ctx, restQtyCnf);
      if (!sv.ok) {
        if (!restQtyCnf && (sv.restQty != null)) {
          return htmlRestQtyCnf(sv.restQty);
        } else if (sv.err != null) {
          callSetMsg("Сканирование поставки " + vbeln + " не принято", ctx);
          return htmlGet(true, ctx);
        }
      }

      if (d.getVbeln().equals(vbeln)) {
        // отсканирована та же поставка - чисто для сохранения
        // текущей поставки нет
        callSetStateText(d.getCell(), ctx);
        callSetTaskState(TaskState.VBELN_CELL, ctx);
        return htmlGet(true, ctx);
      }
    }

    if (!d.isCellVbeln(vbeln)) {
      callSetErr("Поставка " + vbeln + " не была выбрана для комплектации", ctx);
      return htmlGet(true, ctx);
    }

    ComplRetErrCrossdoc ret = loadVedCompl(vbeln, d.getCell(), ctx);
    if (ret.err != null) {
      // ошибка или комплектовать нечего
      callSetErr(ret.err, ctx);
      callSetStateText(d.getCell(), ctx);
      callSetTaskState(TaskState.VBELN_CELL, ctx);
      return htmlGet(true, ctx);
    }

    // выбрана поставка
    String addMsg = getLastMsg();
    if (addMsg == null) {
      addMsg = "";
    } else {
      addMsg = addMsg + "<br>\r\n";
    }
    String addMsg2 = "";
    String addMsg3 = "";
    if (canScanPal()) {
      addMsg2 = " (паллеты: " + d.getPals() + ")";
    }
    if (ret.crossDoc) {
      addMsg3 = "; КРОССДОКИНГ";
    }
    String vq = d.getVbelnCellQty();
    callSetMsg2(addMsg + "Выбрана поставка " + vbeln + " " + vq + addMsg2 + addMsg3, d.getCell() + ", " + vbeln, ctx);
    callAddHist("Поставка " + vbeln + " " + vq + addMsg2 + addMsg3, ctx);
    d.callSetVbeln(vbeln, canScanPal() ? TaskState.FROM_PAL_CELL : TaskState.TOV_CELL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanVbeln2_2(String scan, TaskContext ctx, boolean restQtyCnf) throws Exception {
    // обработка сканирования номера поставки при своб комплектации
    // тип ШК уже проверен

    String vbeln = getScanVbeln(scan);

    // сохранение делаем в любом случае - для сторнирования тр заказа на своб компл
    ComplRetSave sv = saveComplData("", scan, ctx, true);
    if (sv.err != null) {
      callSetMsg("Сканирование поставки " + vbeln + " не принято", ctx);
      return htmlGet(true, ctx);
    }
//      saveComplDataSK(ctx);

    if (d.getScanDataSize() > 0) {
      // имеются несохраненные данные сканирования товара
      if (d.getVbeln().equals(vbeln)) {
        // отсканирована та же поставка - чисто для сохранения
        // текущей поставки нет
        callSetStateText(d.getCell(), ctx);
        callSetTaskState(TaskState.VBELN_CELL, ctx);
        return htmlGet(true, ctx);
      }
    }

    if (!d.isCellVbeln(vbeln)) {
      callSetErr("Поставка " + vbeln + " не была выбрана для комплектации", ctx);
      return htmlGet(true, ctx);
    }

    ComplRetErrCrossdoc ret = loadVedComplSK(vbeln, d.getCell(), ctx);
    if (ret.err != null) {
      // ошибка или комплектовать нечего
      callSetErr(ret.err, ctx);
      callSetStateText(d.getCell(), ctx);
      callSetTaskState(TaskState.VBELN_CELL, ctx);
      return htmlGet(true, ctx);
    }

    // выбрана поставка
    String addMsg = getLastMsg();
    if (addMsg == null) {
      addMsg = "";
    } else {
      addMsg = addMsg + "<br>\r\n";
    }
    String addMsg2 = "";
    String addMsg3 = "";
    if (canScanPal()) {
      addMsg2 = " (паллеты: " + d.getPals() + ")";
    }
    if (ret.crossDoc) {
      addMsg3 = "; КРОССДОКИНГ";
    }
    String vq = d.getVbelnCellQty();
    callSetMsg2(addMsg + "Выбрана поставка " + vbeln + " " + vq + addMsg2 + addMsg3, d.getCell() + ", " + vbeln, ctx);
    callAddHist("Поставка " + vbeln + " " + vq + addMsg2 + addMsg3, ctx);
    d.callSetVbeln(vbeln, canScanPal() ? TaskState.FROM_PAL_CELL : TaskState.TOV_CELL, ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleScanTovCell(String scan, TaskContext ctx) throws Exception {
    boolean sVbel = canScanVbeln();
    boolean sPal = canScanPal();

    if (isScanCell(scan)) {
      return handleScanCell(scan, ctx, false);
    } else if (isScanTov(scan)) {
      return handleScanTov(scan, ctx);
    } else if (sPal && isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (sVbel && isScanVbeln(scan)) {
      return handleScanVbeln2(scan, ctx, false);
    } else {
      String s;
      if (sVbel && sPal) {
        s = "Требуется отсканировать ШК товара, паллеты, поставки или ячейки";
      } else if (sVbel) {
        s = "Требуется отсканировать ШК товара, поставки или ячейки";
      } else if (sPal) {
        s = "Требуется отсканировать ШК товара, паллеты или ячейки";
      } else {
        s = "Требуется отсканировать ШК товара или ячейки";
      }
      callSetErr(s + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmModTov(String scan, TaskContext ctx) throws Exception {
    // сканирование товара при пересканировании СГМ

    if (!isScanTov(scan)) {
      callSetErr("Требуется отсканировать ШК товара (в СГМ) (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    try {
      String charg = getScanCharg(scan);
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      if (ctx.user.getAskQtyCompl(d.getLgort())) {
        String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        callSetMsg(s, ctx);
        d.callSetLastCharg(charg, getScanQty(scan), TaskState.MOD_SGM_QTY, ctx);
        return htmlGet(true, ctx);
      } else {
        return handleScanSgmModTovDo(charg, getScanQty(scan), ctx);
      }
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = e.toString();
        if (s == null) {
          s = "ОШИБКА ! Сообщите разработчику";
        }
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmModQty(String scan, TaskContext ctx) throws Exception {
    if (scan.isEmpty() || isScanCell(scan) || isScanPal(scan)
            || isScanTov(scan) || isScanSgm(scan) || !isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanSgmModTovDo(d.getLastCharg(), qty, ctx);
  }

  private FileData handleScanSgmModTovDo(String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    try {
      if (qty.signum() == 0) {
        callSetMsg("Сканирование товара отменено (введено кол-во 0)", ctx);
        callSetTaskState(TaskState.MOD_SGM, ctx);
        return htmlGet(true, ctx);
      }

      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии: " + charg, ctx);
        callSetTaskState(TaskState.MOD_SGM, ctx);
        return htmlGet(true, ctx);
      }
      String s = "В СГМ " + d.getModSgmNo() + ": " + c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
      d.callAddSgmTov(c.matnr, charg, qty, ctx);
      s = s + ": " + delDecZeros(qty.toString()) + " ед (" + d.getModSgmQty() + ")";
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
      callSetTaskState(TaskState.MOD_SGM, ctx);
      return htmlGet(true, ctx);
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = e.toString();
        if (s == null) {
          s = "ОШИБКА ! Сообщите разработчику";
        }
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanPalCell(String scan, TaskContext ctx) throws Exception {
    boolean sVbel = canScanVbeln();

    if (isScanCell(scan)) {
      return handleScanCell(scan, ctx, false);
    } else if (isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (!sVbel && isScanVbeln(scan)) {
      return handleScanVbeln2(scan, ctx, false);
    } else {
      String s;
      if (sVbel) {
        s = "Требуется отсканировать ШК паллеты, поставки или ячейки";
      } else {
        s = "Требуется отсканировать ШК паллеты или ячейки";
      }
      callSetErr(s + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    // тип ШК уже проверен
    try {
      if (!checkHaveToPal(ctx)) {
        return htmlGet(true, ctx);
      }

      String charg = getScanCharg(scan);
      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      ComplRetQty tq = d.getTovQty(c.matnr, charg);
      if (tq.isEzap) {
        String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        callSetErr("Эта партия относится к особому запасу, её брать нельзя: " + s, ctx);
        return htmlGet(true, ctx);
      } else if (tq.qty == null) {
        String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        callSetErr("Комплектация этого товара не нужна: " + s, ctx);
        return htmlGet(true, ctx);
      } else if (tq.qty.signum() == 0) {
        String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        callSetErr("Уже скомплектовано: " + s, ctx);
        return htmlGet(true, ctx);
      }
      if (ctx.user.getAskQtyCompl(d.getLgort())) {
        String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr)
                + ";\r\nвзять: " + delDecZeros(tq.qty.toString()) + " ед";
        callSetMsg(s, ctx);
        d.callSetLastCharg(charg, getScanQty(scan), TaskState.QTY, ctx);
        return htmlGet(true, ctx);
      } else if (d.isSGM()) {
        d.callSetLastCharg(charg, getScanQty(scan), TaskState.SGM, ctx);
        return htmlGet(true, ctx);
      } else {
        return handleScanTovDo(charg, 0, getScanQty(scan), ctx);
      }
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = e.toString();
        if (s == null) {
          s = "ОШИБКА ! Сообщите разработчику";
        }
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanQty(String scan, TaskContext ctx) throws Exception {
    if (!checkHaveToPal(ctx)) {
      return htmlGet(true, ctx);
    }

    if (scan.isEmpty() || isScanCell(scan) || isScanPal(scan)
            || isScanTov(scan) || !isNumber(scan)) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    BigDecimal qty;

    try {
      qty = new BigDecimal(scan);
    } catch (NumberFormatException e) {
      callSetErr("Требуется ввести кол-во (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    if (d.isSGM()) {
      d.callSetLastQty(qty, TaskState.SGM, ctx);
      return htmlGet(true, ctx);
    } else {
      return handleScanTovDo(d.getLastCharg(), 0, qty, ctx);
    }
  }

  private FileData handleScanSgm(String scan, TaskContext ctx) throws Exception {
    if (!checkHaveToPal(ctx)) {
      return htmlGet(true, ctx);
    }

    if (scan.isEmpty() || !isScanSgm(scan)) {
      callSetErr("Требуется отсканировать номер коробки (СГМ) или 0 (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanTovDo(d.getLastCharg(), getScanSgm(scan), d.getLastQty(), ctx);
  }

  private boolean checkHaveToPal(TaskContext ctx) throws Exception {
    if (d.getComplToPal() && d.getToPal().isEmpty()) {
      callSetErr("Сначала нужно указать паллету, на которую складывается скомплектованный товар (сканирование не принято)", ctx);
      return false;
    }
    return true;
  }

  private FileData handleScanTovDo(String charg, int sgm, BigDecimal qty, TaskContext ctx) throws Exception {
    try {
      if (!checkHaveToPal(ctx)) {
        return htmlGet(true, ctx);
      }

      if (qty.signum() == 0) {
        callSetMsg("Сканирование товара отменено (введено кол-во 0)", ctx);
        callSetTaskState(TaskState.TOV_CELL, ctx);
        return htmlGet(true, ctx);
      }

      RefChargStruct c = RefCharg.get(charg, null);
      if (c == null) {
        callSetErr("Нет такой партии: " + charg, ctx);
        callSetTaskState(TaskState.TOV_CELL, ctx);
        return htmlGet(true, ctx);
      }
      ComplRetQty tq = d.getTovQty(c.matnr, charg);
      String s = c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
      if (sgm != 0) {
        s = s + " (СГМ " + sgm + ")";
      }
      if (tq.isEzap) {
        callSetErr("Эта партия относится к особому запасу, её брать нельзя: " + s, ctx);
      } else if (tq.qty == null) {
        callSetErr("Комплектация этого товара не нужна: " + s, ctx);
      } else if (tq.qty.compareTo(BigDecimal.ZERO) == 0) {
        callSetErr("Уже полностью здесь скомплектовано: " + s, ctx);
      } else if (tq.qty.compareTo(qty) < 0) {
        if ((d.getCheckCompl().equals("X")) && (tq.qty.signum() > 0)) {
          d.callAddTov(c.matnr, charg, sgm, tq.qty, ctx);
          s = s + ": отсканировано " + delDecZeros(qty.toString())
                  + " ед, принято " + delDecZeros(tq.qty.toString()) + " ед ("
                  + d.getPalQtyScan() + ")";
          callSetMsg(s, ctx);
          callAddHist(s, ctx);
        } else {
          callSetErr("Взято " + delDecZeros(qty.toString()) + " ед, нужно не более "
                  + delDecZeros(tq.qty.toString()) + " ед\r\n" + s, ctx);
        }
      } else {
        d.callAddTov(c.matnr, charg, sgm, qty, ctx);
        s = s + ": " + delDecZeros(qty.toString()) + " ед ("
                + d.getPalQtyScan() + ")";
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
      }
      callSetTaskState(TaskState.TOV_CELL, ctx);
      return htmlGet(true, ctx);
    } catch (Exception e) {
      String s = e.getMessage();
      if (s == null) {
        s = e.toString();
        if (s == null) {
          s = "ОШИБКА ! Сообщите разработчику";
        }
      }
      callSetErr(s, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanSgmMod(String scan, TaskContext ctx) throws Exception {
    if (scan.isEmpty() || !isScanSgm(scan) || scan.equals("0")) {
      callSetErr("Требуется отсканировать номер коробки (СГМ) (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    return handleScanSgmModDo(getScanSgm(scan), ctx);
  }

  private FileData handleScanSgmModDo(int sgm, TaskContext ctx) throws Exception {
    Z_TS_COMPL18 f = new Z_TS_COMPL18();
    f.SGM = sgm;

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (!f.isErr) {
      d.callSetModGgmNo(sgm, TaskState.MOD_SGM, ctx);
      String s = "Пересканирование СГМ " + sgm;
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private ComplRetSave saveComplData(String nextCell, String nextVbeln, TaskContext ctx, boolean restQtyCnf) throws Exception {
    String cell = d.getCell();
    if ((cell == null) || cell.isEmpty()) {
      return new ComplRetSave(null, null);
    }

    if (!restQtyCnf) {
      String restQty = d.getCellRestQty();
      if (restQty != null) {
        d.callSetNextCellVbeln(nextCell, nextVbeln, ctx);
        return new ComplRetSave(null, restQty);
      }
    }

    Z_TS_COMPL3 f = new Z_TS_COMPL3();
    f.LGORT = d.getLgort();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.LGPLA = cell;
    f.USER_SHK = ctx.user.getUserSHK();
    f.IT1 = d.getCellVK();
    f.IT_DONE1 = d.getScanDataArray();
    f.IT_PAL_ZONE = d.getPalZoneArray();
    f.INF_COMPL = d.getInfCompl();
    if (canScanPal()) {
      f.COMPL_FROM = "X";
    }
    if (d.isFreeCompl()) {
      f.TANUM1 = d.getTanum1();
      f.TANUM2 = d.getTanum2();
    }
    int n_save = f.IT_DONE1.length;

    f.execute();

    if (!f.isErr) {
      if (n_save > 0) {
        String s = "Данные комплектации по поставке " + d.getVbeln() + " ячейке " + d.getCell() + " сохранены";
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
      }
      d.callSetSaved(ctx);
      return new ComplRetSave(null, null);
    } else {
      return new ComplRetSave(f.err, null);
    }
  }

  private FileData htmlMenu(TaskContext ctx) throws Exception {
    String definition;
    String askQtyMenu = "";
    if (ctx.user.isAskQtyComplEnabled(d.getLgort())) {
      askQtyMenu = ctx.user.getAskQtyCompl(d.getLgort()) ? "ask_qty_off:Выкл ввод кол-ва" : "ask_qty_on:Вкл ввод кол-ва";
    }

    String delVed = "";
    String freeComplEnter = "";
    if (d.getInfCompl().isEmpty() && !d.isFreeCompl() && !d.getNoFreeCompl()) {
      delVed = "delved:Перенос в своб компл;";
      freeComplEnter = "freecompl:Свободная комплектация;";
    }

    String freeCompl = d.isFreeCompl() ? "show_sk_cells:Показать наличие;exit_sk:Выход из своб компл;" : "";
    String toModSGM = d.isSGM() && (d.getScanDataSize() == 0) ? "to_mod_sgm:Пересканировать СГМ;" : "";
    String toPalMenu = d.getComplToPal() ? "to_pal:Компл на паллету;" : "";

    TaskState st;
    int sti = d.getToPalPrevState();
    if (sti == 0) {
      st = getTaskState();
    } else {
      st = TaskState.values()[sti];
    }

    switch (st) {
      case VBELN:
      case CELL_VBELN:
        definition = "cont:Продолжить;later:Отложить;" + freeCompl
                + "show:Показать ведомость на комплектацию;"
                + toModSGM + "fin1:Завершить;"
                + askQtyMenu;
        break;

      case VBELN_CELL:
        definition = "cont:Продолжить;later:Отложить;" + freeCompl
                + "foto:Фото материалов в ячейке;" + toPalMenu
                + "show:Показать ведомость на комплектацию;"
                + "showcell:Ведомость по ячейке;" + freeComplEnter
                + toModSGM + "fin1:Завершить;" + askQtyMenu;
        break;

      case SEL_CELL:
        definition = "cont:Продолжить;later:Отложить;" + toPalMenu + freeCompl
                + "show:Показать ведомость на комплектацию;" + freeComplEnter
                + toModSGM + "fin1:Завершить;" + askQtyMenu;
        break;

      case FROM_PAL_CELL:
      case TOV_CELL:
        if (d.getScanDataSize() == 0) {
          definition = "cont:Продолжить;later:Отложить;"
                  + "foto:Фото материалов в ячейке;" + toPalMenu
                  + "show:Показать ведомость на комплектацию;"
                  + "showcell:Ведомость по ячейке;" + delVed
                  + "showdone:Выполненная компл по ячейке;" + freeComplEnter
                  + toModSGM + "fin1:Завершить;" + askQtyMenu;
        } else {
          definition = "cont:Продолжить;later:Отложить;"
                  + "foto:Фото материалов в ячейке;" + toPalMenu
                  + "show:Показать ведомость на комплектацию;"
                  + "showcell:Ведомость по ячейке;showdone:Выполненная компл по ячейке;"
                  + "dellast:Отменить последнее сканирование товара;"
                  + "delall:Отменить всё несохраненное (по ячейке и поставке);" + askQtyMenu;
        }
        break;

      case QTY:
        definition = "cont:Продолжить;later:Отложить;dellast:Отменить последнее сканирование";
        break;

      case FIN_MSG:
        return null;

      case TO_MOD_SGM:
        definition = "cont:Продолжить;later:Отложить;mod_sgm_cancel:Отменить пересканирование СГМ";
        break;

      case MOD_SGM_QTY:
        definition = "cont:Продолжить;later:Отложить;dellast:Отменить последнее сканирование";
        break;

      case MOD_SGM:
        definition = "cont:Продолжить;later:Отложить;mod_sgm_save:Сохранить пересканирование СГМ;"
                + "mod_sgm_cancel:Отменить пересканирование СГМ;dellast:Отменить последнее сканирование";
        break;

      default:
        definition = "cont:Назад;later:Отложить;" + askQtyMenu;
        break;
    }

    if (RefInfo.haveInfo(ProcType.COMPL)) {
      definition = definition + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Комплектация", "Выберите действие",
            definition, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (getTaskState() == TaskState.SEL_SKL) {
      boolean playSnd = false;

      // обработка выбора склада
      callClearErrMsg(ctx);
      if ((menu.length() == 12) && menu.startsWith("sellgort")) {
        String lg = menu.substring(8);
        if (!rightsLgort(lg, ctx)) {
          callSetErr("Нет прав по складу " + lg, ctx);
          return htmlGet(true, ctx);
        } else {

          // проверяем необходимость сканирования СГМ
          Z_TS_COMPL15 f = new Z_TS_COMPL15();
          f.LGORT = lg;
          f.execute();

          if (f.isErr) {
            HtmlPageMessage p = new HtmlPageMessage("Ошибка Z_TS_COMPL15: " + f.err, "устраните ошибку", null, null);
            return p.getPage();
          }

          if (!f.SGM.equalsIgnoreCase("X")) {
            d.callSetLgort(lg, f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.VBELN, ctx);
            callTaskNameChange(ctx);
          } else if (f.SGM_ASK.equalsIgnoreCase("X")) {
            d.callSetLgort(lg, f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.ASK_SEL_SGM, ctx);
            callTaskNameChange(ctx);
          } else {
            d.callSetLgort(lg, f.NO_FREE_COMPL, f.COMPL_TO_PAL, TaskState.VBELN, ctx);
            d.callSetIsSGM(true, TaskState.VBELN, ctx);
            callTaskNameChange(ctx);
          }
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор склада: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(playSnd, ctx);
    } else if (getTaskState() == TaskState.CNF_CROSSDOC) {
      // подтверждение кроссдокинга
      callClearErrMsg(ctx);
      if ((menu.length() == 12) && menu.startsWith("cnfcrossdoc")) {
        return cnfCrossDocDone(menu.substring(11).equalsIgnoreCase("1"), ctx);
      } else {
        callSetErr("Ошибка программы: неверный выбор признака санирования СГМ: " + menu, ctx);
        return htmlGet(true, ctx);
      }
    } else if (getTaskState() == TaskState.ASK_SEL_SGM) {
      // обработка выбора необходимости сканирования СГМ
      callClearErrMsg(ctx);
      if ((menu.length() == 7) && menu.startsWith("selsgm")) {
        if (menu.substring(6).equalsIgnoreCase("1")) {
          d.callSetIsSGM(true, TaskState.VBELN, ctx);
        } else {
          callSetTaskState(TaskState.VBELN, ctx);
        }
      } else {
        callSetErr("Ошибка программы: неверный выбор признака санирования СГМ: " + menu, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(false, ctx);
    } else if (menu.equals("no")) {
      callClearErr(ctx);
      callSetMsg("Операция отменена", ctx);
      return htmlGet(false, ctx);
    } else if (menu.equals("cont")) {
      return htmlGet(false, ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("fin1")) {
      callClearErrMsg(ctx);
      return handleMenuFin1(ctx);
    } else if (menu.equals("fin")) {
      callTaskFinish(ctx);
      return null;
    } else if (menu.equals("ask_qty_on")) {
      return handleAskQtyOn(ctx);
    } else if (menu.equals("ask_qty_off")) {
      return handleAskQtyOff(ctx);
    } else if (menu.equals("show")) {
      callClearErrMsg(ctx);
      return htmlShowCompl(ctx);
    } else if (menu.equals("showcell")) {
      callClearErrMsg(ctx);
      return htmlShowComplCell(ctx);
    } else if (menu.equals("restqty_cnf")) {
      callClearErrMsg(ctx);
      return handleRestQtyCnf(ctx);
    } else if (menu.equals("foto")) {
      callClearErrMsg(ctx);
      return htmlShowFoto(ctx);
    } else if (menu.equals("to_pal")) {
      callClearErrMsg(ctx);
      return handleMenuToPal(ctx);
    } else if (menu.startsWith("foto_")) {
      callClearErrMsg(ctx);
      return htmlShowMatFoto(menu.substring(5), ctx);
    } else if (menu.equals("delved")) {
      callClearErrMsg(ctx);
      return htmlDelVed(ctx);
    } else if (menu.startsWith("deltrz_")) {
      callClearErrMsg(ctx);
      return handleMenuDelTrz(menu.substring(7), ctx);
    } else if (menu.equals("freecompl")) {
      callClearErrMsg(ctx);
      return handleMenuFreeCompl(ctx);
    } else if (menu.equals("showdone")) {
      callClearErrMsg(ctx);
      return htmlShowComplDone(ctx);
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delall")) {
      callClearErrMsg(ctx);
      return htmlCnfDelAll(); // удалить всё несохраненное (по ячейке/поставке)
    } else if (menu.equals("do_cnf_delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx);
    } else if (menu.equals("show_sk_cells")) {
      callClearErrMsg(ctx);
      return htmlShowSKcells(ctx);
    } else if (menu.equals("show_sk_cells_v")) {
      callClearErrMsg(ctx);
      return htmlShowSKcellsV(ctx);
    } else if (menu.startsWith("setvbeln_")) {
      callClearErrMsg(ctx);
      return handleSetVbeln(menu.substring(9), ctx);
    } else if (menu.startsWith("sskc_")) {
      callClearErrMsg(ctx);
      return htmlSSKC(menu.substring(5), ctx);
    } else if (menu.equals("exit_sk")) {
      callClearErrMsg(ctx);
      return handleMenuExitFreeCompl(ctx);
    } else if (menu.equals("to_mod_sgm")) {
      callClearErrMsg(ctx);
      return handleMenuToModSGM(ctx);
    } else if (menu.equals("mod_sgm_cancel")) {
      callClearErrMsg(ctx);
      return handleMenuModSgmCancel(ctx);
    } else if (menu.equals("mod_sgm_save")) {
      callClearErrMsg(ctx);
      return handleMenuModSgmSave(ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuToPal(TaskContext ctx) throws Exception {
    // переход к сканированию паллеты комплектации

    TaskState st = ctx.task.getTaskState();
    if ((st == TaskState.COMPL_TO_PAL) || (st == TaskState.SEL_ZONE)) {
      st = null;
    }

    if (d.getToPal().isEmpty()) {
      d.callSetToPalPrevState(st, TaskState.COMPL_TO_PAL, ctx);
    } else {
      d.callSetToPalPrevState(st, TaskState.SEL_ZONE, ctx);
    }
    return htmlGet(false, ctx);
  }

  private FileData handleScanZone(String scan, TaskContext ctx) throws Exception {
    if (!isScanZone(scan)) {
      callSetErr("Требуется отсканировать зону склада, в которую помещена паллета "
              + d.getToPal() + " (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String zone = getScanZone(scan);

    Z_TS_CMOVE4 f = new Z_TS_CMOVE4();
    f.LENUM = d.getToPal();
    f.PLACE = zone;
    f.USER_SHK = ctx.user.getUserSHK();
    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    String s1 = "Паллета " + d.getToPal() + " помещена в зону \"" + f.PLACE_NAME + "\"";
    String s2 = "пал " + d.getToPal() + " -> \"" + f.PLACE_NAME + "\"";

    d.callSetZone(zone, TaskState.COMPL_TO_PAL, ctx);
    callSetMsg(s1, ctx);
    callAddHist(s2, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleScanToPal(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать паллету, на которую будет идти комплектация (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    String pal = getScanPal(scan);

    d.callSetToPal(pal, TaskState.values()[d.getToPalPrevState()], ctx);

    callSetMsg("Комплектация на паллету: " + pal, ctx);
    callAddHist("НА ПАЛЛЕТУ: " + pal, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleMenuToModSGM(TaskContext ctx) throws Exception {
    // переход к пересканированию СГМ

    d.callSetPrevState(ctx.task.getTaskState(), TaskState.TO_MOD_SGM, ctx);
    String s = "Переход к пересканированию СГМ";
    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuModSgmCancel(TaskContext ctx) throws Exception {
    // отмена пересканирования СГМ

    callSetTaskState(TaskState.values()[d.getModSgmPrevState()], ctx);
    d.callSgmTovClear(ctx);
    String s = "Пересканирование СГМ отменено";
    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuModSgmSave(TaskContext ctx) throws Exception {
    // сохранение пересканирования СГМ

    int sgm = d.getModSgmNo();
    String q = d.getModSgmQty();

    Z_TS_COMPL19 f = new Z_TS_COMPL19();
    f.SGM = sgm;

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    ArrayList<TovPos> pp = d.getModSgmTov();
    n = pp.size();
    f.IT_TOV_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_TOV[i].MATNR = fillZeros(pp.get(i).matnr, 18);
      f.IT_TOV[i].CHARG = fillZeros(pp.get(i).charg, 10);
      f.IT_TOV[i].QTY = pp.get(i).qty;
    }

    f.execute();

    if (!f.isErr) {
      callSetTaskState(TaskState.values()[d.getModSgmPrevState()], ctx);
      d.callSgmTovClear(ctx);
      String s = f.MSG + "(" + q + ")";
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData htmlSSKC(String matnr_charg, TaskContext ctx) throws Exception {
    // отображение ячеек с выбранным материалом

    if (matnr_charg == null || matnr_charg.isEmpty()) {
      callSetErr("Ошибка программы при выборе пункта меню 3", ctx);
      return htmlGet(true, ctx);
    }

    String[] ss = matnr_charg.split("_");
    if (ss.length == 0) {
      callSetErr("Ошибка программы при выборе пункта меню 4", ctx);
      return htmlGet(true, ctx);
    }

    String matnr = ss[0];
    String charg = "";
    if (ss.length > 1) {
      charg = ss[1];
    }

    Z_TS_COMPL9 f = new Z_TS_COMPL9();
    f.LGORT = d.getLgort();
    f.MATNR = fillZeros(matnr, 18);
    if (!charg.isEmpty()) {
      f.CHARG = fillZeros(charg, 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Ячейки с материалом";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addText("<b>Ячейки с материалом ");
    p.addText(matnr);
    p.addText("</b> ");
    if (!charg.isEmpty()) {
      p.addText(" / ");
      p.addText(charg);
    }
    p.addText(" <font color=blue>");
    p.addText(RefMat.getName(matnr));
    p.addLine("</font>:");
    p.addNewLine();

    for (ZTS_CELL_QTY_S r : f.IT) {
      p.addText("<font color=red><b>");
      p.addText(r.LGPLA);
      p.addText("</b></font> (");
      p.addText(r.LGTYP);
      p.addText(") <b>");
      p.addText(delDecZeros(r.QTY.toString()));
      p.addLine(" ед</b>");
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData htmlShowMatFoto(String matnr, TaskContext ctx) throws Exception {
    // отображение фото материала

    if (matnr == null || matnr.isEmpty()) {
      callSetErr("Ошибка программы при выборе материала для фото", ctx);
      return htmlGet(true, ctx);
    }

    RefMatStruct m = RefMat.getNoNull(matnr);

    HtmlPage p = new HtmlPage();
    p.title = "Фото материала";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addText("<b>Фото материала ");
    p.addText(matnr);
    p.addText("</b> ");
    p.addNewLine();
    p.addText(" <font color=blue>");
    p.addText(m.name);
    p.addLine("</font>:");
    if (m.haveFoto) {
      p.addText("<img src=\"mat" + matnr + ".jpg\" ");
    } else {
      p.addText("<img src=\"no_foto.jpg\" ");
    }
    p.addText("onload=\"var wr=window.innerWidth*0.9; var wr2=window.innerHeight*naturalWidth/naturalHeight; if(wr>wr2){wr=wr2;} if(wr<200){wr=200;} this.width=wr;\">");
    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleSetVbeln(String vbeln, TaskContext ctx) throws Exception {
    // обработка выбора поставки (для выбора материала для отображения ячеек с материалом)

    if (vbeln == null || vbeln.isEmpty()) {
      callSetErr("Ошибка программы при выборе пункта меню", ctx);
      return htmlGet(true, ctx);
    }

    if (!d.haveVbeln(vbeln)) {
      callSetErr("Поставка \"" + vbeln + "\" не была выбрана для комплектации", ctx);
      return htmlGet(true, ctx);
    }

    d.callSetVbeln1(vbeln, ctx);
    return htmlShowSKcellsV(ctx);
  }

  private FileData htmlShowSKcellsV(TaskContext ctx) throws Exception {
    // выбор паставки

    String def = "cont:Отмена";

    String[] vs = d.getVbelnList();
    for (String s : vs) {
      def = def + ";setvbeln_" + s + ":<b>" + s + "</b>";
    }

    HtmlPage p = new HtmlPageMenu("Выбор поставки", "Выбор поставки", def, "cont", null, null);

    return p.getPage();
  }

  private FileData htmlShowSKcells(TaskContext ctx) throws Exception {
    // выбор материала (и возможно партии) для отображения ячеек с наличием
    // при необходимости выбирается поставка

    String vbeln = d.getVbeln();
    String def = "cont:Отмена";
    if ((vbeln == null) || vbeln.isEmpty() || !d.getIs1vbeln()) {
      def = def + ";show_sk_cells_v:Выбор поставки";
    }

    if ((vbeln != null) && !vbeln.isEmpty()) {
      Z_TS_COMPL8 f = new Z_TS_COMPL8();
      f.LGORT = d.getLgort();
      f.VBELN = fillZeros(vbeln, 10);

      f.execute();

      if (f.isErr) {
        callSetErr(f.err, ctx);
        return htmlGet(true, ctx);
      }

      int n = f.IT.length;

      String s;
      ZTS_NO_TRZ_S r;
      for (int i = 0; i < n; i++) {
        r = f.IT[i];
        r.MATNR = delZeros(r.MATNR);
        r.CHARG = delZeros(r.CHARG);

        s = ":<b>" + r.MATNR + "</b>";
        if (r.CHARG.length() > 0) {
          s = s + " / " + r.CHARG;
        }
        s = s + " <font color=blue>" + RefMat.getName(r.MATNR) + "</font> <b>"
                + delDecZeros(r.QTY.toString()) + " ед</b>";

        def = def + ";sskc_" + r.MATNR + "_" + r.CHARG + s;
      }
    }

    String addName = "";
    if ((vbeln != null) && !vbeln.isEmpty() && !d.getIs1vbeln()) {
      addName = " по поставке " + vbeln;
    }

    HtmlPage p = new HtmlPageMenu("Выбор материала",
            "Выбор материала для своб комплектации" + addName,
            def, "cont", null, null);

    return p.getPage();
  }

  private FileData htmlCnfDelAll() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Комплектация",
            "Удалить все полученные данные по ячейке " + d.getCell()
            + ", поставке " + d.getVbeln() + "?",
            "no:Нет;do_cnf_delall:Да", "no", null, null, false);
    return p.getPage();
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    d.callClearTovData(ctx);
    callSetMsg("Все данные сканирования по ячейке " + d.getCell()
            + ", поставке " + d.getVbeln() + " удалены", ctx);
    callAddHist("Удалено всё по ячейке " + d.getCell()
            + ", поставке " + d.getVbeln(), ctx);
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    // Отменить последнее сканирование

    String charg;
    RefChargStruct c;
    String s;

    switch (getTaskState()) {
      case QTY:
        charg = d.getLastCharg();
        c = RefCharg.getNoNull(charg);
        s = "Отменено: " + c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        if (canScanPal()) {
          s = s + " (паллета " + d.getLenum() + ")";
        }
        callSetTaskState(TaskState.TOV_CELL, ctx);
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        return htmlGet(true, ctx);

      case MOD_SGM_QTY:
        charg = d.getLastCharg();
        c = RefCharg.getNoNull(charg);
        s = "Отменено: " + c.matnr + "/" + charg + " " + RefMat.getFullName(c.matnr);
        callSetTaskState(TaskState.MOD_SGM, ctx);
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        return htmlGet(true, ctx);

      case MOD_SGM:
        ArrayList<TovPos> pp = d.getModSgmTov();
        if ((pp == null) || pp.isEmpty()) {
          callSetErr("Нет последнего сканирования", ctx);
          return htmlGet(true, ctx);
        }
        TovPos p = pp.get(pp.size() - 1);
        s = "Отменено в СГМ: " + delDecZeros(p.qty.toString())
                + " ед: " + p.matnr + "/" + p.charg + " "
                + RefMat.getFullName(p.matnr);
        d.callDelLastModSgm(ctx);
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        return htmlGet(true, ctx);
    }

    ComplScanData sd = d.getLastScanData();
    if (sd == null) {
      callSetErr("Нет последнего сканирования", ctx);
      return htmlGet(true, ctx);
    }

    s = "Отменено: " + delDecZeros(sd.qtyP.add(sd.qtyM).toString())
            + " ед: " + sd.matnr + "/" + sd.charg + " "
            + RefMat.getFullName(sd.matnr);
    if (sd.charg != null) {
      s = s + " / " + sd.charg;
    }
    s = s + " " + RefMat.getFullName(sd.matnr);
    if (canScanPal()) {
      s = s + " (паллета " + d.getLenum() + ")";
    }

    d.callDelLast(ctx);

    callSetMsg(s, ctx);
    callAddHist(s, ctx);

    return htmlGet(true, ctx);
  }

  private FileData handleMenuFreeCompl(TaskContext ctx) throws Exception {
    // переключение в режим свободной комплектации

    if ((d.getScanDataSize() > 0) || d.getInfCompl().equals("X")) {
      callSetErr("Переключение в режим свободной комплектации невозможно", ctx);
      return htmlGet(true, ctx);
    }

    if (d.getNoFreeCompl()) {
      callSetErr("Свободная комплектация запрещена на этом складе", ctx);
      return htmlGet(true, ctx);
    }

    callSetStateText("", ctx);
    d.callSetFreeCompl(true, TaskState.SEL_CELL, ctx);
    callSetMsg("Включен режим свободной комплектации", ctx);
    callAddHist("Свободная комплектация", ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleRestQtyCnf(TaskContext ctx) throws Exception {
    if (!d.getNextCellScan().isEmpty()) {
      return handleScanCell(d.getNextCellScan(), ctx, true);
    } else if (!d.getNextVbelnScan().isEmpty()) {
      return handleScanVbeln2(d.getNextVbelnScan(), ctx, true);
    } else {
      ComplRetSave sv = saveComplData("", "", ctx, true);
      if (sv.err != null) {
        callSetErr(sv.err, ctx);
        return htmlGet(true, ctx);
      }
      return htmlGet(true, ctx);
    }
  }

  private FileData handleMenuExitFreeCompl(TaskContext ctx) throws Exception {
    // выход из режима свободной комплектации

    saveComplData("", "", ctx, true);

    callSetStateText("", ctx);
    d.callSetFreeCompl(false, TaskState.SEL_CELL, ctx);
    callSetMsg("Режим свободной комплектации выключен", ctx);
    callAddHist("Своб компл ВЫКЛЮЧЕНА", ctx);
    return htmlGet(false, ctx);
  }

  private FileData htmlShowCompl(TaskContext ctx) throws Exception {
    // отображение нескомплектованных позиций

    Z_TS_COMPL6 f = new Z_TS_COMPL6();

    f.LGORT = d.getLgort();
    f.INF_COMPL = d.getInfCompl();

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    n = f.IT.length;
    if (n == 0) {
      callSetMsg("Нескомплектованных позиций нет", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Нескомплектованные позиции";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Нескомплектованные позиции:</b>");
    p.addNewLine();
    String s;
    ZTS_VED_S r;
    for (int i = 0; i < n; i++) {
      r = f.IT[i];
      r.VBELN = delZeros(r.VBELN);
      r.MATNR = delZeros(r.MATNR);
      r.CHARG = delZeros(r.CHARG);
      if (r.LENUM.length() == 20) {
        r.LENUM = r.LENUM.substring(10);
      }

      s = "<b><font color=red>" + r.LGPLA + "</font>";
      if (canScanVbeln()) {
        s = s + " <font color=blue>" + r.VBELN + "</font>";
      }
      if (r.LENUM.length() > 0) {
        s = s + " <font color=#CC7700>" + r.LENUM + "</font>";
      }
      s = s + " " + r.MATNR;
      if (r.CHARG.length() > 0) {
        s = s + " / " + r.CHARG;
      }
      s = s + "</b> " + RefMat.getFullName(r.MATNR) + " <b>"
              + delDecZeros(r.QTY.toString()) + " ед</b>";
      p.addLine(s);
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData htmlShowComplCell(TaskContext ctx) throws Exception {
//    if (d.isFreeCompl()) {
//      return htmlShowComplCell2(ctx);
//    } else {
    return htmlShowComplCell1(ctx);
//    }
  }

  private FileData htmlShowComplCell1(TaskContext ctx) throws Exception {
    // отображение нескомплектованных позиций (по текущей ячейке)

    String cell = d.getCell();
    String vbeln = d.getVbeln();

    Z_TS_COMPL6 f = new Z_TS_COMPL6();

    f.LGORT = d.getLgort();
    f.LGPLA = cell;
    f.INF_COMPL = d.getInfCompl();

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    n = f.IT.length;
    if (n == 0) {
      callSetMsg("Нескомплектованных позиций по ячейке " + cell + " нет", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Нескомплектованные позиции по ячейке " + cell;
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Нескомплектованные позиции по ячейке " + cell + ":</b>");
    p.addNewLine();
    String s;
    ZTS_VED_S r;
    for (int i = 0; i < n; i++) {
      r = f.IT[i];
      r.VBELN = delZeros(r.VBELN);
      r.MATNR = delZeros(r.MATNR);
      r.CHARG = delZeros(r.CHARG);
      if (r.LENUM.length() == 20) {
        r.LENUM = r.LENUM.substring(10);
      }

      if (r.VBELN.equals(vbeln)) {
        // вычитаем отсканированное и несохраненное кол-во
        ArrayList<ComplScanData> scanData = d.getScanData();
        for (ComplScanData sd : scanData) {
          if (sd.pal.equals(r.LENUM) && sd.matnr.equals(r.MATNR)) {
            if (r.CHARG.length() > 0) {
              if ((sd.qtyP.signum() > 0) && sd.charg.equals(r.CHARG)) {
                r.QTY = r.QTY.subtract(sd.qtyP);
              }
            } else if (sd.qtyM.signum() > 0) {
              r.QTY = r.QTY.subtract(sd.qtyM);
            }
          }
        }
      }

      if (r.QTY.signum() != 0) {
        s = "<b>";
        if (canScanVbeln()) {
          s = s + " <font color=blue>" + r.VBELN + "</font>";
        }
        if (r.LENUM.length() > 0) {
          s = s + " <font color=#CC7700>" + r.LENUM + "</font>";
        }
        s = s + " " + r.MATNR;
        if (r.CHARG.length() > 0) {
          s = s + " / " + r.CHARG;
        }
        s = s + "</b> " + RefMat.getFullName(r.MATNR) + " <b>"
                + delDecZeros(r.QTY.toString()) + " ед</b>";
        p.addLine(s);
      }
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData htmlShowFoto(TaskContext ctx) throws Exception {
    // отображение фото материалов (по текущей ячейке)

    String cell = d.getCell();
    String vbeln = d.getVbeln();

    Z_TS_COMPL17 f = new Z_TS_COMPL17();

    f.LGORT = d.getLgort();
    f.LGPLA = cell;
    f.INF_COMPL = d.getInfCompl();

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    n = f.IT.length;
    if (n == 0) {
      callSetMsg("Нескомплектованных позиций по ячейке " + cell + " нет", ctx);
      return htmlGet(false, ctx);
    }

    String def = "no:Назад";
    RefMatStruct m;
    String matnr;

    for (ZTS_HAVE_FOTO_S wa : f.IT) {
      matnr = delZeros(wa.MATNR);
      m = RefMat.getNoNull(matnr);
      def += ";foto_" + matnr + ":" + (m.haveFoto ? "" : "(нет фото) ") + m.name;
    }

    HtmlPageMenu p = new HtmlPageMenu("Фото материала", "Выберите материал", def, null, null, null);
    return p.getPage();
  }

  private FileData htmlDelVed(TaskContext ctx) throws Exception {
    // выбор к удалению позиций ведомости по ячейке

    String cell = d.getCell();
    String vbeln = d.getVbeln();

    Z_TS_COMPL12 f = new Z_TS_COMPL12();

    f.LGORT = d.getLgort();
    f.LGPLA = cell;
    f.VBELN = fillZeros(vbeln, 10);

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    int n = f.IT.length;
    if (n == 0) {
      callSetMsg("Нескомплектованных позиций по ячейке " + cell
              + " поставке " + vbeln + " нет", ctx);
      return htmlGet(false, ctx);
    }

    String def = "cont:Отменить";

    String s;
    ZTS_MP_QTY_S r;
    for (int i = 0; i < n; i++) {
      r = f.IT[i];
      r.MATNR = delZeros(r.MATNR);
      r.CHARG = delZeros(r.CHARG);

      s = ":<b>" + r.MATNR + "</b>";
      if (r.CHARG.length() > 0) {
        s = s + " / " + r.CHARG;
      }
      s = s + " <font color=blue>" + RefMat.getName(r.MATNR) + "</font> <b>"
              + delDecZeros(r.QTY.toString()) + " ед</b>";

      def = def + ";deltrz_" + r.MATNR + "_" + r.CHARG + s;
    }

    HtmlPage p = new HtmlPageMenu("Перенос в свободную комплектацию", "Перенос в свободную комплектацию", def, "cont", null, null);

    return p.getPage();
  }

  private FileData htmlShowComplDone(TaskContext ctx) throws Exception {
    // отображение скомплектованных позиций (по текущей ячейке)

    String cell = d.getCell();
    String vbeln = d.getVbeln();

    Z_TS_COMPL2 f = new Z_TS_COMPL2();
    f.LGPLA = cell;
    f.VBELN = fillZeros(vbeln, 10);

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    if (f.IT.length == 0) {
      callSetMsg("Скомплектованных позиций по ячейке " + cell
              + " поставке " + vbeln + " нет", ctx);
      return htmlGet(false, ctx);
    }

    HtmlPage p = new HtmlPage();
    p.title = "Скомплектованные позиции";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    p.addLine("<b>Скомплектованные позиции по ячейке " + cell
            + " поставке " + vbeln + ":</b>");
    p.addNewLine();
    String s, pal;
    for (ZTS_COMPL2_S wa : f.IT) {
      s = "<b>" + delZeros(wa.MATNR) + " / " + delZeros(wa.CHARG) + "</b> ";
      if (canScanPal()) {
        pal = wa.LENUM;
        if (pal.length() == 20) {
          pal = pal.substring(10);
        }
        s = s + "<font color=#CC7700>" + pal + "</font> ";
      }
      s = s + RefMat.getFullName(delZeros(wa.MATNR)) + " <b>"
              + delDecZeros(wa.QTY.toString()) + " ед</b>";
      p.addLine(s);
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleMenuFin1(TaskContext ctx) throws Exception {
    if (d.getScanDataSize() > 0) {
      callSetErr("Имеются несохраненные данные комплектации, завершение невозможно", ctx);
      return htmlGet(true, ctx);
    }

    if (d.isFreeCompl()) {
      saveComplData("", "", ctx, true);
    }

    Z_TS_COMPL6 f = new Z_TS_COMPL6();

    f.LGORT = d.getLgort();
    f.INF_COMPL = d.getInfCompl();

    String[] vv = d.getVbelnList();
    int n = vv.length;
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    if (f.IT.length > 0) {
      // имеются нескомплектованные позиции
      return htmlCnfExit();
    } else {
      // нескомплектованных позиций нет, завершаем
      String finMsg = getFinMsg();
      if (finMsg == null) {
        callTaskFinish(ctx);
        return null;
      } else {
        callSetErrMsg("", finMsg, ctx);
        callSetTaskState(TaskState.FIN_MSG, ctx);
        return htmlGet(false, ctx);
      }
    }
  }

  private String getFinMsg() {
    Z_TS_COMPL16 f = new Z_TS_COMPL16();

    String[] vv = d.getVbelnList();
    int n = vv.length;
    if (n == 0) {
      return null;
    }
    f.IT_V_create(n);
    for (int i = 0; i < n; i++) {
      f.IT_V[i].VBELN = fillZeros(vv[i], 10);
    }

    f.execute();

    if (!f.isErr && (f.N_SGM > 0)) {
      if (f.IT_V.length == 1) {
        return "Число СГМ (коробок) по поставке: " + f.N_SGM;
      } else {
        return "Число СГМ (коробок) по " + f.IT_V.length + " поставкам: " + f.N_SGM;
      }
    }

    return null;
  }

  private FileData htmlCnfExit() throws Exception {
    HtmlPageMenu p = new HtmlPageMenu("Комплектация",
            "Имеются нескомплектованные позиции, всё равно выйти из комплектации?",
            "no:Отмена;show:Нет, показать нескомплектованное;fin:Да, выйти", "no", null, getFinMsg(), false);
    return p.getPage();
  }

  private FileData handleAskQtyOn(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(true, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuDelTrz(String matnr_charg, TaskContext ctx) throws Exception {
    if (matnr_charg == null || matnr_charg.isEmpty()) {
      callSetErr("Ошибка программы при выборе пункта меню", ctx);
      return htmlGet(true, ctx);
    }

    String[] ss = matnr_charg.split("_");
    if (ss.length == 0) {
      callSetErr("Ошибка программы при выборе пункта меню 2", ctx);
      return htmlGet(true, ctx);
    }

    String matnr = ss[0];
    String charg = "";
    if (ss.length > 1) {
      charg = ss[1];
    }
    String lgpla = d.getCell();
    String vbeln = d.getVbeln();

    Z_TS_COMPL7 f = new Z_TS_COMPL7();
    f.LGORT = d.getLgort();
    f.VBELN = fillZeros(vbeln, 10);
    f.LGPLA = lgpla;
    f.MATNR = fillZeros(matnr, 18);
    if (!charg.isEmpty()) {
      f.CHARG = fillZeros(charg, 10);
    }

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    ComplRetErrCrossdoc ret = loadVedCompl(d.getVbeln(), d.getCell(), ctx);
    if (ret.err != null) {
      callSetErr(ret.err, ctx);
      return htmlGet(true, ctx);
    }

    String s = "Материал " + matnr;
    if (!charg.isEmpty()) {
      s = s + " (партия " + charg + ")";
    }
    s = s + " удален из ведомости на комплектацию по ячейке " + lgpla
            + " поставке " + vbeln + "; доступен для свободной комплектации";
    callSetMsg(s, ctx);

    return htmlGet(false, ctx);
  }

  private FileData handleFinMsg(String htext1, TaskContext ctx) throws Exception {
    if ((htext1 != null) && htext1.equals("fin_msg")) {
      callTaskFinish(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleAskQtyOff(TaskContext ctx) throws Exception {
    ctx.user.callSetAskQty(false, ctx);
    return htmlGet(false, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    return getProcType().text + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    String addName = ctx.user.getAskQtyCompl(d.getLgort()) ? "<b>ввод кол-ва</b>" : "<b>БЕЗ ввода кол-ва</b>";
    String vbelns = d.getVbelns();
    if (vbelns != null) {
      addName = vbelns + "; " + addName;
    }
    if (d.isFreeCompl()) {
      addName = addName + "; <b>с СГМ</b>";
    }
    if (d.getInfCompl().equals("X")) {
      addName = addName + "; <font color=blue><b>документы готовы</b></font>";
    }
    if (d.getCheckCompl().equals("X")) {
      addName = addName + "; <font color=blue><b>ПРОВЕРКА НАЛИЧИЯ</b></font>";
    }
    if (d.isFreeCompl()) {
      addName = addName + "; <font color=green><b>СВОБ КОМПЛ</b></font>";
    }
    if (d.getComplToPal()) {
      addName += "\r\n<br>на пал: <b>";
      if (d.getToPal().isEmpty()) {
        addName += "НЕТ</b>";
      } else {
        addName += d.getToPal() + "</b>";
      }
    }

    if (!d.getLgort().isEmpty()) {
      return d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name + "; " + addName;
    } else {
      return addName;
    }
  }
}

class ComplVCqty {

  // кол-во к комплектации по ячейкам (по одной поставке) (ключ: ячейка)
  public HashMap<String, BigDecimal> cellQty
          = new HashMap<String, BigDecimal>();
}

class ComplScanData {  // отсканированные позиции (по текущей поставке и ячейке)

  public String pal;
  public String toPal;
  public String matnr;
  public String charg;
  public int sgm;
  public BigDecimal qtyP; // кол-во связано (с ведомостью) по партии
  public BigDecimal qtyM; // кол-во связано (с ведомостью) по материалу

  public ComplScanData(String pal, String toPal, String matnr, String charg, int sgm, BigDecimal qty) {
    this.pal = pal;
    this.toPal = toPal;
    this.matnr = matnr;
    this.charg = charg;
    this.sgm = sgm;
    this.qtyP = qty;
    this.qtyM = qty;
  }
}

class ComplVedVC { // ведомость на комплектацию по поставке и ячейке

  // ведомость на комплектацию (ключ: паллета_материал_партия):
  public HashMap<String, ComplVedVCpos> ved
          = new HashMap<String, ComplVedVCpos>();
  public ZTS_COMPL_S[] data0 = null; // исходные данные по ведомости
  public HashMap<String, Boolean> strictPrt = new HashMap<String, Boolean>(10); // партии в ячейке, которые нужно комплектовать строго по партии (по материалам в ведомости)
  public HashMap<String, Boolean> allPrt = new HashMap<String, Boolean>(10); // все партии в ячейке (по материалам в ведомости)
//  public ZTS_VED_SK2_S[] data1 = null; // исходные данные по своб комплектации
}

class ComplVedVCpos {  // позиция ведомости на комплектацию по поставке и ячейке

  public String pal;
  public String matnr;
  public String charg;
  public BigDecimal qty; // исходное кол-во
  public BigDecimal qty_rest; // что осталось скомплектовать

  public ComplVedVCpos(String pal, String matnr, String charg, BigDecimal qty) {
    this.pal = pal;
    this.matnr = matnr;
    this.charg = charg;
    this.qty = qty;
    this.qty_rest = qty;
  }
}

class TovPos {  // товарная позиция СГМ

  public String matnr;
  public String charg;
  public BigDecimal qty; // кол-во

  public TovPos(String matnr, String charg, BigDecimal qty) {
    this.matnr = matnr;
    this.charg = charg;
    this.qty = qty;
  }
}

class ComplRetErrCrossdoc {

  public final String err;
  public final boolean crossDoc;

  public ComplRetErrCrossdoc(String err, boolean crossDoc) {
    this.err = err;
    this.crossDoc = crossDoc;
  }
}

class ComplRetQty {

  public final BigDecimal qty;
  public final boolean isEzap;

  public ComplRetQty(BigDecimal qty, boolean isEzap) {
    this.qty = qty;
    this.isEzap = isEzap;
  }
}

class ComplRetSave {

  public final String err;
  public final String restQty;
  public final boolean ok;

  public ComplRetSave(String err, String restQty) {
    this.err = err;
    this.restQty = restQty;
    this.ok = (err == null) && (restQty == null);
  }
}

class ComplPalZone {

  public String pal;
  public String zone;

  public ComplPalZone(String pal, String zone) {
    this.pal = pal;
    this.zone = zone;
  }
}

class ComplData extends ProcData {

  private String lgort = ""; // склад
  private boolean noFreeCompl = false; // признак запрета свободной комплектации
  private boolean complToPal = false; // признак указания паллеты, на которую идет комплектация
  private String toPal = ""; // паллета, на которую комплектуем
  private int toPalPrevState = 0; // предыдущее состояние
  private final ArrayList<ComplPalZone> palZone = new ArrayList<ComplPalZone>(); // размещение паллет по зонам
  private String cell = null; // ячейка, из которой берем
  private String nextCellScan = null;
  private String nextVbelnScan = null;
  private String lenum = ""; // паллета, с которой берем
  private String vbeln = null; // текущая комплектуемая поставка
  private String lastVbeln = null; // последняя отсканированная поставка (при сканировании поставок в начале)
  private String lastCharg = ""; // предыдущее сканирование партии
  private BigDecimal lastQty = BigDecimal.ZERO; // предыдущее сканирование кол-ва
  private String infCompl = "-"; // признак информационной комплектации (X)
  private String checkCompl = "-"; // признак проверки наличия под комплектацию (X)
  private boolean freeCompl = false; // признак режима свободной комплектации
  private final HashMap<String, ComplVCqty> vcq
          = new HashMap<String, ComplVCqty>(); // ведомости на комплектацию (только ячейки и кол-во) (ключ - номер поставки)
  private ComplVedVC vedVC = null; // полная ведомость на компл по поставке/ячейке
  private final ArrayList<ComplScanData> scanData
          = new ArrayList<ComplScanData>(); // отсканированный товар (по ячейке и поставке)
  private final HashMap<String, Boolean> fp
          = new HashMap<String, Boolean>(); // признак сканирования паллеты в многопаллетной ячейке
  private String tanum1 = "0"; // мин. и макс. номера трансп заказов
  private String tanum2 = "0"; // свободной комплектации по ячейке (для удаления при выходе из ячейки)
  private boolean isSGM = false; // признак сканирования СГМ
  private int modSgmNo = 0; // номер пересканируемой СГМ
  private final ArrayList<TovPos> modSgmTov = new ArrayList<TovPos>(); // отсканированный товар (при пересканировании СГМ)
  private int modSgmPrevState = 0;

  public String getNextCellScan() {
    return nextCellScan;
  }

  public String getNextVbelnScan() {
    return nextVbelnScan;
  }

  public String getLastVbeln() {
    return lastVbeln;
  }

  public int getModSgmPrevState() {
    return modSgmPrevState;
  }

  public ArrayList<TovPos> getModSgmTov() {
    return modSgmTov;
  }

  public int getModSgmNo() {
    return modSgmNo;
  }

  public boolean getNoFreeCompl() {
    return noFreeCompl;
  }

  public boolean getComplToPal() {
    return complToPal;
  }

  public String getToPal() {
    return toPal;
  }

  public int getToPalPrevState() {
    return toPalPrevState;
  }

  public ArrayList<ComplPalZone> getPalZone() {
    return palZone;
  }

  public ZTSM_PAL_ZONE_S[] getPalZoneArray() throws Exception {
    ZTSM_PAL_ZONE_S[] ret = new ZTSM_PAL_ZONE_S[palZone.size()];
    ComplPalZone pz;
    ZTSM_PAL_ZONE_S rec;
    for (int i = 0; i < ret.length; i++) {
      rec = new ZTSM_PAL_ZONE_S();
      pz = palZone.get(i);
      rec.LENUM = pz.pal;
      if (rec.LENUM.length() == 10) {
        rec.LENUM = ProcessTask.fillZeros(rec.LENUM, 20);
      }
      rec.PLACE = pz.zone;
      ret[i] = rec;
    }
    return ret;
  }

  public String getTanum1() {
    return tanum1;
  }

  public String getTanum2() {
    return tanum2;
  }

  public boolean isFreeCompl() {
    return freeCompl;
  }

  public boolean isSGM() {
    return isSGM;
  }

  public ArrayList<ComplScanData> getScanData() {
    return scanData;
  }

  public String[] getVbelnList() {
    String[] ret = new String[vcq.size()];
    int n = 0;
    for (Entry<String, ComplVCqty> i : vcq.entrySet()) {
      ret[n] = i.getKey();
      n++;
    }
    return ret;
  }

  public String getPals() {
//    if (freeCompl) {
//      return getPals2();
//    } else {
    return getPals1();
//    }
  }

  public String getPals1() {
    // список номеров паллет по текущим ячейке/поставке

    if (vedVC == null) {
      return "";
    }

    ConcurrentSkipListMap<String, String> pp = new ConcurrentSkipListMap<String, String>();

    for (ZTS_COMPL_S wa : vedVC.data0) {
      if (wa.LGPLA.equals(cell)) {
        pp.put(wa.LENUM, "+");
      }
    }

    String ret = "";
    String pal;
    for (Entry<String, String> i : pp.entrySet()) {
      pal = i.getKey();
      if (ret.isEmpty()) {
        ret = pal;
      } else {
        ret = ret + ", " + pal;
      }
    }

    return ret;
  }

  public String getLenum() {
    return lenum;
  }

  public boolean getFP(String lgpla) {
    Boolean ret = fp.get(lgpla);
    return ret == null ? false : ret;
  }

  public boolean getFP() {
    Boolean ret = fp.get(cell);
    return ret == null ? false : ret;
  }

  public String getInfCompl() {
    return infCompl;
  }

  public String getCheckCompl() {
    return checkCompl;
  }

  public ZTS_COMPL_DONE_TO_PAL_S[] getScanDataArray() throws Exception {
    ZTS_COMPL_DONE_TO_PAL_S[] ret = new ZTS_COMPL_DONE_TO_PAL_S[scanData.size()];
    ComplScanData sd;
    ZTS_COMPL_DONE_TO_PAL_S cd;
    for (int i = 0; i < ret.length; i++) {
      cd = new ZTS_COMPL_DONE_TO_PAL_S();
      sd = scanData.get(i);
      cd.LENUM = sd.pal;
      if (cd.LENUM.length() == 10) {
        cd.LENUM = ProcessTask.fillZeros(cd.LENUM, 20);
      }
      cd.TO_PAL = sd.toPal;
      if (cd.TO_PAL.length() == 10) {
        cd.TO_PAL = ProcessTask.fillZeros(cd.TO_PAL, 20);
      }
      cd.MATNR = ProcessTask.fillZeros(sd.matnr, 18);
      cd.CHARG = ProcessTask.fillZeros(sd.charg, 10);
      cd.QTY = sd.qtyP.add(sd.qtyM);
      cd.SGM = sd.sgm;
      ret[i] = cd;
    }
    return ret;
  }

  // ведомость на комплектации по ячейке
  public ZTS_COMPL_S[] getCellVK() throws Exception {
    if ((vbeln == null) || (cell == null)) {
      return new ZTS_COMPL_S[0];
    }
    // получаем данные по текущей поставке
    if ((vedVC == null) || (vedVC.data0 == null)) {
      return new ZTS_COMPL_S[0];
    }
    int n = 0;
    // подсчитываем число записей
    for (ZTS_COMPL_S wa : vedVC.data0) {
      if (wa.LGPLA.equals(cell)) {
        n++;
      }
    }
    if (n == 0) {
      return new ZTS_COMPL_S[0];
    }
    // копируем записи в новый массив
    ZTS_COMPL_S[] ret = new ZTS_COMPL_S[n];
    ZTS_COMPL_S r;
    int j = 0;
    for (ZTS_COMPL_S rr : vedVC.data0) {
      if (rr.LGPLA.equals(cell)) {
        r = new ZTS_COMPL_S();
        r.LGNUM = rr.LGNUM;
        r.TANUM = rr.TANUM;
        r.TAPOS = rr.TAPOS;
        r.LGTYP = rr.LGTYP;
        r.LGPLA = rr.LGPLA;
        r.LENUM = rr.LENUM;
        if (r.LENUM.length() == 10) {
          r.LENUM = ProcessTask.fillZeros(r.LENUM, 20);
        }
        r.MATNR = ProcessTask.fillZeros(rr.MATNR, 18);
        r.CHARG = ProcessTask.fillZeros(rr.CHARG, 10);
        r.ONLY_PRT = rr.ONLY_PRT;
        r.SOBKZ = rr.SOBKZ;
        r.SONUM = rr.SONUM;
        r.QTY = rr.QTY;
        ret[j] = r;
        j++;
      }
    }
    return ret;
  }

  public ComplScanData getLastScanData() {
    if (scanData.isEmpty()) {
      return null;
    } else {
      return scanData.get(scanData.size() - 1);
    }
  }

  public boolean getIs1vbeln() {
    return vcq.size() == 1;
  }

  public String getLgort() {
    return lgort;
  }

  public String getCell() {
    return cell;
  }

  public String getVbeln() {
    return vbeln;
  }

  public String getLastCharg() {
    return lastCharg;
  }

  public BigDecimal getLastQty() {
    return lastQty;
  }

  public int getScanDataSize() {
    return scanData.size();
  }

  public boolean haveVbeln(String vbeln) {
    return vcq.get(vbeln) != null;
  }

  public BigDecimal getVbelnQty(String vbeln) {
    ComplVCqty cq = vcq.get(vbeln);
    if (cq == null) {
      return BigDecimal.ZERO;
    }

    BigDecimal ret = BigDecimal.ZERO;
    for (Entry<String, BigDecimal> i : cq.cellQty.entrySet()) {
      ret = ret.add(i.getValue());
    }
    return ret;
  }

  public String getCellVbelns(String cell) {
    // поставки по ячейке (с кол-вом)
    boolean is1vbeln = (vcq.size() == 1);
    String ret = null;
    ComplVCqty cq;
    int n;
    BigDecimal q;
    for (Entry<String, ComplVCqty> i : vcq.entrySet()) {
      cq = i.getValue();
      n = 0;
      q = BigDecimal.ZERO;
      for (Entry<String, BigDecimal> j : cq.cellQty.entrySet()) {
        if (j.getKey().equals(cell)) {
          n++;
          q = q.add(j.getValue());
        }
      }
      if (n > 0) {
        if (is1vbeln) {
          return "(" + ProcessTask.delDecZeros(q.toString()) + " ед)";
        } else if (ret == null) {
          ret = i.getKey() + " (" + ProcessTask.delDecZeros(q.toString()) + " ед)";
        } else {
          ret = ret + ", " + i.getKey() + " (" + ProcessTask.delDecZeros(q.toString()) + " ед)";
        }
      }
    }
    return ret;
  }

  public String getVbelns() {
    // список поставок
    String ret = null;
    for (Entry<String, ComplVCqty> i : vcq.entrySet()) {
      if (ret == null) {
        ret = i.getKey();
      } else {
        ret = ret + ", " + i.getKey();
      }
    }
    return ret;
  }

  public boolean isCellVbeln(String vbeln) {
    // наличие комплектации (без проверки в САПе) по текущей ячейке и указанной поставке
    ComplVCqty cq = vcq.get(vbeln);
    return cq != null;
  }

  public String getVbelnCellPalQty(String pal) {
    // кол-во (строка) по поставке, паллете и ячейке
    if (vedVC == null) {
      return null;
    }
    ComplVedVCpos vedPos;
    int n = 0;
    BigDecimal q = BigDecimal.ZERO;
    for (Entry<String, ComplVedVCpos> i : vedVC.ved.entrySet()) {
      vedPos = i.getValue();
      if (vedPos.pal.equals(pal)) {
        n++;
        q = q.add(vedPos.qty_rest);
      }
    }
    if (n == 0) {
      return "";
    } else {
      return "(" + ProcessTask.delDecZeros(q.toString()) + " ед)";
    }
  }

  public String getVbelnCellQty() {
    // кол-во (строка) по поставке и ячейке
    if (vedVC == null) {
      return null;
    }
    ComplVedVCpos vedPos;
    int n = 0;
    BigDecimal q = BigDecimal.ZERO;
    for (Entry<String, ComplVedVCpos> i : vedVC.ved.entrySet()) {
      vedPos = i.getValue();
      n++;
      q = q.add(vedPos.qty_rest);
    }
    if (n == 0) {
      return "";
    } else {
      return "(" + ProcessTask.delDecZeros(q.toString()) + " ед)";
    }
  }

  public String getCellRestQty() {
    // оставшееся кол-во (строка) по поставке и ячейке, либо null
    if (vedVC == null) {
      return null;
    }
    ComplVedVCpos vedPos;
    BigDecimal q = BigDecimal.ZERO;
    for (Entry<String, ComplVedVCpos> i : vedVC.ved.entrySet()) {
      vedPos = i.getValue();
      q = q.add(vedPos.qty_rest);
    }
    if (q.signum() == 0) {
      return null;
    } else {
      return ProcessTask.delDecZeros(q.toString()) + " ед";
    }
  }

  public ComplRetQty getTovQty(String matnr, String charg) {
    if (vedVC == null) {
      return new ComplRetQty(null, false);
    }
    int n = 0;
    BigDecimal q = BigDecimal.ZERO;

    String key = lenum + "_" + matnr + "_" + charg;
    ComplVedVCpos vedPos = vedVC.ved.get(key);
    if (vedPos != null) {
      n++;
      q = q.add(vedPos.qty_rest);
    }

    key = lenum + "_" + matnr;
    vedPos = vedVC.ved.get(key);
    if (vedPos != null) {
      n++;
      q = q.add(vedPos.qty_rest);

      if (vedPos.qty_rest.signum() > 0) {
        Boolean bb = vedVC.strictPrt.get(charg);
        if (bb != null) {
          return new ComplRetQty(BigDecimal.ZERO, true);
        }
        bb = vedVC.allPrt.get(charg);
        if (bb == null) { // нужно проверить Е-запас
          Z_TS_COMPL20 f = new Z_TS_COMPL20();
          f.LGORT = lgort;
          f.MATNR = ProcessTask.fillZeros(matnr, 18);
          f.CHARG = ProcessTask.fillZeros(charg, 10);

          f.execute();

          if (f.isErr) {
            return new ComplRetQty(null, false);
          }

          if (f.IS_E.equals("X")) {
            return new ComplRetQty(BigDecimal.ZERO, true);
          }
        }
      }
    }

    if (n == 0) {
      return new ComplRetQty(null, false);
    } else {
      return new ComplRetQty(q, false);
    }
  }

  public String getPalQtyScan() {
    BigDecimal q = new BigDecimal(0);
    int n = 0;
    for (ComplScanData sd : scanData) {
      if (sd.pal.equals(lenum)) {
        q = q.add(sd.qtyM).add(sd.qtyP);
        n++;
      }
    }
    return "всего " + n + " скан, " + ProcessCompl.delDecZeros(q.toString()) + " ед";
  }

  public String getModSgmQty() {
    BigDecimal q = new BigDecimal(0);
    for (TovPos p : modSgmTov) {
      q = q.add(p.qty);
    }
    return "всего " + ProcessCompl.delDecZeros(q.toString()) + " ед";
  }

  public void callSetFreeCompl(boolean freeCompl, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (freeCompl != this.freeCompl) {
      dr.setB(FieldType.FREE_COMPL, freeCompl);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetIsSGM(boolean isSGM, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (isSGM != this.isSGM) {
      dr.setB(FieldType.IS_SGM, isSGM);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddVbeln(String vbeln,
          ZTS_COMPL_CELL_S[] it, ZTS_COMPL_FP_S[] it_fp,
          TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.VBELN, vbeln);
    dr.setI(FieldType.LOG, LogType.ADD_VBELN.ordinal());

    dr.setV(FieldType.TAB3);
    String[] t1 = new String[it.length];
    String[] t2 = new String[it.length];
    BigDecimal[] t3 = new BigDecimal[it.length];
    for (int i = 0; i < it.length; i++) {
      t1[i] = it[i].LGTYP;
      t2[i] = it[i].LGPLA;
      t3[i] = it[i].QTY;
    }
    dr.setSa(FieldType.LGTYPS, t1);
    dr.setSa(FieldType.LGPLAS, t2);
    dr.setNa(FieldType.QTYS, t3);

    dr.setV(FieldType.TAB2);
    t1 = new String[it_fp.length];
    t2 = new String[it_fp.length];
    for (int i = 0; i < it_fp.length; i++) {
      t1[i] = it_fp[i].LGPLA;
      t2[i] = it_fp[i].COMPL_FROM;
    }
    dr.setSa(FieldType.LGPLAS_FP, t1);
    dr.setSa(FieldType.COMPL_FROMS, t2);

    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLenum(String lenum, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lenum, this.lenum)) {
      dr.setS(FieldType.PAL, lenum);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLgort(String lgort, String noFreeCompl, String complToPal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      dr.setI(FieldType.LOG, LogType.SET_LGORT.ordinal());
    }
    if (this.noFreeCompl != strEq(noFreeCompl, "X")) {
      dr.setB(FieldType.NO_FREE_COMPL, strEq(noFreeCompl, "X"));
    }
    if (this.complToPal != strEq(complToPal, "X")) {
      dr.setB(FieldType.COMPL_TO_PAL, strEq(complToPal, "X"));
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetToPal(String toPal, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(toPal, this.toPal)) {
      dr.setS(FieldType.TO_PAL, toPal);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetZone(String zone, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.ZONE, zone);
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCell(String cell, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(cell, this.cell)) {
      dr.setS(FieldType.CELL, cell);
      dr.setI(FieldType.LOG, LogType.SET_CELL.ordinal());
    }
    if (!strEq(lenum, "")) {
      // сброс текущей паллеты
      dr.setS(FieldType.PAL, "");
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetNextCellVbeln(String nextCellScan, String nextVbelnScan, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(nextCellScan, this.nextCellScan)) {
      dr.setS(FieldType.NEXT_CELL, nextCellScan);
    }
    if (!strEq(nextVbelnScan, this.nextVbelnScan)) {
      dr.setS(FieldType.NEXT_VBELN, nextVbelnScan);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVbeln(String vbeln, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(vbeln, this.vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
      dr.setI(FieldType.LOG, LogType.SET_VBELN.ordinal());
    }
    if (!strEq(lenum, "")) {
      // сброс текущей паллеты
      dr.setS(FieldType.PAL, "");
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetModGgmNo(int sgm, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (sgm != modSgmNo) {
      dr.setI(FieldType.MOD_SGM, sgm);
      dr.setI(FieldType.LOG, LogType.SET_MOD_SGM.ordinal());
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetToPalPrevState(TaskState prevState, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (prevState != null) {
      dr.setI(FieldType.TO_PAL_PREV_STATE, prevState.ordinal());
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetPrevState(TaskState prevState, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setI(FieldType.PREV_TASK_STATE, prevState.ordinal());
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVbeln1(String vbeln, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(vbeln, this.vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
      dr.setI(FieldType.LOG, LogType.SET_VBELN.ordinal());
    }
    if (!strEq(lenum, "")) {
      // сброс текущей паллеты
      dr.setS(FieldType.PAL, "");
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLastCharg(String charg, BigDecimal qty, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.lastCharg, charg)) {
      dr.setS(FieldType.LAST_CHARG, charg);
    }
    if (this.lastQty.compareTo(qty) != 0) {
      dr.setN(FieldType.LAST_QTY, qty);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLastQty(BigDecimal qty, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (this.lastQty.compareTo(qty) != 0) {
      dr.setN(FieldType.LAST_QTY, qty);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetInfCompl(String infCompl, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.infCompl, infCompl)) {
      dr.setS(FieldType.INF_COMPL, infCompl);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCheckCompl(String checkCompl, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(this.checkCompl, checkCompl)) {
      dr.setS(FieldType.CHECK_COMPL, checkCompl);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, int sgm, BigDecimal qty, TaskContext ctx) throws Exception {
    // добавление данных о товаре (партия - без ведущих нулей)
    if (qty.signum() == 0) {
      return;
    }
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setI(FieldType.SGM, sgm);
    dr.setN(FieldType.QTY, qty);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddSgmTov(String matnr, String charg, BigDecimal qty, TaskContext ctx) throws Exception {
    // добавление данных о товаре в СГМ
    if (qty.signum() == 0) {
      return;
    }
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    dr.setN(FieldType.MOD_SGM_QTY, qty);
    dr.setI(FieldType.LOG, LogType.ADD_MOD_SGM_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSgmTovClear(TaskContext ctx) throws Exception {
    // удаление данных о товаре в СГМ
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.MOD_SGM_CLEAR);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearTovData(TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!scanData.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLastModSgm(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.MOD_SGM_DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetSaved(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.SAVED);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callClearVed(TaskContext ctx) throws Exception {
    // удаление данных о ведомости на комплектацию по ячейке/поставке
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (vedVC != null) {
      dr.setV(FieldType.CLEAR_VED);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVed(ZTS_COMPL_S[] it, ZDC_MM_SPT_PRT_S[] it_strict_prt, ZDC_MM_SPT_PRT_S[] it_all_prt, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();

    dr.setV(FieldType.TAB4);
    String[] t1 = new String[it.length];
    String[] t2 = new String[it.length];
    String[] t3 = new String[it.length];
    String[] t4 = new String[it.length];
    String[] t5 = new String[it.length];
    String[] t6 = new String[it.length];
    String[] t7 = new String[it.length];
    String[] t8 = new String[it.length];
    String[] t9 = new String[it.length];
    String[] t10 = new String[it.length];
    String[] t11 = new String[it.length];
    BigDecimal[] t12 = new BigDecimal[it.length];
    for (int i = 0; i < it.length; i++) {
      t1[i] = it[i].LGNUM;
      t2[i] = it[i].TANUM;
      t3[i] = it[i].TAPOS;
      t4[i] = it[i].LGTYP;
      t5[i] = it[i].LGPLA;
      t6[i] = it[i].LENUM;
      if (t6[i].length() == 20) {
        t6[i] = t6[i].substring(10);
      }
      t7[i] = ProcessTask.delZeros(it[i].MATNR);
      t8[i] = ProcessTask.delZeros(it[i].CHARG);
      t9[i] = it[i].ONLY_PRT;
      t10[i] = it[i].SOBKZ;
      t11[i] = it[i].SONUM;
      t12[i] = it[i].QTY;
    }
    dr.setSa(FieldType.LGNUMS, t1);
    dr.setSa(FieldType.TANUMS, t2);
    dr.setSa(FieldType.TAPOSS, t3);
    dr.setSa(FieldType.LGTYPS, t4);
    dr.setSa(FieldType.LGPLAS, t5);
    dr.setSa(FieldType.LENUMS, t6);
    dr.setSa(FieldType.MATNRS, t7);
    dr.setSa(FieldType.CHARGS, t8);
    dr.setSa(FieldType.ONLY_PRTS, t9);
    dr.setSa(FieldType.SOBKZS, t10);
    dr.setSa(FieldType.SONUMS, t11);
    dr.setNa(FieldType.QTYS, t12);

    // партии под особым запасом
    String[] t13 = new String[it_strict_prt.length];
    for (int i = 0; i < t13.length; i++) {
      t13[i] = it_strict_prt[i].CHARG;
    }
    dr.setSa(FieldType.STRICT_CHARGS, t13);

    // все партии
    String[] t14 = new String[it_all_prt.length];
    for (int i = 0; i < t14.length; i++) {
      t14[i] = it_all_prt[i].CHARG;
    }
    dr.setSa(FieldType.ALL_CHARGS, t14);

    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetCFTT(String complFrom, String tanum1, String tanum2,
          String a_cell, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();

    if (complFrom.equals("X") && !getFP(a_cell)) {
      dr.setS(FieldType.LGPLA_FP, a_cell);
      dr.setS(FieldType.COMPL_FROM, complFrom);
    }

    if (!strEq(this.tanum1, tanum1)) {
      dr.setS(FieldType.TANUM1, tanum1);
    }

    if (!strEq(this.tanum2, tanum2)) {
      dr.setS(FieldType.TANUM2, tanum2);
    }

    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  private void hdAddVbeln(DataRecord dr) {
    String vb = dr.getValStr(FieldType.VBELN);
    String[] t1 = (String[]) dr.getVal(FieldType.LGTYPS);
    String[] t2 = (String[]) dr.getVal(FieldType.LGPLAS);
    BigDecimal[] t3 = (BigDecimal[]) dr.getVal(FieldType.QTYS);
    int n = t1.length;
    ComplVCqty cq = new ComplVCqty();
    ZTS_COMPL_CELL_S r;
    BigDecimal qty;
    for (int i = 0; i < n; i++) {
      r = new ZTS_COMPL_CELL_S();
      r.LGTYP = t1[i];
      r.LGPLA = t2[i];
      r.QTY = t3[i];
      qty = cq.cellQty.get(r.LGPLA);
      if (qty == null) {
        qty = r.QTY;
      } else {
        qty = qty.add(r.QTY);
      }
      cq.cellQty.put(r.LGPLA, qty);
    }
    vcq.put(vb, cq);
  }

  private void hdAddFP(DataRecord dr) {
    String[] t1 = (String[]) dr.getVal(FieldType.LGPLAS_FP);
    String[] t2 = (String[]) dr.getVal(FieldType.COMPL_FROMS);
    for (int i = 0; i < t1.length; i++) {
      if (t2[i].equals("X")) {
        fp.put(t1[i], Boolean.TRUE);
      } else {
        fp.put(t1[i], Boolean.FALSE);
      }
    }
  }

  private void hdAddFP1(DataRecord dr) {
    String t1 = (String) dr.getVal(FieldType.LGPLA_FP);
    String t2 = (String) dr.getVal(FieldType.COMPL_FROM);
    if (t2.equals("X")) {
      fp.put(t1, Boolean.TRUE);
    } else {
      fp.put(t1, Boolean.FALSE);
    }
  }

  private void hdAddTov(DataRecord dr) {
    ComplScanData sd = new ComplScanData(lenum, toPal, dr.getValStr(FieldType.MATNR),
            dr.getValStr(FieldType.CHARG), (Integer) dr.getVal(FieldType.SGM),
            (BigDecimal) dr.getVal(FieldType.QTY));
    if (vedVC != null) {
      String key = lenum + "_" + sd.matnr + "_" + sd.charg;
      ComplVedVCpos vedPos = vedVC.ved.get(key);
      if ((vedPos == null) || (vedPos.qty_rest.signum() <= 0)) {
        // по партии ничего не связывается
        sd.qtyP = BigDecimal.ZERO;
      } else if (vedPos.qty_rest.compareTo(sd.qtyP) >= 0) {
        // всё связывается по партии
        sd.qtyM = BigDecimal.ZERO;
        vedPos.qty_rest = vedPos.qty_rest.subtract(sd.qtyP);
      } else {
        // по партии связывается только часть
        sd.qtyP = vedPos.qty_rest;
        vedPos.qty_rest = BigDecimal.ZERO;
        sd.qtyM = sd.qtyM.subtract(sd.qtyP);
      }
      // td.qty содержит то, что связалось по партии
      // td.qty_rest содержит то, что можно связать по материалу

      key = lenum + "_" + sd.matnr;
      vedPos = vedVC.ved.get(key);
      if ((vedPos == null) || (vedPos.qty_rest.signum() <= 0)) {
        // по материалу ничего не связывается (!!! такого не должно быть !!!)
        sd.qtyM = BigDecimal.ZERO;
      } else if (vedPos.qty_rest.compareTo(sd.qtyM) >= 0) {
        // всё связывается по материалу
        vedPos.qty_rest = vedPos.qty_rest.subtract(sd.qtyM);
      } else {
        // по материалу связывается только часть (!!! такого не должно быть !!!)
        sd.qtyM = vedPos.qty_rest;
        vedPos.qty_rest = BigDecimal.ZERO;
      }
      scanData.add(sd);
    }
  }

  private void hdDelLast() {
    if (vedVC != null) {
      ComplScanData sd = scanData.get(scanData.size() - 1);
      String key;
      ComplVedVCpos vedPos;
      if (sd.qtyP.signum() > 0) {
        // связь по партии
        key = sd.pal + "_" + sd.matnr + "_" + sd.charg;
        vedPos = vedVC.ved.get(key);
        if (vedPos != null) { // так должно быть всегда
          vedPos.qty_rest = vedPos.qty_rest.add(sd.qtyP);
        }
      }
      if (sd.qtyM.signum() > 0) {
        // связь по материалу
        key = sd.pal + "_" + sd.matnr;
        vedPos = vedVC.ved.get(key);
        if (vedPos != null) { // так должно быть всегда
          vedPos.qty_rest = vedPos.qty_rest.add(sd.qtyM);
        }
      }
    }

    if (scanData.size() > 0) {
      scanData.remove(scanData.size() - 1);
    }
  }

  private void hdClearCellQty() {
    // удаление кол-ва по ячейке и поставке
    ComplVCqty cq = vcq.get(vbeln);
    if ((cq == null) || (cq.cellQty == null)) {
      return;
    }
    BigDecimal qty = cq.cellQty.get(cell);
    if (qty == null) {
      return;
    }
    qty = BigDecimal.ZERO;
    cq.cellQty.put(cell, qty);
  }

  private void hdSetVed(DataRecord dr) {
    String[] t1 = (String[]) dr.getVal(FieldType.LGNUMS);
    String[] t2 = (String[]) dr.getVal(FieldType.TANUMS);
    String[] t3 = (String[]) dr.getVal(FieldType.TAPOSS);
    String[] t4 = (String[]) dr.getVal(FieldType.LGTYPS);
    String[] t5 = (String[]) dr.getVal(FieldType.LGPLAS);
    String[] t6 = (String[]) dr.getVal(FieldType.LENUMS);
    String[] t7 = (String[]) dr.getVal(FieldType.MATNRS);
    String[] t8 = (String[]) dr.getVal(FieldType.CHARGS);
    String[] t9 = (String[]) dr.getVal(FieldType.ONLY_PRTS);
    String[] t10 = (String[]) dr.getVal(FieldType.SOBKZS);
    String[] t11 = (String[]) dr.getVal(FieldType.SONUMS);
    BigDecimal[] t12 = (BigDecimal[]) dr.getVal(FieldType.QTYS);
    int n = t1.length;
    vedVC = new ComplVedVC();
    vedVC.data0 = new ZTS_COMPL_S[n];
    ZTS_COMPL_S r;
    ComplVedVCpos vedPos;
    String key;
    for (int i = 0; i < n; i++) {
      r = new ZTS_COMPL_S();
      r.LGNUM = t1[i];
      r.TANUM = t2[i];
      r.TAPOS = t3[i];
      r.LGTYP = t4[i];
      r.LGPLA = t5[i];
      r.LENUM = t6[i];
      r.MATNR = t7[i];
      r.CHARG = t8[i];
      r.ONLY_PRT = t9[i];
      r.SOBKZ = t10[i];
      r.SONUM = t11[i];
      r.QTY = t12[i];
      vedVC.data0[i] = r;
      if (r.ONLY_PRT.equals("X")) {
        key = r.LENUM + "_" + r.MATNR + "_" + r.CHARG;
      } else {
        key = r.LENUM + "_" + r.MATNR;
      }
      vedPos = vedVC.ved.get(key);
      if (vedPos == null) {
        if (r.ONLY_PRT.equals("X")) {
          vedPos = new ComplVedVCpos(r.LENUM, r.MATNR, r.CHARG, r.QTY);
        } else {
          vedPos = new ComplVedVCpos(r.LENUM, r.MATNR, null, r.QTY);
        }
      } else {
        vedPos.qty = vedPos.qty.add(r.QTY);
        vedPos.qty_rest = vedPos.qty;
      }
      vedVC.ved.put(key, vedPos);
    }

    if (dr.haveVal(FieldType.STRICT_CHARGS)) {
      String[] t13 = (String[]) dr.getVal(FieldType.STRICT_CHARGS);
      for (String s : t13) {
        vedVC.strictPrt.put(s, Boolean.TRUE);
      }
    }

    if (dr.haveVal(FieldType.ALL_CHARGS)) {
      String[] t14 = (String[]) dr.getVal(FieldType.ALL_CHARGS);
      for (String s : t14) {
        vedVC.allPrt.put(s, Boolean.TRUE);
      }
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = (String) dr.getVal(FieldType.LGORT);
        }

        if (dr.haveVal(FieldType.NO_FREE_COMPL)) {
          noFreeCompl = (Boolean) dr.getVal(FieldType.NO_FREE_COMPL);
        }

        if (dr.haveVal(FieldType.COMPL_TO_PAL)) {
          complToPal = (Boolean) dr.getVal(FieldType.COMPL_TO_PAL);
        }

        if (dr.haveVal(FieldType.TO_PAL)) {
          toPal = (String) dr.getVal(FieldType.TO_PAL);
        }

        if (dr.haveVal(FieldType.TO_PAL_PREV_STATE)) {
          toPalPrevState = (Integer) dr.getVal(FieldType.TO_PAL_PREV_STATE);
        }

        if (dr.haveVal(FieldType.ZONE)) {
          String zone = dr.getValStr(FieldType.ZONE);
          if (!toPal.isEmpty() && !zone.isEmpty()) {
            palZone.add(new ComplPalZone(toPal, zone));
          }
          toPal = "";
        }

        if (dr.haveVal(FieldType.CELL)) {
          cell = dr.getValStr(FieldType.CELL);
        }

        if (dr.haveVal(FieldType.NEXT_CELL)) {
          nextCellScan = dr.getValStr(FieldType.NEXT_CELL);
        }

        if (dr.haveVal(FieldType.NEXT_VBELN)) {
          nextVbelnScan = dr.getValStr(FieldType.NEXT_VBELN);
        }

        if (dr.haveVal(FieldType.LAST_CHARG)) {
          lastCharg = dr.getValStr(FieldType.LAST_CHARG);
        }

        if (dr.haveVal(FieldType.LAST_QTY)) {
          lastQty = (BigDecimal) dr.getVal(FieldType.LAST_QTY);
        }

        if (dr.haveVal(FieldType.VBELN)) {
          vbeln = dr.getValStr(FieldType.VBELN);
          lastVbeln = vbeln;
        }

        if (dr.haveVal(FieldType.TAB3)) {
          hdAddVbeln(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.TAB2)) {
          hdAddFP(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.LGPLA_FP) && dr.haveVal(FieldType.COMPL_FROM)) {
          hdAddFP1(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.MATNR) && dr.haveVal(FieldType.CHARG) && dr.haveVal(FieldType.QTY) && dr.haveVal(FieldType.SGM)) {
          hdAddTov(dr); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.DEL_LAST) && scanData.size() > 0) {
          hdDelLast(); // логика перенесена в процедуру
        }

        if (dr.haveVal(FieldType.MOD_SGM_DEL_LAST) && modSgmTov.size() > 0) {
          modSgmTov.remove(modSgmTov.size() - 1);
        }

        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          while (scanData.size() > 0) {
            hdDelLast();
          }
        }

        if (dr.haveVal(FieldType.INF_COMPL)) {
          infCompl = (String) dr.getVal(FieldType.INF_COMPL);
        }

        if (dr.haveVal(FieldType.CHECK_COMPL)) {
          checkCompl = (String) dr.getVal(FieldType.CHECK_COMPL);
        }

        if (dr.haveVal(FieldType.FREE_COMPL)) {
          freeCompl = (Boolean) dr.getVal(FieldType.FREE_COMPL);
        }

        if (dr.haveVal(FieldType.IS_SGM)) {
          isSGM = (Boolean) dr.getVal(FieldType.IS_SGM);
        }

        if (dr.haveVal(FieldType.PAL)) {
          lenum = (String) dr.getVal(FieldType.PAL);
        }

        if (dr.haveVal(FieldType.SAVED)) {
          scanData.clear();
          hdClearCellQty();
          vedVC = null;
        }

        if (dr.haveVal(FieldType.CLEAR_VED)) {
          vedVC = null;
        }

        if (dr.haveVal(FieldType.TAB4)) {
          hdSetVed(dr); // логика перенесена в процедуру
        }

//        if (dr.haveVal(FieldType.TAB5)) {
//          hdSetVedSK(dr); // логика перенесена в процедуру
//        }
        if (dr.haveVal(FieldType.TANUM1)) {
          tanum1 = (String) dr.getVal(FieldType.TANUM1);
        }

        if (dr.haveVal(FieldType.TANUM2)) {
          tanum2 = (String) dr.getVal(FieldType.TANUM2);
        }

        if (dr.haveVal(FieldType.MOD_SGM)) {
          modSgmNo = (Integer) dr.getVal(FieldType.MOD_SGM);
          modSgmTov.clear();
        }

        if (dr.haveVal(FieldType.PREV_TASK_STATE)) {
          modSgmPrevState = (Integer) dr.getVal(FieldType.PREV_TASK_STATE);
        }

        if (dr.haveVal(FieldType.MOD_SGM_CLEAR)) {
          modSgmNo = 0;
          modSgmTov.clear();
        }

        if (dr.haveVal(FieldType.MOD_SGM_QTY)) {
          modSgmTov.add(new TovPos(dr.getValStr(FieldType.MATNR),
                  dr.getValStr(FieldType.CHARG),
                  (BigDecimal) dr.getVal(FieldType.MOD_SGM_QTY)));
        }

        break;
    }
  }
}
