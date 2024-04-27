package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Опись суровья - Проверка ШК
 */
public class Z_TS_OPISSUROV_SHK {

  // importing params
  public String W_SHK = ""; // Штрих-код
  public String W_OPIS = ""; // Идентификатор описи
  public String W_LGORT = ""; // Склад
  //
  // exporting params
  public String ISERR = ""; // Признак ошибки
  public BigDecimal QTY = new BigDecimal(0); // Метраж выпуска партии
  public String INFO = ""; // Информация
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
      System.out.println("Вызов ФМ Z_TS_OPISSUROV_SHK");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_OPISSUROV_SHK:");
      System.out.println("  W_SHK=" + W_SHK);
      System.out.println("  W_OPIS=" + W_OPIS);
      System.out.println("  W_LGORT=" + W_LGORT);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_OPISSUROV_SHK:");
        System.out.println("  ISERR=" + ISERR);
        System.out.println("  QTY=" + QTY);
        System.out.println("  INFO=" + INFO);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_OPISSUROV_SHK:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_OPISSUROV_SHK: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_OPISSUROV_SHK params) {
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

      impParams.setValue("W_SHK", params.W_SHK);
      impParams.setValue("W_OPIS", params.W_OPIS);
      impParams.setValue("W_LGORT", params.W_LGORT);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ISERR = expParams.getString("ISERR");
        params.QTY = expParams.getBigDecimal("QTY");
        params.INFO = expParams.getString("INFO");
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
      function = SAPconn.getFunction("Z_TS_OPISSUROV_SHK");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_OPISSUROV_SHK not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
