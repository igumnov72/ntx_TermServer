package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение id рабочего места по id терминала
 */
public class Z_TS_WP_BY_ID {

  // importing params
  public String TERM_DT_ID = "0"; // Дата-идентификатор планшета
  //
  // exporting params
  public int WP_ID = 0; // Рабочее место (id)
  //
  // table params
  public ZTS_WOK_DATA_S[] IT_WOKS = new ZTS_WOK_DATA_S[0]; // Данные по рабочим смены
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

  public void IT_WOKS_create(int n) {
    IT_WOKS = new ZTS_WOK_DATA_S[n];
    for (int i = 0; i < n; i++) {
      IT_WOKS[i] = new ZTS_WOK_DATA_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_WP_BY_ID");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_WP_BY_ID:");
      System.out.println("  TERM_DT_ID=" + TERM_DT_ID);
      System.out.println("  IT_WOKS.length=" + IT_WOKS.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_WP_BY_ID:");
        System.out.println("  WP_ID=" + WP_ID);
        System.out.println("  err=" + err);
        System.out.println("  IT_WOKS.length=" + IT_WOKS.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_WP_BY_ID:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_WP_BY_ID: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_WP_BY_ID params) {
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

    JCoTable IT_WOKS_t = tabParams.getTable("IT_WOKS");

    impParams.setValue("TERM_DT_ID", params.TERM_DT_ID);

    IT_WOKS_t.appendRows(params.IT_WOKS.length);
    for (int i = 0; i < params.IT_WOKS.length; i++) {
      IT_WOKS_t.setRow(i);
      IT_WOKS_t.setValue("SOTR", params.IT_WOKS[i].SOTR);
      IT_WOKS_t.setValue("DOLGH", params.IT_WOKS[i].DOLGH);
      IT_WOKS_t.setValue("SOVM", params.IT_WOKS[i].SOVM);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.WP_ID = expParams.getInt("WP_ID");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_WOKS = new ZTS_WOK_DATA_S[IT_WOKS_t.getNumRows()];
      ZTS_WOK_DATA_S IT_WOKS_r;
      for (int i = 0; i < params.IT_WOKS.length; i++) {
        IT_WOKS_t.setRow(i);
        IT_WOKS_r = new ZTS_WOK_DATA_S();
        IT_WOKS_r.SOTR = IT_WOKS_t.getString("SOTR");
        IT_WOKS_r.DOLGH = IT_WOKS_t.getString("DOLGH");
        IT_WOKS_r.SOVM = IT_WOKS_t.getInt("SOVM");
        params.IT_WOKS[i] = IT_WOKS_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_WP_BY_ID");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_WP_BY_ID not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
