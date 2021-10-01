package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получение данных о признаке ABC
 */
public class Z_TS_ABC {

  // importing params
  public String LGORT = ""; // Склад
  public String MATNR = ""; // Номер материала
  //
  // exporting params
  public String ABC = ""; // Признак материала на складе ABC
  public String XYZ = ""; // XYZ
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
      System.out.println("Вызов ФМ Z_TS_ABC");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_ABC:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  MATNR=" + MATNR);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_ABC:");
        System.out.println("  ABC=" + ABC);
        System.out.println("  XYZ=" + XYZ);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_ABC:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_ABC: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_ABC params) {
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

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("MATNR", params.MATNR);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ABC = expParams.getString("ABC");
        params.XYZ = expParams.getString("XYZ");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_ABC");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_ABC not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
