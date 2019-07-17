package ntx.ts.html;

import java.util.Date;
import ntx.ts.http.FileData;
import ntx.ts.sysproc.ProcessContext;

public class HtmlPage {

  public String title = null;
  public String sound = null; // ok.wav, err.wav, done.wav, ask.wav
//  public String setFocus = null; // <имя формы>.<имя элемента>
//  public boolean selectFocus = false; // выделить содержимое поля в фокусе
  public StringBuilder body = new StringBuilder(2000);
//  public String javaScript = null;
  public boolean scrollToTop = false; // прокрутка страницы в начало после загрузки
  public String savedSetFocus = null; // сюда запоминается setFocus для radioButton
  private String lastFormName;
  public int fontSize = 0; // размер шрифта
  public int fontSize2 = 0; // размер шрифта (чуть мельче)
  public int fontSize3 = 0; // размер шрифта (еще мельче)
  private long formTime = 0; // метка времени в скрытом поле формы
  private static long lastFormTime = 0;
  protected boolean refreshByEnter = false;

  private static synchronized long nextFormTime() {
    long dt = (new Date()).getTime();
    if (dt <= lastFormTime) {
      dt = lastFormTime + 1;
    }
    lastFormTime = dt;
    return dt;
  }

  public long getFormTime() {
    return formTime;
  }

  public void addText(String txt) {
    body.append(txt);
  }

  public void addBlock(String txt) {
    body.append("<div>");
    body.append(txt);
    body.append("</div>\r\n");
  }

  public void addBlock(String txt, int fontSz, String color) {
    addBlockStart(fontSz, color);
    body.append(txt);
    body.append("</div>\r\n");
  }

  public void addBlockStart(int fontSz, String color) {
    if ((fontSz > 0) || ((color != null) && !color.isEmpty())) {
      body.append("<div style=\"");
      if (fontSz > 0) {
        body.append("font-size:");
        body.append(fontSz);
        body.append("pt");
      }
      if ((color != null) && !color.isEmpty()) {
        if (fontSz > 0) {
          body.append(";");
        }
        body.append("color:");
        body.append(color);
      }
      body.append("\">");
    } else {
      body.append("<div>");
    }
  }

  public void addBlockFin() {
    body.append("</div>\r\n");
  }

  public void addText(int i) {
    body.append(i);
  }

  public void addText(long l) {
    body.append(l);
  }

  public void addLine(String txt) {
    body.append(txt);
    body.append(" <br>\r\n");
  }

  public void addNewLine() {
    body.append(" <br> \r\n");
  }

  public void addRef(String ref, String txt) {
    body.append("<a href='");
    body.append(ref);
    body.append("'>");
    body.append(txt);
    body.append("</a> \r\n");
  }

  public void addFormStart(String action, String name) throws Exception {
    body.append("<form action=\"/");
    body.append(action);
    body.append("\" method=post name=\"");
    body.append(name);
    body.append("\" enctype=\"application/x-www-form-urlencoded\"> \r\n");
    formTime = nextFormTime();
    addFormFieldHidden("formtime", 1, "" + formTime);

    lastFormName = name;
  }

  private void addSetFocus(String a_setFocus, boolean a_selectFocus) {
    // setFocus: <имя формы>.<имя элемента>
    // selectFocus: выделить содержимое поля в фокусе
    String setFocus = a_setFocus;
    if (savedSetFocus != null) {
      setFocus = savedSetFocus;
    }
    if (setFocus != null) {
      body.append("<script type=\"text/javascript\">\r\n");
      body.append("try{document.");
      body.append(setFocus);
      if (a_selectFocus) {
        body.append(".focus(); document.");
        body.append(setFocus);
        body.append(".select()}catch(e){};\r\n");
      } else {
        body.append(".focus()}catch(e){};\r\n");
      }
      body.append("</script>\r\n");
    }
  }

  public void addFormButtonSubmit(String val, String setFocus, boolean selectFocus) {
    // setFocus: <имя формы>.<имя элемента>
    // selectFocus: выделить содержимое поля в фокусе
    body.append("<input type=submit value=\"");
    body.append(val);
    if (fontSize > 0) {
      body.append("\" style=\"font-size:");
      body.append(fontSize - 1);
      body.append("pt\">\r\n");
    } else {
      body.append("\">\r\n");
    }
    addSetFocus(setFocus, selectFocus);
  }

  public void addFormButtonSubmitGo(String val) {
    // setFocus: <имя формы>.<имя элемента>
    // selectFocus: выделить содержимое поля в фокусе
    body.append("<div align=center> <input type=submit value=\"");
    body.append(val);
    if (fontSize > 0) {
      body.append("\" name=go style=\"font-size:");
      body.append(fontSize);
      body.append("pt\"> </div>\r\n");
    } else {
      body.append("\" name=go> </div>\r\n");
    }
    addSetFocus("f.go", false);
  }

  public void addFormButtonReset(String val) {
    body.append("<input type=reset value=\"");
    body.append(val);
    if (fontSize > 0) {
      body.append("\" style=\"font-size:");
      body.append(fontSize);
      body.append("pt\">\r\n");
    } else {
      body.append("\"> \r\n");
    }
  }

  public void addFormEnd() {
    body.append("</form> \r\n");
  }

  public void addFormField(String name, int size) {
    body.append("<input type=text name=");
    body.append(name);
    body.append(" size=");
    body.append(size);
    if (fontSize > 0) {
      body.append(" style=\"font-size:");
      body.append(fontSize);
      body.append("pt\">\r\n");
    } else {
      body.append("> \r\n");
    }
  }

  public void addFormFieldTxt(String name, int size, String txt) throws Exception {
    addFormFieldTxt(name, size, txt, false);
  }

  public void addFormFieldTxt(String name, int size, String txt, boolean readOnly) throws Exception {
    if (txt.contains("\"")) {
      throw new Exception("HtmlPage.addFormFieldTxt: text cannot contain \"");
    } else {
      body.append("<input type=text name=");
      body.append(name);
      body.append(" size=");
      body.append(size);
      body.append(" value=\"");
      body.append(txt);
      if (fontSize > 0) {
        body.append("\" style=\"font-size:");
        body.append(fontSize);
        body.append("pt\"");
      } else {
        body.append("\"");
      }
      if (readOnly) {
        body.append(" readonly>\r\n");
      } else {
        body.append(">\r\n");
      }
    }
  }

  public void addFormFieldHidden(String name, int size, String txt) throws Exception {
    if (txt.contains("\"")) {
      throw new Exception("HtmlPage.addFormFieldTxt: text cannot contain \"");
    } else {
      body.append("<input type=hidden name=");
      body.append(name);
      body.append(" size=");
      body.append(size);
      body.append(" value=\"");
      body.append(txt);
      if (fontSize > 0) {
        body.append("\" style=\"font-size:");
        body.append(fontSize);
        body.append("pt\">\r\n");
      } else {
        body.append("\"> \r\n");
      }
    }
  }

  public void addFormFieldPassword(String name, int size) {
    body.append("<input type=password name=");
    body.append(name);
    body.append(" size=");
    body.append(size);
    if (fontSize > 0) {
      body.append(" style=\"font-size:");
      body.append(fontSize);
      body.append("pt\">\r\n");
    } else {
      body.append("> \r\n");
    }
  }

  public void addFormCheckbox(String name, boolean checked) {
    body.append("<input type=checkbox name=");
    body.append(name);
    if (checked) {
      body.append(" checked");
    }
    if (fontSize > 0) {
      body.append(" style=\"font-size:");
      body.append(fontSize);
      body.append("pt\">\r\n");
    } else {
      body.append(">\r\n");
    }
  }

  public void addFormRadio(String name, String definition,
          String currentValue, boolean doSetFocus) {

    // definition - строка описания в формате:
    // <value1>:<text1>;<value2>:<text2>; ...
    String cv = currentValue;

    String[] sa = definition.split(";");
    int n;
    String val;
    String cv0 = null;
    int nn = -1;
    int saLen = 0;

    for (int i = 0; i < sa.length; i++) {
      if (!sa[i].isEmpty()) {
        saLen++;
      }
    }

    // проверяем наличие выбранной позиции
    for (int i = 0; i < sa.length; i++) {
      if (!sa[i].isEmpty()) {
        n = sa[i].indexOf(":");
        if (n >= 0) {
          val = sa[i].substring(0, n);
        } else {
          val = sa[i];
        }
        if (i == 0) {
          cv0 = val;
        }
        if (val.equalsIgnoreCase(currentValue)) {
          nn = i;
          break;
        }
      }
    }

    if (nn == -1) {
      cv = cv0;
      nn = 0;
    } else {
      cv = currentValue;
    }

    if (doSetFocus) {
      savedSetFocus = lastFormName + "." + name;
      if (saLen > 1) {
        savedSetFocus = savedSetFocus + "[" + nn + "]";
      }
    }

    String txt;
    for (int i = 0; i < sa.length; i++) {
      if (!sa[i].isEmpty()) {
        n = sa[i].indexOf(":");
        if (n >= 0) {
          val = sa[i].substring(0, n);
          txt = sa[i].substring(n + 1);
        } else {
          val = sa[i];
          txt = "";
        }
        body.append("<input type=radio name=");
        body.append(name);
        body.append(" value=");
        body.append(val);
        if (val.equalsIgnoreCase(cv)) {
          body.append(" checked");
        }
        if (fontSize > 0) {
          body.append(" style=\"font-size:");
          body.append(fontSize);
          body.append("pt\"");
        }
        body.append(">");
        body.append(txt);
        body.append(" <br> \r\n");
      }
    }
  }

  public FileData getPage() throws Exception {
    StringBuilder sb = new StringBuilder(2000);

    sb.append("<!DOCTYPE html>\r\n");
    sb.append("<HTML>\r\n<head><title> ");
    if (title == null) {
      sb.append("???");
    } else {
      sb.append(title);
    }
    sb.append(" </title>\r\n");

    if (scrollToTop) {
      sb.append("<script type=\"text/javascript\">\r\n");
      sb.append("window.onload=function(){try{window.location.replace(\"#top\");}catch(e){};}");
      sb.append("\r\n</script>\r\n");
    }

    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/HTML; charset=utf-8\">\r\n");
    sb.append("<meta http-equiv=\"Cache-Control\" content=\"no-cache\" />\r\n");
    if (sound != null) {
      sb.append("<bgsound src=\"");
      sb.append(sound);
      sb.append("\">\r\n");
    }
    sb.append("</head>\r\n<body link=blue vlink=blue ");
    if (fontSize > 0) {
      sb.append("\r\nstyle=\"font-size:");
      sb.append(fontSize);
      sb.append("pt;font-family:sans-serif\"");
    }
    sb.append(">\r\n");

    String termMsg = ProcessContext.getTermMsg();
    if (!termMsg.isEmpty()) {
      sb.append("<div style=\"background-color:orange\">\r\n<br>\r\n<b>\r\n");
      sb.append(termMsg);
      sb.append("</b>\r\n<br>\r\n");
      if (refreshByEnter) {
        sb.append("(нажмите Enter для обновления)\r\n<br>\r\n");
      }
      sb.append("<br>\r\n</div>\r\n<br>\r\n");
    }

    sb.append(body);

    sb.append("\r\n</body>\r\n</HTML>\r\n");

    FileData ret = new FileData(sb.toString());
    ret.formTime = formTime;
    return ret;
  }
}
