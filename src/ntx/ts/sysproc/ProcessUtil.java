package ntx.ts.sysproc;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import ntx.sap.fm.ZSHK_INFO;
import ntx.sap.refs.RefMat;
import ntx.sap.refs.RefMatStruct;
import ntx.ts.html.HtmlPage;
import ntx.ts.html.HtmlPageWork;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.TaskState;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.Track;
import ntx.ts.srv.ScanChargQty;

/**
 * предок для процессов, содержащий полезные утилиты
 */
public abstract class ProcessUtil extends Process {

  private String lastErr = null; // ошибка после последнего сканирования
  private String lastMsg = null; // сообщение после последнего сканирования
  private String stateText = null; // описание текущего состояния
  private TaskState taskState = TaskState.START; // состояние процесса на терминале
  private ArrayList<String> hist = new ArrayList<String>(); // история работы
  private static final TaskState[] taskStates = TaskState.values();
  public static DateFormat parseDf1 = new SimpleDateFormat("ddMMyy");
  public static DateFormat parseDf2 = new SimpleDateFormat("yyyyMMdd");
  public static DateFormat parseDf3 = new SimpleDateFormat("yyyyMMdd HHmmss");
  private String specSound = null; // one-time sound, example att.wav
  
  public void setSpecSound(String sound) {
    specSound = sound;
  }

  public String[] getHist() {
    int n = hist.size();
    String[] ret = new String[n];
    for (int i = 0; i < n; i++) {
      ret[i] = "* " + hist.get(n - i - 1);
    }
    return ret;
  }

  public String getUserAndSkl(ProcessContext ctx) throws Exception {
    String addTaskName = getAddTaskName(ctx);
    if (UserContext.class.isInstance(ctx)) {
      UserContext uctx = (UserContext) ctx;

        int i = 0;
        String userName = uctx.user.getUserName();
        for (; i < userName.length(); i++ )
            if (userName.charAt(i) == ' ') break;
        userName = userName.substring(0, i);

      if (addTaskName == null) {
        return userName; //uctx.user.getUserName();
      } else {
        return userName //uctx.user.getUserName() 
                + "; " + addTaskName;
      }
    } else {
      return addTaskName == null ? "" : addTaskName;
    }
  }

  public String getAddTaskName(ProcessContext ctx) throws Exception {
    // дополнительное описание задачи (номер и название склада, номер поставки и т.п.)
    // может быть быть переопределена
    return null;
  }

  public void callAddHist(String msg, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if ((msg != null) && !msg.isEmpty()) {
      dr.setS(FieldType.ADD_HIST, msg);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callDelHist(ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    dr.setV(FieldType.DEL_HIST);
    Track.saveProcessChange(dr, this, ctx);
  }

  public TaskState getTaskState() {
    return taskState;
  }

  public void callSetTaskState(TaskState tstate, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (tstate != taskState) {
      dr.setI(FieldType.TASK_STATE, tstate.ordinal());
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callTaskFinish(ProcessContext ctx) throws Exception {
    Track.saveProcessFinish(getProcId(), this, ctx);
  }

  public ProcessUtil(ProcType procType, long procId) throws Exception {
    super(procType, procId);
    parseDf1.setLenient(false);
    parseDf2.setLenient(false);
  }

  public Date parseDateDDMMYY(String dt) {
    if ((dt == null) || (dt.length() != 6)) {
      return null;
    }
    Date ret;
    try {
      ret = parseDf1.parse(dt);
    } catch (Exception e) {
      ret = null;
    }
    return ret;
  }

  public Date parseDateYYYYMMDD_HHMMSS(String dt) {
    if ((dt == null) || (dt.length() != 15)) {
      return null;
    }
    Date ret;
    try {
      ret = parseDf3.parse(dt);
    } catch (Exception e) {
      ret = null;
    }
    return ret;
  }

  public String sapDateDDMMYY(String dt) {
    Date d = parseDateDDMMYY(dt);
    if (d == null) {
      return null;
    } else {
      return parseDf2.format(d);
    }
  }

  public String getLastErr() {
    return lastErr;
  }

  public String getLastMsg() {
    return lastMsg;
  }

  public String getStateText() {
    return stateText;
  }

  public boolean isErr() {
    return lastErr != null;
  }

  private String getSound(boolean playSound) {
    String sound = playSound ? (isErr() ? "err.wav" : "ok.wav") : "ask.wav";
    if (specSound != null) {
        sound = specSound;
        specSound = null;
    }
    return sound;
  }
  
  public FileData htmlWork(String title, boolean playSound, ProcessContext ctx) throws Exception {
    // помимо форматирования тут нужно вывести имя пользователя и склад (если есть)
    HtmlPageWork p = new HtmlPageWork(
            title,
            getSound(playSound),
            title + " (" + getUserAndSkl(ctx) + ")",
            lastErr,
            lastMsg,
            stateText,
            getActionText(),
            getHist());

    return p.getPage();
  }

  public String getActionText() {
    // процедура получения текста над полем ввода
    // м.б. переопределена
    return getTaskState().actionText;
  }

  public FileData htmlWork(String title, boolean playSound, String deflt, ProcessContext ctx) throws Exception {
    // помимо форматирования тут нужно вывести имя пользователя и склад (если есть)
    HtmlPageWork p = new HtmlPageWork(
            title,
            getSound(playSound),
            title + " (" + getUserAndSkl(ctx) + ")",
            lastErr,
            lastMsg,
            stateText,
            getTaskState().actionText,
            getHist(),
            deflt);

    return p.getPage();
  }

  public FileData htmlWork(String title, boolean playSound, String deflt, 
          ProcessContext ctx, String umsg, String uumsg) throws Exception {
    // помимо форматирования тут нужно вывести имя пользователя и склад (если есть)
    HtmlPageWork p = new HtmlPageWork(
            title,
            getSound(playSound),
            title + " (" + getUserAndSkl(ctx) + ")",
            lastErr,
            lastMsg,
            stateText,
            getTaskState().actionText,
            getHist(),
            deflt,
            umsg,
            uumsg);

    return p.getPage();
  }
  public static boolean isScanPal(String scan) {
    if ((scan.length() == 11) && (scan.charAt(0) == 'P')
            && isAllDigits(scan.substring(1))) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanVbelnVa(String scan) {
    if ((scan.length() <= 7) && isAllDigits(scan)) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanEbeln(String scan) {
    if ((scan.length() == 10) && isAllDigits(scan) && scan.startsWith("45")) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanJvoz(String scan) {
    if ((scan.length() == 10) && (scan.charAt(0) == 'J')
            && isAllDigits(scan.substring(1))) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanCell(String scan) {
    int n = scan.length();
    if ((n >= 3) && (n <= 11) && (scan.charAt(0) == 'C')) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanProgopis(String scan) {
    int n = scan.length();
    if ((n == 13 && isAllDigits(scan)) || 
        ((n == 15) && isAllDigits(scan.substring(0, 7)) && isAllDigits(scan.substring(8)) && 
                ((scan.charAt(7) == 'D') || (scan.charAt(7) == 'C'))) ||
        ((n == 15) && isAllDigits(scan.substring(1)) && 
                (scan.charAt(0) == 'R')) ||
        (scan.length() > 10 && scan.charAt(7) == ':' && isAllDigits(scan.substring(1,7)))
            )
    {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanSgm(String scan) {
    if (scan.equals("0")) {
      return true;
    } else if ((scan.length() == 10) && (scan.charAt(0) == 'K')
            && isAllDigits(scan.substring(1))) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanZone(String scan) {
    if ((scan.length() > 2) && scan.substring(0, 2).equals("ZZ")) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean isScanTov(String scan) {
    int n = scan.length();
    if ((n == 7) && isAllDigits(scan)) {
      return true;
    }
    if ((n == 15) && isAllDigits(scan.substring(0, 7)) && isAllDigits(scan.substring(8))) {
      char c = scan.charAt(7);
      if ((c >= 'A') && (c <= 'D')) {
        return true;
      }
    }
    return false;
  }

  public static boolean isScanSur(String scan) {
    int n = scan.length();
    if ((n == 15) && isAllDigits(scan.substring(0, 4)) && isAllDigits(scan.substring(5))) {
      char c = scan.charAt(4);
      if (c == 'Z') {
        return true;
      }
    }
    return false;
  }

  public static boolean isScanMkSn(String scan) {
    int n = scan.length();
    char c = scan.charAt(0);
    if ((n == 14) && c == 'S' && isAllDigits(scan.substring(1))) {
      return true;
    }
    return false;
  }

  public static boolean isScanMkPb(String scan) {
    int n = scan.length();
    if ((n == 12) && scan.startsWith("YN") && isAllDigits(scan.substring(2))) {
      return true;
    }
    return false;
  }

  public static boolean isScanMk(String scan) {
    return isScanMkSn(scan) || isScanMkPb(scan);
  }

  public static boolean isScanTovMk(String scan) {
    return isScanTov(scan) || isScanMk(scan);
  }

  public static boolean isScanSsccBox(String scan) {
    int n = scan.length();
    char c = scan.charAt(0);
    if ((n == 18) && c == '0' && isAllDigits(scan)) {
      return true;
    }
    return false;
  }

  public static boolean isScanSsccPal(String scan) {
    int n = scan.length();
    char c = scan.charAt(0);
    if ((n == 18) && c == '1' && isAllDigits(scan)) {
      return true;
    }
    return false;
  }
  
  public static boolean isScanVbeln(String scan) {
    int n = scan.length();
    if ((n >= 8) && (n <= 10) && isAllDigits(scan)) {
      return true;
    }
    if (n != 11) {
      return false;
    }
    char c = scan.charAt(0);
    if (((c == 'A') || (c == 'B') || (c == 'C') || (c == 'D') || (c == 'F') || (c == 'H'))
            && (isAllDigits(scan.substring(1)))) {
      return true;
    }
    return false;
  }

  public static boolean isScanWP(String scan) {
    // проверка ШК рабочего места (WP00000123)
    int n = scan.length();
    if ((n == 10) && scan.substring(0, 2).equalsIgnoreCase("WP") && isAllDigits(scan.substring(2))) {
      return true;
    }
    return false;
  }

  public static boolean isScanWPsme(String scan) {
    // проверка ШК начала/окончания смены (WS0, WS1)
    int n = scan.length();
    if ((n == 3) && scan.substring(0, 2).equalsIgnoreCase("WS")
            && (scan.endsWith("0") || scan.endsWith("1"))) {
      return true;
    }
    return false;
  }

  public static boolean isScanWPdolgh(String scan) {
    // проверка ШК должности (WD00000123)
    int n = scan.length();
    if ((n == 10) && scan.substring(0, 2).equalsIgnoreCase("WD") && isAllDigits(scan.substring(2))) {
      return true;
    }
    return false;
  }

  public static boolean isScanWPsovm(String scan) {
    // проверка ШК совмещения (WM050)
    int n = scan.length();
    if ((n == 5) && scan.substring(0, 2).equalsIgnoreCase("WM") && isAllDigits(scan.substring(2))) {
      return true;
    }
    return false;
  }

  public static boolean isAllDigits(String str) {
    int n = str.length();
    char c;
    for (int i = 0; i < n; i++) {
      c = str.charAt(i);
      if ((c < '0') || (c > '9')) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAllDigitsComma(String str) {
    int n = str.length();
    char c;
    for (int i = 0; i < n; i++) {
      c = str.charAt(i);
      if ((c < '0') || (c > '9')) {
        if (c != ',') { 
          return false;
        }  
      }
    }
    return true;
  }  
  
  public static boolean isNumber(String str) {
    int n = str.length();
    char c;
    boolean havePoint = false;
    for (int i = 0; i < n; i++) {
      c = str.charAt(i);
      if ((c < '0') || (c > '9')) {
        if ((c == '.') && !havePoint) {
          havePoint = true;
        } else {
          return false;
        }
      }
    }
    return true;
  }

  public static String getScanCharg(String scan) {
    if (scan.length() <= 7) {
      return delZeros(scan);
    } else {
      return delZeros(scan.substring(0, 7));
    }
  }

  public static BigDecimal getScanQty(String scan) {
    if (scan.length() == 15) {
      BigDecimal ret = new BigDecimal(Long.parseLong(scan.substring(8)));
      ret = ret.setScale(3);
      switch (scan.charAt(7)) {
        case 'A':
          ret = ret.divide(new BigDecimal(1000));
          break;
        case 'B':
          ret = ret.divide(new BigDecimal(100));
          break;
        case 'C':
          ret = ret.divide(new BigDecimal(10));
          break;
      }
      return ret;
    } else {
      return BigDecimal.ONE;
    }
  }

  public static ScanChargQty getScanChargQty(String scan) {
    ScanChargQty ret = new ScanChargQty();
    if (!isScanTovMk(scan)) {
      ret.err = "Нетоварный ШК";
      return ret;
    }

    if (isScanTov(scan)) {
      ret.charg = getScanCharg(scan);
      ret.qty = getScanQty(scan);
    } else if (isScanMkSn(scan)) {
      ret.charg = delZeros(scan.substring(2, 9));
      ret.qty = new BigDecimal(1);
    } else if (isScanMkPb(scan)) {
        ZSHK_INFO f = new ZSHK_INFO();
        f.SHK = scan;
        f.execute();
        if (f.isErr) {
          ret.err = f.err;
          return ret;
        }
        ret.charg = delZeros(f.CHARG);
        ret.qty = f.QTY;
    } else {
      ret.err = "Нетоварный ШК";
      return ret;
    }

    return ret;
  }
  
  public static String getScanVbeln(String scan) {
    if (scan.length() == 11) {
      return delZeros(scan.substring(1));
    } else {
      return delZeros(scan);
    }
  }

  public static String getScanPal(String scan) {
    if (scan.length() == 11) {
      return scan.substring(1);
    } else {
      return scan;
    }
  }

  public static String getScanZone(String scan) {
    if ((scan.length() > 2) && scan.substring(0, 2).equals("ZZ")) {
      return scan.substring(2);
    } else {
      return scan;
    }
  }

  public static int getScanJvoz(String scan) {
    return Integer.parseInt(scan.substring(1));
  }

  public static int getScanSgm(String scan) {
    if (scan.equals("0")) {
      return 0;
    } else {
      return Integer.parseInt(scan.substring(1));
    }
  }

  public static int getScanWP(String scan) {
    return Integer.parseInt(scan.substring(2));
  }

  public static int getScanWPdolgh(String scan) {
    return Integer.parseInt(scan.substring(2));
  }

  public static boolean getScanWPsme(String scan) {
    return (scan.substring(2).equalsIgnoreCase("1"));
  }

  public static int getScanWPsovm(String scan) {
    return Integer.parseInt(scan.substring(2));
  }
  
  public void callSetErr(String err, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(err, lastErr)) {
      dr.setS(FieldType.ERR, err);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetMsg(String msg, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(msg, lastMsg)) {
      dr.setS(FieldType.MSG, msg);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetMsg2(String msg, String stateText, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(msg, lastMsg)) {
      dr.setS(FieldType.MSG, msg);
    }
    if (!strEq(stateText, this.stateText)) {
      dr.setS(FieldType.STATE_TEXT, stateText);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetStateText(String stateText, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(stateText, this.stateText)) {
      dr.setS(FieldType.STATE_TEXT, stateText);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetErrMsg(String err, String msg, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(err, lastErr)) {
      dr.setS(FieldType.ERR, err);
    }
    if (!strEq(msg, lastMsg)) {
      dr.setS(FieldType.MSG, msg);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callSetErrMsg2(String err, String msg, String stateText, ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (!strEq(err, lastErr)) {
      dr.setS(FieldType.ERR, err);
    }
    if (!strEq(msg, lastMsg)) {
      dr.setS(FieldType.MSG, msg);
    }
    if (!strEq(stateText, this.stateText)) {
      dr.setS(FieldType.STATE_TEXT, stateText);
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callClearErrMsg(ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (lastErr != null) {
      dr.setS(FieldType.ERR, "");
    }
    if (lastMsg != null) {
      dr.setS(FieldType.MSG, "");
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callClearErr(ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (lastErr != null) {
      dr.setS(FieldType.ERR, "");
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callClearStateText(ProcessContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    if (stateText != null) {
      dr.setS(FieldType.STATE_TEXT, "");
    }
    Track.saveProcessChange(dr, this, ctx);
  }

  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    // обработка записи трака (в т.ч. в реальном времени)
    // обязательно должна вызываться из handleData дочернего класса

    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.ERR)) {
          lastErr = dr.getValStr(FieldType.ERR);
        }
        if (dr.haveVal(FieldType.MSG)) {
          lastMsg = dr.getValStr(FieldType.MSG);
        }
        if (dr.haveVal(FieldType.STATE_TEXT)) {
          stateText = dr.getValStr(FieldType.STATE_TEXT);
        }
        if (dr.haveVal(FieldType.TASK_STATE)) {
          taskState = taskStates[(Integer) dr.getVal(FieldType.TASK_STATE)];
        }
        if (dr.haveVal(FieldType.ADD_HIST)) {
          hist.add(dr.getValStr(FieldType.ADD_HIST));
        }
        if (dr.haveVal(FieldType.DEL_HIST)) {
          hist.clear();
        }
        break;
    }
  }

  public static boolean strEq(String s1, String s2) {
    // сравнение строк
    // считаем null == ""
    if (((s1 == null) || s1.isEmpty()) && ((s2 == null) || s2.isEmpty())) {
      return true;
    } else if ((s1 == null) || s1.isEmpty() || (s2 == null) || s2.isEmpty()) {
      return false;
    } else {
      return s1.equals(s2);
    }
  }

  public static String delZeros(String txt) {
    // удаление ведущих нулей из строки

    if ((txt == null) || txt.isEmpty()) {
      return txt;
    }

    int n = txt.length();
    int n1 = n;
    for (int i = 0; i < n; i++) {
      if (txt.charAt(i) != '0') {
        n1 = i;
        break;
      }
    }
    if (n1 == n) {
      n1--;
    }

    if (n1 == 0) {
      return txt;
    } else {
      return txt.substring(n1);
    }
  }

  public static String delDecZeros(String num) {
    // удаление десятичных нулей (и точки) из строкового представления числа

    if ((num == null) || num.isEmpty() || (num.indexOf('.') == -1)) {
      return num;
    }

    int n = num.length();
    int n1 = n - 1;
    // отсекаем пробелы
    for (int i = n1; i > 0; i--) {
      if (num.charAt(i) != ' ') {
        n1 = i;
        break;
      }
    }
    // отсекаем нули
    for (int i = n1; i > 0; i--) {
      if (num.charAt(i) != '0') {
        n1 = i;
        break;
      }
    }
    // отсекаем точки
    for (int i = n1; i >= 0; i--) {
      if (num.charAt(i) != '.') {
        n1 = i;
        break;
      }
    }
    n1++;

    if (n1 == n) {
      return num;
    } else {
      return num.substring(0, n1);
    }
  }

  public static String fillZeros(String txt, int len) {
    // добавление ведущих нулей до len символов

    if ((txt == null) || txt.isEmpty()) {
      return txt;
    }

    int n = txt.length();
    if (n >= len) {
      return txt;
    }

    char[] c1 = txt.toCharArray();
    char[] c2 = new char[len];
    for (int i = 0; i < (len - n); i++) {
      c2[i] = '0';
    }
    for (int i = 0; i < (n); i++) {
      c2[i + len - n] = c1[i];
    }

    return new String(c2, 0, c2.length);
  }

  public static FileData htmlMatFoto(String matnr) throws Exception {
    // формирование страницы с фото материала

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

}
