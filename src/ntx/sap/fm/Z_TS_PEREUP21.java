package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;
import ntx.sap.struct.*;

/**
 * "Из переупаковки" - отсканирована исходная паллета
 */
public class Z_TS_PEREUP21 {

  // importing params
  public String PAL = ""; // № единицы складирования
  //
  // exporting params
  public BigDecimal QTY = new BigDecimal(0); // Количество (неразмещенное)
  public int ID = 0; // Идентификатор
  public String LGORT2 = ""; // Склад принимающий
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  //
  // table params
  public ZTS_MP_QTY_S[] IT = new ZTS_MP_QTY_S[0]; // Неразмещенный товар
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
    IT = new ZTS_MP_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_MP_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PEREUP21");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PEREUP21:");
      System.out.println("  PAL=" + PAL);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PEREUP21:");
        System.out.println("  QTY=" + QTY);
        System.out.println("  ID=" + ID);
        System.out.println("  LGORT2=" + LGORT2);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  LGTYP=" + LGTYP);
        System.out.println("  LGPLA=" + LGPLA);
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

      System.err.println("Error calling SAP procedure Z_TS_PEREUP21:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PEREUP21: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_PEREUP21 params) {
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

      impParams.setValue("PAL", params.PAL);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("QTY", params.IT[i].QTY);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.QTY = expParams.getBigDecimal("QTY");
        params.ID = expParams.getInt("ID");
        params.LGORT2 = expParams.getString("LGORT2");
        params.LGNUM = expParams.getString("LGNUM");
        params.LGTYP = expParams.getString("LGTYP");
        params.LGPLA = expParams.getString("LGPLA");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_MP_QTY_S[IT_t.getNumRows()];
        ZTS_MP_QTY_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_MP_QTY_S();
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
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
      function = SAPconn.getFunction("Z_TS_PEREUP21");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PEREUP21 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
