package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение списка заказов на поставку
 */
public class Z_TS_PROD8 {

  // importing params
  public String SHK = ""; // Штрих-код
  //
  // table params
  public ZTS_SHK_S[] IT_SHK = new ZTS_SHK_S[0]; // ШК и название
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

  public void IT_SHK_create(int n) {
    IT_SHK = new ZTS_SHK_S[n];
    for (int i = 0; i < n; i++) {
      IT_SHK[i] = new ZTS_SHK_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PROD8");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PROD8:");
      System.out.println("  SHK=" + SHK);
      System.out.println("  IT_SHK.length=" + IT_SHK.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PROD8:");
        System.out.println("  err=" + err);
        System.out.println("  IT_SHK.length=" + IT_SHK.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PROD8:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PROD8: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_PROD8 params) {
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

      JCoTable IT_SHK_t = tabParams.getTable("IT_SHK");

      impParams.setValue("SHK", params.SHK);

      IT_SHK_t.appendRows(params.IT_SHK.length);
      for (int i = 0; i < params.IT_SHK.length; i++) {
        IT_SHK_t.setRow(i);
        IT_SHK_t.setValue("SHK", params.IT_SHK[i].SHK);
        IT_SHK_t.setValue("SHK_NAME", params.IT_SHK[i].SHK_NAME);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT_SHK = new ZTS_SHK_S[IT_SHK_t.getNumRows()];
        ZTS_SHK_S IT_SHK_r;
        for (int i = 0; i < params.IT_SHK.length; i++) {
          IT_SHK_t.setRow(i);
          IT_SHK_r = new ZTS_SHK_S();
          IT_SHK_r.SHK = IT_SHK_t.getString("SHK");
          IT_SHK_r.SHK_NAME = IT_SHK_t.getString("SHK_NAME");
          params.IT_SHK[i] = IT_SHK_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PROD8");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PROD8 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
