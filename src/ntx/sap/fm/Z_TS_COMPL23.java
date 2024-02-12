package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Список ИП для комплектации сотрудником
 */
public class Z_TS_COMPL23 {

  // importing params
  public String LGORT = ""; // Склад
  public String USER_SHK = ""; // Штрих-код
  //
  // table params
  public ZTS_CHOOSE_VBELN_S[] IT = new ZTS_CHOOSE_VBELN_S[0]; // ZTS_CHOOSE_VBELN_S TAB
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
    IT = new ZTS_CHOOSE_VBELN_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_CHOOSE_VBELN_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL23");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL23:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL23:");
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL23:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL23: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL23 params) {
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

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("USER_SHK", params.USER_SHK);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("PRIOR_NUM", params.IT[i].PRIOR_NUM);
        IT_t.setValue("VBELN", params.IT[i].VBELN);
        IT_t.setValue("VBELN_DESC", params.IT[i].VBELN_DESC);
        IT_t.setValue("TANUM", params.IT[i].TANUM);
        IT_t.setValue("KUNWE", params.IT[i].KUNWE);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.IT = new ZTS_CHOOSE_VBELN_S[IT_t.getNumRows()];
        ZTS_CHOOSE_VBELN_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_CHOOSE_VBELN_S();
          IT_r.PRIOR_NUM = IT_t.getBigDecimal("PRIOR_NUM");
          IT_r.VBELN = IT_t.getString("VBELN");
          IT_r.VBELN_DESC = IT_t.getString("VBELN_DESC");
          IT_r.TANUM = IT_t.getString("TANUM");
          IT_r.KUNWE = IT_t.getString("KUNWE");
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
      function = SAPconn.getFunction("Z_TS_COMPL23");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL23 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
