package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получение сообщения для терминалов
 */
public class Z_TS_TERM_MSG_GET {

  // exporting params
  public String TERM_MSG = ""; // Сообщение для терминалов
  public String IS_ERR = ""; // Признак наличия ошибки
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList expParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_TERM_MSG_GET");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_TERM_MSG_GET:");
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_TERM_MSG_GET:");
        System.out.println("  TERM_MSG=" + TERM_MSG);
        System.out.println("  IS_ERR=" + IS_ERR);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_TERM_MSG_GET:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_TERM_MSG_GET: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_TERM_MSG_GET params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      expParams.clear();

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.TERM_MSG = expParams.getString("TERM_MSG");
        params.IS_ERR = expParams.getString("IS_ERR");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_TERM_MSG_GET");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_TERM_MSG_GET not found in SAP.");
    }

    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
