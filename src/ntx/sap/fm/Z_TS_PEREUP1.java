package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * "В переупаковку" - отсканирована ячейка
 */
public class Z_TS_PEREUP1 {

  // importing params
  public String LGORT = ""; // Склад
  public String CELL = ""; // Складское место
  //
  // exporting params
  public String WERKS = ""; // Завод
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public ZTS_SETT WA_I = new ZTS_SETT(); // Настройки по складам
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
      System.out.println("Вызов ФМ Z_TS_PEREUP1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PEREUP1:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  CELL=" + CELL);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PEREUP1:");
        System.out.println("  WERKS=" + WERKS);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  LGTYP=" + LGTYP);
        System.out.println("  WA_I.MANDT=" + WA_I.MANDT);
        System.out.println("  WA_I.LGORT=" + WA_I.LGORT);
        System.out.println("  WA_I.LGPLA_PEREUP=" + WA_I.LGPLA_PEREUP);
        System.out.println("  WA_I.SHOW_MAT_CELL=" + WA_I.SHOW_MAT_CELL);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PEREUP1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PEREUP1: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_PEREUP1 params) {
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

      JCoStructure WA_I_s = expParams.getStructure("WA_I");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("CELL", params.CELL);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.WERKS = expParams.getString("WERKS");
        params.LGNUM = expParams.getString("LGNUM");
        params.LGTYP = expParams.getString("LGTYP");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }
        params.WA_I.MANDT = WA_I_s.getString("MANDT");
        params.WA_I.LGORT = WA_I_s.getString("LGORT");
        params.WA_I.LGPLA_PEREUP = WA_I_s.getString("LGPLA_PEREUP");
        params.WA_I.SHOW_MAT_CELL = WA_I_s.getString("SHOW_MAT_CELL");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PEREUP1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PEREUP1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
