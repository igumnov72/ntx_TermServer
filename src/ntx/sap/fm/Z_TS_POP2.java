package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Ячейки, откуда пополнять
 */
public class Z_TS_POP2 {

  // importing params
  public String LGORT = ""; // Склад
  //
  // exporting params
  public String LGTYP1 = ""; // Тип склада
  public String LGTYP2 = ""; // Тип склада
  public String DT = ""; // Дата/время создания
  public String LGNUM = ""; // Номер склада/комплекс
  //
  // table params
  public ZTS_CELLS_S[] IT = new ZTS_CELLS_S[0]; // Ячейки
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
    IT = new ZTS_CELLS_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_CELLS_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_POP2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_POP2:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_POP2:");
        System.out.println("  LGTYP1=" + LGTYP1);
        System.out.println("  LGTYP2=" + LGTYP2);
        System.out.println("  DT=" + DT);
        System.out.println("  LGNUM=" + LGNUM);
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

      System.err.println("Error calling SAP procedure Z_TS_POP2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_POP2: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_POP2 params) {
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

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("LGTYP", params.IT[i].LGTYP);
      IT_t.setValue("LGPLA", params.IT[i].LGPLA);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.LGTYP1 = expParams.getString("LGTYP1");
      params.LGTYP2 = expParams.getString("LGTYP2");
      params.DT = expParams.getString("DT");
      params.LGNUM = expParams.getString("LGNUM");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT = new ZTS_CELLS_S[IT_t.getNumRows()];
      ZTS_CELLS_S IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new ZTS_CELLS_S();
        IT_r.LGTYP = IT_t.getString("LGTYP");
        IT_r.LGPLA = IT_t.getString("LGPLA");
        params.IT[i] = IT_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_POP2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_POP2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
