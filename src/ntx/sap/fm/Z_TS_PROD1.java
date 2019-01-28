package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Сканирование партии при приемке из пр-ва
 */
public class Z_TS_PROD1 {

  // importing params
  public String LGORT = ""; // Склад
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY_TOT = new BigDecimal(0); // Общее количество партии
  public String EBELNS = ""; // Список заказов на поставку или "-"
  //
  // exporting params
  public String EBELNS2 = ""; // Список заказов на поставку
  public BigDecimal QTY_MAX = new BigDecimal(0); // Максимальное кол-во (мин по всем заказам)
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
      System.out.println("Вызов ФМ Z_TS_PROD1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PROD1:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  CHARG=" + CHARG);
      System.out.println("  QTY_TOT=" + QTY_TOT);
      System.out.println("  EBELNS=" + EBELNS);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PROD1:");
        System.out.println("  EBELNS2=" + EBELNS2);
        System.out.println("  QTY_MAX=" + QTY_MAX);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PROD1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PROD1: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PROD1 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("CHARG", params.CHARG);
    impParams.setValue("QTY_TOT", params.QTY_TOT);
    impParams.setValue("EBELNS", params.EBELNS);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.EBELNS2 = expParams.getString("EBELNS2");
      params.QTY_MAX = expParams.getBigDecimal("QTY_MAX");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PROD1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PROD1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
