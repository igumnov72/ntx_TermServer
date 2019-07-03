package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Неразмещенные паллеты (по журналу)
 */
public class Z_TS_NERAZM_PAL {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public String LGORT = ""; // Склад
  public String NO_AUTO_MAT = ""; // без учета автоматически расставляемых материалов
  //
  // table params
  public ZTS_LENUM_S[] IT_NERAZM = new ZTS_LENUM_S[0]; // Паллеты
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

  public void IT_NERAZM_create(int n) {
    IT_NERAZM = new ZTS_LENUM_S[n];
    for (int i = 0; i < n; i++) {
      IT_NERAZM[i] = new ZTS_LENUM_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_NERAZM_PAL");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_NERAZM_PAL:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  NO_AUTO_MAT=" + NO_AUTO_MAT);
      System.out.println("  IT_NERAZM.length=" + IT_NERAZM.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_NERAZM_PAL:");
        System.out.println("  IT_NERAZM.length=" + IT_NERAZM.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_NERAZM_PAL:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_NERAZM_PAL: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_NERAZM_PAL params) {
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

      JCoTable IT_NERAZM_t = tabParams.getTable("IT_NERAZM");

      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("NO_AUTO_MAT", params.NO_AUTO_MAT);

      IT_NERAZM_t.appendRows(params.IT_NERAZM.length);
      for (int i = 0; i < params.IT_NERAZM.length; i++) {
        IT_NERAZM_t.setRow(i);
        IT_NERAZM_t.setValue("LENUM", params.IT_NERAZM[i].LENUM);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.IT_NERAZM = new ZTS_LENUM_S[IT_NERAZM_t.getNumRows()];
        ZTS_LENUM_S IT_NERAZM_r;
        for (int i = 0; i < params.IT_NERAZM.length; i++) {
          IT_NERAZM_t.setRow(i);
          IT_NERAZM_r = new ZTS_LENUM_S();
          IT_NERAZM_r.LENUM = IT_NERAZM_t.getString("LENUM");
          params.IT_NERAZM[i] = IT_NERAZM_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_NERAZM_PAL");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_NERAZM_PAL not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
