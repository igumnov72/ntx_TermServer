package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных свободной комплектации
 */
public class Z_TS_COMPL10 {

  // importing params
  public String LGORT = ""; // Склад
  public String VBELN = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  public String USER_SHK = ""; // ШК кладовщика
  public String COMPL_FROM = ""; // Комплектация с паллеты
  //
  // table params
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
      System.out.println("Вызов ФМ Z_TS_COMPL10");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL10:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  COMPL_FROM=" + COMPL_FROM);
      System.out.println("  IT_DONE.length=" + IT_DONE.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL10:");
        System.out.println("  err=" + err);
        System.out.println("  IT_DONE.length=" + IT_DONE.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL10:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL10: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL10 params) {
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

    JCoTable IT_DONE_t = tabParams.getTable("IT_DONE");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("VBELN", params.VBELN);
    impParams.setValue("LGPLA", params.LGPLA);
    impParams.setValue("USER_SHK", params.USER_SHK);
    impParams.setValue("COMPL_FROM", params.COMPL_FROM);

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
      function = SAPconn.getFunction("Z_TS_COMPL10");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL10 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
