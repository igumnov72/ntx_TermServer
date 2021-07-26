package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Запас адресного склада
 */
public class Z_TS_LG_STOCK {

  // importing params
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  public String LENUM = ""; // № единицы складирования
  //
  // table params
  public ZTS_LG_STOCK_S[] IT = new ZTS_LG_STOCK_S[0]; // ZTS_LG_STOCK_T
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

  public void IT_create(int n) {
    IT = new ZTS_LG_STOCK_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_LG_STOCK_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_LG_STOCK");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_LG_STOCK:");
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP=" + LGTYP);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  LENUM=" + LENUM);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_LG_STOCK:");
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_LG_STOCK:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_LG_STOCK: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_LG_STOCK params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      impParams.clear();
      tabParams.clear();

      JCoTable IT_t = tabParams.getTable("IT");

      impParams.setValue("LGNUM", params.LGNUM);
      impParams.setValue("LGTYP", params.LGTYP);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("LENUM", params.LENUM);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("LGNUM", params.IT[i].LGNUM);
        IT_t.setValue("LGTYP", params.IT[i].LGTYP);
        IT_t.setValue("LGPLA", params.IT[i].LGPLA);
        IT_t.setValue("LENUM", params.IT[i].LENUM);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("SOBKZ", params.IT[i].SOBKZ);
        IT_t.setValue("SONUM", params.IT[i].SONUM);
        IT_t.setValue("GESME", params.IT[i].GESME);
        IT_t.setValue("VERME", params.IT[i].VERME);
        IT_t.setValue("EINME", params.IT[i].EINME);
        IT_t.setValue("AUSME", params.IT[i].AUSME);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.IT = new ZTS_LG_STOCK_S[IT_t.getNumRows()];
        ZTS_LG_STOCK_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_LG_STOCK_S();
          IT_r.LGNUM = IT_t.getString("LGNUM");
          IT_r.LGTYP = IT_t.getString("LGTYP");
          IT_r.LGPLA = IT_t.getString("LGPLA");
          IT_r.LENUM = IT_t.getString("LENUM");
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.SOBKZ = IT_t.getString("SOBKZ");
          IT_r.SONUM = IT_t.getString("SONUM");
          IT_r.GESME = IT_t.getBigDecimal("GESME");
          IT_r.VERME = IT_t.getBigDecimal("VERME");
          IT_r.EINME = IT_t.getBigDecimal("EINME");
          IT_r.AUSME = IT_t.getBigDecimal("AUSME");
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
      function = SAPconn.getFunction("Z_TS_LG_STOCK");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_LG_STOCK not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
