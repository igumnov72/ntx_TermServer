package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Проверка скана
 */
public class Z_TS_OPISK4 {

  // importing params
  public String SHK = ""; // Штрих-код
  public String VBELN = ""; // Номер документа сбыта
  //
  // exporting params
  public String INF = "";
  public String MATNR = "";
  public String CHARG = "";
  public BigDecimal QTY = new BigDecimal(0);
  public BigDecimal QTY2 = new BigDecimal(0);
  public String INF2 = "";
  public BigDecimal QTY3 = new BigDecimal(0);
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
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_OPISK4");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_OPISK4:");
      System.out.println("  SHK=" + SHK);
      System.out.println("  VBELN=" + VBELN);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_OPISK4:");
        System.out.println("  INF=" + INF);
        System.out.println("  MATNR=" + MATNR);
        System.out.println("  CHARG=" + CHARG);
        System.out.println("  QTY=" + QTY);
        System.out.println("  QTY2=" + QTY2);
        System.out.println("  INF2=" + INF2);
        System.out.println("  QTY3=" + QTY3);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_OPISK4:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_OPISK4: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_OPISK4 params) {
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

      impParams.setValue("SHK", params.SHK);
      impParams.setValue("VBELN", params.VBELN);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INF = expParams.getString("INF");
        params.MATNR = expParams.getString("MATNR");
        params.CHARG = expParams.getString("CHARG");
        params.QTY = expParams.getBigDecimal("QTY");
        params.QTY2 = expParams.getBigDecimal("QTY2");
        params.INF2 = expParams.getString("INF2");
        params.QTY3 = expParams.getBigDecimal("QTY3");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_OPISK4");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_OPISK4 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
