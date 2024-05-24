package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Проверка Партии ПУ
 */
public class Z_TS_SHKLIST3_IS_CHARG_PU {

  // importing params
  public String USER_SHK = ""; // Штрих-код
  public String W_STELL = ""; // Номер стеллажа
  public String W_CHARG_PU = ""; // Номер партии
  //
  // exporting params
  public String ISERR = ""; // Признак ошибки
  public String W_NEW_CHARG_PU = ""; // Номер партии
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
      System.out.println("Вызов ФМ Z_TS_SHKLIST3_IS_CHARG_PU");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SHKLIST3_IS_CHARG_PU:");
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  W_STELL=" + W_STELL);
      System.out.println("  W_CHARG_PU=" + W_CHARG_PU);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SHKLIST3_IS_CHARG_PU:");
        System.out.println("  ISERR=" + ISERR);
        System.out.println("  W_NEW_CHARG_PU=" + W_NEW_CHARG_PU);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SHKLIST3_IS_CHARG_PU:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SHKLIST3_IS_CHARG_PU: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SHKLIST3_IS_CHARG_PU params) {
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

      impParams.setValue("USER_SHK", params.USER_SHK);
      impParams.setValue("W_STELL", params.W_STELL);
      impParams.setValue("W_CHARG_PU", params.W_CHARG_PU);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ISERR = expParams.getString("ISERR");
        params.W_NEW_CHARG_PU = expParams.getString("W_NEW_CHARG_PU");
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
      function = SAPconn.getFunction("Z_TS_SHKLIST3_IS_CHARG_PU");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SHKLIST3_IS_CHARG_PU not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
