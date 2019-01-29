package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получение даты/времени последней записи
 */
public class Z_TS_LOG_MAX_DT {

  // importing params
  public String SRV_NAME = ""; // Имя сервера
  //
  // exporting params
  public String MAX_DT = ""; // дата/время последней записи
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_LOG_MAX_DT");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_LOG_MAX_DT:");
      System.out.println("  SRV_NAME=" + SRV_NAME);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_LOG_MAX_DT:");
        System.out.println("  MAX_DT=" + MAX_DT);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_LOG_MAX_DT:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_LOG_MAX_DT: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_LOG_MAX_DT params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    impParams.setValue("SRV_NAME", params.SRV_NAME);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.MAX_DT = expParams.getString("MAX_DT");
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_LOG_MAX_DT");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_LOG_MAX_DT not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}