package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Проверка статуса возврата (перед добавлением данных)
 */
public class Z_TS_VOZ2 {

  // importing params
  public String LGORT = ""; // Склад
  public int ID = 0; // 1-й ключ таблицы
  //
  // table params
  public ZTS_VOZ1_S[] IT_TOV = new ZTS_VOZ1_S[0]; // Товар (возврат клиента)
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

  public void IT_TOV_create(int n) {
    IT_TOV = new ZTS_VOZ1_S[n];
    for (int i = 0; i < n; i++) {
      IT_TOV[i] = new ZTS_VOZ1_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_VOZ2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_VOZ2:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  ID=" + ID);
      System.out.println("  IT_TOV.length=" + IT_TOV.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_VOZ2:");
        System.out.println("  err=" + err);
        System.out.println("  IT_TOV.length=" + IT_TOV.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_VOZ2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_VOZ2: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_VOZ2 params) {
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

      JCoTable IT_TOV_t = tabParams.getTable("IT_TOV");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("ID", params.ID);

      IT_TOV_t.appendRows(params.IT_TOV.length);
      for (int i = 0; i < params.IT_TOV.length; i++) {
        IT_TOV_t.setRow(i);
        IT_TOV_t.setValue("LENUM", params.IT_TOV[i].LENUM);
        IT_TOV_t.setValue("MATNR", params.IT_TOV[i].MATNR);
        IT_TOV_t.setValue("CHARG", params.IT_TOV[i].CHARG);
        IT_TOV_t.setValue("QTY", params.IT_TOV[i].QTY);
        IT_TOV_t.setValue("N_POS", params.IT_TOV[i].N_POS);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT_TOV = new ZTS_VOZ1_S[IT_TOV_t.getNumRows()];
        ZTS_VOZ1_S IT_TOV_r;
        for (int i = 0; i < params.IT_TOV.length; i++) {
          IT_TOV_t.setRow(i);
          IT_TOV_r = new ZTS_VOZ1_S();
          IT_TOV_r.LENUM = IT_TOV_t.getString("LENUM");
          IT_TOV_r.MATNR = IT_TOV_t.getString("MATNR");
          IT_TOV_r.CHARG = IT_TOV_t.getString("CHARG");
          IT_TOV_r.QTY = IT_TOV_t.getBigDecimal("QTY");
          IT_TOV_r.N_POS = IT_TOV_t.getInt("N_POS");
          params.IT_TOV[i] = IT_TOV_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_VOZ2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_VOZ2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
