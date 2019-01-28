package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранение содержимого СГМ при комплектации (при изменении СГМ)
 */
public class Z_TS_COMPL19 {

  // importing params
  public int SGM = 0; // Номер СГМ
  //
  // exporting params
  public String MSG = ""; // Сообщение
  //
  // table params
  public ZTS_VBELN_S[] IT_V = new ZTS_VBELN_S[0]; // Номера поставок
  public ZTS_MP_QTY_S[] IT_TOV = new ZTS_MP_QTY_S[0]; // Кол-во по материалу и партии
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

  public void IT_V_create(int n) {
    IT_V = new ZTS_VBELN_S[n];
    for (int i = 0; i < n; i++) {
      IT_V[i] = new ZTS_VBELN_S();
    }
  }

  public void IT_TOV_create(int n) {
    IT_TOV = new ZTS_MP_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_TOV[i] = new ZTS_MP_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL19");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL19:");
      System.out.println("  SGM=" + SGM);
      System.out.println("  IT_V.length=" + IT_V.length);
      System.out.println("  IT_TOV.length=" + IT_TOV.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL19:");
        System.out.println("  MSG=" + MSG);
        System.out.println("  err=" + err);
        System.out.println("  IT_V.length=" + IT_V.length);
        System.out.println("  IT_TOV.length=" + IT_TOV.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL19:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL19: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL19 params) {
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

    JCoTable IT_V_t = tabParams.getTable("IT_V");
    JCoTable IT_TOV_t = tabParams.getTable("IT_TOV");

    impParams.setValue("SGM", params.SGM);

    IT_V_t.appendRows(params.IT_V.length);
    for (int i = 0; i < params.IT_V.length; i++) {
      IT_V_t.setRow(i);
      IT_V_t.setValue("VBELN", params.IT_V[i].VBELN);
    }

    IT_TOV_t.appendRows(params.IT_TOV.length);
    for (int i = 0; i < params.IT_TOV.length; i++) {
      IT_TOV_t.setRow(i);
      IT_TOV_t.setValue("MATNR", params.IT_TOV[i].MATNR);
      IT_TOV_t.setValue("CHARG", params.IT_TOV[i].CHARG);
      IT_TOV_t.setValue("QTY", params.IT_TOV[i].QTY);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.MSG = expParams.getString("MSG");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT_V = new ZTS_VBELN_S[IT_V_t.getNumRows()];
      ZTS_VBELN_S IT_V_r;
      for (int i = 0; i < params.IT_V.length; i++) {
        IT_V_t.setRow(i);
        IT_V_r = new ZTS_VBELN_S();
        IT_V_r.VBELN = IT_V_t.getString("VBELN");
        params.IT_V[i] = IT_V_r;
      }

      params.IT_TOV = new ZTS_MP_QTY_S[IT_TOV_t.getNumRows()];
      ZTS_MP_QTY_S IT_TOV_r;
      for (int i = 0; i < params.IT_TOV.length; i++) {
        IT_TOV_t.setRow(i);
        IT_TOV_r = new ZTS_MP_QTY_S();
        IT_TOV_r.MATNR = IT_TOV_t.getString("MATNR");
        IT_TOV_r.CHARG = IT_TOV_t.getString("CHARG");
        IT_TOV_r.QTY = IT_TOV_t.getBigDecimal("QTY");
        params.IT_TOV[i] = IT_TOV_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL19");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL19 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
