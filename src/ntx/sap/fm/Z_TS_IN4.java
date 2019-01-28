package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных по паллете
 */
public class Z_TS_IN4 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public String PAL = ""; // № единицы складирования
  public String LGORT = ""; // Склад
  public String LGNUM = ""; // Номер склада/комплекс
  public String TSD_USER = ""; // Пользователь ТСД
  //
  // exporting params
  public String LGPLA = ""; // Складское место (если паллета размещена или в процессе)
  public String PREV_DELETED = ""; // Признак удаления предыдущей раскладки по паллете
  public ZTS_QTY_DIF_S QTY_DIF = new ZTS_QTY_DIF_S(); // Расхождения по кол-ву при приемке
  //
  // table params
  public ZTS_IN4_S[] IT_TOV = new ZTS_IN4_S[0]; // Товар на паллете
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

  public void IT_TOV_create(int n) {
    IT_TOV = new ZTS_IN4_S[n];
    for (int i = 0; i < n; i++) {
      IT_TOV[i] = new ZTS_IN4_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_IN4");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_IN4:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  PAL=" + PAL);
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  TSD_USER=" + TSD_USER);
      System.out.println("  IT_TOV.length=" + IT_TOV.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_IN4:");
        System.out.println("  LGPLA=" + LGPLA);
        System.out.println("  PREV_DELETED=" + PREV_DELETED);
        System.out.println("  QTY_DIF.QTY_VBEL=" + QTY_DIF.QTY_VBEL);
        System.out.println("  QTY_DIF.QTY_SCAN=" + QTY_DIF.QTY_SCAN);
        System.out.println("  QTY_DIF.QTY_NEDOST=" + QTY_DIF.QTY_NEDOST);
        System.out.println("  QTY_DIF.QTY_IZL=" + QTY_DIF.QTY_IZL);
        System.out.println("  QTY_DIF.QTY_PRT=" + QTY_DIF.QTY_PRT);
        System.out.println("  err=" + err);
        System.out.println("  IT_TOV.length=" + IT_TOV.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_IN4:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_IN4: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_IN4 params) {
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

    JCoStructure QTY_DIF_s = expParams.getStructure("QTY_DIF");
    JCoTable IT_TOV_t = tabParams.getTable("IT_TOV");

    impParams.setValue("VBELN", params.VBELN);
    impParams.setValue("PAL", params.PAL);
    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("LGNUM", params.LGNUM);
    impParams.setValue("TSD_USER", params.TSD_USER);

    IT_TOV_t.appendRows(params.IT_TOV.length);
    for (int i = 0; i < params.IT_TOV.length; i++) {
      IT_TOV_t.setRow(i);
      IT_TOV_t.setValue("MATNR", params.IT_TOV[i].MATNR);
      IT_TOV_t.setValue("CHARG", params.IT_TOV[i].CHARG);
      IT_TOV_t.setValue("PROD_DT", params.IT_TOV[i].PROD_DT);
      IT_TOV_t.setValue("QTY", params.IT_TOV[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.LGPLA = expParams.getString("LGPLA");
      params.PREV_DELETED = expParams.getString("PREV_DELETED");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }
      params.QTY_DIF.QTY_VBEL = QTY_DIF_s.getBigDecimal("QTY_VBEL");
      params.QTY_DIF.QTY_SCAN = QTY_DIF_s.getBigDecimal("QTY_SCAN");
      params.QTY_DIF.QTY_NEDOST = QTY_DIF_s.getBigDecimal("QTY_NEDOST");
      params.QTY_DIF.QTY_IZL = QTY_DIF_s.getBigDecimal("QTY_IZL");
      params.QTY_DIF.QTY_PRT = QTY_DIF_s.getBigDecimal("QTY_PRT");

      params.IT_TOV = new ZTS_IN4_S[IT_TOV_t.getNumRows()];
      ZTS_IN4_S IT_TOV_r;
      for (int i = 0; i < params.IT_TOV.length; i++) {
        IT_TOV_t.setRow(i);
        IT_TOV_r = new ZTS_IN4_S();
        IT_TOV_r.MATNR = IT_TOV_t.getString("MATNR");
        IT_TOV_r.CHARG = IT_TOV_t.getString("CHARG");
        IT_TOV_r.PROD_DT = IT_TOV_t.getString("PROD_DT");
        IT_TOV_r.QTY = IT_TOV_t.getBigDecimal("QTY");
        params.IT_TOV[i] = IT_TOV_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_IN4");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_IN4 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
