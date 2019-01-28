package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Закрытие инв
 */
public class Z_TS_INV3 {

  // importing params
  public String WERKS = ""; // Завод
  public String LGORT = ""; // Склад
  public String LGNUM = ""; // Номер склада/комплекс
  public String IVNUM = ""; // Номер инвентаризационной описи
  public int INV_ID = 0; // ID
  public String TSD_USER = ""; // Пользователь ТСД
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  //
  // exporting params
  public String NOT_CLOSED = ""; // Подсчет сохранен, но разницы не списаны
  //
  // table params
  public ZTS_INV_QTY_S[] IT_INV = new ZTS_INV_QTY_S[0]; // Данные подсчета инвентаризации
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

  public void IT_INV_create(int n) {
    IT_INV = new ZTS_INV_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_INV[i] = new ZTS_INV_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_INV3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_INV3:");
      System.out.println("  WERKS=" + WERKS);
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  IVNUM=" + IVNUM);
      System.out.println("  INV_ID=" + INV_ID);
      System.out.println("  TSD_USER=" + TSD_USER);
      System.out.println("  LGTYP=" + LGTYP);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  IT_INV.length=" + IT_INV.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_INV3:");
        System.out.println("  NOT_CLOSED=" + NOT_CLOSED);
        System.out.println("  err=" + err);
        System.out.println("  IT_INV.length=" + IT_INV.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_INV3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_INV3: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_INV3 params) {
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

    JCoTable IT_INV_t = tabParams.getTable("IT_INV");

    impParams.setValue("WERKS", params.WERKS);
    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("LGNUM", params.LGNUM);
    impParams.setValue("IVNUM", params.IVNUM);
    impParams.setValue("INV_ID", params.INV_ID);
    impParams.setValue("TSD_USER", params.TSD_USER);
    impParams.setValue("LGTYP", params.LGTYP);
    impParams.setValue("LGPLA", params.LGPLA);

    IT_INV_t.appendRows(params.IT_INV.length);
    for (int i = 0; i < params.IT_INV.length; i++) {
      IT_INV_t.setRow(i);
      IT_INV_t.setValue("LENUM", params.IT_INV[i].LENUM);
      IT_INV_t.setValue("MATNR", params.IT_INV[i].MATNR);
      IT_INV_t.setValue("CHARG", params.IT_INV[i].CHARG);
      IT_INV_t.setValue("QTY", params.IT_INV[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.NOT_CLOSED = expParams.getString("NOT_CLOSED");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_INV = new ZTS_INV_QTY_S[IT_INV_t.getNumRows()];
      ZTS_INV_QTY_S IT_INV_r;
      for (int i = 0; i < params.IT_INV.length; i++) {
        IT_INV_t.setRow(i);
        IT_INV_r = new ZTS_INV_QTY_S();
        IT_INV_r.LENUM = IT_INV_t.getString("LENUM");
        IT_INV_r.MATNR = IT_INV_t.getString("MATNR");
        IT_INV_r.CHARG = IT_INV_t.getString("CHARG");
        IT_INV_r.QTY = IT_INV_t.getBigDecimal("QTY");
        params.IT_INV[i] = IT_INV_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_INV3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_INV3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
