package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение вед на компл по ячейке при своб компл (для отображения)
 */
public class Z_TS_COMPL13 {

  // importing params
  public String LGORT = ""; // Склад
  public String LGPLA = ""; // Складское место
  //
  // table params
  public ZTS_VBELN_S[] IT_V = new ZTS_VBELN_S[0]; // Номера поставок
  public ZTS_VED_S[] IT = new ZTS_VED_S[0]; // Ведомость на комплектацию (для отображения)
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

  public void IT_V_create(int n) {
    IT_V = new ZTS_VBELN_S[n];
    for (int i = 0; i < n; i++) {
      IT_V[i] = new ZTS_VBELN_S();
    }
  }

  public void IT_create(int n) {
    IT = new ZTS_VED_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_VED_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL13");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL13:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  IT_V.length=" + IT_V.length);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL13:");
        System.out.println("  err=" + err);
        System.out.println("  IT_V.length=" + IT_V.length);
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL13:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL13: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL13 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();
    tabParams.clear();

    JCoTable IT_V_t = tabParams.getTable("IT_V");
    JCoTable IT_t = tabParams.getTable("IT");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("LGPLA", params.LGPLA);

    IT_V_t.appendRows(params.IT_V.length);
    for (int i = 0; i < params.IT_V.length; i++) {
      IT_V_t.setRow(i);
      IT_V_t.setValue("VBELN", params.IT_V[i].VBELN);
    }

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("LGNUM", params.IT[i].LGNUM);
      IT_t.setValue("LGTYP", params.IT[i].LGTYP);
      IT_t.setValue("LGPLA", params.IT[i].LGPLA);
      IT_t.setValue("VBELN", params.IT[i].VBELN);
      IT_t.setValue("LENUM", params.IT[i].LENUM);
      IT_t.setValue("MATNR", params.IT[i].MATNR);
      IT_t.setValue("CHARG", params.IT[i].CHARG);
      IT_t.setValue("QTY", params.IT[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_V = new ZTS_VBELN_S[IT_V_t.getNumRows()];
      ZTS_VBELN_S IT_V_r;
      for (int i = 0; i < params.IT_V.length; i++) {
        IT_V_t.setRow(i);
        IT_V_r = new ZTS_VBELN_S();
        IT_V_r.VBELN = IT_V_t.getString("VBELN");
        params.IT_V[i] = IT_V_r;
      }

      params.IT = new ZTS_VED_S[IT_t.getNumRows()];
      ZTS_VED_S IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new ZTS_VED_S();
        IT_r.LGNUM = IT_t.getString("LGNUM");
        IT_r.LGTYP = IT_t.getString("LGTYP");
        IT_r.LGPLA = IT_t.getString("LGPLA");
        IT_r.VBELN = IT_t.getString("VBELN");
        IT_r.LENUM = IT_t.getString("LENUM");
        IT_r.MATNR = IT_t.getString("MATNR");
        IT_r.CHARG = IT_t.getString("CHARG");
        IT_r.QTY = IT_t.getBigDecimal("QTY");
        params.IT[i] = IT_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL13");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL13 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
