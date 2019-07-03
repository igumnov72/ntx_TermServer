package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Список скомплектованных позиций по ячейке
 */
public class Z_TS_COMPL2 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  //
  // table params
  public ZTS_COMPL2_S[] IT = new ZTS_COMPL2_S[0]; // Данные ведомости на комплектацию (упрощенные)
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void IT_create(int n) {
    IT = new ZTS_COMPL2_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_COMPL2_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL2:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL2:");
        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL2: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL2 params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      impParams.clear();
      tabParams.clear();

      JCoTable IT_t = tabParams.getTable("IT");

      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("LGPLA", params.LGPLA);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("LENUM", params.IT[i].LENUM);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("SOBKZ", params.IT[i].SOBKZ);
        IT_t.setValue("QTY", params.IT[i].QTY);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.IT = new ZTS_COMPL2_S[IT_t.getNumRows()];
        ZTS_COMPL2_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_COMPL2_S();
          IT_r.LENUM = IT_t.getString("LENUM");
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.SOBKZ = IT_t.getString("SOBKZ");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          params.IT[i] = IT_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
