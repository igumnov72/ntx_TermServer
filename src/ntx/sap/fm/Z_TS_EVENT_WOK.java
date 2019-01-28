package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Регистрация рабочего смены
 */
public class Z_TS_EVENT_WOK {

  // importing params
  public int WP_ID = 0; // Рабочее место (id)
  public String SOTR = ""; // Сотрудник
  //
  // table params
  public ZTS_WOK_DATA1_S[] IT_WOK = new ZTS_WOK_DATA1_S[0]; // Данные по рабочему смены
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

  public void IT_WOK_create(int n) {
    IT_WOK = new ZTS_WOK_DATA1_S[n];
    for (int i = 0; i < n; i++) {
      IT_WOK[i] = new ZTS_WOK_DATA1_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_EVENT_WOK");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_EVENT_WOK:");
      System.out.println("  WP_ID=" + WP_ID);
      System.out.println("  SOTR=" + SOTR);
      System.out.println("  IT_WOK.length=" + IT_WOK.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_EVENT_WOK:");
        System.out.println("  err=" + err);
        System.out.println("  IT_WOK.length=" + IT_WOK.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_EVENT_WOK:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_EVENT_WOK: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_EVENT_WOK params) {
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

    JCoTable IT_WOK_t = tabParams.getTable("IT_WOK");

    impParams.setValue("WP_ID", params.WP_ID);
    impParams.setValue("SOTR", params.SOTR);

    IT_WOK_t.appendRows(params.IT_WOK.length);
    for (int i = 0; i < params.IT_WOK.length; i++) {
      IT_WOK_t.setRow(i);
      IT_WOK_t.setValue("DOLGH_ID", params.IT_WOK[i].DOLGH_ID);
      IT_WOK_t.setValue("SOVM", params.IT_WOK[i].SOVM);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_WOK = new ZTS_WOK_DATA1_S[IT_WOK_t.getNumRows()];
      ZTS_WOK_DATA1_S IT_WOK_r;
      for (int i = 0; i < params.IT_WOK.length; i++) {
        IT_WOK_t.setRow(i);
        IT_WOK_r = new ZTS_WOK_DATA1_S();
        IT_WOK_r.DOLGH_ID = IT_WOK_t.getInt("DOLGH_ID");
        IT_WOK_r.SOVM = IT_WOK_t.getInt("SOVM");
        params.IT_WOK[i] = IT_WOK_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_EVENT_WOK");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_EVENT_WOK not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
