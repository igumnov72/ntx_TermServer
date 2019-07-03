package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Регистрация рабочего места
 */
public class Z_TS_EVENT_WP {

  // importing params
  public int WP_ID = 0; // Рабочее место (id)
  public String WP_TYP = ""; // Тип рабочего места
  public String LAST_IP = ""; // ip-адрес
  public String LAST_DT_ID = "0"; // Дата-идентификатор планшета
  public String LAST_DT_STR = ""; // Строковый дата-идентификатор планшета
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  public boolean isSapErr;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_EVENT_WP");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_EVENT_WP:");
      System.out.println("  WP_ID=" + WP_ID);
      System.out.println("  WP_TYP=" + WP_TYP);
      System.out.println("  LAST_IP=" + LAST_IP);
      System.out.println("  LAST_DT_ID=" + LAST_DT_ID);
      System.out.println("  LAST_DT_STR=" + LAST_DT_STR);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_EVENT_WP:");
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_EVENT_WP:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_EVENT_WP: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_EVENT_WP params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      impParams.clear();
      expParams.clear();

      impParams.setValue("WP_ID", params.WP_ID);
      impParams.setValue("WP_TYP", params.WP_TYP);
      impParams.setValue("LAST_IP", params.LAST_IP);
      impParams.setValue("LAST_DT_ID", params.LAST_DT_ID);
      impParams.setValue("LAST_DT_STR", params.LAST_DT_STR);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_EVENT_WP");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_EVENT_WP not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
