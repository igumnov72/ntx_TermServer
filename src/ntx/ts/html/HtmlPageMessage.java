package ntx.ts.html;

import ntx.ts.http.FileData;
import ntx.ts.srv.TSparams;

/**
 * Окно с сообщением и кнопкой продолжения
 */
public class HtmlPageMessage extends HtmlPage {

  public String err;
  public String msg;
  public String hiddenText1;
  public String hiddenText2;

  public HtmlPageMessage(
          String err,
          String msg,
          String hiddenText1,
          String hiddenText2) {
    title = (err == null ? "Сообщение" : "Ошибка");
    sound = (err == null ? "ask.wav" : "err.wav");
    this.err = err;
    this.msg = msg;
    this.hiddenText1 = hiddenText1;
    this.hiddenText2 = hiddenText2;
    fontSize = TSparams.fontSize;
    scrollToTop = true;
  }

  @Override
  public FileData getPage() throws Exception {
    createPage();
    return super.getPage();
  }

  private void createPage() throws Exception {
    if (msg != null) {
      addBlock(msg, 0, "blue");
    }
    if (err != null) {
      addBlock(err, 0, "red");
    }

    addNewLine();

    addFormStart("work.html", "f");
    addFormButtonSubmitGo("Продолжить");
    if (hiddenText1 != null) {
      addFormFieldHidden("htext1", 1, hiddenText1);
    }
    if (hiddenText2 != null) {
      addFormFieldHidden("htext2", 1, hiddenText2);
    }
    addFormEnd();
  }
}
