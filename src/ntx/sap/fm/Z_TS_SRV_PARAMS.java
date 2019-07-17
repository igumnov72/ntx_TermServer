package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Сохранение параметров сервера тенрминалов
 */
public class Z_TS_SRV_PARAMS {

  // importing params
  public String SRV_NAME = ""; // Имя сервера
  public String IP = ""; // ip-адрес
  public String PORT = ""; // Порт сервера
  //
  // exporting params
  public String TERM_MSG = ""; // Сообщение для терминалов
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
      System.out.println("Вызов ФМ Z_TS_SRV_PARAMS");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SRV_PARAMS:");
      System.out.println("  SRV_NAME=" + SRV_NAME);
      System.out.println("  IP=" + IP);
      System.out.println("  PORT=" + PORT);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SRV_PARAMS:");
        System.out.println("  TERM_MSG=" + TERM_MSG);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SRV_PARAMS:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SRV_PARAMS: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SRV_PARAMS params) {
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

      impParams.setValue("SRV_NAME", params.SRV_NAME);
      impParams.setValue("IP", params.IP);
      impParams.setValue("PORT", params.PORT);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.TERM_MSG = expParams.getString("TERM_MSG");
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
      function = SAPconn.getFunction("Z_TS_SRV_PARAMS");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SRV_PARAMS not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
