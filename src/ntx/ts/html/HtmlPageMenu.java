package ntx.ts.html;

import ntx.ts.http.FileData;
import ntx.ts.srv.TSparams;

public class HtmlPageMenu extends HtmlPage {

  public String menuHeader;
  public String menuDefinition;
  public String menuCurValue;
  public String err;
  public String msg;

  public HtmlPageMenu(
          String a_title,
          String header,
          String definition,
          String curValue,
          String err,
          String msg) {
    creHtmlPageMenu(a_title, header, definition, curValue, err, msg, false);
  }

  public HtmlPageMenu(
          String a_title,
          String header,
          String definition,
          String curValue,
          String err,
          String msg,
          boolean playSound) {
    creHtmlPageMenu(a_title, header, definition, curValue, err, msg, playSound);
  }

  private void creHtmlPageMenu(
          String a_title,
          String header,
          String definition,
          String curValue,
          String err,
          String msg,
          boolean playSound) {
    title = a_title;
    sound = playSound ? (err != null ? "err.wav" : "ok.wav") : "ask.wav";
    menuHeader = header;
    menuDefinition = definition;
    menuCurValue = curValue;
    this.err = err;
    this.msg = msg;
    fontSize = TSparams.fontSize;
  }

  @Override
  public FileData getPage() throws Exception {
    createPage();
    return super.getPage();
  }

  private void createPage() throws Exception {
    addFormStart("work.html", "m");

    if (msg != null) {
      addBlock("<b>" + msg + "</b>", 0, "blue");
    }
    if (err != null) {
      addBlock("<b>" + err + "</b>", 0, "red");
    }
    if (menuHeader != null) {
      addBlock("<b>" + menuHeader + "</b>");
    }

    addFormRadio("menu", menuDefinition,
            menuCurValue == null ? "" : menuCurValue, true);
    addFormButtonSubmit("Выбор", null, false);
    addFormEnd();
  }
}
