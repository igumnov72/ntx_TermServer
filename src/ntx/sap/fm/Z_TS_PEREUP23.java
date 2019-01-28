package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;
import ntx.sap.struct.*;

/**
 * "Из переупаковки" - отсканирована ячейка
 */
public class Z_TS_PEREUP23 {

  // importing params
  public String PAL1 = ""; // исходная паллета
  public String PAL2 = ""; // новая паллета
  public String CELL = ""; // Складское место
  public String MERGE_CNF = ""; // Наличие подтверждения объединения товара
  public String TSD_USER = ""; // Пользователь ТСД
  //
  // exporting params
  public String NEED_MERGE_CNF = ""; // Признак необходимости подтверждения объединения товара
  public BigDecimal QTY0 = new BigDecimal(0); // Кол-во в ячейке
  public String LGORT2 = ""; // Склад принимающий
  public String ADD_MSG = ""; // Дополнительное сообщение
  public String ADD_ERR = ""; // Дополнительная ошибка
  //
  // table params
  public ZTS_MP_QTY_S[] IT = new ZTS_MP_QTY_S[0]; // Отсканированный товар
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
      System.out.println("Вызов ФМ Z_TS_PEREUP23");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PEREUP23:");
      System.out.println("  PAL1=" + PAL1);
      System.out.println("  PAL2=" + PAL2);
      System.out.println("  CELL=" + CELL);
      System.out.println("  MERGE_CNF=" + MERGE_CNF);
      System.out.println("  TSD_USER=" + TSD_USER);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PEREUP23:");
        System.out.println("  NEED_MERGE_CNF=" + NEED_MERGE_CNF);
        System.out.println("  QTY0=" + QTY0);
        System.out.println("  LGORT2=" + LGORT2);
        System.out.println("  ADD_MSG=" + ADD_MSG);
        System.out.println("  ADD_ERR=" + ADD_ERR);
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

      System.err.println("Error calling SAP procedure Z_TS_PEREUP23:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PEREUP23: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PEREUP23 params) {
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

    impParams.setValue("PAL1", params.PAL1);
    impParams.setValue("PAL2", params.PAL2);
    impParams.setValue("CELL", params.CELL);
    impParams.setValue("MERGE_CNF", params.MERGE_CNF);
    impParams.setValue("TSD_USER", params.TSD_USER);

    IT_t.appendRows(params.IT.length);
    for (int i = 0; i < params.IT.length; i++) {
      IT_t.setRow(i);
      IT_t.setValue("MATNR", params.IT[i].MATNR);
      IT_t.setValue("CHARG", params.IT[i].CHARG);
      IT_t.setValue("QTY", params.IT[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.NEED_MERGE_CNF = expParams.getString("NEED_MERGE_CNF");
      params.QTY0 = expParams.getBigDecimal("QTY0");
      params.LGORT2 = expParams.getString("LGORT2");
      params.ADD_MSG = expParams.getString("ADD_MSG");
      params.ADD_ERR = expParams.getString("ADD_ERR");
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

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PEREUP23");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PEREUP23 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
