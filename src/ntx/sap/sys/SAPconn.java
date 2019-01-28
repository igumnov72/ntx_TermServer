package ntx.sap.sys;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import ntx.ts.srv.TSparams;

public class SAPconn implements DestinationDataProvider {

  private static JCoDestination destination;
  private static JCoDestination destination2 = null; // резервное подключение
  private static volatile int currDest = 1; // 1 или 2
  private static boolean lastConnErr = false; // признак наличия ошибки подключения (нужно ли его проверить)
  private static volatile long switchTime = 0;

  public static int getCurrDest() {
    return currDest;
  }

  private SAPconn() {
  }

  public Properties getDestinationProperties(String destinationName) {
    if (!destinationName.equalsIgnoreCase("work") && !destinationName.equalsIgnoreCase("reserv")) {
      return null;
    }
    Properties ret = new Properties();
    String key;

    if (destinationName.equalsIgnoreCase("work")) {
      for (Entry<Object, Object> e : TSparams.props.entrySet()) {
        key = (String) e.getKey();
        if (key.startsWith("jco.")) {
          if (key.equalsIgnoreCase("jco.client.passwd")) {
            ret.setProperty(key, getPaswd((String) e.getValue()));
          } else {
            ret.setProperty(key, (String) e.getValue());
          }
        }
      }
    } else {
      for (Entry<Object, Object> e : TSparams.props.entrySet()) {
        key = (String) e.getKey();
        if (key.startsWith("reserv.jco.")) {
          key = key.substring(7);
          if (key.equalsIgnoreCase("jco.client.passwd")) {
            ret.setProperty(key, getPaswd((String) e.getValue()));
          } else {
            ret.setProperty(key, (String) e.getValue());
          }
        }
      }
    }

    return ret;
  }

  private static String getPaswd(String passwd1) {
    // расшифровка пароля
    int n = passwd1.length();
    int n1 = n / 2 + n % 2;
    int n2 = n / 2;
    char[] c1 = new char[n1];
    char[] c2 = new char[n2];
    int i1 = 0, i2 = 0;

    for (int i = 0; i < n; i++) {
      if ((i % 2) == 0) {
        c1[i1] = passwd1.charAt(i);
        i1++;
      } else {
        c2[i2] = passwd1.charAt(i);
        i2++;
      }
    }

    return (new String(c1)) + (new String(c2));
  }

  public boolean supportsEvents() {
    return false;
  }

  public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
  }

  private static boolean checkReserv() {
    for (Entry<Object, Object> e : TSparams.props.entrySet()) {
      if (((String) e.getKey()).startsWith("reserv.jco.")) {
        return true;
      }
    }
    return false;
  }

  public static boolean haveReserv() {
    return destination2 != null;
  }

  public static void init() throws JCoException {
    SAPconn sc = new SAPconn();
    Environment.registerDestinationDataProvider(sc);
    destination = JCoDestinationManager.getDestination("work");
    if (checkReserv()) {
      destination2 = JCoDestinationManager.getDestination("reserv");
    } else {
      destination2 = null;
    }
  }

  public static boolean isConnErr(String errFull) {
    return errFull.startsWith("com.sap.conn.jco.JCoException: (102) JCO_ERROR_COMMUNICATION:");
  }

  public static boolean isLoginErr(String errFull) {
    return errFull.startsWith("com.sap.conn.jco.JCoException: (103) JCO_ERROR_LOGON_FAILURE:");
  }

  public static ErrDescr describeErr(String errFull) {
    ErrDescr ret = new ErrDescr();
    String s = errFull;

    if (isConnErr(s)) {
      ret.err = "Нет связи с САПом";
      ret.isShort = true;
      return ret;
    } else if (isLoginErr(s)) {
      ret.err = "Ошибка логина в САП";
      ret.isShort = true;
      return ret;
    } else if (s.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
      int n1 = 63;
      int n2 = s.indexOf(" (raised by system ");
      if (n2 == -1) {
        n2 = s.length();
      }
      while ((s.charAt(n1) == ' ') && (n1 < n2)) {
        n1 = n1 + 1;
      }
      while ((s.charAt(n2 - 1) == ' ') && (n1 < n2)) {
        n2 = n2 - 1;
      }
      s = s.substring(n1, n2);
      s = "САП вернул ошибку: " + s;
      ret.isShort = true;
    }

    char[] cc = new char[s.length()];
    s.getChars(0, s.length(), cc, 0);
    char c;
    char c11 = 'Ð', c12 = 'ï', c13 = 'а';
    char c21 = '°', c22 = 'Ï', c23 = 'А';
    char c31 = 'ñ', c32 = 'ё';
    char c41 = '¡', c42 = 'Ё';
    for (int i = 0; i < cc.length; i++) {
      c = cc[i];
      if (c == c31) {
        cc[i] = c32;
      } else if (c == c41) {
        cc[i] = c42;
      } else if ((c >= c11) && (c <= c12)) {
        cc[i] = (char) (c - c11 + c13);
      } else if ((c >= c21) && (c <= c22)) {
        cc[i] = (char) (c - c21 + c23);
      }
    }

    s = new String(cc);
    ret.err = s;
    return ret;
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

  private static JCoDestination currentDestination() {
    if (currDest == 1) {
      return destination;
    } else {
      return destination2;
    }
  }

  private static JCoDestination otherDestination() {
    if (currDest == 1) {
      return destination2;
    } else {
      return destination;
    }
  }

  public static JCoFunction getFunction(String funcName) throws JCoException {
    // описание ФМ при ошибке подключения к САПу пробуем получить и по резервной линии,
    // но переключение не делаем; логика переключения линии - только при вызове ФМ

    JCoFunction ret = null;
    JCoException err = null;

    try {
      ret = currentDestination().getRepository().getFunction(funcName);
    } catch (JCoException e) {
      err = e;
      if (!haveReserv() || !isConnErr(e.toString())) {
        throw err;
      }
    }

    if (err != null) {
      // пробуем получить описание по другой линии
      try {
        ret = otherDestination().getRepository().getFunction(funcName);
      } catch (JCoException e) {
        throw err;
      }
    }

    return ret;
  }

  private static JCoException testConn(int destNo) {
    JCoDestination dest = (destNo == 1 ? destination : destination2);
    try {
      JCoFunction function = dest.getRepository().getFunction("Z_TS_TEST_CONN");
      if (function == null) {
        throw new RuntimeException("Z_TS_TEST_CONN not found in SAP.");
      }
      function.execute(dest);
    } catch (JCoException e) {
      System.err.println("проверка линии " + destNo + ": нет связи");
      System.err.flush();
      return e;
    }

    System.err.println("проверка линии " + destNo + ": OK");
    System.err.flush();
    return null;
  }

  public static JCoException executeFunction(JCoFunction function) {
    ConnData d = getConnData();
    if (d.e != null) {
      return d.e;
    }

    try {
      function.execute(d.destination);
    } catch (JCoException e) {
      boolean connErr = isConnErr(e.toString());
      if (connErr) {
        lastConnErr = true;
      }
      if (!haveReserv() || !connErr) {
        return e;
      }
      d.e = e;
      // ошибка связи и есть резервный канал
    }

    if (d.e != null) {
      // ошибка связи с САПом, пробуем другой канал
      d = trySwitchConn(d);
      if (d.e != null) {
        return d.e;
      }

      try {
        function.execute(d.destination);
      } catch (JCoException e) {
        if (isConnErr(e.toString())) {
          lastConnErr = true;
        }
        return e;
      }
    }

    return null;
  }

  private static synchronized ConnData getConnData() {
    // проверяем отсутствие ошибки подключения
    // по истечении времени переключаемся на основной канал

    ConnData ret = new ConnData();
    ret.destNo = currDest;
    ret.destination = (ret.destNo == 1 ? destination : destination2);

    if (lastConnErr) {
      ret.e = testConn(ret.destNo);
      if (ret.e != null) {
        // ошибка подключения осталась
        if (!haveReserv()) {
          // резервной линии нет
          return ret;
        }

        // пробуем переключиться на другую линию
        return trySwitchConn(ret);
      }
      lastConnErr = false;
    }

    if ((ret.destNo == 2) && ((new Date()).getTime() > switchTime)) {
      ConnData ret2 = trySwitchConn(ret);
      if (ret2.e == null) {
        return ret2;
      }
      setSwitchTime();
    }

    return ret;
  }

  private static synchronized ConnData trySwitchConn(ConnData from) {
    ConnData ret = new ConnData();
    ret.destNo = (from.destNo == 1 ? 2 : 1);
    ret.destination = (ret.destNo == 1 ? destination : destination2);

    ret.e = testConn(ret.destNo);
    if (ret.e == null) {
      // переключаемся
      currDest = ret.destNo;
      lastConnErr = false;
      if (ret.destNo == 2) {
        setSwitchTime();
      }
      System.err.println("ПЕРЕКЛЮЧЕНИЕ НА ЛИНИЮ " + ret.destNo);
      System.err.flush();
    }

    return ret;
  }

  private static void setSwitchTime() {
    switchTime = (new Date()).getTime() + 1800000l; // 30 мин
//    switchTime = (new Date()).getTime() + 30000l; // 30 сек
  }
}

class ConnData {
  // данные для подключения (выполнения ФМ)

  public JCoDestination destination = null;
  public int destNo = 0;
  JCoException e = null;
}
