package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Данные по ABC по товарам в ячейке
 */
public class Z_TS_SKL_MOVE6 {

  // importing params
  public String LGORT = ""; // Склад
  public String LGPLA = ""; // Складское место
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String PAL = ""; // № единицы складирования
  //
  // table params
  public ZTS_MAT_ABC_S[] IT = new ZTS_MAT_ABC_S[0]; // Данные ABC по материалу
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

  public void IT_create(int n) {
    IT = new ZTS_MAT_ABC_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_MAT_ABC_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE6");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE6:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP=" + LGTYP);
      System.out.println("  PAL=" + PAL);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKL_MOVE6:");
        System.out.println("  err=" + err);
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SKL_MOVE6:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKL_MOVE6: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SKL_MOVE6 params) {
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

      JCoTable IT_t = tabParams.getTable("IT");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("LGNUM", params.LGNUM);
      impParams.setValue("LGTYP", params.LGTYP);
      impParams.setValue("PAL", params.PAL);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("MAKTX", params.IT[i].MAKTX);
        IT_t.setValue("QTY", params.IT[i].QTY);
        IT_t.setValue("ZABCN", params.IT[i].ZABCN);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_MAT_ABC_S[IT_t.getNumRows()];
        ZTS_MAT_ABC_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_MAT_ABC_S();
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.MAKTX = IT_t.getString("MAKTX");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          IT_r.ZABCN = IT_t.getString("ZABCN");
          params.IT[i] = IT_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_SKL_MOVE6");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKL_MOVE6 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
