package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Ведомость перемещения по КП
 */
public class Z_TS_CVL3 {

  // importing params
  public String CVL = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  //
  // table params
  public ZCVL_MV[] IT = new ZCVL_MV[0]; // ZCVL_MV TAB
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
    IT = new ZCVL_MV[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZCVL_MV();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_CVL3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_CVL3:");
      System.out.println("  CVL=" + CVL);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_CVL3:");
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

      System.err.println("Error calling SAP procedure Z_TS_CVL3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_CVL3: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_CVL3 params) {
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

      impParams.setValue("CVL", params.CVL);
      impParams.setValue("LGPLA", params.LGPLA);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("IDX", params.IT[i].IDX);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("MAKTX", params.IT[i].MAKTX);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("LGPLA", params.IT[i].LGPLA);
        IT_t.setValue("QTY", params.IT[i].QTY);
        IT_t.setValue("TANUM", params.IT[i].TANUM);
        IT_t.setValue("PICKED", params.IT[i].PICKED);
        IT_t.setValue("KQUIT", params.IT[i].KQUIT);
        IT_t.setValue("TO_PICK", params.IT[i].TO_PICK);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZCVL_MV[IT_t.getNumRows()];
        ZCVL_MV IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZCVL_MV();
          IT_r.IDX = IT_t.getString("IDX");
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.MAKTX = IT_t.getString("MAKTX");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.LGPLA = IT_t.getString("LGPLA");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          IT_r.TANUM = IT_t.getString("TANUM");
          IT_r.PICKED = IT_t.getBigDecimal("PICKED");
          IT_r.KQUIT = IT_t.getString("KQUIT");
          IT_r.TO_PICK = IT_t.getBigDecimal("TO_PICK");
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
      function = SAPconn.getFunction("Z_TS_CVL3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_CVL3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
