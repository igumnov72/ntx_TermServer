package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Паллета-получатель
 */
public class Z_TS_SKL_MOVE5 {

  // importing params
  public String LGORT = ""; // Склад
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP1 = ""; // Тип склада
  public String CELL1 = ""; // Складское место
  public String PAL1 = ""; // № единицы складирования
  public String LGTYP2 = ""; // Тип склада
  public String CELL2 = ""; // Складское место
  public String PAL2 = ""; // № единицы складирования
  public String TSD_USER = ""; // Пользователь ТСД
  //
  // exporting params
  public int KEY1 = 0; // 1-й ключ таблицы
  public String TR_ZAK_CREATED = ""; // Признак созданного тр заказа
  //
  // table params
  public ZTS_PRT_QTY_S[] IT_CQ = new ZTS_PRT_QTY_S[0]; // Кол-во по партии
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

  public void IT_CQ_create(int n) {
    IT_CQ = new ZTS_PRT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_CQ[i] = new ZTS_PRT_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE5");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE5:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP1=" + LGTYP1);
      System.out.println("  CELL1=" + CELL1);
      System.out.println("  PAL1=" + PAL1);
      System.out.println("  LGTYP2=" + LGTYP2);
      System.out.println("  CELL2=" + CELL2);
      System.out.println("  PAL2=" + PAL2);
      System.out.println("  TSD_USER=" + TSD_USER);
      System.out.println("  IT_CQ.length=" + IT_CQ.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKL_MOVE5:");
        System.out.println("  KEY1=" + KEY1);
        System.out.println("  TR_ZAK_CREATED=" + TR_ZAK_CREATED);
        System.out.println("  err=" + err);
        System.out.println("  IT_CQ.length=" + IT_CQ.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SKL_MOVE5:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKL_MOVE5: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_SKL_MOVE5 params) {
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

    JCoTable IT_CQ_t = tabParams.getTable("IT_CQ");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("LGNUM", params.LGNUM);
    impParams.setValue("LGTYP1", params.LGTYP1);
    impParams.setValue("CELL1", params.CELL1);
    impParams.setValue("PAL1", params.PAL1);
    impParams.setValue("LGTYP2", params.LGTYP2);
    impParams.setValue("CELL2", params.CELL2);
    impParams.setValue("PAL2", params.PAL2);
    impParams.setValue("TSD_USER", params.TSD_USER);

    IT_CQ_t.appendRows(params.IT_CQ.length);
    for (int i = 0; i < params.IT_CQ.length; i++) {
      IT_CQ_t.setRow(i);
      IT_CQ_t.setValue("CHARG", params.IT_CQ[i].CHARG);
      IT_CQ_t.setValue("QTY", params.IT_CQ[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.KEY1 = expParams.getInt("KEY1");
      params.TR_ZAK_CREATED = expParams.getString("TR_ZAK_CREATED");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_CQ = new ZTS_PRT_QTY_S[IT_CQ_t.getNumRows()];
      ZTS_PRT_QTY_S IT_CQ_r;
      for (int i = 0; i < params.IT_CQ.length; i++) {
        IT_CQ_t.setRow(i);
        IT_CQ_r = new ZTS_PRT_QTY_S();
        IT_CQ_r.CHARG = IT_CQ_t.getString("CHARG");
        IT_CQ_r.QTY = IT_CQ_t.getBigDecimal("QTY");
        params.IT_CQ[i] = IT_CQ_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_SKL_MOVE5");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKL_MOVE5 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
