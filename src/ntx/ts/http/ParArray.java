package ntx.ts.http;

public class ParArray {

  private String[] nam;
  private String[] val;
  private int len;

  public ParArray(String data, String delim) throws Exception {
    String[] sa = data.split(delim);
    len = sa.length;
    nam = new String[len];
    val = new String[len];
    for (int i = 0; i < len; i++) {
      handlePair(i, sa[i]);
    }
  }

  private void handlePair(int no, String s) throws Exception {
    int n = s.indexOf('=');
    if (n <= 0) {
      nam[no] = "";
      val[no] = decodeUTF8(s);
    } else {
      if (s.startsWith(" ")) {
        nam[no] = s.substring(1, n);
      } else {
        nam[no] = s.substring(0, n);
      }
      val[no] = decodeUTF8(s.substring(n + 1));
    }
  }

  public String getNam(int i) {
    if ((i < 0) || (i >= len)) {
      return null;
    }
    return nam[i];
  }

  public String getVal(int i) {
    if ((i < 0) || (i >= len)) {
      return null;
    }
    return val[i];
  }

  public String getPar(String parNam) {
    for (int i = 0; i < len; i++) {
      if (nam[i].equalsIgnoreCase(parNam)) {
        return val[i];
      }
    }
    return "";
  }

  public String getParNull(String parNam) {
    for (int i = 0; i < len; i++) {
      if (nam[i].equalsIgnoreCase(parNam)) {
        return val[i];
      }
    }
    return null;
  }

  public int length() {
    return len;
  }

  public static String decode(String s) {
    if ((s == null) || (s.isEmpty())) {
      return "";
    }
    String[] sa = s.replace('+', ' ').split("%");
    int n = sa.length;
    StringBuilder sb = new StringBuilder(sa[0]);

    // берем закодированные символы
    byte[] ba = new byte[n - 1];
    for (int i = 1; i < n; i++) {
      ba[i - 1] = (byte) Integer.parseInt(sa[i].substring(0, 2), 16);
    }
    // преобразуем в строку и записываем результат в буфер
    String ss = new String(ba);
    for (int i = 1; i < n; i++) {
      sb.append(ss.charAt(i - 1));
      sb.append(sa[i].substring(2));
    }
    return sb.toString();
  }

  public static String decodeUTF8(String s) throws Exception {
    if ((s == null) || (s.isEmpty())) {
      return "";
    }
    String[] sa = s.replace('+', ' ').split("%");
    int n = sa.length;
    StringBuilder sb = new StringBuilder(sa[0]);

    // берем закодированные символы
    byte[] ba = new byte[n]; // ba[0] не используется
    for (int i = 1; i < n; i++) {
      ba[i] = (byte) Integer.parseInt(sa[i].substring(0, 2), 16);
    }

    // преобразуем в строку и записываем результат в буфер
    int strStart = 1;
    for (int i = 1; i < n; i++) {
      String t = sa[i].substring(2);
      if (!t.isEmpty()) {
        sb.append(new String(ba, strStart, i - strStart + 1, "UTF-8"));
        strStart = i + 1;
        sb.append(t);
      }
    }
    if (strStart < n) {
      sb.append(new String(ba, strStart, n - strStart, "UTF-8"));
    }

    return sb.toString();
  }
}
