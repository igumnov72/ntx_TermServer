package ntx.ts.sysproc;

import java.util.HashMap;
import ntx.sap.refs.RefInfo;
import ntx.ts.html.HtmlPage;
import ntx.ts.html.HtmlPageMenu;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.TermServer;
import ntx.ts.srv.Track;

/**
 * процесс пользователя
 */
public class ProcessUser extends Process {

  private String userName = "";
  private String userSHK = "";
  private String password = "";
  private boolean isAdmin = false;
  private boolean locked = false;
  private boolean canMQ = false;
  private long terminal = 0;
  public final HandlerTaskList tasks; // работа с задачами пользователя
  private long formTime = 0;
  private static final ProcType[] procTypes = ProcType.values();
  private static final ProcType[] procTypesSorted = getProcTypesSorted();
  private String lastTaskType = null;
  private String lastLgort = "";
  private String lastPrinter = "";
  private boolean askQty = true;
  private String lgorts = ""; // список складов в настройках пользователя
  private String lgorts1 = ""; // список складов пользователя (только те, которые есть в настройках сервера)
  private String lgorts2 = ""; // то же что lgorts1, только с запятыми в начале и конце
  private HashMap<Integer, Integer> infoIds = new HashMap<Integer, Integer>(); // иды полученной пользователем информации по типу задачи
  private ProcType[] ttypes = null; // массив с правами пользователя на типы задач (если пусто - все права)

  private static ProcType[] getProcTypesSorted() {
    ProcType[] pt = ProcType.values();
    int n = 0;
    for (int i = 0; i < pt.length; i++) {
      if (pt[i].isUserProc) {
        n++;
      }
    }
    // n содержит число пользовательских типов задач

    ProcType[] ret = new ProcType[n];
    int j = 0;
    for (int i = 0; i < pt.length; i++) {
      if (pt[i].isUserProc) {
        ret[j] = pt[i];
        j++;
      }
    }
    // ret содержит пользовательские типы задач, сортируем (пузырьковая сортировка)

    ProcType t;
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 1; i < ret.length; i++) {
        if (ret[i].order < ret[i - 1].order) {
          t = ret[i];
          ret[i] = ret[i - 1];
          ret[i - 1] = t;
          changed = true;
        }
      }
    }

    return ret;
  }

  public ProcessUser(long procId) throws Exception {
    super(ProcType.USER, procId);
    tasks = new HandlerTaskList(getProcId(), 3);
  }

  public String getLastLgort() {
    return lastLgort;
  }

  public String getLastPrinter() {
    return lastPrinter;
  }

  public boolean getAskQty() {
    return askQty;
  }

  public boolean getAskQtyCompl(String lgort) {
    if (!isAskQtyComplEnabled(lgort)) {
      return false;
    }
    return askQty;
  }

  public boolean isAskQtyComplEnabled(String lgort) {
    // разрешен ли пользователю ручной ввод кол-ва при комплектации на этом складе
    if (canMQ || TSparams.lgortsNoMQ.isEmpty()) {
      return true;
    }

    if ((lgort == null) || lgort.isEmpty()) {
      return false;
    }
    if (TSparams.lgortsNoMQ2.indexOf("," + lgort + ",") >= 0) {
      return false;
    }
    return true;
  }

  public String getUserName() {
    // получение имени пользователя
    return userName;
  }

  public String getUserLgorts() {
    return lgorts1;
  }

  public String getUserCode() {
    // получение кода пользователя

    if (userSHK == null) {
      return "???";
    }

    int n1 = 2;
    int n2 = userSHK.length();
    if (n2 > 10) {
      n2 = 10;
    }
    while ((n1 < n2) && (userSHK.charAt(n1) == '0')) {
      n1++;
    }

    if (n1 >= n2) {
      return userSHK;
    }

    return userSHK.substring(n1, n2);
  }

  public String getUserSHK() {
    // получение ШК пользователя
    return userSHK;
  }

  public String getPassword() {
    // получение пароля пользователя
    return password;
  }

  public boolean getIsAdmin() {
    return isAdmin;
  }

  public boolean getCanMQ() {
    return canMQ;
  }

  public boolean getLocked() {
    return locked;
  }

  public long getTerminal() {
    return terminal;
  }

  public boolean rightsLgort(String lgort) {
    // проверка прав на выполнение операций по складу
    if ((lgort == null) || lgort.isEmpty()) {
      return false;
    }
    if (TSparams.lgorts2.indexOf("," + lgort + ",") == -1) {
      return false;
    }
    if (lgorts.isEmpty()) {
      return true;
    }
    if (lgorts2.indexOf("," + lgort + ",") == -1) {
      return false;
    }
    return true;
  }

  public void callAssignTerminal(long terminal, ProcessContext ctx) throws Exception {
    // присвоение терминала пользователю (как процессу)
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (terminal != this.terminal) {
      dr.setL(FieldType.TERM, terminal);
      dr.setI(FieldType.LOG, terminal == 0 ? LogType.USER_OUT.ordinal() : LogType.USER_IN.ordinal());
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public boolean rightsTtype(ProcType tt) {
    // проверка прав на тип задачи
    if ((tt == null) || !tt.isUserProc) {
      return false;
    }
    if (ttypes == null) {
      return true;
    }
    for (int i = 0; i < ttypes.length; i++) {
      if (tt == ttypes[i]) {
        return true;
      }
    }
    return false;
  }

  public String getUserTtypes() {
    String ret = null;
    if (ttypes == null) {
      for (ProcType pt : ProcType.values()) {
        if (pt.isUserProc) {
          if (ret == null) {
            ret = "" + pt.ordinal();
          } else {
            ret = ret + "," + pt.ordinal();
          }
        }
      }
    } else {
      for (int i = 0; i < ttypes.length; i++) {
        if (ret == null) {
          ret = "" + ttypes[i].ordinal();
        } else {
          ret = ret + "," + ttypes[i].ordinal();
        }
      }
    }
    if (ret == null) {
      return "";
    } else {
      return ret;
    }
  }

  private void fillSetTtypes(ProcType[] tts, DataRecord dr) throws Exception {
    // установка прав на типы задач
    // сравнение прав с текущими (если не изменено - не сохраняем)
    boolean dif = false;
    boolean r1, r2;
    for (ProcType pt : ProcType.values()) {
      if (pt.isUserProc) {
        r1 = rightsTtype(pt);
        r2 = false;
        for (int i = 0; i < tts.length; i++) {
          if (pt == tts[i]) {
            r2 = true;
            break;
          }
        }
        if (r1 != r2) {
          dif = true;
          break;
        }
      }
    }

    if (!dif) {
      // права совпадают
      return;
    }

    int n = 0;
    int[] it = new int[tts.length];
    for (int i = 0; i < tts.length; i++) {
      if (tts[i].isUserProc) {
        it[n] = tts[i].ordinal();
        n++;
      }
    }

    if (n == tts.length) {
      dr.setIa(FieldType.TTYPES, it);
    } else {
      int[] it2 = new int[n];
      for (int i = 0; i < n; i++) {
        it2[i] = it[i];
      }
      dr.setIa(FieldType.TTYPES, it2);
    }
  }

  public void callChangeUser(String userName, String userSHK, String password,
          String lgorts, boolean isAdmin, boolean locked, boolean canMQ, ProcType[] tts,
          ProcessContext ctx) throws Exception {
    // изменение параметров пользователя
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!userName.equals(this.userName)) {
      dr.setS(FieldType.NAME, userName);
    }
    if (!userSHK.equals(this.userSHK)) {
      dr.setS(FieldType.SHK, userSHK);
    }
    if (!password.equals(this.password)) {
      dr.setS(FieldType.PASSWORD, password);
    }
    if (!lgorts.equals(this.lgorts)) {
      dr.setS(FieldType.LGORTS, lgorts);
    }
    if (isAdmin != this.isAdmin) {
      dr.setB(FieldType.IS_ADMIN, isAdmin);
    }
    if (locked != this.locked) {
      dr.setB(FieldType.LOCKED, locked);
    }
    if (canMQ != this.canMQ) {
      dr.setB(FieldType.CAN_MQ, canMQ);
    }
    fillSetTtypes(tts, dr);

    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetPassword(String password, ProcessContext ctx) throws Exception {
    // изменение пароля
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!password.equals(this.password)) {
      dr.setS(FieldType.PASSWORD, password);
    }

    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetLgort(String lgort, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!lgort.equals(lastLgort)) {
      dr.setS(FieldType.LGORT, lgort);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetPrinter(String printer, ProcessContext ctx) throws Exception {
    if (printer == null) {
      return;
    }
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!printer.equals(lastPrinter)) {
      dr.setS(FieldType.PRINTER, printer);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetAskQty(boolean askQty, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (askQty != this.askQty) {
      dr.setB(FieldType.ASK_QTY, askQty);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callDeleteUser(ProcessContext ctx) throws Exception {
    if (tasks.getTaskId(0) != 0) {
      throw new Exception("Ошибка программы: удаление пользователя с активными процессами запрещено");
    }
    Track.saveProcessFinish(getProcId(), this, ctx);
  }

  public void callSetLastTaskType(String tt, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!ProcessTask.strEq(tt, lastTaskType)) {
      dr.setS(FieldType.LAST_TASK_TYPE, tt);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    // обработка записи трака (в т.ч. в реальном времени)

    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.NAME)) {
          userName = (String) dr.getVal(FieldType.NAME);
        }
        if (dr.haveVal(FieldType.SHK) && !userSHK.equals((String) dr.getVal(FieldType.SHK))) {
          if (!userSHK.isEmpty()) {
            ctx.track.tabSHK.remove(userSHK);
          }
          userSHK = (String) dr.getVal(FieldType.SHK);
          if (!userSHK.isEmpty()) {
            ctx.track.tabSHK.put(userSHK, this);
          }
        }
        if (dr.haveVal(FieldType.PASSWORD)) {
          password = (String) dr.getVal(FieldType.PASSWORD);
        }
        if (dr.haveVal(FieldType.LGORTS)) {
          lgorts = (String) dr.getVal(FieldType.LGORTS);
          if (lgorts.isEmpty()) {
            lgorts1 = "";
            lgorts2 = "";
          } else {
            String[] lgs = lgorts.split(",");
            lgorts1 = "";
            for (int i = 0; i < lgs.length; i++) {
              if (TSparams.lgorts2.indexOf("," + lgs[i] + ",") != -1) {
                lgorts1 = (lgorts1.isEmpty() ? "" : lgorts1 + ",") + lgs[i];
              }
            }
            if (lgorts1.isEmpty()) {
              lgorts1 = "----";
            }
            lgorts2 = "," + lgorts1 + ",";
          }
        }
        if (dr.haveVal(FieldType.IS_ADMIN) && isAdmin != (Boolean) dr.getVal(FieldType.IS_ADMIN)) {
          isAdmin = (Boolean) dr.getVal(FieldType.IS_ADMIN);
          if (!locked) {
            if (isAdmin) {
              ctx.track.adminCount++;
            } else {
              ctx.track.adminCount--;
            }
          }
        }
        if (dr.haveVal(FieldType.LOCKED) && locked != (Boolean) dr.getVal(FieldType.LOCKED)) {
          locked = (Boolean) dr.getVal(FieldType.LOCKED);
          if (isAdmin) {
            if (!locked) {
              ctx.track.adminCount++;
            } else {
              ctx.track.adminCount--;
            }
          }
        }
        if (dr.haveVal(FieldType.CAN_MQ) && canMQ != (Boolean) dr.getVal(FieldType.CAN_MQ)) {
          canMQ = (Boolean) dr.getVal(FieldType.CAN_MQ);
        }
        if (dr.haveVal(FieldType.TERM)) {
          if (terminal != 0) {
            ctx.track.tabTerm.remove(terminal);
          }
          terminal = (Long) dr.getVal(FieldType.TERM);
          if (terminal != 0) {
            ctx.track.tabTerm.put(terminal, this);
          }
        }
        if (dr.haveVal(FieldType.LGORT)) {
          lastLgort = (String) dr.getVal(FieldType.LGORT);
        }
        if (dr.haveVal(FieldType.PRINTER)) {
          lastPrinter = (String) dr.getVal(FieldType.PRINTER);
        }
        if (dr.haveVal(FieldType.ASK_QTY)) {
          askQty = (Boolean) dr.getVal(FieldType.ASK_QTY);
        }
        if (dr.haveVal(FieldType.PTYP) && dr.haveVal(FieldType.INFO_ID)) {
          int ptyp = (Integer) dr.getVal(FieldType.PTYP);
          int infoId = (Integer) dr.getVal(FieldType.INFO_ID);
          if (infoId == -1) {
            infoIds.remove(ptyp);
          } else {
            infoIds.put(ptyp, infoId);
          }
        }
        if (dr.haveVal(FieldType.TTYPES)) {
          int[] tt = (int[]) dr.getVal(FieldType.TTYPES);
          if (tt == null) {
            ttypes = null;
          } else {
            ttypes = new ProcType[tt.length];
            for (int i = 0; i < tt.length; i++) {
              ttypes[i] = procTypes[tt[i]];
            }
          }
        }

        // поддержка списка задач
        tasks.handleData(dr, ctx);
        if (dr.haveVal(FieldType.LAST_TASK_TYPE)) {
          lastTaskType = (String) dr.getVal(FieldType.LAST_TASK_TYPE);
        }
        break;

      case 2:
        ctx.track.tabSHK.remove(userSHK);
        if (terminal != 0) {
          ctx.track.tabTerm.remove(terminal);
        }
        if (isAdmin && !locked) {
          ctx.track.adminCount--;
        }
        break;
    }
  }

  public FileData handleQuery(TermQuery tq, ProcessContext ctx) throws Exception {
    // проверка на тестовый файл
    if ((tq != null) && (tq.fname != null) && tq.fname.equalsIgnoreCase("test.html") && isAdmin) {
      return TermServer.getFile(tq);
    }

    FileData ret;
    UserContext ctxUser = new UserContext(ctx, this);

    String ftime = (tq.params == null ? null : tq.params.getParNull("formtime"));
    long ft = (((ftime == null) || ftime.isEmpty()) ? 0 : Long.parseLong(ftime));

    if ((ft == 0) || (formTime == 0) || (ft == formTime)) {
      ret = handleQueryDo(tq, ctxUser);
    } else {
      // получены данные формы, которая отправлена не последней
      HtmlPageMessage p = new HtmlPageMessage("Полученная форма устарела",
              "Проверьте правильность последнего сканирования", null, null);
      ret = p.getPage();
    }

    formTime = ret.formTime;
    return ret;
  }

  public FileData handleQueryDo(TermQuery tq, UserContext ctx) throws Exception {
    // обработка инф с терминала

    ProcessTask activeTask = tasks.getActiveTask(ctx);

    String scan = (tq.params == null ? null : tq.params.getParNull("scan"));
    String menu = (tq.params == null ? null : tq.params.getParNull("menu"));
    String passw1 = (tq.params == null ? null : tq.params.getParNull("passw1"));
    String htext1 = (tq.params == null ? null : tq.params.getParNull("htext1"));

    if (activeTask == null) {
      // выбор/создание задачи
      if (scan != null) {
        return htmlSelTask(null, null);
      } else if (menu != null) {
        return handleMenu(menu.toLowerCase(), ctx);
      } else if (passw1 != null) {
        return htmlPassw(tq, ctx);
      }
      return htmlSelTask(null, null);
    }

    if ((htext1 != null) && (htext1.startsWith("shownmsg"))) {
      handleShownMsg(htext1, ctx);
    }

    // перенаправление запроса в активную задачу
    FileData ret = null;
    if ((scan != null) && scan.isEmpty()) {
      // пустой скан не передаем, просто открываем рабочую страницу
      ret = activeTask.handleQuery(new TermQuery(), new UserContext(ctx, this));
    } else {
      // сервисные ф-ии для активной задачи (вынесены из кода задачи для удобства и универсальности)
      ret = serviceActiveTask(activeTask, scan, menu, ctx);
      if (ret != null) {
        return ret;
      }

      // отправляем запрос в активную задачу
      ret = activeTask.handleQuery(tq, new UserContext(ctx, this));
    }

    return ret == null ? htmlSelTask(null, null) : ret;
  }

  private FileData serviceActiveTask(ProcessTask activeTask, String scan,
          String menu, ProcessContext ctx) throws Exception {
    // сервисные ф-ии для активной задачи (вынесены из кода задачи для удобства и универсальности)

    // сначала запоминаем выбор склада, если есть
    if ((menu != null) && (menu.length() == 12) && menu.startsWith("sellgort")) {
      callSetLgort(menu.substring(8), ctx);
    }

    // обработка запроса на отображение инструкций
    if (menu != null) {
      if (menu.equals("manuals")) {
        // возвращаем меню выбора инструкции
        return RefInfo.htmlManualMenu(activeTask.getProcType().ordinal());
      } else if (menu.startsWith("manual")) {
        // отображаем выбранную инструкцию
        return RefInfo.htmlManualShow(activeTask.getProcType().ordinal(), Integer.parseInt(menu.substring(6)));
      }
    }

    // перед отправкой запроса в активную задачу
    // сохраняем ввод на траке
    String inp = null;
    if ((scan != null) && !scan.isEmpty()) {
      inp = scan;
    } else if ((menu != null) && !menu.isEmpty()) {
      inp = menu;
    }
    if (inp != null) {
      DataRecord dr = new DataRecord();
      dr.procId = activeTask.getProcId();
      dr.setS(FieldType.INPUT, inp);
      Track.saveProcessChange(dr, activeTask, ctx);
    }

    return null;
  }

  private void handleShownMsg(String htext1, ProcessContext ctx) throws Exception {
    String[] ss = htext1.substring(8).split("\\.");
    callSetInfoId(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]), ctx);
  }

  public FileData handleMenu(String menu, UserContext ctx) throws Exception {
    if (menu.equals("create")) {
      // переход к созданию новой задачи
      return htmlCreTask();
    } else if (menu.startsWith("task")) {
      // переключение к задаче
      long task = Long.parseLong(menu.substring(4));
      int n = tasks.getActiveTaskCount();
      for (int i = 0; i < n; i++) {
        if (tasks.getTaskId(i) == task) {
          tasks.callTaskActivate(task, ctx);
          return startingTask(tasks.getActiveTask(ctx), ctx);
        }
      }
    } else if (menu.startsWith("new")) {
      // создание новой задачи
      if (!rightsTtype(procTypes[Integer.parseInt(menu.substring(3))])) {
        return htmlCreTask();
      }
      callSetLastTaskType(menu, ctx);
      int tp = Integer.parseInt(menu.substring(3));
      tasks.callTaskCreate(procTypes[tp], ctx);
      return startingTask(tasks.getActiveTask(ctx), ctx);
    } else if (menu.equals("password")) {
      // переход к изменению пароля
      return htmlPassw(new TermQuery(), ctx);
    } else if (menu.equals("exit")) {
      callAssignTerminal(0, ctx);
      ctx.track.log.save();
      return TermServer.getLoginPage1(new TermQuery(), null);
    } else if (menu.equals("admin")) {
      return TermServer.getRedirectPage("admin.html");
    } else if (menu.equals("test") && isAdmin) {
      return TermServer.getRedirectPage("test.html");
    }

    return htmlSelTask(null, null);
  }

  private FileData startingTask(ProcessTask task, UserContext ctx) throws Exception {
    if (haveNewInfo(task.getProcType())) {
      return htmlNewInfo(task.getProcType());
    }
    return task.handleQuery(new TermQuery(), new UserContext(ctx, this));
  }

  public FileData htmlPassw(TermQuery tq, UserContext ctx) throws Exception {
    String passw1 = (tq.params == null ? null : tq.params.getParNull("passw1"));
    String passw2 = (tq.params == null ? null : tq.params.getParNull("passw2"));
    String passw3 = (tq.params == null ? null : tq.params.getParNull("passw3"));

    if (passw1 == null) {
      if (password.isEmpty()) {
        passw1 = "";
      }
    } else if (!passw1.equals(password)) {
      return htmlSelTask("Введен неправильный пароль", null);
    } else if ((passw2 != null) && passw2.isEmpty()) {
      return htmlSelTask("Пароль не может быть пустым", "Пароль не изменен");
    } else if (passw3 != null) {
      if (passw3.equals(passw2)) {
        // меняем пароль
        callSetPassword(passw3, ctx);
        return htmlSelTask(null, "Пароль успешно изменен");
      } else {
        return htmlSelTask("Пароли не совпадают", "Пароль не изменен");
      }
    }

    HtmlPage p = new HtmlPage();
    p.fontSize = TSparams.fontSize;
    p.title = "Изменение пароля";
    p.sound = "ask.wav";
    p.addBlock("Изменение пароля<br>(" + userName + ")");
    String setFocus;
    if (passw1 == null) {
      setFocus = "f.passw1";
      p.addBlock("<b>Введите СТАРЫЙ пароль:</b>");
      p.addFormStart("work.html", "f");
      p.addFormFieldPassword("passw1", 20);
    } else if (passw2 == null) {
      setFocus = "f.passw2";
      p.addBlock("<b>Введите НОВЫЙ пароль:</b>");
      p.addFormStart("work.html", "f");
      p.addFormFieldHidden("passw1", 1, passw1);
      p.addFormFieldPassword("passw2", 20);
    } else {
      setFocus = "f.passw3";
      p.addBlock("<b>Повторите НОВЫЙ пароль:</b>");
      p.addFormStart("work.html", "f");
      p.addFormFieldHidden("passw1", 1, passw1);
      p.addFormFieldHidden("passw2", 1, passw2);
      p.addFormFieldPassword("passw3", 20);
    }
    p.addFormButtonSubmit("Ввод", setFocus, false);
    p.addFormEnd();
    return p.getPage();
  }

  public FileData htmlSelTask(String err, String msg) throws Exception {
    if (password.isEmpty()) {
      HtmlPageMenu p = new HtmlPageMenu("Меню", userName + " <br> Выбор задачи",
              "password:Сменить пароль;exit:Выход", null, null,
              "Для работы необходимо установить непустой пароль");
      return p.getPage();
    }

    String definition = "";

    int n = tasks.getActiveTaskCount();
    for (int i = 0; i < n; i++) {
      if (!definition.isEmpty()) {
        definition = definition + ";";
      }
      definition = definition + "task" + tasks.getTaskId(i) + ":" + tasks.getTaskName(i);
    }

    if (tasks.getActiveTaskCount() < tasks.initSize) {
      if (!definition.isEmpty()) {
        definition = definition + ";";
      }
      definition = definition + "create:Новая задача;password:Сменить пароль;exit:Выход";
    }

    if (isAdmin) {
      definition = definition + ";admin:Администрирование;test:Тестовая страница";
    }

    HtmlPageMenu p = new HtmlPageMenu("Меню", userName + " <br> Выбор задачи", definition, null, err, msg);

    if (tasks.taskFinished) {
      p.sound = "done.wav";
      tasks.taskFinished = false;
    } else if (err != null) {
      p.sound = "err.wav";
    } else if (msg != null) {
      p.sound = "done.wav";
    }

    return p.getPage();
  }

  public FileData htmlCreTask() throws Exception {
    String definition = "";

    for (int i = 0; i < procTypesSorted.length; i++) {
      if (rightsTtype(procTypesSorted[i])) {
        definition = definition + "new" + procTypesSorted[i].ordinal()
                + ":" + procTypesSorted[i].text + ";";
      }

    }

    definition = definition + "ret:Назад";

    HtmlPageMenu p = new HtmlPageMenu("Меню", userName + " <br> Создание задачи",
            definition, lastTaskType, null, null);

    return p.getPage();
  }

  private void callSetInfoId(int ptyp, int infoId, ProcessContext ctx) throws Exception {
    Integer infoId0 = infoIds.get(ptyp);
    if ((ptyp == 0) || ((infoId0 != null) && (infoId0 == infoId))) {
      return;
    } else {
      DataRecord dr = new DataRecord();
      dr.procId = getProcId();
      if (ptyp != 0) {
        dr.setI(FieldType.PTYP, ptyp);
      }
      if ((infoId0 == null) || (infoId0 != infoId)) {
        dr.setI(FieldType.INFO_ID, infoId);
      }
      Track.saveProcessChange(dr, this, ctx);
    }
  }

  private boolean haveNewInfo(ProcType ptyp) throws Exception {
    // проверка наличия новых сообщений по пользователю и типу задачи
    Integer infoId = infoIds.get(ptyp.ordinal());
    return RefInfo.haveNewInfo(ptyp, infoId);
  }

  private FileData htmlNewInfo(ProcType ptyp) throws Exception {
    // выдача новых сообщений по пользователю и типу задачи
    Integer infoId = infoIds.get(ptyp.ordinal());
    return RefInfo.htmlNewInfo(ptyp.ordinal(), infoId);
  }
}
