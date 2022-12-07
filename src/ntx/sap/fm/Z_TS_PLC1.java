package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Размещение - проверка паллеты
 */
public class Z_TS_PLC1 {

  // importing params
  public String PAL = ""; // Номер паллеты
  //
  // exporting params
  public String CAN_USE = ""; // Паллету можно размещать
  public String LGORT = ""; // Склад
  public String LENUM = ""; // № единицы складирования
  public String VBELN = ""; // Номер документа сбыта
  public String WERKS = ""; // Завод
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGPLA = ""; // Складское место
  public String MATNR = ""; // Номер материала
  public int NERAZM = 0; // Число неразмещенных паллет
  public String IS_PNP = ""; // Приемка по ПНП
  public String ASK_CNF_PM = ""; // требуется запрос создания ПМ (есть вход пост)
  public String ABC = ""; // Признак материала на складе ABC
  public String INF = "";
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
      System.out.println("Вызов ФМ Z_TS_PLC1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PLC1:");
      System.out.println("  PAL=" + PAL);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PLC1:");
        System.out.println("  CAN_USE=" + CAN_USE);
        System.out.println("  LGORT=" + LGORT);
        System.out.println("  LENUM=" + LENUM);
        System.out.println("  VBELN=" + VBELN);
        System.out.println("  WERKS=" + WERKS);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  LGPLA=" + LGPLA);
        System.out.println("  MATNR=" + MATNR);
        System.out.println("  NERAZM=" + NERAZM);
        System.out.println("  IS_PNP=" + IS_PNP);
        System.out.println("  ASK_CNF_PM=" + ASK_CNF_PM);
        System.out.println("  ABC=" + ABC);
        System.out.println("  INF=" + INF);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PLC1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PLC1: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_PLC1 params) {
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

      impParams.setValue("PAL", params.PAL);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.CAN_USE = expParams.getString("CAN_USE");
        params.LGORT = expParams.getString("LGORT");
        params.LENUM = expParams.getString("LENUM");
        params.VBELN = expParams.getString("VBELN");
        params.WERKS = expParams.getString("WERKS");
        params.LGNUM = expParams.getString("LGNUM");
        params.LGPLA = expParams.getString("LGPLA");
        params.MATNR = expParams.getString("MATNR");
        params.NERAZM = expParams.getInt("NERAZM");
        params.IS_PNP = expParams.getString("IS_PNP");
        params.ASK_CNF_PM = expParams.getString("ASK_CNF_PM");
        params.ABC = expParams.getString("ABC");
        params.INF = expParams.getString("INF");
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
      function = SAPconn.getFunction("Z_TS_PLC1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PLC1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
