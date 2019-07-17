package ntx.ts.admin;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.sap.fm.*;
import ntx.sap.sys.SAPconn;
import ntx.ts.html.HtmlPage;
import ntx.ts.http.FileData;
import ntx.ts.http.HttpSrv;
import ntx.ts.http.ParArray;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.TermServer;
import ntx.ts.srv.Track;
import ntx.ts.srv.ValType;
import ntx.ts.sysproc.Process;
import ntx.ts.sysproc.ProcessUser;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.UserContext;

/**
 * веб-интерфейс администрирования сервера терминалов
 */
public class Admin {

  private static ProcType[] procTypes = ProcType.values();
  private static TaskState[] procStates = TaskState.values();
  private static FieldType[] fieldTypes = FieldType.values();
  private static LogType[] logTypes = LogType.values();
  private static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
  private static final DateFormat df2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  public static String termMsgErr = "";

  public static FileData getPage(TermQuery tq, ProcessUser user) throws Exception {
    FileData ret;
    boolean noRights = false;

    String rest = "";
    if ((tq.sf.length > 2) && tq.sf[0].equalsIgnoreCase("admin")) {
      for (int i = 2; i < tq.sf.length; i++) {
        if (rest.isEmpty()) {
          rest = tq.sf[i].toLowerCase();
        } else {
          rest = rest + "." + tq.sf[i].toLowerCase();
        }
      }
    }

    if (!user.getIsAdmin()) {
      // нет админ прав
      if (tq.sf[tq.sf.length - 1].equalsIgnoreCase("log")) {
        ret = null;
      } else {
        ret = getPageNoRights(tq);
      }
      noRights = true;
    } else if (user.getLocked()) {
      // пользователь заблокирован
      if (tq.sf[tq.sf.length - 1].equalsIgnoreCase("log")) {
        ret = null;
      } else {
        HtmlPage p = new HtmlPage();
        p.fontSize = 11;
        p.title = "Пользователь заблокирован";
        p.addLine("<b>Ошибка: пользователь \"" + user.getUserName() + "\" заблокирован</b>");
        p.addNewLine();
        ret = p.getPage();
      }
      noRights = true;
    } else if (tq.fname.equalsIgnoreCase("admin.html")) {
      // главная страница
      ret = getPageMain(tq, user);
    } else if (tq.fname.equalsIgnoreCase("admin.arch.html")) {
      // архив (таблица архивных файлов)
      ret = getPageArch(tq);
    } else if (rest.equals("restart.html")) {
      // перезагрузка сервера (службы)
      ret = getPageRestart();
    } else if (rest.equals("track.html")) {
      // выдача трака (запрос на диапазон записей)
      ret = getPageTrackDialog(tq);
    } else if (rest.equals("track.str.html")) {
      // выдача трака (запрос строки поиска)
      ret = getPageTrackDialogStr(tq);
    } else if (rest.equals("track.str_do.html")) {
      // выдача трака (по строке поиска)
      ret = getPageTrackDialogStr2(tq);
    } else if (rest.equals("track.dates.html")) {
      // выдача трака (запрос на диапазон дат)
      ret = getPageTrackDialogDates(tq);
    } else if (rest.equals("track.dates2.html")) {
      // перенаправление на выдачу трака
      ret = getPageTrackDialogDates2(tq);
    } else if (rest.equals("track.recs.html")) {
      // перенаправление на выдачу трака
      ret = getPageTrackDialog2(tq);
    } else if ((tq.sf.length == 6) && tq.sf[2].equalsIgnoreCase("track")) {
      // выдача трака
      ret = getPageTrack(tq);
    } else if (tq.fname.equalsIgnoreCase("admin.compress1.html")) {
      // сжатие трака (запрос на чмсло оставляемых дней)
      ret = getPageCompressDialog(tq);
    } else if (tq.fname.equalsIgnoreCase("admin.compress2.html")) {
      // перенаправление на сжатие трака
      ret = getPageCompressDialog2(tq);
    } else if ((tq.sf.length == 4) && tq.sf[1].equalsIgnoreCase("compress")) {
      // сжатие трака (выполнение)
      ret = doCompressTrack(tq);
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("proc")) {
      // выдача трака по процессу
      ret = getPageTrackProc(Long.parseLong(tq.sf[3]), Integer.parseInt(tq.sf[1]));
    } else if ((tq.sf.length == 4) && tq.sf[1].equalsIgnoreCase("fintask")) {
      // завершение задачи пользователя
      ret = finUserTask(Long.parseLong(tq.sf[2]), user);
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("useractivetasks")) {
      // выдача списка активных задач пользователя
      ret = getPageUserTasks(Integer.parseInt(tq.sf[1]), Long.parseLong(tq.sf[3]), true);
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("usertasks")) {
      // выдача списка всех задач пользователя
      ret = getPageUserTasks(Integer.parseInt(tq.sf[1]), Long.parseLong(tq.sf[3]), false);
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("msgs")) {
      // выдача сообщений по задаче
      ret = getPageMsgs(Long.parseLong(tq.sf[3]), Integer.parseInt(tq.sf[1]));
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("arr")) {
      // выдача содержимого массивов по записи
      ret = getPageTrackArr(Integer.parseInt(tq.sf[3]), Integer.parseInt(tq.sf[1]));
    } else if (rest.equals("users.html")) {
      // пользователи
      ret = getPageUsers(tq);
    } else if ((tq.sf.length == 3) && tq.sf[1].equalsIgnoreCase("createuser")) {
      // создание пользователя
      ret = getPageUserCreate(tq);
    } else if (tq.fname.equalsIgnoreCase("admin.usercreation.html")) {
      // создание пользователя (выполнение)
      ret = getPageUserCreate2(tq);
    } else if ((tq.sf.length == 5) && tq.sf[2].equalsIgnoreCase("user")) {
      // просмотр/изменение пользователя
      ret = getPageUserChange(tq);
    } else if ((tq.sf.length == 4) && tq.sf[1].equalsIgnoreCase("userdeleting")) {
      // выполнение удаления пользователя
      ret = getPageUserDelete(tq);
    } else if (tq.fname.equalsIgnoreCase("admin.userchanging.html")) {
      // изменение пользователя (выполнение)
      ret = getPageUserChange3(tq);
    } else if ((tq.sf.length == 4) && tq.sf[1].equalsIgnoreCase("ts")
            && tq.sf[3].equalsIgnoreCase("log")) {
      // выдача log-файла
      tq.fname = tq.sf[1] + "." + tq.sf[2] + "." + tq.sf[3];
      ret = TermServer.getFile(tq);
    } else {
      ret = getPageNoPage(tq);
    }

    if (noRights) {
      TermServer.docLog.println("attempt to access admin page " + tq.fname + " without rights");
    } else {
      TermServer.docLog.println("returning admin page " + tq.fname);
    }
    TermServer.docLog.flush();

    return ret;
  }

  public static FileData getPageNoRights(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Нет прав";
    p.addLine("<b>Ошибка: нет прав на администрирование сервера терминалов</b>");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageNoPage(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "404";
    p.addLine("<b>Ошибка: запрошенная страница не существует</b>");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageMain(TermQuery tq, ProcessUser user) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Администрирование сервера терминалов";
    p.addLine("<b>Администрирование сервера терминалов</b>");
    p.addNewLine();
    p.addLine(user.getUserName());
    p.addNewLine();
    p.addRef("admin.ts.doc.log", "ts.doc.log");
    p.addNewLine();
    p.addRef("admin.ts.out.log", "ts.out.log");
    p.addNewLine();
    p.addRef("admin.ts.err.log", "ts.err.log");
    p.addNewLine();
    p.addNewLine();

    p.addRef("admin.0.users.html", "Пользователи");
    p.addNewLine();

    p.addRef("admin.0.track.html", "Файл данных");
    p.addNewLine();

    p.addRef("work.html", "Рабочая страница");
    p.addNewLine();
    p.addNewLine();

    p.addRef("admin.arch.html", "Архив");
    p.addNewLine();
    p.addNewLine();

    p.addText("Сервер запущен: " + df2.format(Track.started) + " ");
    p.addRef("admin.0.restart.html", "рестарт");
    p.addNewLine();
    if (SAPconn.haveReserv()) {
      p.addLine("Линия связи: " + SAPconn.getCurrDest());
    }
    p.addNewLine();

    p.addLine("<b>Loaded parameters (file " + TSparams.fileName + "):</b>");
    p.addNewLine();

    p.addLine("srvName=" + TSparams.srvName);
    p.addLine("printers=" + TSparams.printers);
    p.addLine("timeout=" + TSparams.timeout);
    p.addLine("workers=" + TSparams.workers);
    p.addLine("port=" + TSparams.port);
    p.addLine("compr_date1=" + (TSparams.comprDate1 == null ? "_undefined_" : TSparams.parseDate.format(TSparams.comprDate1)));
    p.addLine("compr_weeks=" + TSparams.comprWeeks);
    p.addLine("compr_days=" + TSparams.comprDays);
    p.addLine("fontSize=" + TSparams.fontSize);
    p.addLine("fontSize2=" + TSparams.fontSize2);
    p.addLine("fontSize3=" + TSparams.fontSize3);
    p.addLine("lgorts=" + TSparams.lgorts);
    p.addLine("lgortsNoMQ=" + TSparams.lgortsNoMQ);
    p.addLine("logDocLevel=" + TSparams.logDocLevel);
    if (TSparams.cnfPlc) {
      p.addLine("cnfPlc=1");
    } else {
      p.addLine("cnfPlc=0");
    }

    String key;
    String val;
    for (Entry<Object, Object> i : TSparams.props.entrySet()) {
      key = (String) i.getKey();
      val = (String) i.getValue();
      if (!key.equalsIgnoreCase("jco.client.passwd") && (key.startsWith("jco."))) {
        p.addLine(key + "=" + val);
      }
    }
    for (Entry<Object, Object> i : TSparams.props.entrySet()) {
      key = (String) i.getKey();
      val = (String) i.getValue();
      if (!key.equalsIgnoreCase("reserv.jco.client.passwd") && (key.startsWith("reserv.jco."))) {
        p.addLine(key + "=" + val);
      }
    }
    p.addNewLine();

    p.addLine("local IP: " + Inet4Address.getLocalHost().getHostAddress());
    p.addLine("term msg: " + ProcessContext.getTermMsg());
    p.addNewLine();

    if (!termMsgErr.isEmpty()) {
      p.addLine("term msg err: <div style=\"color:red\">" + termMsgErr + "</div>");
      p.addNewLine();
    }

    p.addLine("Последний запуск автоматического сжатия файла данных: "
            + (TermServer.ctx.track.lastComprDate == null ? "нет" : df2.format(TermServer.ctx.track.lastComprDate)));
    p.addLine("Следующий запуск автоматического сжатия файла данных: "
            + TermServer.ctx.track.nextComprDateStr(false));
    p.addNewLine();

    p.addLine("threads size = " + HttpSrv.getThreadsSize());
    p.addLine("worker No = " + HttpSrv.getWorkerNo());
    p.addText("data file size: " + String.format("%,d", (new File(Track.trackFileName(0))).length()) + "\r\n");
    p.addRef("admin.compress1.html", "сжать");
    p.addNewLine();
    p.addLine("records count: " + String.format("%,d", TermServer.ctx.track.getRecCount(0)));
    p.addNewLine();
    p.addNewLine();
    p.addRef("logout.html", "Завершение сеанса");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageArch(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Архивные файлы данных";
    p.addLine("<b>Архивные файлы данных</b>");
    p.addNewLine();
    p.addRef("admin.html", "На главную");
    p.addNewLine();
    p.addNewLine();

    p.addText("<table border=1 cellpadding=4 cellspacing=0>\r\n");
    p.addText("<tr><td align=center><b>Файл</b></td><td align=center><b>Пользователи</b></td>");
    p.addText("<td align=center><b>Размер файла</b></td><td align=center><b>Число записей</b></td>");
    p.addText("<td align=center><b>Дата сжатия</b></td><td align=center><b>Дата файла</b></td></tr>\r\n");

    DataInputStream ds;
    DataRecord dr = new DataRecord();
    boolean haveData;
    long recCount;

    int j = 1;
    File f = new File(Track.trackFileName(j));
    Date comprDate;
    while (f.exists()) {
      p.addText("<tr><td><b>");
      p.addRef("admin." + j + ".track.html", Track.trackFileName(j));
      p.addText("</b></td><td>");
      p.addRef("admin." + j + ".users.html", "архив " + j);
      p.addText("</td><td align=right>");
      p.addText(String.format("%,d", f.length()));
      p.addText("</td><td align=right>");

      ds = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 524288));
      recCount = 0;
      comprDate = null;
      haveData = dr.readDataRecord(ds);
      while (haveData) {
        recCount++;
        if (dr.recType == 3) {
          // системный параметр
          if (dr.haveVal(FieldType.LAST_COMPR_DATE)) {
            comprDate = (Date) dr.getVal(FieldType.LAST_COMPR_DATE);
          }
        }
        haveData = dr.readDataRecord(ds);
      }
      ds.close();

      p.addText(String.format("%,d", recCount));
      p.addText("</td><td align=right>");
      if (comprDate != null) {
        p.addText(df2.format(comprDate));
      }
      p.addText("</td><td align=right>");
      p.addText(df2.format(new Date(f.lastModified())));
      p.addText("</td></tr>\r\n");

      j++;
      f = new File(Track.trackFileName(j));
    }

    p.addText("</table>\r\n");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageCompressDialog(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Сжатие файла данных";
    p.addLine("Выбор числа сохраняемых дней на траке");
    p.addLine("(0 - удалить все завершенные; 1 - удалить до полуночи; 2 - оставить весь вчерашний день и т.д.)");
    p.addRef("admin.html", "На главную");
    p.addNewLine();
    p.addNewLine();

    long rec2 = TermServer.ctx.track.getRecCount(0);
    String r2 = String.valueOf(rec2);
    String r1;
    if (rec2 > 200) {
      r1 = String.valueOf(rec2 - 200);
    } else {
      r1 = "1";
    }

    p.addFormStart("admin.compress2.html", "f");
    p.addText("Оставить дней: ");
    p.addFormFieldTxt("days", 10, "7");
    p.addFormButtonSubmit("Выбор", "f.days", true);
    p.addFormEnd();

    return p.getPage();
  }

  public static FileData getPageCompressDialog2(TermQuery tq) throws Exception {
    String days = tq.getPar("days");
    if (days.isEmpty()) {
      days = "7";
    }
    return TermServer.getRedirectPage("admin.compress." + days + ".html");
  }

  private static int getFileNo(TermQuery tq) {
    int ret = -1;
    if (tq.sf.length > 1) {
      try {
        ret = Integer.parseInt(tq.sf[1]);
      } catch (Exception e) {
      }
    }
    return ret;
  }

  private static ProcessContext getContext(int fileNo) throws Exception {
    ProcessContext ret;
    if (fileNo == 0) {
      ret = TermServer.ctx;
    } else {
      ret = new ProcessContext(new Track(fileNo));
      ret.track.loadData(false);
    }
    return ret;
  }

  public static FileData getPageTrackDialog(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных - выбор диапазона записей";
    if (fileNo == 0) {
      p.addLine("Выбор диапазона записей файла данных");
    } else {
      p.addLine("Выбор диапазона записей из архива " + fileNo);
    }
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    long rec2 = TermServer.ctx.track.getRecCount(fileNo);
    String r2 = String.valueOf(rec2);
    String r1;
    if (rec2 > 200) {
      r1 = String.valueOf(rec2 - 200);
    } else {
      r1 = "1";
    }

    p.addFormStart("admin." + fileNo + ".track.recs.html", "f");
    p.addText("Записи с ");
    p.addFormFieldTxt("rec1", 10, r1);
    p.addText(" по ");
    p.addFormFieldTxt("rec2", 10, r2);
    p.addFormButtonSubmit("Выбор", "f.rec1", true);
    p.addFormEnd();

    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.dates.html", "Выбор по дате");
    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.str.html", "Поиск по строке");

    return p.getPage();
  }

  public static FileData getPageTrackDialogDates(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных - выбор диапазона дат";
    if (fileNo == 0) {
      p.addLine("Выбор диапазона дат записей файла данных");
    } else {
      p.addLine("Выбор диапазона дат записей из архива " + fileNo);
    }
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addFormStart("admin." + fileNo + ".track.dates2.html", "f");
    p.addText("Даты с ");
    p.addFormFieldTxt("dt1", 12, TSparams.parseDate.format(new Date()));
    p.addText(" по ");
    p.addFormField("dt2", 12);
    p.addFormButtonSubmit("Выбор", "f.dt1", true);
    p.addFormEnd();

    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.html", "Выбор по номерам записей");
    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.str.html", "Поиск по строке");

    return p.getPage();
  }

  public static FileData getPageTrackDialogStr(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных - поиск по строке";
    if (fileNo == 0) {
      p.addLine("Ввод строки для поиска записей файла данных");
    } else {
      p.addLine("Ввод строки для поиска записей из архива " + fileNo);
    }
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addFormStart("admin." + fileNo + ".track.str_do.html", "f");
    p.addText("Строка поиска: ");
    p.addFormField("str", 30);
    p.addNewLine();
    p.addText("Максимальное число записей: ");
    p.addFormFieldTxt("maxrec", 10, "1000");
    p.addNewLine();
    p.addFormButtonSubmit("Поиск", "f.str", false);
    p.addFormEnd();

    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.html", "Выбор по номерам записей");
    p.addNewLine();
    p.addNewLine();
    p.addRef("admin." + fileNo + ".track.dates.html", "Выбор по дате");

    return p.getPage();
  }

  public static FileData getPageTrackDialog2(TermQuery tq) throws Exception {
    String r1 = tq.getPar("rec1");
    if (r1.isEmpty()) {
      r1 = "0";
    }
    String r2 = tq.getPar("rec2");
    if (r2.isEmpty()) {
      r2 = "0";
    }
    return TermServer.getRedirectPage("admin." + tq.sf[1] + ".track." + r1 + "." + r2 + ".html");
  }

  public static FileData getPageTrackDialogDates2(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);

    String d1 = tq.getPar("dt1");
    if (d1.isEmpty()) {
      d1 = TSparams.parseDate.format(new Date());
    }
    String d2 = tq.getPar("dt2");
    if (d2.isEmpty()) {
      d2 = d1;
    }

    long r1 = 0, r2 = 0;
    try {
      long dt1 = TSparams.parseDate.parse(d1).getTime();
      long dt2 = TSparams.parseDate.parse(d2).getTime() + 86400000;

      Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
      synchronized (sync) {
        DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
        DataRecord dr = new DataRecord();
        boolean haveData = dr.readDataRecord(ds);
        while (haveData) {
          if (dr.recDt > dt2) {
            break;
          }
          if (dr.recDt >= dt1) {
            if (r1 == 0) {
              r1 = dr.no;
            }
            r2 = dr.no;
          }
          haveData = dr.readDataRecord(ds);
        }
        ds.close();
      }
    } catch (Exception e) {
      HtmlPage p = new HtmlPage();
      p.fontSize = 11;

      p.title = "Ошибка";
      p.addLine("Ошибка при чтении дат и поиске диапазона номеров записей");
      p.addLine("<font color=red><b>" + e.toString() + "</b></font>");
      return p.getPage();
    }

    return TermServer.getRedirectPage("admin." + tq.sf[1] + ".track." + r1 + "." + r2 + ".html");
  }

  public static FileData getPageTrack(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);

    long rec1 = Long.parseLong(tq.sf[3]);
    long rec2 = Long.parseLong(tq.sf[4]);
    long rec = 0;

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных";
    if (fileNo == 0) {
      p.addLine("Содержание файла данных, записи с " + rec1 + " по " + rec2);
    } else {
      p.addLine("Содержание <b>архива " + fileNo + "</b>, записи с " + rec1 + " по " + rec2);
    }
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n");

    Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
    synchronized (sync) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);

      while (haveData) {
        rec++;
        if (rec >= rec1) {
          if (rec > rec2) {
            break;
          }
          writeTrackRec(p, dr, true, fileNo);
        }
        haveData = dr.readDataRecord(ds);
      }

      ds.close();
    }

    p.addLine("</table> </font>");

    p.addNewLine();
    p.addLine("<b>Структура таблицы:</b>");
    p.addLine("* Номер записи файла (трака)");
    p.addLine("* Id процесса (со ссылкой на его трак)");
    p.addLine("* Дата/время записи");
    p.addLine("* Тип записи: new - создание процесса; пусто - изменение; fin - завершение; sys - системные данные");
    p.addLine("* Поля записи");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData doCompressTrack(TermQuery tq) throws Exception {
    int days = Integer.parseInt(tq.sf[2]);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Сжатие файла данных";
    p.addLine("Сжатие файла данных, оставляем трак за " + days + " дней");
    p.addRef("admin.html", "На главную");
    p.addNewLine();
    p.addNewLine();
    p.addLine("Старый размер файла: " + String.format("%,d", (new File(Track.trackFileName(0))).length()));
    p.addLine("Старое число записей: " + String.format("%,d", TermServer.ctx.track.getRecCount(0)));
    p.addNewLine();

    String err = TermServer.ctx.track.compressTrack(days);
    if (err == null) {
      p.addLine("Новый размер файла: " + String.format("%,d", (new File(Track.trackFileName(0))).length()));
      p.addLine("Новое число записей: " + String.format("%,d", TermServer.ctx.track.getRecCount(0)));
      p.addNewLine();
      p.addLine("Сжатие файла данных завершено успешно");
    } else {
      p.addLine("<font color=red><b>Ошибка при сжатии файла данных:</b></font>");
      p.addLine("<font color=red><b>" + err + "</b></font>");
    }
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageTrackProc(long proc, int fileNo) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных";
    if (fileNo != 0) {
      p.addLine("<b>Архив " + fileNo + "</b>");
    }
    p.addLine("Содержание файла данных по процессу <font color=blue><b>"
            + df.format(new Date(proc)) + "</b></font> (" + proc + ")");
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n");

    Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
    synchronized (sync) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);
//      boolean isUserTask = false;
      boolean canFinishTask = false;
      boolean isFinished = false;
      ProcType ptyp;

      while (haveData) {
        if (dr.procId == proc) {
          writeTrackRec(p, dr, false, fileNo);
          if (dr.recType == 1) {
            if (dr.haveVal(FieldType.PROC_TYPE)) {
              ptyp = procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)];
            } else {
              ptyp = null;
            }
//            if ((ptyp != null) && ptyp.isUserProc) {
//              isUserTask = true;
//            }
            if ((ptyp != null) && ptyp.canFinish) {
              canFinishTask = true;
            }
          } else if (dr.recType == 2) {
            isFinished = true;
          }
        }
        haveData = dr.readDataRecord(ds);
      }

      ds.close();

      p.addLine("</table> </font>");

      p.addNewLine();
      p.addLine("<b>Структура таблицы:</b>");
      p.addLine("* Номер записи файла (трака)");
      p.addLine("* Дата/время записи");
      p.addLine("* Тип записи: new - создание процесса; пусто - изменение; fin - завершение; sys - системные данные");
      p.addLine("* Поля записи");
      p.addNewLine();

      if (canFinishTask) {
        p.addNewLine();
        p.addRef("admin." + fileNo + ".msgs." + proc + ".html", "Сообщения");
        if ((fileNo == 0) && !isFinished) {
          p.addRef("admin.fintask." + proc + ".html", "Завершить задачу");
          p.addNewLine();
          p.addNewLine();
        }
      }
    }

    return p.getPage();
  }

  public static FileData getPageTrackArr(int no, int fileNo) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Файл данных";
    if (fileNo != 0) {
      p.addLine("<b>Архив " + fileNo + "</b>");
    }
    p.addLine("Содержание файла данных: массивы в записи <font color=blue><b>"
            + no + "</b></font>");
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 cellpadding=1 cellspacing=0>\r\n");

    Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
    synchronized (sync) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);

      while (haveData) {
        if (dr.no == no) {
          FieldType tp;
          int arCount = 0;
          for (int i = 0; i < fieldTypes.length; i++) {
            tp = fieldTypes[i];
            if (tp.valType.isArray && dr.haveVal(tp)) {
              arCount++;
            }
          }
          if (arCount == 0) {
            break;
          }

          Object[] aa = new Object[arCount];
          FieldType[] ta = new FieldType[arCount];

          int j = 0;
          int maxSize = 0; // наибольший размер массива
          int nn;
          for (int i = 0; i < fieldTypes.length; i++) {
            tp = fieldTypes[i];
            if (tp.valType.isArray && dr.haveVal(tp)) {
              ta[j] = tp;
              aa[j] = dr.getVal(tp);

              nn = 0;
              switch (tp.valType) {
                case INT_AR:
                  nn = ((int[]) aa[j]).length;
                  break;
                case LONG_AR:
                  nn = ((long[]) aa[j]).length;
                  break;
                case STRING_AR:
                  nn = ((String[]) aa[j]).length;
                  break;
                case BOOL_AR:
                  nn = ((boolean[]) aa[j]).length;
                  break;
                case DATE_AR:
                  nn = ((Date[]) aa[j]).length;
                  break;
                case DEC_AR:
                  nn = ((BigDecimal[]) aa[j]).length;
                  break;
              }
              if (nn > maxSize) {
                maxSize = nn;
              }
              j++;
            }
          }

          p.addText("<tr><td><b>NN</b></td>");
          for (int i = 0; i < arCount; i++) {
            p.addText("<td align=center><b>");
            p.addText(ta[i].name());
            p.addText("</b></td>");
          }
          p.addText("</tr>\r\n");

          j = 0;
          while (j < maxSize) {
            p.addText("<tr><td>");
            p.addText(j + 1);
            p.addText("</td>");

            for (int i = 0; i < arCount; i++) {
              tp = ta[i];
              switch (tp.valType) {
                case INT_AR:
                  nn = ((int[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText("" + ((int[]) aa[i])[j]);
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
                case LONG_AR:
                  nn = ((long[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText("" + ((long[]) aa[i])[j]);
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
                case STRING_AR:
                  nn = ((String[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText(((String[]) aa[i])[j]);
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
                case BOOL_AR:
                  nn = ((boolean[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText(((boolean[]) aa[i])[j] ? "ДА" : "НЕТ");
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
                case DATE_AR:
                  nn = ((Date[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText(((Date[]) aa[i])[j].toString());
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
                case DEC_AR:
                  nn = ((BigDecimal[]) aa[i]).length;
                  if (j < nn) {
                    p.addText("<td>");
                    p.addText(((BigDecimal[]) aa[i])[j].toString());
                    p.addText("</td>");
                  } else {
                    p.addText("<td align=center>x</td>");
                  }
                  break;
              }
            }

            p.addText("</tr>\r\n");
            j++;
          }

          break;
        }
        haveData = dr.readDataRecord(ds);
      }

      ds.close();
    }

    p.addLine("</table> </font>");

    return p.getPage();
  }

  private static void writeTrackRec(HtmlPage p, DataRecord dr, boolean showProc, int fileNo) {
    p.addText("<tr><td>");
    p.addText(dr.no);
    if (showProc) {
      p.addText("</td><td nowrap><a href='admin.");
      p.addText(fileNo);
      p.addText(".proc.");
      p.addText(dr.procId);
      p.addText(".html'>");
      p.addText(df.format(new Date(dr.procId)));
      p.addText("</a>");
    }
    p.addText("</td><td nowrap>");
    p.addText(df.format(new Date(dr.recDt)));
    p.addText("</td><td>");
    switch (dr.recType) {
      case 1:
        p.addText("new");
        break;
      case 2:
        p.addText("fin");
        break;
      case 3:
        p.addText("sys");
        break;
    }
    p.addText("</td><td>\r\n");

    boolean is1st = true;
    FieldType tp;

    for (int i = 0; i < fieldTypes.length; i++) {
      tp = fieldTypes[i];
      if (dr.haveVal(tp)) {
        if (!is1st) {
          p.addText("; \r\n");
        }
        is1st = false;

        p.addText("<b>" + tp.name() + "</b>");
        if (tp.valType != ValType.VOID) {
          p.addText("=");
        }

        switch (tp) {
          case PROC_TYPE:
            p.addText(procTypes[(Integer) dr.getVal(tp)].name());
            break;

          case TASK_STATE:
          case PREV_TASK_STATE:
            p.addText(procStates[(Integer) dr.getVal(tp)].name());
            break;

          case LOG:
            p.addText(logTypes[(Integer) dr.getVal(tp)].name());
            break;

          case TERM:
            long term = (Long) dr.getVal(tp);
            p.addText(df.format(new Date(term)) + " (" + term + ")");
            break;

          case PARENT:
          case TASK_ADD:
          case TASK_DEL:
          case TASK_NO:
            long l = (Long) dr.getVal(tp);
            p.addText("<a href='admin.");
            p.addText(fileNo);
            p.addText(".proc.");
            p.addText(l);
            p.addText(".html'>");
            p.addText(df.format(new Date(l)));
            p.addText("</a>");
            break;

          default:
            if (!tp.valType.isArray) {
              switch (tp.valType) {
                case BOOL:
                  if ((Boolean) dr.getVal(tp)) {
                    p.addText("ДА");
                  } else {
                    p.addText("НЕТ");
                  }
                  break;

                case VOID:
                  break;

                default:
                  p.addText(dr.getVal(tp).toString());
                  break;
              }
            } else {
              p.addText("<a href='admin.");
              p.addText(fileNo);
              p.addText(".arr.");
              p.addText(dr.no);
              p.addText(".html'>");
              switch (tp.valType) {
                case INT_AR:
                  p.addText(((int[]) dr.getVal(tp)).length);
                  break;
                case LONG_AR:
                  p.addText(((long[]) dr.getVal(tp)).length);
                  break;
                case STRING_AR:
                  p.addText(((String[]) dr.getVal(tp)).length);
                  break;
                case BOOL_AR:
                  p.addText(((boolean[]) dr.getVal(tp)).length);
                  break;
                case DATE_AR:
                  p.addText(((Date[]) dr.getVal(tp)).length);
                  break;
                case DEC_AR:
                  p.addText(((BigDecimal[]) dr.getVal(tp)).length);
                  break;
              }
              p.addText(" зап</a>");
            }
        }
//        p.addText("\r\n");
      }
    }
    p.addText("</td></tr>\r\n");
  }

  private static String trackRecText(DataRecord dr) {
    StringBuilder ret = new StringBuilder(200);

    FieldType tp;

    for (int i = 0; i < fieldTypes.length; i++) {
      tp = fieldTypes[i];
      if (dr.haveVal(tp)) {
        if (ret != null) {
          ret.append("; ");
        }

        ret.append(tp.name());
        if (tp.valType != ValType.VOID) {
          ret.append("=");
        }

        switch (tp) {
          case PROC_TYPE:
            ret.append(procTypes[(Integer) dr.getVal(tp)].name());
            break;

          case TASK_STATE:
          case PREV_TASK_STATE:
            ret.append(procStates[(Integer) dr.getVal(tp)].name());
            break;

          case LOG:
            ret.append(logTypes[(Integer) dr.getVal(tp)].name());
            break;

          case TERM:
            ret.append(df.format(new Date((Long) dr.getVal(tp))));
            break;

          case PARENT:
          case TASK_ADD:
          case TASK_DEL:
          case TASK_NO:
            long l = (Long) dr.getVal(tp);
            ret.append(df.format(new Date(l)));
            break;

          default:
            if (!tp.valType.isArray) {
              switch (tp.valType) {
                case BOOL:
                  if ((Boolean) dr.getVal(tp)) {
                    ret.append("ДА");
                  } else {
                    ret.append("НЕТ");
                  }
                  break;

                case VOID:
                  break;

                default:
                  ret.append(dr.getVal(tp).toString());
                  break;
              }
            } else {
              switch (tp.valType) {
                case INT_AR:
                  ret.append(((int[]) dr.getVal(tp)).length);
                  break;
                case LONG_AR:
                  ret.append(((long[]) dr.getVal(tp)).length);
                  break;
                case STRING_AR:
                  ret.append(((String[]) dr.getVal(tp)).length);
                  break;
                case BOOL_AR:
                  ret.append(((boolean[]) dr.getVal(tp)).length);
                  break;
                case DATE_AR:
                  ret.append(((Date[]) dr.getVal(tp)).length);
                  break;
                case DEC_AR:
                  ret.append(((BigDecimal[]) dr.getVal(tp)).length);
                  break;
              }
              ret.append(" зап");
            }
        }
      }
    }

    return ret.toString();
  }

  public static FileData getPageUsers(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);
    ProcessContext ctx = getContext(fileNo);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    // получение имен из САПа
    HashMap<String, String> usersSAP = usersFromSAP(ctx);

    ProcessUser u;
    long terminal;
    String user2;

    p.title = "Пользователи";
    if (fileNo == 0) {
      p.addLine("<b>Пользователи терминалов</b>");
    } else {
      p.addLine("<b>Пользователи терминалов (архив " + fileNo + ")</b>");
    }
    p.addNewLine();
    if (fileNo == 0) {
      p.addRef("admin.createuser.html", "Создать пользователя");
    }
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n<tr>");
    p.addText("<td align=center><b>Имя</b></td>");
    p.addText("<td align=center><b>Пароль</b></td>");
    p.addText("<td align=center><b>Акт. задачи</b></td>");
    p.addText("<td align=center><b>Задачи</b></td>");
    p.addText("<td align=center><b>ID</b></td>");
    p.addText("<td align=center><b>ШК</b></td>");
    p.addText("<td align=center><b>Код</b></td>");
    p.addText("<td align=center><b>Терминал</b></td>");
    p.addText("<td align=center><b>Админ</b></td>");
    p.addText("<td align=center><b>Блокир</b></td>");
    p.addText("<td align=center><b>Ввод кол</b></td>");
    p.addText("<td align=center><b>Склады</b></td>");
    p.addText("<td align=center><b>Задачи</b></td>");
    p.addText("<td align=center><b>Имя САП</b></td></tr>\r\n");

    ConcurrentSkipListMap<String, ProcessUser> uList = ctx.track.getUsersSorted();
    String psw;
    for (Entry<String, ProcessUser> i : uList.entrySet()) {
      u = i.getValue();
      p.addText("<tr><td>");
      p.addRef("admin." + fileNo + ".user." + u.getProcId() + ".html", i.getKey());
      p.addText("</td><td>");
      psw = u.getPassword();
      if ((psw != null) && !psw.isEmpty()) {
        p.addText("***");
      }
      p.addText("</td><td>");
      if (u.tasks.getActiveTaskCount() > 0) {
        p.addRef("admin." + fileNo + ".useractivetasks." + u.getProcId() + ".html", "" + u.tasks.getActiveTaskCount());
      }
      p.addText("</td><td>");
      if (u.tasks.getTaskCount() > 0) {
        p.addRef("admin." + fileNo + ".usertasks." + u.getProcId() + ".html", "" + u.tasks.getTaskCount());
      }
      p.addText("</td><td><a href='admin.");
      p.addText(fileNo);
      p.addText(".proc.");
      p.addText(u.getProcId());
      p.addText(".html'>");
      p.addText(df.format(new Date(u.getProcId())));
      p.addText("</a></td><td>");
      p.addText(u.getUserSHK());
      p.addText("</td><td>");
      p.addText(u.getUserCode());
      p.addText("</td><td>");
      terminal = u.getTerminal();
      if (terminal != 0) {
        p.addText(df.format(new Date(terminal)));
      }
      p.addText("</td><td align=center>");
      if (u.getIsAdmin()) {
        p.addText("admin");
      }
      p.addText("</td><td align=center>");
      if (u.getLocked()) {
        p.addText("да");
      }
      p.addText("</td><td align=center>");
      if (u.getCanMQ()) {
        p.addText("да");
      }
      p.addText("</td><td>");
      p.addText(u.getUserLgorts());
      p.addText("</td><td>");
      p.addText(u.getUserTtypes());
      p.addText("</td><td>");
      user2 = usersSAP.get(u.getUserSHK());
      if (user2 == null) {
        user2 = "???";
      }
      if (!i.getKey().equalsIgnoreCase(user2)) {
        p.addText(user2);
      }
      p.addText("</td></tr>\r\n");
    }
    p.addText("</table></font>\r\n");
    p.addNewLine();

    return p.getPage();
  }

  public static HashMap<String, String> usersFromSAP(ProcessContext ctx) {
    // получение пользователей по ШК из САПа

    HashMap<String, String> ret = new HashMap<String, String>(300);

    String[] shk = ctx.track.getUsersSHK();
    Z_TS_USERS p = new Z_TS_USERS();
    p.USERS_create(shk.length);

    for (int i = 0; i < shk.length; i++) {
      p.USERS[i].SHK = shk[i];
    }

    p.execute();

    if (p.isErr) {
      return ret;
    }

    for (int i = 0; i < p.USERS.length; i++) {
      if ((p.USERS[i].NAME1 != null) && !p.USERS[i].NAME1.isEmpty()) {
        ret.put(p.USERS[i].SHK, p.USERS[i].NAME1);
      }
    }

    return ret;
  }

  public static FileData getPageUserCreate(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Создание пользователя";
    p.addLine("<b>Создание пользователя</b>");
    p.addNewLine();
    p.addLine("Введите код или ШК пользователя или номер кредитора (сотрудника):");

    p.addFormStart("admin.usercreation.html", "f");
    p.addFormField("ucode", 20);
    p.addFormButtonSubmit("Ввод", "f.ucode", false);
    p.addFormEnd();

    p.addNewLine();
    p.addLine("Примечание: пользователь должен существовать как сотрудник (кредитор) в САПе");
    return p.getPage();
  }

  public static FileData getPageUserCreate2(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;
    p.title = "Создание пользователя";

    String ucode = tq.params.getPar("ucode");

    if (ucode.isEmpty()) {
      p.addLine("Ошибка: не указан код пользователя");
      return p.getPage();
    }

    Z_TS_GET_USER f = new Z_TS_GET_USER();
    f.USER_CODE = ucode;
    f.execute();

    if (f.isErr) {
      p.addLine("Ошибка при получении имени пользователя " + ucode + ":");
      p.addLine(f.err);
      return p.getPage();
    }

    if (f.SHK.isEmpty()) {
      p.addLine("Ошибка: сотрудник с кодом " + ucode + " не существует в САПе");
      p.addLine(f.err);
      return p.getPage();
    }

    ProcessUser u = TermServer.ctx.track.getUserBySHK(f.SHK);

    if (u != null) {
      p.addLine("<b>Ошибка: пользователь уже создан:</b>");
      p.addNewLine();
      p.addLine(u.getUserName());
      p.addLine(u.getUserSHK());
      p.addLine(u.getUserCode());
      p.addNewLine();
      p.addRef("admin.0.users.html", "Продолжить");
      return p.getPage();
    }

    u = TermServer.ctx.track.callCreateUser(f.NAME1, f.SHK, "");

    if (u == null) {
      p.addLine("<b>Ошибка: пользователь не создан</b>");
      return p.getPage();
    }

    return getPageUserChange2(u, true, 0);
  }

  public static FileData getPageUserChange(TermQuery tq) throws Exception {
    int fileNo = getFileNo(tq);
    ProcessContext ctx = getContext(fileNo);

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    long userId = Long.parseLong(tq.sf[3]);
    ProcessUser user = (ProcessUser) ctx.track.getProcess(userId, null);
    if (user == null) {
      p.title = "404";
      p.addLine("<b>Ошибка: нет такого пользователя</b>");
      p.addNewLine();
      return p.getPage();
    }

    return getPageUserChange2(user, false, fileNo);
  }

  public static FileData getPageUserChange2(ProcessUser user, boolean newUser, int fileNo) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    synchronized (user) {
      p.title = user.getUserName();
      if (newUser) {
        p.addLine("<b>Пользователь " + user.getUserName() + " успешно создан</b>");
      } else {
        p.addLine("<b>Пользователь " + user.getUserName() + "</b>");
      }
      p.addNewLine();
      p.addRef("admin." + fileNo + ".users.html", "Вернуться к списку");
      p.addNewLine();
      p.addNewLine();

      p.addFormStart("admin.userchanging.html", "f");
      p.addText("<table cellpadding=1 cellspacing=0><tr><td>\r\n");
      p.addText("<table cellpadding=1 cellspacing=0>\r\n");
      p.addText("<tr><td>Имя:</td><td> ");
      p.addFormFieldTxt("uname", 30, user.getUserName(), fileNo != 0);
      p.addFormFieldHidden("uid", 20, "" + user.getProcId());
      p.addText("</td></tr>\r\n<tr><td>Имя САП:</td><td> ");
      String sapName = "";
      try {
        Z_TS_GET_USER f = new Z_TS_GET_USER();
        f.USER_CODE = user.getUserSHK();
        f.execute();
        if (f.USER_CODE.equals(f.SHK)) {
          sapName = f.NAME1;
        }
      } catch (Exception e) {
      }
      p.addText(sapName);
      p.addText("</td></tr>\r\n<tr><td>ШК:</td><td> ");
      p.addFormFieldTxt("shk", 20, user.getUserSHK(), fileNo != 0);
      p.addText("</td></tr>\r\n<tr><td>Код:</td><td> ");
      p.addText(user.getUserCode());
      p.addText("</td></tr>\r\n<tr><td>Терминал:</td><td> ");
      long terminal = user.getTerminal();
      if (terminal != 0) {
        p.addText(df.format(new Date(terminal)));
      } else {
        p.addText("нет");
      }
      p.addText("</td></tr>\r\n<tr><td>Пароль:</td><td> ");
      p.addFormFieldTxt("passw", 20, user.getPassword(), fileNo != 0);
      p.addText("</td></tr>\r\n<tr><td>Разр. ввод кол. при компл:</td><td> ");
      if (fileNo == 0) {
        p.addFormCheckbox("can_mq", user.getCanMQ());
      } else {
        p.addText(user.getCanMQ() ? "да" : "нет");
      }
      p.addText("</td></tr>\r\n<tr><td>Список складов:</td><td> ");
      p.addFormFieldTxt("lgorts", 50, user.getUserLgorts(), fileNo != 0);
      p.addText("</td></tr>\r\n<tr><td>Админ:</td><td> ");
      if (fileNo == 0) {
        p.addFormCheckbox("admin", user.getIsAdmin());
      } else {
        p.addText(user.getIsAdmin() ? "да" : "нет");
      }
      p.addText("</td></tr>\r\n<tr><td>Блокировка:</td><td> ");
      if (fileNo == 0) {
        p.addFormCheckbox("locked", user.getLocked());
      } else {
        p.addText(user.getLocked() ? "да" : "нет");
      }
      p.addText("</td></tr>\r\n<tr><td>Число активных задач:</td><td> ");
      p.addText("" + user.tasks.getActiveTaskCount());
      p.addText("</td></tr>\r\n<tr><td>Число задач:</td><td> ");
      p.addText("" + user.tasks.getTaskCount());
      p.addText("</td></tr>\r\n</table>\r\n</td><td valign=top>\r\n");

      p.addText("<table cellpadding=1 cellspacing=0>\r\n");
      boolean rt;
      for (ProcType pt : ProcType.values()) {
        if (pt.isUserProc) {
          rt = user.rightsTtype(pt);
          p.addText("<tr><td>" + pt.ordinal() + " " + pt.text + "</td><td> ");
          if (fileNo == 0) {
            p.addFormCheckbox("ttype" + pt.ordinal(), rt);
          } else {
            p.addText(rt ? "да" : "нет");
          }
          p.addText("</td></tr>\r\n");
        }
      }
      p.addText("</table>\r\n");

      p.addText("</td></tr>\r\n</table>\r\n");
      if (fileNo == 0) {
        p.addFormButtonSubmit("Сохранить", null, false);
      }
      p.addFormEnd();
      p.addNewLine();
      p.addNewLine();
      if ((fileNo == 0) && (user.getLocked())) {
        p.addNewLine();
        p.addNewLine();
        p.addRef("admin.userdeleting." + user.getProcId() + ".html", "Удалить пользователя");
      }
    }

    return p.getPage();
  }

  public static FileData getPageUserChange3(TermQuery tq) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;
    p.title = "Изменение пользователя";

    String uid = tq.params.getPar("uid");
    String uname = tq.params.getPar("uname");
    String shk = tq.params.getPar("shk");
    String passw = tq.params.getPar("passw");
    String lgorts = tq.params.getPar("lgorts");
    String admin = tq.params.getPar("admin");
    boolean isAdm = (!admin.isEmpty() && !admin.equalsIgnoreCase("of") && !admin.equalsIgnoreCase("off"));
    String locked = tq.params.getPar("locked");
    boolean isLocked = (!locked.isEmpty() && !locked.equalsIgnoreCase("of") && !locked.equalsIgnoreCase("off"));
    String can_mq = tq.params.getPar("can_mq");
    boolean canMQ = (!can_mq.isEmpty() && !can_mq.equalsIgnoreCase("of") && !can_mq.equalsIgnoreCase("off"));

    ProcessUser u = (ProcessUser) TermServer.ctx.track.getProcess(Long.parseLong(uid), null);
    if (u == null) {
      p.addLine("Ошибка: не найден пользователь " + uid);
      return p.getPage();
    }
    if (uname.isEmpty()) {
      p.addLine("Ошибка: не указано имя пользователя");
      return p.getPage();
    }
    if (shk.isEmpty()) {
      p.addLine("Ошибка: не указан ШК пользователя");
      return p.getPage();
    }

    ProcessUser u1 = TermServer.ctx.track.getUserBySHK(shk);
    if ((u1 != null) && (u1 != u)) {
      p.addLine("Ошибка: ШК " + shk + " имеет другой пользователь:");
      p.addLine(u1.getUserName());
      p.addLine("" + u1.getProcId());
      return p.getPage();
    }

    if (uname.indexOf('"') != -1) {
      p.addLine("Ошибка: имя не должно содержать двойные кавычки");
      return p.getPage();
    }
    if (shk.indexOf('"') != -1) {
      p.addLine("Ошибка: ШК не должен содержать двойные кавычки");
      return p.getPage();
    }
    if (passw.indexOf('"') != -1) {
      p.addLine("Ошибка: пароль не должен содержать двойные кавычки");
      return p.getPage();
    }
    if (lgorts.indexOf('"') != -1) {
      p.addLine("Ошибка: список складов не должен содержать двойные кавычки");
      return p.getPage();
    }

    String rts;
    boolean rt;
    ArrayList<ProcType> pts = new ArrayList<ProcType>();
    for (ProcType pt : ProcType.values()) {
      if (pt.isUserProc) {
        rts = tq.params.getPar("ttype" + pt.ordinal());
        rt = (!rts.isEmpty() && !rts.equalsIgnoreCase("of") && !rts.equalsIgnoreCase("off"));
        if (rt) {
          pts.add(pt);
        }
      }
    }

    ProcType[] pti = new ProcType[pts.size()];
    for (int i = 0; i < pti.length; i++) {
      pti[i] = pts.get(i);
    }

    synchronized (u) {
      u.callChangeUser(uname, shk, passw, lgorts, isAdm, isLocked, canMQ,
              pti, TermServer.ctx);
    }

    return TermServer.getRedirectPage("admin.0.users.html");
  }

  public static FileData getPageUserDelete(TermQuery tq) throws Exception {

    long userId = Long.parseLong(tq.sf[2]);
    ProcessUser user = (ProcessUser) TermServer.ctx.track.getProcess(userId, null);
    if (user == null) {
      HtmlPage p = new HtmlPage();
      p.fontSize = 11;
      p.title = "404";
      p.addLine("<b>Ошибка: нет такого пользователя</b>");
      p.addNewLine();
      return p.getPage();
    }

    if (user.tasks.getActiveTaskCount() > 0) {
      HtmlPage p = new HtmlPage();
      p.fontSize = 11;
      p.title = "Ошибка удаления";
      p.addLine("<b>Ошибка: у пользователя есть незавершенные задачи, удаление невозможно</b>");
      p.addLine(user.getUserName());
      p.addNewLine();
      return p.getPage();
    }

    synchronized (user) {
      user.callDeleteUser(TermServer.ctx);
    }

    return TermServer.getRedirectPage("admin.0.users.html");
  }

  public static FileData getPageUserTasks(int fileNo, long userId, boolean activeOnly) throws Exception {
    ProcessContext ctx = getContext(fileNo);

    ProcessUser user = (ProcessUser) ctx.track.getProcess(userId, null);
    if (user == null) {
      HtmlPage p = new HtmlPage();
      p.fontSize = 11;
      p.title = "404";
      p.addLine("<b>Ошибка: нет такого пользователя</b>");
      p.addNewLine();
      return p.getPage();
    }

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    if (fileNo != 0) {
      p.addLine("<b>Архив " + fileNo + "</b>");
    }

    if (activeOnly) {
      p.title = "Активные задачи пользователя";
      p.addText("Активные задачи пользователя ");
    } else {
      p.title = "Все задачи пользователя";
      p.addText("Все задачи пользователя ");
    }
    p.addText(user.getUserName());
    p.addRef("admin." + fileNo + ".proc." + userId + ".html", df.format(new Date(userId)));
    p.addLine(" (" + userId + ")");

    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    HashMap<Long, TaskDescr> tasks = new HashMap<Long, TaskDescr>(1000); // задачи пользователя (и возможно не только)

    synchronized (ctx.track.lockRead) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);

      ProcType ptyp;
      TaskDescr tdes;

      while (haveData) {
        if (dr.recType == 1) {
          // новую задачу всегда помещаем в tasks (если есть её тип и он не системный)
          if (dr.haveVal(FieldType.PROC_TYPE)) {
            ptyp = procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)];
          } else {
            ptyp = null;
          }

          if ((ptyp != null) && ptyp.isUserProc) {
            tdes = new TaskDescr(ptyp);
            tasks.put(dr.procId, tdes);
          }
        } else if (dr.procId == userId) {
          // с трака пользователя берем название задачи
          // (если задача еще не удалена из tasks)
          if (dr.haveVal(FieldType.TASK_ADD) && dr.haveVal(FieldType.TASK_NAME)) {
            tdes = tasks.get((Long) dr.getVal(FieldType.TASK_ADD));
            if (tdes != null) {
              tdes.taskName = dr.getValStr(FieldType.TASK_NAME);
            }
          }
        } else {
          tdes = tasks.get(dr.procId);
          if (tdes != null) {
            // если задача еще не удалена из tasks, заполняем информацию о ней
            tdes.recCount++;
            if (dr.recType == 2) {
              tdes.finTime = dr.recDt;
            } else {
              if (dr.haveVal(FieldType.PARENT)) {
                if (userId == (Long) dr.getVal(FieldType.PARENT)) {
                  tdes.userTask = true;
                } else {
                  // это задача другого пользователя, удаляем из tasks
                  tasks.remove(dr.procId);
                  tdes = null;
                }
              }
              if (tdes != null) {
                // тут можно заполнять какие-нибудь еще поля
              }
            }
          }
        }

        haveData = dr.readDataRecord(ds);
      }

      ds.close();

      p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n");

      long pid;
      int n;
      ConcurrentSkipListMap<Long, TaskDescr> ss = new ConcurrentSkipListMap<Long, TaskDescr>(tasks);
      for (Entry<Long, TaskDescr> i : ss.entrySet()) {
        pid = i.getKey();
        tdes = i.getValue();

        if (!tdes.userTask) {
          continue;
        }
        if (activeOnly && (tdes.finTime > 0)) {
          continue;
        }

        p.addText("<tr><td>");
        p.addText(tdes.procType.name());
        p.addText("</td><td><a href='admin.");
        p.addText(fileNo);
        p.addText(".msgs.");
        p.addText(pid);
        p.addText(".html'>");
        p.addText(tdes.taskName);
        p.addText("</a></td><td><a href='admin.");
        p.addText(fileNo);
        p.addText(".proc.");
        p.addText(pid);
        p.addText(".html'>");
        p.addText(df.format(new Date(pid)));
        p.addText("</a>");
        if (!activeOnly) {
          p.addText("</td><td nowrap>");
          if (tdes.finTime > 0) {
            p.addText(df.format(new Date(tdes.finTime)));
          }
        }
        p.addText("</td><td>");
        p.addText(tdes.recCount);
        p.addText("</td><td align=center>");
        n = user.tasks.getActiveTaskCount();
        for (int j = 0; j < n; j++) {
          if (user.tasks.getTaskId(j) == pid) {
            p.addText("<b>+</b>");
            break;
          }
        }
        p.addText("</td></tr>\r\n");
      }

      p.addLine("</table> </font>");
    }

    p.addNewLine();
    p.addLine("<b>Структура таблицы:</b>");
    p.addLine("* Тип задачи пользователя");
    p.addLine("* Название задачи (со ссылкой на сообщения по задаче)");
    p.addLine("* Id задачи (со ссылкой на её трак)");
    p.addLine("* Дата/время завершения задачи");
    p.addLine("* Число записей на траке задачи");
    p.addLine("* Признак присутствия задачи в списке задач пользователя");
    p.addNewLine();

    return p.getPage();
  }

  public static FileData getPageMsgs(long proc, int fileNo) throws Exception {
    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Сообщения по задаче";
    if (fileNo != 0) {
      p.addLine("<b>Архив " + fileNo + "</b>");
    }
    p.addText("Сообщения по задаче ");
    p.addRef("admin." + fileNo + ".proc." + proc + ".html", df.format(new Date(proc)));
    p.addLine(" (" + proc + ")");
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n");

    Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
    synchronized (sync) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);
      String s;
//      boolean isUserTask = false;
      boolean canFinishTask = false;
      boolean isFinished = false;
      ProcType ptyp;

      while (haveData) {
        if (dr.procId == proc) {
          if (dr.haveVal(FieldType.INPUT)) {
            s = dr.getValStr(FieldType.INPUT);
            if (s != null) {
              p.addText("<tr><td>");
              p.addText(dr.no);
              p.addText("</td><td nowrap>");
              p.addText(df.format(new Date(dr.recDt)));
              p.addText("</td><td><b>");
              p.addText(s);
              p.addText("</b></td></tr>");
            }
          }

          if (dr.haveVal(FieldType.ERR)) {
            s = dr.getValStr(FieldType.ERR);
            if (s != null) {
              p.addText("<tr><td>");
              p.addText(dr.no);
              p.addText("</td><td nowrap>");
              p.addText(df.format(new Date(dr.recDt)));
              p.addText("</td><td><font color=red>");
              p.addText(s);
              p.addText("</font></td></tr>");
            }
          }

          if (dr.haveVal(FieldType.MSG)) {
            s = dr.getValStr(FieldType.MSG);
            if (s != null) {
              p.addText("<tr><td>");
              p.addText(dr.no);
              p.addText("</td><td nowrap>");
              p.addText(df.format(new Date(dr.recDt)));
              p.addText("</td><td><font color=blue>");
              p.addText(s);
              p.addText("</font></td></tr>");
            }
          }

          if (dr.haveVal(FieldType.STATE_TEXT)) {
            s = dr.getValStr(FieldType.STATE_TEXT);
            if (s != null) {
              p.addText("<tr><td>");
              p.addText(dr.no);
              p.addText("</td><td nowrap>");
              p.addText(df.format(new Date(dr.recDt)));
              p.addText("</td><td><i>");
              p.addText(s);
              p.addText("</i></td></tr>");
            }
          }

          if (dr.haveVal(FieldType.ADD_HIST)) {
            s = dr.getValStr(FieldType.ADD_HIST);
            if (s != null) {
              p.addText("<tr><td>");
              p.addText(dr.no);
              p.addText("</td><td nowrap>");
              p.addText(df.format(new Date(dr.recDt)));
              p.addText("</td><td>");
              p.addText(s);
              p.addText("</td></tr>");
            }
          }

          if (dr.recType == 1) {
            if (dr.haveVal(FieldType.PROC_TYPE)) {
              ptyp = procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)];
            } else {
              ptyp = null;
            }
//            if ((ptyp != null) && ptyp.isUserProc) {
//              isUserTask = true;
//            }
            if ((ptyp != null) && ptyp.canFinish) {
              canFinishTask = true;
            }
          } else if (dr.recType == 2) {
            isFinished = true;
          }
        }
        haveData = dr.readDataRecord(ds);
      }

      ds.close();

      p.addLine("</table> </font>");

      p.addNewLine();
      p.addLine("<b>Структура таблицы:</b>");

      p.addLine("* Номер записи файла");
      p.addLine("* Дата/время записи");
      p.addLine("* Сообщение: <b>ввод пользователя (скан, ввод или выбор меню)</b>; "
              + "<font color=blue>сообщение пользователю</font>; "
              + "<font color=red>ошибка</font>; <i>состояние выполнения</i>; история по задаче");
      p.addNewLine();

      if ((fileNo == 0) && canFinishTask && !isFinished) {
        p.addNewLine();
        p.addRef("admin.fintask." + proc + ".html", "Завершить задачу");
        p.addNewLine();
        p.addNewLine();
      }
    }

    return p.getPage();
  }

  public static FileData finUserTask(long proc, ProcessUser curUser) throws Exception {
    // проверяем что задача не завершена и и принадлежит к завершаемому типу
    synchronized (TermServer.ctx.track.lockRead) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(0)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);
      boolean canFinishTask = false;
      boolean isFinished = false;
      ProcType ptyp;
      long userId = 0;

      while (haveData) {
        if (dr.procId == proc) {
          if (dr.recType == 1) {
            if (dr.haveVal(FieldType.PROC_TYPE)) {
              ptyp = procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)];
            } else {
              ptyp = null;
            }
            if ((ptyp != null) && ptyp.canFinish) {
              canFinishTask = true;
            }
          } else if (dr.recType == 2) {
            isFinished = true;
          } else if (dr.haveVal(FieldType.PARENT)) {
            userId = (Long) dr.getVal(FieldType.PARENT);
          }
        }
        haveData = dr.readDataRecord(ds);
      }
      ds.close();
      if (!canFinishTask) {
        HtmlPage p = new HtmlPage();
        p.fontSize = 11;
        p.title = "Ошибка завершения задачи";
        p.addLine("Ошибка: процессы этого типа нельзя завершать принудительно");
        return p.getPage();
      }
      if (isFinished) {
        HtmlPage p = new HtmlPage();
        p.fontSize = 11;
        p.title = "Ошибка завершения задачи";
        p.addLine("Ошибка: эта задача пользователя уже завершена");
        return p.getPage();
      }
      if (userId == 0) {
        HtmlPage p = new HtmlPage();
        p.fontSize = 11;
        p.title = "Ошибка завершения задачи";
        p.addLine("Ошибка: не могу поличить ид пользователя");
        return p.getPage();
      }

      // загружаем задачу
      Process task = TermServer.ctx.track.loadProcess(proc);

      // пишем в неё сообщение (ошибку)
      dr.clearAll();
      dr.procId = task.getProcId();
      dr.setS(FieldType.ERR, "Принудительное завершение задачи (" + curUser.getUserName() + ")");
      Track.saveProcessChange(dr, task, TermServer.ctx);

      UserContext ctxUser = new UserContext(TermServer.ctx, (ProcessUser) TermServer.ctx.track.getProcess(userId, null));
      if (task instanceof ProcessTask) {
        ((ProcessTask) task).callTaskFinish(ctxUser);
      } else {
        // отправляем в неё выбор меню fin
        TermQuery tq = new TermQuery();
        tq.params = new ParArray("menu=fin", ";");
        task.handleQuery(tq, ctxUser);
      }

      // перенаправление на список активных задач пользователя
      return TermServer.getRedirectPage("admin.0.useractivetasks." + userId + ".html");
    }
  }

  public static FileData getPageTrackDialogStr2(TermQuery tq) throws Exception {
    int fileNo = Integer.parseInt(tq.sf[1]);
    String str = tq.getPar("str");
    int maxRec = Integer.parseInt(tq.getPar("maxrec"));
    int cntRec = 0;

    HtmlPage p = new HtmlPage();
    p.fontSize = 11;

    p.title = "Поиск данных";
    if (fileNo != 0) {
      p.addLine("<b>Архив " + fileNo + "</b>");
    }

    if ((str == null) || str.isEmpty()) {
      p.addLine("Укажите строку для поиска");
      return p.getPage();
    }
    if (maxRec < 1) {
      p.addLine("Максимальное число записей должно быть больше нуля");
      return p.getPage();
    }

    p.addLine("Записи файла данных со строкой <font color=blue><b>"
            + str + "</b></font>");
    p.addRef("admin.html", "На главную");
    if (fileNo != 0) {
      p.addRef("admin.arch.html", "Архив");
    }
    p.addNewLine();
    p.addNewLine();

    p.addText("<font size=2> <table border=1 width=100% cellpadding=1 cellspacing=0>\r\n");

    Object sync = (fileNo == 0 ? TermServer.ctx.track.lockRead : new Object());
    synchronized (sync) {
      DataInputStream ds = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(ds);
      String recText;

      while (haveData) {
        recText = trackRecText(dr);
        if (recText.contains(str)) {
          cntRec++;
          if (cntRec <= maxRec) {
            writeTrackRec(p, dr, true, fileNo);
          }
        }
        haveData = dr.readDataRecord(ds);
      }

      ds.close();

      if (cntRec > maxRec) {
        HtmlPage pp = new HtmlPage();
        pp.fontSize = 11;
        pp.title = "Поиск данных";
        if (fileNo != 0) {
          pp.addLine("<b>Архив " + fileNo + "</b>");
        }
        p.addLine("Превышение максимального числа записей: " + cntRec);
        return pp.getPage();
      }

      p.addLine("</table> </font>");

      p.addNewLine();
      p.addLine("<b>Структура таблицы:</b>");
      p.addLine("* Номер записи файла (трака)");
      p.addLine("* Id процесса (со ссылкой на его трак)");
      p.addLine("* Дата/время записи");
      p.addLine("* Тип записи: new - создание процесса; пусто - изменение; fin - завершение; sys - системные данные");
      p.addLine("* Поля записи");
      p.addNewLine();
    }

    return p.getPage();
  }

  public static FileData getPageRestart() throws Exception {
    new Runnable() {

      public void run() {
        try {
          // пауза, чтобы успеть перенаправить на админ страницу
          Thread.sleep(3000);
          // перезапуск службы
          Runtime.getRuntime().exec("restart_ts.cmd");
        } catch (Exception e) {
        }
      }
    }.run();

    return TermServer.getRedirectPage("admin.html");
  }
}
