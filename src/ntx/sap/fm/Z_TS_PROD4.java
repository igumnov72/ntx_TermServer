package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Заказы и кол-во по партиям
 */
public class Z_TS_PROD4 {

  // importing params
  public String LGORT = ""; // Склад
  //
  // table params
  public ZTS_PRT_QTY_S[] IT_TOV = new ZTS_PRT_QTY_S[0]; // Кол-во по партии
  public ZTS_CHARG_EBELN_S[] IT_EBELNS = new ZTS_CHARG_EBELN_S[0]; // Заказы и кол-во по партии
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void IT_TOV_create(int n) {
    IT_TOV = new ZTS_PRT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_TOV[i] = new ZTS_PRT_QTY_S();
    }
  }

  public void IT_EBELNS_create(int n) {
    IT_EBELNS = new ZTS_CHARG_EBELN_S[n];
    for (int i = 0; i < n; i++) {
      IT_EBELNS[i] = new ZTS_CHARG_EBELN_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PROD4");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PROD4:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  IT_TOV.length=" + IT_TOV.length);
      System.out.println("  IT_EBELNS.length=" + IT_EBELNS.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PROD4:");
        System.out.println("  IT_TOV.length=" + IT_TOV.length);
        System.out.println("  IT_EBELNS.length=" + IT_EBELNS.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PROD4:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PROD4: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PROD4 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    tabParams.clear();

    JCoTable IT_TOV_t = tabParams.getTable("IT_TOV");
    JCoTable IT_EBELNS_t = tabParams.getTable("IT_EBELNS");

    impParams.setValue("LGORT", params.LGORT);

    IT_TOV_t.appendRows(params.IT_TOV.length);
    for (int i = 0; i < params.IT_TOV.length; i++) {
      IT_TOV_t.setRow(i);
      IT_TOV_t.setValue("CHARG", params.IT_TOV[i].CHARG);
      IT_TOV_t.setValue("QTY", params.IT_TOV[i].QTY);
    }

    IT_EBELNS_t.appendRows(params.IT_EBELNS.length);
    for (int i = 0; i < params.IT_EBELNS.length; i++) {
      IT_EBELNS_t.setRow(i);
      IT_EBELNS_t.setValue("CHARG", params.IT_EBELNS[i].CHARG);
      IT_EBELNS_t.setValue("EBELN", params.IT_EBELNS[i].EBELN);
      IT_EBELNS_t.setValue("QTY_SCAN", params.IT_EBELNS[i].QTY_SCAN);
      IT_EBELNS_t.setValue("QTY", params.IT_EBELNS[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.IT_TOV = new ZTS_PRT_QTY_S[IT_TOV_t.getNumRows()];
      ZTS_PRT_QTY_S IT_TOV_r;
      for (int i = 0; i < params.IT_TOV.length; i++) {
        IT_TOV_t.setRow(i);
        IT_TOV_r = new ZTS_PRT_QTY_S();
        IT_TOV_r.CHARG = IT_TOV_t.getString("CHARG");
        IT_TOV_r.QTY = IT_TOV_t.getBigDecimal("QTY");
        params.IT_TOV[i] = IT_TOV_r;
      }

      params.IT_EBELNS = new ZTS_CHARG_EBELN_S[IT_EBELNS_t.getNumRows()];
      ZTS_CHARG_EBELN_S IT_EBELNS_r;
      for (int i = 0; i < params.IT_EBELNS.length; i++) {
        IT_EBELNS_t.setRow(i);
        IT_EBELNS_r = new ZTS_CHARG_EBELN_S();
        IT_EBELNS_r.CHARG = IT_EBELNS_t.getString("CHARG");
        IT_EBELNS_r.EBELN = IT_EBELNS_t.getString("EBELN");
        IT_EBELNS_r.QTY_SCAN = IT_EBELNS_t.getBigDecimal("QTY_SCAN");
        IT_EBELNS_r.QTY = IT_EBELNS_t.getBigDecimal("QTY");
        params.IT_EBELNS[i] = IT_EBELNS_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PROD4");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PROD4 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
