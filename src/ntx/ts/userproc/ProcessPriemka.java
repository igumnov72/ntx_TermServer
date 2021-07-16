package ntx.ts.userproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import ntx.sap.fm.*;
import ntx.sap.refs.*;
import ntx.sap.struct.ZTS_IN12_S;
import ntx.ts.html.*;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.ScanChargQty;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;
import ntx.ts.sysproc.ProcData;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import static ntx.ts.sysproc.ProcessUtil.getScanChargQty;
import static ntx.ts.sysproc.ProcessUtil.isScanTov;
import ntx.ts.sysproc.TaskContext;
import ntx.ts.sysproc.UserContext;

/**
 * Выполнение приемки товара на склад Состояния: START VBELN VBELN_TOV DAT_PR
 * TOV_PAL TOV_PAL_CELL FIN_MSG CHECK_PAL
 */
public class ProcessPriemka extends ProcessTask {

  private final PriemkaData d = new PriemkaData();

  public ProcessPriemka(long procId) throws Exception {
    super(ProcType.PRIEMKA, procId);
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
      case FIN_MSG:
        return (new HtmlPageMessage(getLastErr(), getLastMsg(), "fin_msg", null)).getPage();

      default:
        return htmlWork("Приемка", playSound, ctx);
    }
  }

  private FileData init(TaskContext ctx) throws Exception {
    // запуск задачи
    callSetTaskState(TaskState.VBELN, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleScan(String scan, TaskContext ctx) throws Exception {
    if (scan.equals("00")) {
      return htmlMenu();
    }

    switch (getTaskState()) {
      case VBELN:
        return handleScanVbeln0(scan, ctx);

      case VBELN_TOV:
        return handleScanVbelnTov(scan, ctx);

      case TOV_PAL:
        return handleScanTovPal(scan, ctx);

      case TOV_PAL_CELL:
        return handleScanTovPalCell(scan, ctx);

      case DAT_PR:
        return handleScanDatPr(scan, ctx);

      case CHECK_PAL:
        return handleScanCheckPal(scan, ctx);

      default:
        callSetErr("Ошибка программы: недопустимое состояние "
                + getTaskState().name() + " (сканирование не принято)", ctx);
        return htmlGet(true, ctx);
    }
  }

  private FileData htmlMenu() throws Exception {
    String def;

    switch (getTaskState()) {
      case VBELN:
        def = "cont:Назад;later:Отложить;fin:Завершить";
        break;

      case VBELN_TOV:
        def = "cont:Назад;delvbeln:Удалить поставку из ПНП;delpal:Удалить паллету;check_pal:Проверка паллет;diff:Расхождения;later:Отложить;fin:Завершить";
        break;

      case TOV_PAL:
      case TOV_PAL_CELL:
      case DAT_PR:
        def = "cont:Назад;dellast:Отменить последнее сканирование;delall:Отменить всё по текущей паллете;later:Отложить";
        break;

      case CHECK_PAL:
        def = "cont:Назад;exit_check_pal:Выход из проверки паллет;later:Отложить";
        break;

      case FIN_MSG:
        return null;

      default:
        def = "cont:Назад;later:Отложить";
        break;
    }

    if (RefInfo.haveInfo(ProcType.PRIEMKA)) {
      def = def + ";manuals:Инструкции";
    }

    HtmlPageMenu p = new HtmlPageMenu("Приемка", "Выберите действие",
            def, null, null, null);

    return p.getPage();
  }

  private FileData handleMenu(String menu, TaskContext ctx) throws Exception {
    if (menu.equals("delall")) {
      callClearErrMsg(ctx);
      return handleMenuDelAll(ctx); // Отменить всё по текущей паллете
    } else if (menu.equals("dellast")) {
      callClearErrMsg(ctx);
      return handleMenuDelLast(ctx); // Отменить последнее сканирование
    } else if (menu.equals("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPal(ctx); // Удалить паллету
    } else if (menu.startsWith("delpal")) {
      callClearErrMsg(ctx);
      return handleMenuDelPalDo(menu.substring(6), ctx); // Удалить паллету
    } else if (menu.equals("delvbeln")) {
      callClearErrMsg(ctx);
      return handleMenuDelVbeln(ctx); // Удаление поставки из ПНП
    } else if (menu.startsWith("delvbeln")) {
      callClearErrMsg(ctx);
      return handleMenuDelVbelnDo(menu.substring(8), ctx); // Удаление поставки из ПНП
    } else if (menu.equals("fin")) {
      callClearErrMsg(ctx);
      return handleMenuFin(ctx); // Завершить
    } else if (menu.equals("exit_check_pal")) {
      callClearErrMsg(ctx);
      return handleExitCheckPal(ctx);
    } else if (menu.equals("check_pal")) {
      callClearErrMsg(ctx);
      return handleCheckPal(ctx);
    } else if (menu.equals("later")) {
      callTaskDeactivate(ctx);
      return null;
    } else if (menu.equals("diff")) {
      callClearErrMsg(ctx);
      return handleMenuDiff(ctx);
    } else {
      return htmlGet(false, ctx);
    }
  }

  private FileData handleMenuDiff(TaskContext ctx) throws Exception {
    HtmlPage p = new HtmlPage();
    p.title = "Расхождения";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;

    Z_TS_IN12 f = new Z_TS_IN12();
    f.VBELN = fillZeros(d.getVbeln(), 10);

    f.execute();

    if (f.isErr) {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }

    p.addLine("<b>Расхождения отсканированного товара с данными поставки</b>");
    p.addLine("<font color=blue>недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
            + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
            + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString()) + "</font>");
    if (f.IT.length > 0) {
      p.addText("<div style=\"font-size:" + TSparams.fontSize3
              + "pt\"><table width=100% border=1 cellpadding=1 cellspacing=0>\r\n");
      p.addText("<tr valign=top><td colspan=4><b>Номер и название мат</b></td></tr>\r\n");
      p.addText("<tr valign=top><td rowspan=2><b>Партия</b></td>");
      p.addText("<td align=right><b>Пост</b></td><td align=right><b>Скан</b></td><td align=right><b>Разн</b></td></tr>\r\n");
      p.addText("<tr valign=top><td colspan=3><b>Паллеты (посл 3 циф)</b></td></tr>\r\n");

      String m1 = "";
      String m2;
      for (ZTS_IN12_S wa : f.IT) {
        m2 = delZeros(wa.MATNR);

        if (!m1.equals(m2)) {
          m1 = m2;
          p.addText("<tr><td colspan=4>");
          p.addText(m1);
          p.addText(": <font color=blue>");
          p.addText(RefMat.getName(m1));
          p.addText("</font></td></tr>\r\n");
        }

        if (wa.PALS.length() > 0) {
          p.addText("<tr valign=top><td rowspan=2>");
        } else {
          p.addText("<tr valign=top><td>");
        }
        p.addText(delZeros(wa.CHARG));
        p.addText("</td><td align=right><b><font color=blue>" + delDecZeros(wa.QTY_V.toString())
                + "</font></b></td><td align=right><b>" + delDecZeros(wa.QTY_SCAN.toString())
                + "</b></td><td align = right><b>" + delDecZeros(wa.QTY_DIF.toString())
                + "</b></td></tr>\r\n");
        if (wa.PALS.length() > 0) {
          p.addText("<tr><td colspan=3><font color=green>" + wa.PALS + "</font></td></tr>\r\n");
        }
      }
      p.addText("</table></div>\r\n");
    }

    p.addNewLine();

    p.addFormStart("work.html", "f");
    p.addFormButtonSubmitGo("Продолжить");
    p.addFormEnd();

    return p.getPage();
  }

  private FileData handleMenuDelAll(TaskContext ctx) throws Exception {
    // Отменить всё по текущей паллете
    d.callClearTov(ctx);
    callSetTaskState(TaskState.VBELN_TOV, ctx);
    callSetMsg("Сканирование товара по текущей паллете отменено", ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuDelLast(TaskContext ctx) throws Exception {
    // Отменить последнее сканирование
    if (getTaskState() == TaskState.DAT_PR) {
      // состояние - ввод даты выпуска, товар в массив еще не записан
      String scan = d.getLastScan();
      String charg = getScanCharg(scan);
      try {
        RefChargStruct c = RefCharg.getNoNull(charg, d.getVbeln());
        String s = "Отменено: " + delDecZeros(getScanQty(scan).toString()) + " ед: "
                + c.matnr + "/" + charg + " " + RefMat.getName(c.matnr);
        s = s + " (на паллете: " + d.getTovM().size() + " мат; "
                + delDecZeros(d.getQtyPal().toString()) + " ед; "
                + d.getNScan() + " скан)";
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        if (d.getTov().isEmpty()) {
          callSetTaskState(TaskState.VBELN_TOV, ctx);
        } else if (d.getIsRet()) {
          callSetTaskState(TaskState.TOV_PAL_CELL, ctx);
        } else {
          callSetTaskState(TaskState.TOV_PAL, ctx);
        }
      } catch (Exception e) {
        callSetErr(e.getMessage(), ctx);
      }
      return htmlGet(true, ctx);
    }

    ArrayList<PriemkaTovData> tov = d.getTov();

    if (tov.isEmpty()) {
      callSetErr("Нет последнего сканирования", ctx);
      return htmlGet(true, ctx);
    }

//    try {
    PriemkaTovData td = tov.get(tov.size() - 1);
    String s = "Отменено: " + delDecZeros(td.qty.toString())
            + " ед: " + td.matnr + "/" + td.charg + " "
            + RefMat.getName(td.matnr);
    if (td.prDat != null) {
      s = s + "; выпуск: " + td.prDat.substring(6, 8) + "."
              + td.prDat.substring(4, 6) + "." + td.prDat.substring(0, 4);
    }

    callSetMsg(s, ctx);
    callAddHist(s, ctx);
    d.callDelLast(ctx);
    if (d.getTov().isEmpty()) {
      callSetTaskState(TaskState.VBELN_TOV, ctx);
      d.callClearTov(ctx);
    } else if (d.getIsRet()) {
      callSetTaskState(TaskState.TOV_PAL_CELL, ctx);
    } else {
      callSetTaskState(TaskState.TOV_PAL, ctx);
    }
//    } catch (Exception e) {
//      callSetErr(e.getMessage());
//    }
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelPal(TaskContext ctx) throws Exception {
    // Удалить паллету

    Z_TS_IN6 f = new Z_TS_IN6();
    f.VBELN = fillZeros(d.getVbeln(), 10);

    f.execute();

    if (!f.isErr) {
      if (f.PALS.isEmpty()) {
        callSetErr("Не просканировано ни одной паллеты", ctx);
        return htmlGet(true, ctx);
      }
      String[] pp = f.PALS.split(",");
      String def = "cont:Назад";
      for (String s : pp) {
        def = def + ";" + "delpal" + s + ":" + s;
      }
      return (new HtmlPageMenu("Удаление паллеты", "Удаление раскладки по паллете:",
              def, null, null, null)).getPage();
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleCheckPal(TaskContext ctx) throws Exception {
    callSetMsg("Включен режим проверки паллет", ctx);
    callSetTaskState(TaskState.CHECK_PAL, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleExitCheckPal(TaskContext ctx) throws Exception {
    callSetMsg("Проверка паллет завершена", ctx);
    callSetTaskState(TaskState.VBELN_TOV, ctx);
    return htmlGet(false, ctx);
  }

  private FileData handleMenuDelPalDo(String pal, TaskContext ctx) throws Exception {
    // Удалить паллету

    Z_TS_IN7 f = new Z_TS_IN7();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.PAL = pal;

    f.execute();

    if (!f.isErr) {
      String s = "Удалена раскладка по паллете  " + pal
              + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
              + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
      if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
        s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelVbeln(TaskContext ctx) throws Exception {
    // Удаление поставки из ПНП

    Z_TS_IN8 f = new Z_TS_IN8();
    f.VBELN = fillZeros(d.getVbeln(), 10);

    f.execute();

    if (!f.isErr) {
      if (f.VBELNS.isEmpty()) {
        callSetErr("Приемка не является приемкой по нескольким поставкам (ПНП), удалять нечего", ctx);
        return htmlGet(true, ctx);
      }
      String[] vv = f.VBELNS.split(",");
      if (vv.length == 1) {
        callSetErr("Ошибка программы: в списке всего одна поставка", ctx);
        return htmlGet(true, ctx);
      }

      String def = "cont:Назад";
      String v0 = d.getVbeln();
      for (String s : vv) {
        if (!v0.equals(s)) {
          def = def + ";" + "delvbeln" + s + ":" + s;
        }
      }
      return (new HtmlPageMenu("Удаление поставки", "Удаление поставки из ПНП:",
              def, null, null, null)).getPage();
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleMenuDelVbelnDo(String vbeln, TaskContext ctx) throws Exception {
    // Удаление поставки из ПНП

    Z_TS_IN3 f = new Z_TS_IN3();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.VBELN2 = fillZeros(vbeln, 10);

    f.execute();

    if (!f.isErr) {
      String s = "Удалена поставка  " + vbeln;
      if (!f.VBELNS.isEmpty()) {
        s = s + " (ПНП: " + f.VBELNS + ")";
      }
      s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
              + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
      if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
        s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleMenuFin(TaskContext ctx) throws Exception {
    // Завершить

    Z_TS_IN5 f = new Z_TS_IN5();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.TASK_ID = Long.toString(getProcId());

    f.execute();

    if (!f.isErr) {
      if ((f.N_ERR_DP == 0) && (f.QTY_DIF.QTY_IZL.signum() == 0)
              && (f.QTY_DIF.QTY_NEDOST.signum() == 0)
              && (f.QTY_DIF.QTY_PRT.signum() == 0)) {
        callTaskFinish(ctx);
        return null;
      }

      // есть что сказать пользователю
      String s = "Приемка по поставке " + d.getVbeln() + " завершена";
      if (f.N_ERR_DP > 0) {
        s = s + "\r\n<br>Имеются ошибки в датах выпуска (" + f.N_ERR_DP + " шт), исправьте их в транзакции za12";
      }
      if ((f.QTY_DIF.QTY_IZL.signum() != 0)
              || (f.QTY_DIF.QTY_NEDOST.signum() != 0)
              || (f.QTY_DIF.QTY_PRT.signum() != 0)) {
        s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
                + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
        if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
          s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                  + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                  + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
        }
      }
      callSetMsg(s, ctx);
      callSetTaskState(TaskState.FIN_MSG, ctx);
      return htmlGet(false, ctx);
    } else {
      callSetErr(f.err, ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanVbeln0(String scan, TaskContext ctx) throws Exception {
    if (!isScanVbeln(scan)) {
      callSetErr("Требуется отсканировать ШК поставки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_IN1 f = new Z_TS_IN1();
    f.VBELN = fillZeros(getScanVbeln(scan), 10);
    f.TASK_ID = Long.toString(getProcId());
    f.TASK_DT = df.format(new Date(getProcId()));
    f.TASK_USER = ctx.user.getUserName();

    f.execute();

    if (!f.isErr) {
      if (!rightsLgort(f.LGORT, ctx)) {
        callSetErr("Нет прав по складу " + f.LGORT, ctx);
      } else {
        try {
          d.callSetVbeln0(delZeros(f.VBELN2), f.LGORT,
                  (f.CHECK_DP.equals("X")), TaskState.VBELN_TOV,
                  (f.IS_RET.equals("X")), (f.IS_1_MAT.equals("X")), ctx);
        } catch (Exception e) {
          callSetErr(e.getMessage(), ctx);
        }
        String s = "Приемка по поставке " + d.getVbeln();
        if (!f.VBELNS.isEmpty()) {
          s = s + " (ПНП: " + f.VBELNS + ")";
        }
        if (f.ZCOMP_CLIENT.equals("X")) {
          s = s + "; КРОССДОКИНГ";
        }
        s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
                + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
        if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
          s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                  + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                  + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
        }
        callSetMsg(s, ctx);
        callAddHist(s, ctx);
        callTaskNameChange(ctx);
      }
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanVbelnTov(String scan, TaskContext ctx) throws Exception {
    if (isScanVbeln(scan)) {
      return handleScanVbeln(scan, ctx);
    } else if (isScanTovMk(scan)) {
      d.callClearTov(ctx);
      return handleScanTov(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК поставки или товар (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanTovPal(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTov(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара или паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanCheckPal(String scan, TaskContext ctx) throws Exception {
    if (!isScanPal(scan)) {
      callSetErr("Требуется отсканировать ШК проверяемой паллеты (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_IN11 f = new Z_TS_IN11();
    f.PAL = scan.substring(1);
    f.VBELN = fillZeros(d.getVbeln(), 10);

    f.execute();

    if (!f.isErr) {
      callSetMsg(f.MSG, ctx);
    } else {
      callSetErr(f.err, ctx);
    }

    return htmlGet(true, ctx);
  }

  private FileData handleScanTovPalCell(String scan, TaskContext ctx) throws Exception {
    if (isScanPal(scan)) {
      return handleScanPal(scan, ctx);
    } else if (isScanCell(scan)) {
      return handleScanCell(scan, ctx);
    } else if (isScanTovMk(scan)) {
      return handleScanTov(scan, ctx);
    } else {
      callSetErr("Требуется отсканировать ШК товара, паллеты или ячейки (сканирование " + scan + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
  }

  private FileData handleScanVbeln(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана
    Z_TS_IN2 f = new Z_TS_IN2();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.VBELN2 = fillZeros(getScanVbeln(scan), 10);

    f.execute();

    if (!f.isErr) {
      String s = "Добавлена поставка " + delZeros(f.VBELN2);
      if (!f.VBELNS.isEmpty()) {
        s = s + " (ПНП: " + f.VBELNS + ")";
      }
      if (f.ZCOMP_CLIENT.equals("X")) {
        s = s + "; КРОССДОКИНГ";
      }
      s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
              + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
      if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
        s = s + "; недост: "
                + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanTov(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана

    if (d.getCheckDp()) {
      try {
        String charg = getScanCharg(scan);
        RefChargStruct c = RefCharg.get(charg, d.getVbeln());
        if (c == null) {
          callSetErr("Нет такой партии (сканирование " + scan + " не принято)", ctx);
          return htmlGet(true, ctx);
        }
        d.callSetLastScan(scan, TaskState.DAT_PR, ctx);

        String s = delDecZeros(getScanQty(scan).toString()) + " ед: " + c.matnr + "/" + charg + " " + RefMat.getName(c.matnr);
// если нужно выводить список ячеект до ввода даты выпуска:
//        if (d.getIsRet()) {
//          String s2 = getTovCells(c.matnr);
//          if (s2 != null) {
//            s = s + " <br>\r\n" + s2;
//          }
//        }
        callSetMsg(s, ctx);
      } catch (Exception e) {
        callSetErr(e.getMessage(), ctx);
      }
      return htmlGet(true, ctx);
    } else {
      return handleScanTovDo(scan, null, ctx);
    }
  }

  private FileData handleScanDatPr(String scan, TaskContext ctx) throws Exception {
    return handleScanTovDo(d.getLastScan(), scan, ctx);
  }

  private FileData handleScanTovDo(String scanTov, String scanDp, TaskContext ctx) throws Exception {
    String dp = null;

    if (d.getCheckDp()) {
      // проверяем дату выпуска
      Date dt = parseDateDDMMYY(scanDp);
      if (dt == null) {
        callSetErr("Дата выпуска должна быть в формате ДДММГГ (6 цифр)", ctx);
        return htmlGet(true, ctx);
      }
      if (dt.compareTo(new Date()) > 0) {
        callSetErr("Будущая дата выпуска недопустима", ctx);
        return htmlGet(true, ctx);
      }
      dp = parseDf2.format(dt);
    }

    if (d.scanIsDouble(scanTov)) {
      callSetErr("ШК дублирован (сканирование " + scanTov + " не принято)", ctx);
      return htmlGet(true, ctx);
    }

    ScanChargQty scanInf; 
    scanInf = getScanChargQty(scanTov);
    if (!scanInf.err.isEmpty()) {
      callSetErr(scanInf.err + " (сканирование " + scanTov + " не принято)", ctx);
      return htmlGet(true, ctx);
    }
    
    try {
      String charg = scanInf.charg;// getScanCharg(scanTov);
      RefChargStruct c = RefCharg.get(charg, d.getVbeln());
      if (c == null) {
        callSetErr("Нет такой партии (сканирование " + scanTov + " не принято)", ctx);
        return htmlGet(true, ctx);
      }
      BigDecimal qty = scanInf.qty;// getScanQty(scanTov);
      d.callAddTov(c.matnr, charg, dp, qty, scanTov, ctx);
      String s = delDecZeros(qty.toString()) + " ед: " + c.matnr + "/" + charg + " " + RefMat.getName(c.matnr);
      if (d.getCheckDp() && (dp != null)) {
        s = s + "; выпуск: " + dp.substring(6, 8) + "."
                + dp.substring(4, 6) + "." + dp.substring(0, 4);
      }
      s = s + " (на паллете: " + d.getTovM().size() + " мат; "
              + delDecZeros(d.getQtyPal().toString()) + " ед; "
              + d.getNScan() + " скан)";
      callAddHist(s, ctx);
      if (d.getIsRet()) {
        String s2 = getTovCells(c.matnr);
        if (s2 != null) {
          s = s + " <br>\r\n" + s2;
        }
      }
      callSetMsg(s, ctx);
      if (d.getIsRet()) {
        callSetTaskState(TaskState.TOV_PAL_CELL, ctx);
      } else {
        callSetTaskState(TaskState.TOV_PAL, ctx);
      }
    } catch (Exception e) {
      callSetErr(e.getMessage(), ctx);
    }
    return htmlGet(true, ctx);
  }

  private String getTovCells(String matnr) throws Exception {
    Z_TS_IN10 f = new Z_TS_IN10();
    f.LGNUM = d.getLgnum();
    f.MATNR = fillZeros(matnr, 18);
    f.execute();
    return f.isErr || f.CELLS.isEmpty() ? null : f.CELLS;
  }

  private FileData handleScanPal(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана

    ArrayList<PriemkaTovData> tov = d.getTov();
    if (tov.isEmpty()) {
      callSetErr("Товар не отсканирован, нечего привязывать к паллете", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_IN4 f = new Z_TS_IN4();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.PAL = scan.substring(1);
    f.LGORT = d.getLgort();
    f.LGNUM = d.getLgnum();
    f.TSD_USER = ctx.user.getUserSHK();

    int n = tov.size();
    f.IT_TOV_create(n);
    PriemkaTovData tt;
    for (int i = 0; i < n; i++) {
      tt = tov.get(i);
      f.IT_TOV[i].MATNR = fillZeros(tt.matnr, 18);
      f.IT_TOV[i].CHARG = fillZeros(tt.charg, 10);
      if (d.getCheckDp()) {
        f.IT_TOV[i].PROD_DT = tt.prDat;
      }
      f.IT_TOV[i].QTY = tt.qty;
    }

    f.execute();

    if (!f.isErr) {
      String s = "Товар привязан к паллете " + f.PAL;
      if (f.PREV_DELETED.equals("X")) {
        s = s + " (предыдущие данные по паллете удалены)";
      }
      if (!f.LGPLA.isEmpty()) {
        s = s + "\r\n<br>!!! По учету паллета находится в " + f.LGPLA;
      }
      s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
              + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
      if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
        s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
      callSetTaskState(TaskState.VBELN_TOV, ctx);
      d.callClearTov(ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleScanCell(String scan, TaskContext ctx) throws Exception {
    // проверка типа ШК уже сделана

    ArrayList<PriemkaTovData> tov = d.getTov();
    if (tov.isEmpty()) {
      callSetErr("Товар не отсканирован, нечего привязывать к паллете", ctx);
      return htmlGet(true, ctx);
    }

    Z_TS_IN9 f = new Z_TS_IN9();
    f.VBELN = fillZeros(d.getVbeln(), 10);
    f.CELL = scan.substring(1);
    f.LGORT = d.getLgort();
    f.LGNUM = d.getLgnum();
    f.TSD_USER = ctx.user.getUserSHK();

    int n = tov.size();
    f.IT_TOV_create(n);
    PriemkaTovData tt;
    for (int i = 0; i < n; i++) {
      tt = tov.get(i);
      f.IT_TOV[i].MATNR = fillZeros(tt.matnr, 18);
      f.IT_TOV[i].CHARG = fillZeros(tt.charg, 10);
      if (d.getCheckDp()) {
        f.IT_TOV[i].PROD_DT = tt.prDat;
      }
      f.IT_TOV[i].QTY = tt.qty;
    }

    f.execute();

    if (!f.isErr) {
      f.LENUM = f.LENUM.substring(1);
      String s = "Товар привязан к паллете " + f.LENUM.substring(10) + " в ячейке " + f.LGPLA;
      if (f.PREV_DELETED.equals("X")) {
        s = s + " (предыдущие данные по паллете удалены)";
      }
      s = s + "\r\n<br>КОЛ-ВО пост: " + delDecZeros(f.QTY_DIF.QTY_VBEL.toString())
              + "; скан: " + delDecZeros(f.QTY_DIF.QTY_SCAN.toString());
      if (f.QTY_DIF.QTY_SCAN.signum() == 1) {
        s = s + "; недост: " + delDecZeros(f.QTY_DIF.QTY_NEDOST.toString())
                + "; изл: " + delDecZeros(f.QTY_DIF.QTY_IZL.toString())
                + "; парт: " + delDecZeros(f.QTY_DIF.QTY_PRT.toString());
      }
      callSetMsg(s, ctx);
      callAddHist(s, ctx);
      callSetTaskState(TaskState.VBELN_TOV, ctx);
      d.callClearTov(ctx);
    } else {
      callSetErr(f.err, ctx);
    }
    return htmlGet(true, ctx);
  }

  private FileData handleFinMsg(String htext1, TaskContext ctx) throws Exception {
    if ((htext1 != null) && htext1.equals("fin_msg")) {
      callTaskFinish(ctx);
      return null;
    } else {
      return htmlGet(false, ctx);
    }
  }

  private boolean haveTov() {
    if (d.getTov().isEmpty()) {
      return false;
    }
    Iterator i = d.getTov().iterator();
    PriemkaTovData td;
    while (i.hasNext()) {
      td = (PriemkaTovData) i.next();
      if (td.qty.signum() == 1) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    super.handleData(dr, ctx);
    d.handleData(dr, ctx);
  }

  @Override
  public String procName() {
    return getProcType().text + " " + d.getVbeln() + " " + d.getLgort() + " " + df2.format(new Date(getProcId()));
  }

  @Override
  public String getAddTaskName(UserContext ctx) throws Exception {
    if (!d.getLgort().isEmpty() && !d.getVbeln().isEmpty()) {
      return d.getVbeln() + " на " + d.getLgort() + " " + RefLgort.getNoNull(d.getLgort()).name;
    } else {
      return null;
    }
  }
}

class PriemkaTovData {

  public String matnr;
  public String charg;
  public String prDat;
  public BigDecimal qty;
  public String scan;

  public PriemkaTovData(String matnr, String charg, String prDat, 
          BigDecimal qty, String scan) {
    this.matnr = matnr;
    this.charg = charg;
    this.prDat = prDat;
    this.qty = qty;
    this.scan = scan;
  }

  public PriemkaTovData(String matnr, String charg, BigDecimal qty, 
          String scan) {
    this.matnr = matnr;
    this.charg = charg;
    this.prDat = null;
    this.qty = qty;
    this.scan = scan;
  }
}

class PriemkaData extends ProcData {

  private String vbeln = ""; // главная поставка
  private String lgort = ""; // склад
  private String lgnum = "";
  private String lastScan = "";
  private boolean checkDp = false; // признак необходимости ввода даты производства
  private boolean isRet = false; // признак возврата
  private boolean mat1 = false; // один материал в ячейке
  private final ArrayList<PriemkaTovData> tov = new ArrayList<PriemkaTovData>(); // товар на паллете
  private BigDecimal qtyPal = BigDecimal.ZERO; // кол-во на паллете
  private final HashMap<String, BigDecimal> tovM = new HashMap<String, BigDecimal>(); // товар на паллете по материалам

  public int getNScan() {
    return tov.size();
  }

  public BigDecimal getQtyPal() {
    return qtyPal;
  }

  public String getVbeln() {
    return vbeln;
  }

  public String getLgort() {
    return lgort;
  }

  public String getLgnum() {
    return lgnum;
  }

  public String getLastScan() {
    return lastScan;
  }

  public boolean getCheckDp() {
    return checkDp;
  }

  public boolean getIsRet() {
    return isRet;
  }

  public boolean getMat1() {
    return mat1;
  }

  public ArrayList<PriemkaTovData> getTov() {
    return tov;
  }

  public HashMap<String, BigDecimal> getTovM() {
    return tovM;
  }

  public boolean scanIsDouble(String scan) {
    int n = tov.size();
    for (int i = 0; i < n; i++) 
        if (tov.get(i).scan.equals(scan) && !isScanTov(scan)) return true;
    return false;
  }
  
  public void callClearTov(TaskContext ctx) throws Exception {
    // удаление данных о товаре
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!tov.isEmpty()) {
      dr.setV(FieldType.CLEAR_TOV_DATA);
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetVbeln0(String vbeln, String lgort,
          boolean checkDp, TaskState state,
          boolean isRet, boolean mat1, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(vbeln, this.vbeln)) {
      dr.setS(FieldType.VBELN, vbeln);
      dr.setI(FieldType.LOG, LogType.SET_VBELN.ordinal());
    }
    if (!strEq(lgort, this.lgort)) {
      dr.setS(FieldType.LGORT, lgort);
      String lgnum1;
      if (!strEq(lgort, null)) {
        RefLgortStruct l = RefLgort.get(lgort);
        lgnum1 = l.lgnum;
      } else {
        lgnum1 = "";
      }
      if (!strEq(lgnum1, this.lgnum)) {
        dr.setS(FieldType.LGNUM, lgnum1);
      }
    }
    if (checkDp != this.checkDp) {
      dr.setB(FieldType.CHECK_DP, checkDp);
    }
    if (isRet != this.isRet) {
      dr.setB(FieldType.IS_RET, isRet);
    }
    if (mat1 != this.mat1) {
      dr.setB(FieldType.MAT1, mat1);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callSetLastScan(String scan, TaskState state, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    if (!strEq(scan, this.lastScan)) {
      dr.setS(FieldType.LAST_SCAN, scan);
    }
    if ((state != null) && (state != ctx.task.getTaskState())) {
      dr.setI(FieldType.TASK_STATE, state.ordinal());
    }
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callAddTov(String matnr, String charg, String dp, BigDecimal qty, 
          String scan, TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setS(FieldType.MATNR, matnr);
    dr.setS(FieldType.CHARG, charg);
    if (dp != null) {
      dr.setS(FieldType.DAT_PR, dp);
    }
    dr.setN(FieldType.QTY, qty);
    dr.setS(FieldType.SHK, scan);
    dr.setI(FieldType.LOG, LogType.ADD_TOV.ordinal());
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  public void callDelLast(TaskContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = ctx.task.getProcId();
    dr.setV(FieldType.DEL_LAST);
    Track.saveProcessChange(dr, ctx.task, ctx);
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.LGORT)) {
          lgort = (String) dr.getVal(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.LGNUM)) {
          lgnum = (String) dr.getVal(FieldType.LGNUM);
        }
        if (dr.haveVal(FieldType.VBELN)) {
          vbeln = dr.getValStr(FieldType.VBELN);
        }
        if (dr.haveVal(FieldType.CHECK_DP)) {
          checkDp = (Boolean) dr.getVal(FieldType.CHECK_DP);
        }
        if (dr.haveVal(FieldType.IS_RET)) {
          isRet = (Boolean) dr.getVal(FieldType.IS_RET);
        }
        if (dr.haveVal(FieldType.MAT1)) {
          mat1 = (Boolean) dr.getVal(FieldType.MAT1);
        }
        if (dr.haveVal(FieldType.LAST_SCAN)) {
          lastScan = dr.getValStr(FieldType.LAST_SCAN);
        }
        if (dr.haveVal(FieldType.CLEAR_TOV_DATA)) {
          tov.clear();
          tovM.clear();
          qtyPal = BigDecimal.ZERO;
        }
        if (dr.haveVal(FieldType.MATNR) && dr.haveVal(FieldType.CHARG) && 
                dr.haveVal(FieldType.QTY) && dr.haveVal(FieldType.SHK)) {
          String matnr = dr.getValStr(FieldType.MATNR);
          String charg = dr.getValStr(FieldType.CHARG);
          String scan = dr.getValStr(FieldType.SHK);
          BigDecimal nn = (BigDecimal) dr.getVal(FieldType.QTY);
          String dp = null;
          if (dr.haveVal(FieldType.DAT_PR)) {
            dp = dr.getValStr(FieldType.DAT_PR);
          }
          PriemkaTovData td = new PriemkaTovData(matnr, charg, dp, nn, scan);
          tov.add(td);
          qtyPal = qtyPal.add(nn);

          BigDecimal nn0 = tovM.get(matnr);
          if (nn0 != null) {
            nn = nn.add(nn0);
          }
          tovM.put(matnr, nn);
        }
        if (dr.haveVal(FieldType.DEL_LAST)) {
          if (tov.isEmpty()) {
            tovM.clear();
            qtyPal = BigDecimal.ZERO;
          } else {
            int i = tov.size() - 1;
            PriemkaTovData td = tov.get(i);
            tov.remove(i);
            qtyPal = qtyPal.subtract(td.qty);

            BigDecimal nn = td.qty.negate();
            BigDecimal nn0 = tovM.get(td.matnr);
            if (nn0 != null) {
              nn = nn.add(nn0);
            }
            if (nn.signum() == 0) {
              tovM.remove(td.matnr);
            } else {
              tovM.put(td.matnr, nn);
            }
          }
        }
        break;
    }
  }
}
