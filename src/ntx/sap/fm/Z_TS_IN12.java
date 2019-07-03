package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Расхождения по поставке
 */
public class Z_TS_IN12 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  //
  // exporting params
  public ZTS_QTY_DIF_S QTY_DIF = new ZTS_QTY_DIF_S(); // Расхождения по кол-ву при приемке
  //
  // table params
  public ZTS_IN12_S[] IT = new ZTS_IN12_S[0]; // Расхождения при приемке
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
    IT = new ZTS_IN12_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_IN12_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_IN12");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_IN12:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_IN12:");
        System.out.println("  QTY_DIF.QTY_VBEL=" + QTY_DIF.QTY_VBEL);
        System.out.println("  QTY_DIF.QTY_SCAN=" + QTY_DIF.QTY_SCAN);
        System.out.println("  QTY_DIF.QTY_NEDOST=" + QTY_DIF.QTY_NEDOST);
        System.out.println("  QTY_DIF.QTY_IZL=" + QTY_DIF.QTY_IZL);
        System.out.println("  QTY_DIF.QTY_PRT=" + QTY_DIF.QTY_PRT);
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_IN12:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_IN12: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_IN12 params) {
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

      JCoStructure QTY_DIF_s = expParams.getStructure("QTY_DIF");
      JCoTable IT_t = tabParams.getTable("IT");

      impParams.setValue("VBELN", params.VBELN);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("QTY_SCAN", params.IT[i].QTY_SCAN);
        IT_t.setValue("QTY_V", params.IT[i].QTY_V);
        IT_t.setValue("QTY_DIF", params.IT[i].QTY_DIF);
        IT_t.setValue("PALS", params.IT[i].PALS);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.QTY_DIF.QTY_VBEL = QTY_DIF_s.getBigDecimal("QTY_VBEL");
        params.QTY_DIF.QTY_SCAN = QTY_DIF_s.getBigDecimal("QTY_SCAN");
        params.QTY_DIF.QTY_NEDOST = QTY_DIF_s.getBigDecimal("QTY_NEDOST");
        params.QTY_DIF.QTY_IZL = QTY_DIF_s.getBigDecimal("QTY_IZL");
        params.QTY_DIF.QTY_PRT = QTY_DIF_s.getBigDecimal("QTY_PRT");

        params.IT = new ZTS_IN12_S[IT_t.getNumRows()];
        ZTS_IN12_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_IN12_S();
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.QTY_SCAN = IT_t.getBigDecimal("QTY_SCAN");
          IT_r.QTY_V = IT_t.getBigDecimal("QTY_V");
          IT_r.QTY_DIF = IT_t.getBigDecimal("QTY_DIF");
          IT_r.PALS = IT_t.getString("PALS");
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
      function = SAPconn.getFunction("Z_TS_IN12");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_IN12 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
