package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Сохранение кол-ва паллет и коробов по ИП
 */
public class Z_TS_COMPL25 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public String SET_INCOR_CORR = ""; // Общий флаг
  public String INCOR_CORR = ""; // Общий флаг
  //
  // changing params
  public String INCOR_CORR_CUR = ""; // Общий флаг
  public String INCOR_KMS = "";
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList chaParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL25");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL25:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  SET_INCOR_CORR=" + SET_INCOR_CORR);
      System.out.println("  INCOR_CORR=" + INCOR_CORR);
      System.out.println("  INCOR_CORR_CUR=" + INCOR_CORR_CUR);
      System.out.println("  INCOR_KMS=" + INCOR_KMS);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL25:");
        System.out.println("  INCOR_CORR_CUR=" + INCOR_CORR_CUR);
        System.out.println("  INCOR_KMS=" + INCOR_KMS);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL25:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL25: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL25 params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      impParams.clear();
      chaParams.clear();

      chaParams.setValue("INCOR_CORR_CUR", params.INCOR_CORR_CUR);
      chaParams.setValue("INCOR_KMS", params.INCOR_KMS);
      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("SET_INCOR_CORR", params.SET_INCOR_CORR);
      impParams.setValue("INCOR_CORR", params.INCOR_CORR);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INCOR_CORR_CUR = chaParams.getString("INCOR_CORR_CUR");
        params.INCOR_KMS = chaParams.getString("INCOR_KMS");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL25");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL25 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    chaParams = function.getChangingParameterList();

    isInit = true;

    return null;
  }
}
