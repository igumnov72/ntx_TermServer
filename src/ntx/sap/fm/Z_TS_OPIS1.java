package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных по описи паллеты
 */
public class Z_TS_OPIS1 {

  // importing params
  public String LGORT = ""; // Склад
  public String USER_SHK = ""; // ШК кладовщика
  public String LENUM = ""; // № единицы складирования
  //
  // exporting params
  public String IS_NEW = ""; // Новое сопоставление (паллеты не было)
  //
  // table params
  public ZTS_SGM_S[] IT = new ZTS_SGM_S[0]; // Список СГМ
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
    IT = new ZTS_SGM_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_SGM_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_OPIS1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_OPIS1:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  LENUM=" + LENUM);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_OPIS1:");
        System.out.println("  IS_NEW=" + IS_NEW);
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

      System.err.println("Error calling SAP procedure Z_TS_OPIS1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_OPIS1: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_OPIS1 params) {
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

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("USER_SHK", params.USER_SHK);
    impParams.setValue("LENUM", params.LENUM);

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("SGM", params.IT[i].SGM);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.IS_NEW = expParams.getString("IS_NEW");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT = new ZTS_SGM_S[IT_t.getNumRows()];
      ZTS_SGM_S IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new ZTS_SGM_S();
        IT_r.SGM = IT_t.getInt("SGM");
        params.IT[i] = IT_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_OPIS1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_OPIS1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
