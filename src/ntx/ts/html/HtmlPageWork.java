package ntx.ts.html;

import ntx.ts.http.FileData;
import ntx.ts.srv.TSparams;

public class HtmlPageWork extends HtmlPage {

  public String workTask;
  public String err;
  public String msg;
  public String umsg;
  public String uumsg;
  public String stateText;
  public String workAction;
  public String deflt;
  public String[] workHist;

  public HtmlPageWork(
          String a_title,
          String a_sound,
          String task,
          String err,
          String msg,
          String stateText,
          String action,
          String[] hist,
          String deflt) {
    init(a_title, a_sound, task, err, msg, stateText, action, hist, deflt, null, null);
    refreshByEnter = true;
  }

  public HtmlPageWork(
          String a_title,
          String a_sound,
          String task,
          String err,
          String msg,
          String stateText,
          String action,
          String[] hist) {
    init(a_title, a_sound, task, err, msg, stateText, action, hist, null, null, null);
    refreshByEnter = true;
  }

  public HtmlPageWork(
          String a_title,
          String a_sound,
          String task,
          String err,
          String msg,
          String stateText,
          String action,
          String[] hist,
          String deflt,
          String umsg,
          String uumsg
          ) {
    init(a_title, a_sound, task, err, msg, stateText, action, hist, deflt, umsg, uumsg);
    refreshByEnter = true;
  }

  private void init(
          String a_title,
          String a_sound,
          String task,
          String err,
          String msg,
          String stateText,
          String action,
          String[] hist,
          String deflt,
          String umsg,
          String uumsg
  ) {
    title = a_title;
    sound = a_sound;
    workTask = task;
    this.msg = msg;
    this.stateText = stateText;
    workAction = action;
    workHist = hist;
    this.err = err;
    fontSize = TSparams.fontSize;
    fontSize2 = TSparams.fontSize2;
    fontSize3 = TSparams.fontSize3;
    this.deflt = deflt;
    this.umsg = umsg;
    this.uumsg = uumsg;
  }

  @Override
  public FileData getPage() throws Exception {
    createPage();
    return super.getPage();
  }

  private void createPage() throws Exception {
    addFormStart("work.html", "f");

    if (workTask != null) {
      addBlock(workTask, fontSize3, null);
    }
    if (msg != null) {
      addBlock("<b>" + msg + "</b>", 0, "blue");
    }
    if (umsg != null) {
      addBlock(umsg, fontSize2, null);
    }
    if (uumsg != null) {
      addBlock(uumsg, fontSize2, null);
    }
    if (err != null) {
      addBlock("<b>" + err + "</b>", 0, "red");
    }
    if (stateText != null) {
      addBlock("<i>" + stateText + "</i>", fontSize2, "green");
    }
    if (workAction != null) {
      addBlock("<b>" + workAction + "</b>");
    }

    if (deflt == null) {
      addFormField("scan", 20);
    } else {
      addFormFieldTxt("scan", 20, deflt);
    }
    addFormButtonSubmit(">", "f.scan", deflt != null);

    if (workHist != null) {
      addBlockStart(fontSize2, null);
      addText("\r\n");
      for (int i = 0; i < workHist.length; i++) {
        if (i < (workHist.length - 1)) {
          addLine(workHist[i]);
        } else {
          addText(workHist[i]);
          addText("\r\n");
        }
      }
      addBlockFin();
    }

    addFormEnd();
  }
}
