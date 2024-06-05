package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Создание Наряда
 */
public class Z_TS_CREATE_ZTPRJ {

  // importing params
  public String USER_SHK = ""; // ШК пользователя
  public String W_SHK_ZEQ = ""; // ШК Станка
  public String W_SHK_CHARG = ""; // ШК Навоя
  //
  // exporting params
  public String ISERR = ""; // Признак ошибки
  public String ZTPRJ = ""; // Наряды
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
      System.out.println("Вызов ФМ Z_TS_CREATE_ZTPRJ");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_CREATE_ZTPRJ:");
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  W_SHK_ZEQ=" + W_SHK_ZEQ);
      System.out.println("  W_SHK_CHARG=" + W_SHK_CHARG);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_CREATE_ZTPRJ:");
        System.out.println("  ISERR=" + ISERR);
        System.out.println("  ZTPRJ=" + ZTPRJ);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_CREATE_ZTPRJ:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_CREATE_ZTPRJ: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_CREATE_ZTPRJ params) {
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
      impParams.setValue("W_SHK_ZEQ", params.W_SHK_ZEQ);
      impParams.setValue("W_SHK_CHARG", params.W_SHK_CHARG);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ISERR = expParams.getString("ISERR");
        params.ZTPRJ = expParams.getString("ZTPRJ");
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
      function = SAPconn.getFunction("Z_TS_CREATE_ZTPRJ");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_CREATE_ZTPRJ not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
