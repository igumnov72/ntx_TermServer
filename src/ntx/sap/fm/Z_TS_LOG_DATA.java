package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Обработка log-данных с терминала
 */
public class Z_TS_LOG_DATA {

  // importing params
  public String SRV_NAME = ""; // Имя сервера
  public String DO_RECALC = ""; // Признак полного пересчета
  //
  // exporting params
  public String MAX_DT = ""; // дата/время последней записи
  //
  // table params
  public ZTS_LOG_S[] IT = new ZTS_LOG_S[0]; // Структура log-данных (с терминала)
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void IT_create(int n) {
    IT = new ZTS_LOG_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_LOG_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_LOG_DATA");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_LOG_DATA:");
      System.out.println("  SRV_NAME=" + SRV_NAME);
      System.out.println("  DO_RECALC=" + DO_RECALC);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_LOG_DATA:");
        System.out.println("  MAX_DT=" + MAX_DT);
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_LOG_DATA:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_LOG_DATA: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_LOG_DATA params) {
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

      impParams.setValue("SRV_NAME", params.SRV_NAME);
      impParams.setValue("DO_RECALC", params.DO_RECALC);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("LOG_TYP", params.IT[i].LOG_TYP);
        IT_t.setValue("LOG_DT", params.IT[i].LOG_DT);
        IT_t.setValue("TASK_ID", params.IT[i].TASK_ID);
        IT_t.setValue("PTYP", params.IT[i].PTYP);
        IT_t.setValue("SOTR", params.IT[i].SOTR);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("QTY", params.IT[i].QTY);
        IT_t.setValue("LENUM", params.IT[i].LENUM);
        IT_t.setValue("VBELN", params.IT[i].VBELN);
        IT_t.setValue("LGPLA", params.IT[i].LGPLA);
        IT_t.setValue("LGORT", params.IT[i].LGORT);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.MAX_DT = expParams.getString("MAX_DT");

        params.IT = new ZTS_LOG_S[IT_t.getNumRows()];
        ZTS_LOG_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_LOG_S();
          IT_r.LOG_TYP = IT_t.getString("LOG_TYP");
          IT_r.LOG_DT = IT_t.getString("LOG_DT");
          IT_r.TASK_ID = IT_t.getString("TASK_ID");
          IT_r.PTYP = IT_t.getString("PTYP");
          IT_r.SOTR = IT_t.getString("SOTR");
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          IT_r.LENUM = IT_t.getString("LENUM");
          IT_r.VBELN = IT_t.getString("VBELN");
          IT_r.LGPLA = IT_t.getString("LGPLA");
          IT_r.LGORT = IT_t.getString("LGORT");
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
      function = SAPconn.getFunction("Z_TS_LOG_DATA");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_LOG_DATA not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
