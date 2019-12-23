package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Выдача списка складов ДПДТ, с которых может делаться приемка на этот склад
 */
public class Z_TS_DPDT3 {

  // importing params
  public String LGORT2 = ""; // Склад принимающий
  //
  // table params
  public ZTS_LGORT1_S[] IT_LG = new ZTS_LGORT1_S[0]; // Отпускающий склад
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
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void IT_LG_create(int n) {
    IT_LG = new ZTS_LGORT1_S[n];
    for (int i = 0; i < n; i++) {
      IT_LG[i] = new ZTS_LGORT1_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_DPDT3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_DPDT3:");
      System.out.println("  LGORT2=" + LGORT2);
      System.out.println("  IT_LG.length=" + IT_LG.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_DPDT3:");
        System.out.println("  err=" + err);
        System.out.println("  IT_LG.length=" + IT_LG.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_DPDT3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_DPDT3: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_DPDT3 params) {
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
      tabParams.clear();

      JCoTable IT_LG_t = tabParams.getTable("IT_LG");

      impParams.setValue("LGORT2", params.LGORT2);

      IT_LG_t.appendRows(params.IT_LG.length);
      for (int i = 0; i < params.IT_LG.length; i++) {
        IT_LG_t.setRow(i);
        IT_LG_t.setValue("LGORT1", params.IT_LG[i].LGORT1);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT_LG = new ZTS_LGORT1_S[IT_LG_t.getNumRows()];
        ZTS_LGORT1_S IT_LG_r;
        for (int i = 0; i < params.IT_LG.length; i++) {
          IT_LG_t.setRow(i);
          IT_LG_r = new ZTS_LGORT1_S();
          IT_LG_r.LGORT1 = IT_LG_t.getString("LGORT1");
          params.IT_LG[i] = IT_LG_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_DPDT3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_DPDT3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
