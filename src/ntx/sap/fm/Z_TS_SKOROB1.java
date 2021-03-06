package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Проверка номера заказа
 */
public class Z_TS_SKOROB1 {

  // importing params
  public String ZV = ""; // Номер заказа или поставки
  //
  // exporting params
  public String NAME1 = ""; // Имя 1
  public String LGORT = ""; // Склад
  public String MARKED = ""; // Индикатор из одной позиции
  public String NUM_TYP = ""; // Индикатор из одной позиции
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
      System.out.println("Вызов ФМ Z_TS_SKOROB1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKOROB1:");
      System.out.println("  ZV=" + ZV);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKOROB1:");
        System.out.println("  NAME1=" + NAME1);
        System.out.println("  LGORT=" + LGORT);
        System.out.println("  MARKED=" + MARKED);
        System.out.println("  NUM_TYP=" + NUM_TYP);
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

      System.err.println("Error calling SAP procedure Z_TS_SKOROB1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKOROB1: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SKOROB1 params) {
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

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("SHK_KOR", params.IT[i].SHK_KOR);
        IT_t.setValue("N_KOR", params.IT[i].N_KOR);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.NAME1 = expParams.getString("NAME1");
        params.LGORT = expParams.getString("LGORT");
        params.MARKED = expParams.getString("MARKED");
        params.NUM_TYP = expParams.getString("NUM_TYP");
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
      function = SAPconn.getFunction("Z_TS_SKOROB1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKOROB1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
