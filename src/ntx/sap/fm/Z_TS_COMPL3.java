package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных комплектации
 */
public class Z_TS_COMPL3 {

  // importing params
  public String LGORT = ""; // Склад
  public String VBELN = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  public String USER_SHK = ""; // ШК кладовщика
  public String INF_COMPL = ""; // Признак информационной комплектации
  public String COMPL_FROM = ""; // Комплектация с паллеты
  public String TANUM1 = "0"; // MIN Номер  транспортного заказа
  public String TANUM2 = "0"; // MAX Номер транспортного заказа
  //
  // table params
  public ZTS_COMPL_S[] IT1 = new ZTS_COMPL_S[0]; // Исходные данные ведомости на комплектацию
  public ZTS_COMPL_DONE_S[] IT_DONE = new ZTS_COMPL_DONE_S[0]; // Скомплектованный товар
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

  public void IT1_create(int n) {
    IT1 = new ZTS_COMPL_S[n];
    for (int i = 0; i < n; i++) {
      IT1[i] = new ZTS_COMPL_S();
    }
  }

  public void IT_DONE_create(int n) {
    IT_DONE = new ZTS_COMPL_DONE_S[n];
    for (int i = 0; i < n; i++) {
      IT_DONE[i] = new ZTS_COMPL_DONE_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL3:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  INF_COMPL=" + INF_COMPL);
      System.out.println("  COMPL_FROM=" + COMPL_FROM);
      System.out.println("  TANUM1=" + TANUM1);
      System.out.println("  TANUM2=" + TANUM2);
      System.out.println("  IT1.length=" + IT1.length);
      System.out.println("  IT_DONE.length=" + IT_DONE.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL3:");
        System.out.println("  err=" + err);
        System.out.println("  IT1.length=" + IT1.length);
        System.out.println("  IT_DONE.length=" + IT_DONE.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL3: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL3 params) {
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

    JCoTable IT1_t = tabParams.getTable("IT1");
    JCoTable IT_DONE_t = tabParams.getTable("IT_DONE");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("VBELN", params.VBELN);
    impParams.setValue("LGPLA", params.LGPLA);
    impParams.setValue("USER_SHK", params.USER_SHK);
    impParams.setValue("INF_COMPL", params.INF_COMPL);
    impParams.setValue("COMPL_FROM", params.COMPL_FROM);
    impParams.setValue("TANUM1", params.TANUM1);
    impParams.setValue("TANUM2", params.TANUM2);

    IT1_t.appendRows(params.IT1.length);
    for (int i = 0; i < params.IT1.length; i++) {
      IT1_t.setRow(i);
      IT1_t.setValue("LGNUM", params.IT1[i].LGNUM);
      IT1_t.setValue("TANUM", params.IT1[i].TANUM);
      IT1_t.setValue("TAPOS", params.IT1[i].TAPOS);
      IT1_t.setValue("LGTYP", params.IT1[i].LGTYP);
      IT1_t.setValue("LGPLA", params.IT1[i].LGPLA);
      IT1_t.setValue("LENUM", params.IT1[i].LENUM);
      IT1_t.setValue("MATNR", params.IT1[i].MATNR);
      IT1_t.setValue("CHARG", params.IT1[i].CHARG);
      IT1_t.setValue("ONLY_PRT", params.IT1[i].ONLY_PRT);
      IT1_t.setValue("SOBKZ", params.IT1[i].SOBKZ);
      IT1_t.setValue("SONUM", params.IT1[i].SONUM);
      IT1_t.setValue("QTY", params.IT1[i].QTY);
    }

    IT_DONE_t.appendRows(params.IT_DONE.length);
    for (int i = 0; i < params.IT_DONE.length; i++) {
      IT_DONE_t.setRow(i);
      IT_DONE_t.setValue("SGM", params.IT_DONE[i].SGM);
      IT_DONE_t.setValue("LENUM", params.IT_DONE[i].LENUM);
      IT_DONE_t.setValue("MATNR", params.IT_DONE[i].MATNR);
      IT_DONE_t.setValue("CHARG", params.IT_DONE[i].CHARG);
      IT_DONE_t.setValue("QTY", params.IT_DONE[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT1 = new ZTS_COMPL_S[IT1_t.getNumRows()];
      ZTS_COMPL_S IT1_r;
      for (int i = 0; i < params.IT1.length; i++) {
        IT1_t.setRow(i);
        IT1_r = new ZTS_COMPL_S();
        IT1_r.LGNUM = IT1_t.getString("LGNUM");
        IT1_r.TANUM = IT1_t.getString("TANUM");
        IT1_r.TAPOS = IT1_t.getString("TAPOS");
        IT1_r.LGTYP = IT1_t.getString("LGTYP");
        IT1_r.LGPLA = IT1_t.getString("LGPLA");
        IT1_r.LENUM = IT1_t.getString("LENUM");
        IT1_r.MATNR = IT1_t.getString("MATNR");
        IT1_r.CHARG = IT1_t.getString("CHARG");
        IT1_r.ONLY_PRT = IT1_t.getString("ONLY_PRT");
        IT1_r.SOBKZ = IT1_t.getString("SOBKZ");
        IT1_r.SONUM = IT1_t.getString("SONUM");
        IT1_r.QTY = IT1_t.getBigDecimal("QTY");
        params.IT1[i] = IT1_r;
      }

      params.IT_DONE = new ZTS_COMPL_DONE_S[IT_DONE_t.getNumRows()];
      ZTS_COMPL_DONE_S IT_DONE_r;
      for (int i = 0; i < params.IT_DONE.length; i++) {
        IT_DONE_t.setRow(i);
        IT_DONE_r = new ZTS_COMPL_DONE_S();
        IT_DONE_r.SGM = IT_DONE_t.getInt("SGM");
        IT_DONE_r.LENUM = IT_DONE_t.getString("LENUM");
        IT_DONE_r.MATNR = IT_DONE_t.getString("MATNR");
        IT_DONE_r.CHARG = IT_DONE_t.getString("CHARG");
        IT_DONE_r.QTY = IT_DONE_t.getBigDecimal("QTY");
        params.IT_DONE[i] = IT_DONE_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
