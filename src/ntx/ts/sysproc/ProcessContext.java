package ntx.ts.sysproc;

import ntx.sap.fm.Z_TS_TERM_MSG_GET;
import ntx.ts.srv.Track;

/**
 * контекст выполнения процесса (доступ к глобальным переменным)
 */
public class ProcessContext {

  public Track track;
  private static String termMsg = ""; // сообщение о перезагрузке САПа
  private static long lastMsgQuery = 0; // последнее время запроса сообщения из САПа

  public ProcessContext(Track track) {
    this.track = track;
  }

  public static String getTermMsg() {
    long tim = System.currentTimeMillis();

    if (!termMsg.isEmpty()) {
      synchronized (ProcessContext.class) {
        if ((tim - lastMsgQuery) >= 5000) {
          Z_TS_TERM_MSG_GET f = new Z_TS_TERM_MSG_GET();
          f.execute();
          if (!f.isErr && !f.IS_ERR.equals("X")) {
            termMsg = f.TERM_MSG;
          }
          lastMsgQuery = System.currentTimeMillis();
        }
      }
    }

    return termMsg;
  }

  public static void setTermMsg(String msg) {
    termMsg = msg;
    lastMsgQuery = System.currentTimeMillis();
  }
}
