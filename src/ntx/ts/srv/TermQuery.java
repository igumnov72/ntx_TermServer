package ntx.ts.srv;

import ntx.ts.http.ParArray;
import java.util.Date;

public class TermQuery {

  public boolean doingGet;
  public boolean doingPost;
  public String fname;    // имя файла
  public String[] sf;     // массив частей имени файла
  public ParArray params; // данные формы
  public ParArray cookies;
  public long terminal;
  public boolean setTerminal = false; // принудительная передача в браузер id терминала
  public String fromAddr;
  public int querySource; // 0 - терминалы склада, 1 - планшет в цехе, 2 - сканер в цехе
  public boolean isUDP;

  public void setNewTerminal() {
    if (terminal == 0) {
      terminal = (new Date()).getTime();
      setTerminal = true;
    }
  }

  public void print() {
    System.out.println((doingGet ? "GET" : "") + (doingPost ? "POST" : "")
            + (fname == null ? "" : " " + fname));
    System.out.println("terminal " + terminal
            + " (" + DateTimeLogger.df.format(new Date(terminal)) + ") "
            + (fromAddr == null ? "" : fromAddr));
    if (params != null) {
      int n = params.length();
      for (int i = 0; i < n; i++) {
        System.out.println(params.getNam(i) + "=" + params.getVal(i));
      }
    }
  }

  public String getPar(String parNam) {
    if (params == null) {
      return "";
    } else {
      return params.getPar(parNam);
    }
  }
}
