package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение названий принтеров
 */
public class Z_TS_PRINTERS {

  // importing params
  public String PRINTERS = ""; // Список принтеров через запятую
  //
  // table params
  public ZTS_PRN_S[] IT = new ZTS_PRN_S[0]; // Названия принтеров
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

  public void IT_create(int n) {
    IT = new ZTS_PRN_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_PRN_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PRINTERS");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PRINTERS:");
      System.out.println("  PRINTERS=" + PRINTERS);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PRINTERS:");
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PRINTERS:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PRINTERS: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PRINTERS params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    tabParams.clear();

    JCoTable IT_t = tabParams.getTable("IT");

    impParams.setValue("PRINTERS", params.PRINTERS);

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("PADEST", params.IT[i].PADEST);
      IT_t.setValue("PASTANDORT", params.IT[i].PASTANDORT);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.IT = new ZTS_PRN_S[IT_t.getNumRows()];
      ZTS_PRN_S IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new ZTS_PRN_S();
        IT_r.PADEST = IT_t.getString("PADEST");
        IT_r.PASTANDORT = IT_t.getString("PASTANDORT");
        params.IT[i] = IT_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PRINTERS");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PRINTERS not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
