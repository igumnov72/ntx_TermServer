package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение данных по приходу с пр-ва
 */
public class Z_TS_PROD3 {

  // importing params
  public String LGORT = ""; // Склад
  public String EBELNS = ""; // Номера подходящих заказов на поставку
  public String PAL = ""; // № единицы складирования
  public String LGPLA = ""; // Складское место
  public String USER_CODE = ""; // Штрих-код
  //
  // exporting params
  public String J_CREATED = ""; // Признак создания записи в журнале
  public String VBELN = ""; // Номер документа сбыта
  //
  // table params
  public ZTS_PRT_QTY_S[] IT_TOV = new ZTS_PRT_QTY_S[0]; // Кол-во по партии
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
    IT_TOV = new ZTS_PRT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_TOV[i] = new ZTS_PRT_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PROD3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PROD3:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  EBELNS=" + EBELNS);
      System.out.println("  PAL=" + PAL);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  USER_CODE=" + USER_CODE);
      System.out.println("  IT_TOV.length=" + IT_TOV.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PROD3:");
        System.out.println("  J_CREATED=" + J_CREATED);
        System.out.println("  VBELN=" + VBELN);
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

      System.err.println("Error calling SAP procedure Z_TS_PROD3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PROD3: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PROD3 params) {
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

    JCoTable IT_TOV_t = tabParams.getTable("IT_TOV");

    impParams.setValue("LGORT", params.LGORT);
    impParams.setValue("EBELNS", params.EBELNS);
    impParams.setValue("PAL", params.PAL);
    impParams.setValue("LGPLA", params.LGPLA);
    impParams.setValue("USER_CODE", params.USER_CODE);

    IT_TOV_t.appendRows(params.IT_TOV.length);
    for (int i = 0; i < params.IT_TOV.length; i++) {
      IT_TOV_t.setRow(i);
      IT_TOV_t.setValue("CHARG", params.IT_TOV[i].CHARG);
      IT_TOV_t.setValue("QTY", params.IT_TOV[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.J_CREATED = expParams.getString("J_CREATED");
      params.VBELN = expParams.getString("VBELN");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_TOV = new ZTS_PRT_QTY_S[IT_TOV_t.getNumRows()];
      ZTS_PRT_QTY_S IT_TOV_r;
      for (int i = 0; i < params.IT_TOV.length; i++) {
        IT_TOV_t.setRow(i);
        IT_TOV_r = new ZTS_PRT_QTY_S();
        IT_TOV_r.CHARG = IT_TOV_t.getString("CHARG");
        IT_TOV_r.QTY = IT_TOV_t.getBigDecimal("QTY");
        params.IT_TOV[i] = IT_TOV_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PROD3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PROD3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
