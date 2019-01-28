package ntx.ts.srv;

import ntx.ts.http.FileData;
import ntx.ts.html.HtmlPage;
import java.io.*;
import java.util.Date;
import ntx.sap.fm.Z_TS_WP_BY_ID;
import ntx.sap.refs.RefMat;
import ntx.sap.refs.RefMatStruct;
import ntx.ts.admin.Admin;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.sysproc.ProcessContext;
import ntx.ts.sysproc.ProcessTask;
import ntx.ts.sysproc.ProcessUser;
import ntx.ts.sysproc.ProcessWP;

public class TermServer {

  public static PrintStream docLog;
  public static ProcessContext ctx;

  public static void init() throws Exception {
    docLog = new PrintStream(
            new BufferedOutputStream(
                    new DateTimeLogger(
                            new FileOutputStream("ts.doc.log", true))), false);
    ctx = new ProcessContext(new Track(0));
    ctx.track.init();
  }

  public static FileData handleQuery(TermQuery tq) throws Exception {
    // !!! сюда могут одновременно заходить разные потоки !!!

    log("handleQuery started");
    log("tq.doingGet = " + tq.doingGet);
    log("tq.doingPost = " + tq.doingPost);
    log("tq.fname = " + tq.fname);
    if (tq.params != null) {
      for (int i = 0; i < tq.params.length(); i++) {
        log("tq.params[" + tq.params.getNam(i) + "] = " + tq.params.getVal(i));
      }
    }

    if ((tq.sf.length == 2) && (tq.sf[1].equalsIgnoreCase("wav"))
            && (tq.sf[0].equalsIgnoreCase("ok") || tq.sf[0].equalsIgnoreCase("err")
            || tq.sf[0].equalsIgnoreCase("done") || tq.sf[0].equalsIgnoreCase("ask"))) {
      // выдача звукового файла
      FileData ret = getFile(tq);
      return ret;
    } else if ((tq.sf.length == 2) && tq.sf[1].equalsIgnoreCase("jpg")
            && (tq.sf[0].length() > 3) && (tq.sf[0].startsWith("mat"))) {
      // выдача фото материала
      RefMatStruct m = RefMat.getNoNull(tq.sf[0].substring(3));
      FileData ret = new FileData(m.getFoto(), new Date(m.dtMod));
      log("returning file " + tq.fname);
      logFlush();
      return ret;
    } else if ((tq.sf.length == 2) && (tq.sf[1].equalsIgnoreCase("jpg"))
            && (tq.sf[0].equalsIgnoreCase("no_foto") || tq.sf[0].equalsIgnoreCase("err_foto"))) {
      // выдача звукового файла
      FileData ret = getFile(tq);
      return ret;
    } else if ((tq.sf.length > 2) && tq.sf[0].equalsIgnoreCase("login")) {
      // регистрация в системе
      return getLoginPage(tq);
    }

    // поиск процесса терминала
    ProcessUser user = null;
    if (tq.terminal > 0) {
      user = ctx.track.getUserByTerm(tq.terminal);
    }

    if ((user == null) && tq.sf[tq.sf.length - 1].equalsIgnoreCase("html")) {
      // процесс терминала не найден, выдаем 1-ю страницу регистрации
      return getLoginPage1(tq, null);
    } else if (user == null) {
      return null;
    } else if ((tq.sf.length > 1) && tq.sf[0].equalsIgnoreCase("admin")) {
      // выдача админ данных
      return Admin.getPage(tq, user);
    } else if (user.getLocked()) {
      // пользователь заблокирован
      return getLoginPage1(tq, "Пользователь \"" + user.getUserName() + "\" заблокирован");
    } else if (tq.fname.equalsIgnoreCase("logout.html")) {
      // завершение работы пользователя на терминале
      synchronized (user) {
        user.callAssignTerminal(0, ctx);
      }
      return getRedirectPage(tq.fname); // разве редирект д.б. не на login?
    } else if (!tq.sf[tq.sf.length - 1].equalsIgnoreCase("html")) {
      return null;
    } else {
      // рабочая страница
      synchronized (user) {
        return user.handleQuery(tq, ctx);
      }
    }
  }

  private static FileData getLoginPage(TermQuery tq) throws Exception {
    if (tq.sf[1].equals("1")) {
      // получен ШК пользователя, проверяем
      String scan = tq.getPar("scan");
      if (scan.isEmpty()) {
        // ШК пользователя не отсканирован
        return getLoginPage1(tq, null);
      }

      if (!scan.equals("1")) {
        if (scan.length() < 8) {
          while (scan.length() < 8) {
            scan = "0" + scan;
          }
          scan = "01" + scan + "A";
        } else if (scan.length() > 11) {
          // неверный ШК пользователя
          tq.fname = tq.params.getPar("redir");
          return getLoginPage1(tq, "Неверный ШК пользователя");
        }
      }

      ProcessUser user = ctx.track.getUserBySHK(scan);

      if (user == null) {
        tq.fname = tq.params.getPar("redir");
        return getLoginPage1(tq, "Нет такого пользователя");
      } else if (user.getLocked()) {
        tq.fname = tq.params.getPar("redir");
        return getLoginPage1(tq, "Пользователь \"" + user.getUserName() + "\" заблокирован");
      }

      return getLoginPage2(tq, user);
    } else if (tq.sf[1].equals("2")) {
      // проверка пароля
      long userId = Long.parseLong(tq.params.getPar("uid"));
      ProcessUser user = (ProcessUser) ctx.track.getProcess(userId, null);
      if (user == null) {
        return getLoginPage1(tq, "Ошибка проверки пароля пользователя");
      }

      if (tq.terminal == 0) {
        return getLoginPage1(tq, "Ошибка проверки пароля пользователя (нет номера терминала)");
      }

      String scan = tq.getPar("scan");

      if (!scan.equals(user.getPassword())) {
        return getLoginPage1(tq, "Неверный пароль");
      }

      user.callAssignTerminal(tq.terminal, ctx);

      return getRedirectPage(tq.params.getPar("redir"));
    } else {
      return getLoginPage1(tq, null);
    }
  }

  public static FileData getLoginPage1(TermQuery tq, String err) throws Exception {
    HtmlPage p = new HtmlPage();

    p.title = "Регистрация в системе";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize;

    String scan = tq.getPar("scan");
    if (!scan.isEmpty()) {
      p.sound = "err.wav";
      p.addBlock("Ошибка! Сканирование не принято: " + scan, 0, "red");
      if ((err != null) && !err.isEmpty()) {
        p.addBlock(err, 0, "red");
      }
      p.addNewLine();
    }

    p.addBlock("<b>Отсканируйте свой штрих-код или введите свой код:</b>");
    p.addFormStart("login.1.html", "f");
    p.addFormField("scan", 20);
    p.addFormFieldHidden("redir", 1, tq.fname == null ? "work.html" : tq.fname);
    p.addFormButtonSubmit(">", "f.scan", false);
    p.addFormEnd();

    return p.getPage();
  }

  private static FileData getLoginPage2(TermQuery tq, ProcessUser user) throws Exception {
    HtmlPage p = new HtmlPage();

    p.title = "Регистрация в системе";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize;

    p.addBlock(user.getUserName() + "<br>код " + user.getUserCode());
    p.addNewLine();
    p.addBlock("<b>Введите свой пароль:</b>");
    p.addFormStart("login.2.html", "f");
    p.addFormFieldPassword("scan", 20);
    p.addFormFieldHidden("redir", 1, tq.params.getPar("redir"));
    p.addFormFieldHidden("uid", 1, "" + user.getProcId());
    p.addFormButtonSubmit(">", "f.scan", false);
    p.addFormEnd();

    return p.getPage();
  }

  public static FileData getSelWpPage(TermQuery tq, String err) throws Exception {
    HtmlPage p = new HtmlPage();

    p.title = "Привязка терминала к рабочему месту";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize;

    String scan = tq.getPar("scan");
    if (!scan.isEmpty() || ((err != null) && !err.isEmpty())) {
      p.sound = "err.wav";
      if (!scan.isEmpty()) {
        p.addBlock("Ошибка! Сканирование не принято: " + scan, 0, "red");
      }
      if ((err != null) && !err.isEmpty()) {
        p.addBlock(err, 0, "red");
      }
      p.addNewLine();
    }

    p.addBlock("<b>Отсканируйте ШК рабочего места:</b>");
    p.addFormStart("work.html", "f");
    p.addFormField("scan", 20);
    p.addFormFieldHidden("redir", 1, tq.fname == null ? "work.html" : tq.fname);
    p.addFormButtonSubmit(">", "f.scan", false);
    p.addFormEnd();

    return p.getPage();
  }

  public static FileData getFile(TermQuery tq) throws Exception {
    // читаем и возвращаем указанный файл

    File f = new File(tq.fname);

    if (!f.exists() || !f.isFile()) {
      throw new Exception("Файл " + tq.fname + " не найден");
    }
    String l_fileName = f.getName();
    long l_fileTimeStamp = f.lastModified();
    int l_fileSize = (int) f.length();
    byte[] l_fileData = new byte[l_fileSize];

    FileInputStream is = new FileInputStream(f);
    int r = 0;
    int off = 0;

    while (r != -1) {
      r = is.read(l_fileData, off, l_fileSize - off);
      if (r > 0) {
        off += r;
      } else if (r == 0) {
        break;
      }
    }
    if (off != l_fileSize) {
      throw new Exception("При чтении файла " + l_fileName + " получено " + off + " байт, а д.б. " + l_fileSize);
    }

    FileData ret = new FileData(l_fileData, new Date(l_fileTimeStamp));

    log("returning file " + tq.fname);
    logFlush();

    return ret;
  }

  private static void log(String txt) {
    docLog.println(txt);
  }

  private static void logFlush() {
    docLog.flush();
  }

  public static FileData getRedirectPage(String fname) throws Exception {
    // возвращает страницу с перенаправлением на указанную страницу
    String n = fname;
    if ((n == null) || n.isEmpty() || n.equalsIgnoreCase("logout.html")
            || ((n.length() > 5) && (n.substring(0, 5).equalsIgnoreCase("login")))) {
      n = "work.html";
    }

    HtmlPage p = new HtmlPage();
    p.title = n;
    p.addRef(n, n);
    p.addNewLine();

    FileData fd = p.getPage();
    fd.redirect = n;
    return fd;
  }

  public static FileData handleQueryWP(TermQuery tq) throws Exception {
    // !!! сюда могут одновременно заходить разные потоки !!!

    log("handleQueryWP started");
    log("tq.doingGet = " + tq.doingGet);
    log("tq.doingPost = " + tq.doingPost);
    log("tq.fname = " + tq.fname);
    if (tq.params != null) {
      for (int i = 0; i < tq.params.length(); i++) {
        log("tq.params[" + tq.params.getNam(i) + "] = " + tq.params.getVal(i));
      }
    }

    if ((tq.sf.length == 2) && (tq.sf[1].equalsIgnoreCase("wav"))
            && (tq.sf[0].equalsIgnoreCase("ok") || tq.sf[0].equalsIgnoreCase("err")
            || tq.sf[0].equalsIgnoreCase("done") || tq.sf[0].equalsIgnoreCase("ask"))) {
      // выдача звукового файла
      FileData ret = getFile(tq);
      return ret;
    } else if ((tq.sf.length > 2) && tq.sf[0].equalsIgnoreCase("wp")) {
      // регистрация терминала в системе (привязка к рабочему месту)
      return getSelWpPage(tq, null);
    }

    if (!tq.sf[tq.sf.length - 1].equalsIgnoreCase("html")) {
      return null;
    } else {
      try {
        // поиск процесса
        ProcessWP wp = null;

        // проверяем сканирование ШК WP
        String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
        if ((scan != null) && ProcessTask.isScanWP(scan)) {
          int wpNum = Integer.parseInt(scan.substring(2));
          wp = ctx.track.getWPbyNum(wpNum);
          if (wp == null) {
            // нет такого процесса, создаем
            wp = ProcessWP.createWP(wpNum, ctx);
          }
          if (wp == null) {
            // неправильный ШК WP, выдаем ошибку
            return getSelWpPage(tq, "Нет такого рабочего места или ошибка обращения к САПу; обратитесь к администртору");
          }
        }

        if (wp == null) {
          // ШК рабочего места не сканировался (прямо сейчас), процесс еще не найден
          if (tq.terminal == 0) {
            return getSelWpPage(tq, null);
          }

          wp = ctx.track.getWPbyTerm(tq.terminal);
        }

        if (wp == null) {
          // id терминала имеется, а процесс еще не найден
          // делаем запрос в САП
          Z_TS_WP_BY_ID f = new Z_TS_WP_BY_ID();
          f.TERM_DT_ID = Long.toString(tq.terminal);
          f.execute();
          if (f.isErr) {
            HtmlPageMessage p = new HtmlPageMessage(f.err,
                    "(ошибка при обращении к САПу)", null, null);
            return p.getPage();
          }

          if (f.WP_ID != 0) {
            // теперь поиск процесса по номеру рабочего места
            wp = ctx.track.getWPbyNum(f.WP_ID);
            if (wp == null) {
              // или создание процесса
              wp = ProcessWP.createWP(f.WP_ID, ctx);
            }
          } else {
            return getSelWpPage(tq, null);
          }
        }

        if (wp == null) {
          return getSelWpPage(tq, "Ошибка инициализации рабочего места; обратитесь к администртору");
        }

        // получен процесс, передаем ему запрос
        synchronized (wp) {
          return wp.handleQuery(tq, ctx);
        }
      } catch (Exception e) {
        HtmlPageMessage p = new HtmlPageMessage(e.getMessage(),
                "ошибка в программе, обратитесь к администратору", null, null);
        return p.getPage();
      }
    }
  }
}
