package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных по сборному коробу/паллете
 */
public class Z_TS_SKOROB2 {

  // importing params
  public String ZV = ""; // Номер заказа или поставки
  public String LENUM = ""; // № единицы складирования
  //
  // exporting params
  public String INF = "";
  //
  // table params
  public ZTS_SKOROB_SHK_S[] IT = new ZTS_SKOROB_SHK_S[0]; // Число коробов по ШК короба
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
    IT = new ZTS_SKOROB_SHK_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_SKOROB_SHK_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SKOROB2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKOROB2:");
      System.out.println("  ZV=" + ZV);
      System.out.println("  LENUM=" + LENUM);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKOROB2:");
        System.out.println("  INF=" + INF);
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

      System.err.println("Error calling SAP procedure Z_TS_SKOROB2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKOROB2: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SKOROB2 params) {
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

      impParams.setValue("ZV", params.ZV);
      impParams.setValue("LENUM", params.LENUM);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("SHK_KOR", params.IT[i].SHK_KOR);
        IT_t.setValue("N_KOR", params.IT[i].N_KOR);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INF = expParams.getString("INF");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_SKOROB_SHK_S[IT_t.getNumRows()];
        ZTS_SKOROB_SHK_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_SKOROB_SHK_S();
          IT_r.SHK_KOR = IT_t.getString("SHK_KOR");
          IT_r.N_KOR = IT_t.getInt("N_KOR");
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
      function = SAPconn.getFunction("Z_TS_SKOROB2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKOROB2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
