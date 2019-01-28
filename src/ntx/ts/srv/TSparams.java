package ntx.ts.srv;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;

public class TSparams {

  public static Properties props = new Properties();
  public static String fileName;
  public static String srvName = "test";
  public static int timeout;
  public static int workers;
  public static int port = 0; // номер порта для терминалов на складе
  public static int port_pl = 0; // номер порта для планшетов в пр-ве
  public static int port_scan_udp = 0; // номер порта для сканеров в пр-ве
  public static int fontSize;
  public static int fontSize2;
  public static int fontSize3;
  public static int logDocLevel = 3;
  public static String lgorts = "";
  public static String lgorts2 = ""; // то же что lgorts, только с запятыми в начале и в конце
  public static boolean cnfPlc = false; // требовать подтверждение при размещении
  public static Date comprDate1 = null; // первая дата автоматического сжатия файла данных ДД.ММ.ГГГГ
  public static int comprWeeks = 0; // периодичность автом сжатия в неделях
  public static int comprDays = 0; // инф за сколько дней оставлять при сжатии
  public static String printers = ""; // список доступных принтеров
  public static DateFormat parseDate = new SimpleDateFormat("dd.MM.yyyy");
  public static String lgortsNoMQ = ""; // склады с запретом ручного ввода кол-ва при комплектации
  public static String lgortsNoMQ2 = ""; // то же что lgortsNoMQ, только с запятыми в начале и в конце

  public static void load(String fname) throws Exception { // чтение параметров
    fileName = fname;

    File f = new File(fname);
    if (f.exists()) {
      InputStream is = new BufferedInputStream(new FileInputStream(f));
      props.load(is);
      is.close();
      String r = props.getProperty("timeout");
      if (r != null) {
        timeout = Integer.parseInt(r);
      }
      r = props.getProperty("workers");
      if (r != null) {
        workers = Integer.parseInt(r);
      }
      r = props.getProperty("port");
      if (r != null) {
        port = Integer.parseInt(r);
      }
      r = props.getProperty("port_pl");
      if (r != null) {
        port_pl = Integer.parseInt(r);
      }
      r = props.getProperty("port_scan_udp");
      if (r != null) {
        port_scan_udp = Integer.parseInt(r);
      }
      r = props.getProperty("fontSize");
      if (r != null) {
        fontSize = Integer.parseInt(r);
      }
      r = props.getProperty("fontSize2");
      if (r != null) {
        fontSize2 = Integer.parseInt(r);
      }
      r = props.getProperty("fontSize3");
      if (r != null) {
        fontSize3 = Integer.parseInt(r);
      }
      r = props.getProperty("lgorts");
      if (r != null) {
        lgorts = r;
        lgorts2 = "," + r + ",";
      }
      r = props.getProperty("lgortsNoMQ");
      if (r != null) {
        lgortsNoMQ = r;
        lgortsNoMQ2 = "," + r + ",";
      }
      r = props.getProperty("logDocLevel");
      if (r != null) {
        logDocLevel = Integer.parseInt(r);
      }
      r = props.getProperty("cnfPlc");
      if (r != null) {
        if (r.equals("1")) {
          cnfPlc = true;
        }
      }
      r = props.getProperty("srvName");
      if (r != null) {
        srvName = r;
      }
      r = props.getProperty("compr_date1");
      if (r != null) {
        comprDate1 = parseDate.parse(r);
      }
      r = props.getProperty("compr_weeks");
      if (r != null) {
        comprWeeks = Integer.parseInt(r);
      }
      r = props.getProperty("compr_days");
      if (r != null) {
        comprDays = Integer.parseInt(r);
      }
      r = props.getProperty("printers");
      if (r != null) {
        printers = r;
      }
    }

    /* if no properties were specified, choose defaults */
    if (timeout <= 1000) {
      timeout = 5000;
    }
    if (workers < 1) {
      workers = 3;
    }
    if (fontSize < 6) {
      fontSize = 11;
    }
    if (fontSize2 < 5) {
      fontSize2 = (fontSize * 5) / 6;
    }
    if (fontSize3 < 5) {
      fontSize3 = (fontSize * 3) / 4;
    }
    if ((logDocLevel < 0) || (logDocLevel > 3)) {
      logDocLevel = 3;
    }

    print();
  }

  private static void print() {
    System.out.println("Loaded parameters (file " + fileName + "):");

    String key;
    String val;
    for (Entry<Object, Object> i : props.entrySet()) {
      key = (String) i.getKey();
      val = (String) i.getValue();
      if (!key.equalsIgnoreCase("jco.client.passwd") && !key.equalsIgnoreCase("reserv.jco.client.passwd")) {
        System.out.println(key + "=" + val);
      }
    }
    System.out.println("Required params:   ---------------");
    System.out.println("srvName=" + srvName);
    System.out.println("printers=" + printers);
    System.out.println("timeout=" + timeout);
    System.out.println("workers=" + workers);
    System.out.println("port=" + port);
    System.out.println("port_pl=" + port_pl);
    System.out.println("port_scan_udp=" + port_scan_udp);
    System.out.println("lgorts=" + lgorts);
    System.out.println("lgortsNoMQ=" + lgortsNoMQ);
    System.out.println("compr_date1=" + (comprDate1 == null ? "_undefined_" : parseDate.format(comprDate1)));
    System.out.println("compr_weeks=" + comprWeeks);
    System.out.println("compr_days=" + comprDays);
    System.out.println("fontSize=" + fontSize);
    System.out.println("fontSize2=" + fontSize2);
    System.out.println("fontSize3=" + fontSize3);
    System.out.println("logDocLevel=" + logDocLevel);
    if (cnfPlc) {
      System.out.println("cnfPlc=1");
    } else {
      System.out.println("cnfPlc=0");
    }
  }
}
