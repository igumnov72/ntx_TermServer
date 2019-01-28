package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение ведомости на комплектацию
 */
public class Z_TS_COMPL1 {

  // importing params
  public String LGORT = ""; // Склад
  public String VBELN = ""; // Номер документа сбыта
  public String INF_COMPL1 = ""; // Признак информационной комплектации
  //
  // exporting params
  public String INF_COMPL = ""; // Признак информационной комплектации
  //
  // table params
  public ZTS_COMPL_S[] IT = new ZTS_COMPL_S[0]; // Данные ведомости на комплектацию
  public ZTS_COMPL_FP_S[] IT_FP = new ZTS_COMPL_FP_S[0]; // Признак комплектации с паллеты
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
    IT = new ZTS_COMPL_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_COMPL_S();
    }
  }

  public void IT_FP_create(int n) {
    IT_FP = new ZTS_COMPL_FP_S[n];
    for (int i = 0; i < n; i++) {
      IT_FP[i] = new ZTS_COMPL_FP_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL1:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  INF_COMPL1=" + INF_COMPL1);
      System.out.println("  IT.length=" + IT.length);
      System.out.println("  IT_FP.length=" + IT_FP.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL1:");
        System.out.println("  INF_COMPL=" + INF_COMPL);
        System.out.println("  err=" + err);
        System.out.println("  IT.length=" + IT.length);
        System.out.println("  IT_FP.length=" + IT_FP.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL1: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL1 params) {
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

    JCoTable IT_t = tabParams.getTable("IT");
    JCoTable IT_FP_t = tabParams.getTable("IT_FP");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("VBELN", params.VBELN);
    impParams.setValue("INF_COMPL1", params.INF_COMPL1);

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("LGNUM", params.IT[i].LGNUM);
      IT_t.setValue("TANUM", params.IT[i].TANUM);
      IT_t.setValue("TAPOS", params.IT[i].TAPOS);
      IT_t.setValue("LGTYP", params.IT[i].LGTYP);
      IT_t.setValue("LGPLA", params.IT[i].LGPLA);
      IT_t.setValue("LENUM", params.IT[i].LENUM);
      IT_t.setValue("MATNR", params.IT[i].MATNR);
      IT_t.setValue("CHARG", params.IT[i].CHARG);
      IT_t.setValue("ONLY_PRT", params.IT[i].ONLY_PRT);
      IT_t.setValue("SOBKZ", params.IT[i].SOBKZ);
      IT_t.setValue("SONUM", params.IT[i].SONUM);
      IT_t.setValue("QTY", params.IT[i].QTY);
    }

    IT_FP_t.appendRows(params.IT_FP.length);
    for (int i = 0; i < params.IT_FP.length; i++) {
      IT_FP_t.setRow(i);
      IT_FP_t.setValue("LGPLA", params.IT_FP[i].LGPLA);
      IT_FP_t.setValue("COMPL_FROM", params.IT_FP[i].COMPL_FROM);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.INF_COMPL = expParams.getString("INF_COMPL");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT = new ZTS_COMPL_S[IT_t.getNumRows()];
      ZTS_COMPL_S IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new ZTS_COMPL_S();
        IT_r.LGNUM = IT_t.getString("LGNUM");
        IT_r.TANUM = IT_t.getString("TANUM");
        IT_r.TAPOS = IT_t.getString("TAPOS");
        IT_r.LGTYP = IT_t.getString("LGTYP");
        IT_r.LGPLA = IT_t.getString("LGPLA");
        IT_r.LENUM = IT_t.getString("LENUM");
        IT_r.MATNR = IT_t.getString("MATNR");
        IT_r.CHARG = IT_t.getString("CHARG");
        IT_r.ONLY_PRT = IT_t.getString("ONLY_PRT");
        IT_r.SOBKZ = IT_t.getString("SOBKZ");
        IT_r.SONUM = IT_t.getString("SONUM");
        IT_r.QTY = IT_t.getBigDecimal("QTY");
        params.IT[i] = IT_r;
      }

      params.IT_FP = new ZTS_COMPL_FP_S[IT_FP_t.getNumRows()];
      ZTS_COMPL_FP_S IT_FP_r;
      for (int i = 0; i < params.IT_FP.length; i++) {
        IT_FP_t.setRow(i);
        IT_FP_r = new ZTS_COMPL_FP_S();
        IT_FP_r.LGPLA = IT_FP_t.getString("LGPLA");
        IT_FP_r.COMPL_FROM = IT_FP_t.getString("COMPL_FROM");
        params.IT_FP[i] = IT_FP_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
