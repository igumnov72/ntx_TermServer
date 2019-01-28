package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получение настроек СГМ по складу
 */
public class Z_TS_COMPL15 {

  // importing params
  public String LGORT = ""; // Склад
  //
  // exporting params
  public String SGM = ""; // Сканировать СГМ (коробки)
  public String SGM_ASK = ""; // Спрашивать о сканировании СГМ (коробок)
  public String NO_FREE_COMPL = ""; // Запрет доступа к свободной комплектации
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
      System.out.println("Вызов ФМ Z_TS_COMPL15");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL15:");
      System.out.println("  LGORT=" + LGORT);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL15:");
        System.out.println("  SGM=" + SGM);
        System.out.println("  SGM_ASK=" + SGM_ASK);
        System.out.println("  NO_FREE_COMPL=" + NO_FREE_COMPL);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL15:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL15: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL15 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    impParams.setValue("LGORT", params.LGORT);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.SGM = expParams.getString("SGM");
      params.SGM_ASK = expParams.getString("SGM_ASK");
      params.NO_FREE_COMPL = expParams.getString("NO_FREE_COMPL");
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL15");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL15 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
