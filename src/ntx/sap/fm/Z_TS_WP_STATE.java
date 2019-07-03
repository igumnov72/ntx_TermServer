package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение состояния рабочего места
 */
public class Z_TS_WP_STATE {

  // importing params
  public int WP_ID = 0; // Рабочее место (id)
  //
  // exporting params
  public String WP_NAME = ""; // Название рабочего места
  public int CEH_ID = 0; // Цех (id)
  public String CEH_NAME = ""; // Название цеха
  public String EV_SME = ""; // Начало/конец смены (событие SME)
  public String DT_SME = ""; // Дата/время
  public String WP_TYP = ""; // Тип рабочего места
  public String LAST_IP = ""; // ip-адрес
  public String LAST_DT_ID = "0"; // Дата-идентификатор планшета
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
      System.out.println("Вызов ФМ Z_TS_WP_STATE");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_WP_STATE:");
      System.out.println("  WP_ID=" + WP_ID);
      System.out.println("  IT_WOKS.length=" + IT_WOKS.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_WP_STATE:");
        System.out.println("  WP_NAME=" + WP_NAME);
        System.out.println("  CEH_ID=" + CEH_ID);
        System.out.println("  CEH_NAME=" + CEH_NAME);
        System.out.println("  EV_SME=" + EV_SME);
        System.out.println("  DT_SME=" + DT_SME);
        System.out.println("  WP_TYP=" + WP_TYP);
        System.out.println("  LAST_IP=" + LAST_IP);
        System.out.println("  LAST_DT_ID=" + LAST_DT_ID);
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

      System.err.println("Error calling SAP procedure Z_TS_WP_STATE:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_WP_STATE: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_WP_STATE params) {
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

      JCoTable IT_WOKS_t = tabParams.getTable("IT_WOKS");

      impParams.setValue("WP_ID", params.WP_ID);

      IT_WOKS_t.appendRows(params.IT_WOKS.length);
      for (int i = 0; i < params.IT_WOKS.length; i++) {
        IT_WOKS_t.setRow(i);
        IT_WOKS_t.setValue("SOTR", params.IT_WOKS[i].SOTR);
        IT_WOKS_t.setValue("DOLGH", params.IT_WOKS[i].DOLGH);
        IT_WOKS_t.setValue("SOVM", params.IT_WOKS[i].SOVM);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.WP_NAME = expParams.getString("WP_NAME");
        params.CEH_ID = expParams.getInt("CEH_ID");
        params.CEH_NAME = expParams.getString("CEH_NAME");
        params.EV_SME = expParams.getString("EV_SME");
        params.DT_SME = expParams.getString("DT_SME");
        params.WP_TYP = expParams.getString("WP_TYP");
        params.LAST_IP = expParams.getString("LAST_IP");
        params.LAST_DT_ID = expParams.getString("LAST_DT_ID");
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
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_WP_STATE");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_WP_STATE not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
