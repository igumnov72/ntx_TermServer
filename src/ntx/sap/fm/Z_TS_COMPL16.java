package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение числа СГМ (коробок) по поставкам
 */
public class Z_TS_COMPL16 {

  // exporting params
  public int N_SGM = 0; // Число СГМ (коробок)
  //
  // table params
  public ZTS_VBELN_S[] IT_V = new ZTS_VBELN_S[0]; // Номера поставок
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList expParams;
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void IT_V_create(int n) {
    IT_V = new ZTS_VBELN_S[n];
    for (int i = 0; i < n; i++) {
      IT_V[i] = new ZTS_VBELN_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL16");
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL16:");
        System.out.println("  N_SGM=" + N_SGM);
        System.out.println("  IT_V.length=" + IT_V.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL16:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL16: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_COMPL16 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    expParams.clear();
    tabParams.clear();

    JCoTable IT_V_t = tabParams.getTable("IT_V");

    IT_V_t.appendRows(params.IT_V.length);
    for (int i = 0; i < params.IT_V.length; i++) {
      IT_V_t.setRow(i);
      IT_V_t.setValue("VBELN", params.IT_V[i].VBELN);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.N_SGM = expParams.getInt("N_SGM");

      params.IT_V = new ZTS_VBELN_S[IT_V_t.getNumRows()];
      ZTS_VBELN_S IT_V_r;
      for (int i = 0; i < params.IT_V.length; i++) {
        IT_V_t.setRow(i);
        IT_V_r = new ZTS_VBELN_S();
        IT_V_r.VBELN = IT_V_t.getString("VBELN");
        params.IT_V[i] = IT_V_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL16");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL16 not found in SAP.");
    }

    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
