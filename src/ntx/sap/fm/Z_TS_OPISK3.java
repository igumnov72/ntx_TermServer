package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;
import ntx.sap.struct.*;

/**
 * Сохранение и печать описи
 */
public class Z_TS_OPISK3 {

  // importing params
  public String VBELN = ""; // Торговый документ
  public String TDDEST = ""; // Спул: устройство вывода (если надо печатать)
  //
  // exporting params
  public String KOROB = ""; // Номер короба
  public BigDecimal QTY_TOT = new BigDecimal(0); // Общее кол-во
  public String ERR2 = ""; // ошибка печати
  public String KOROB_SSCC = ""; // SSCC
  //
  // table params
  public ZTS_MAT_QTY_S[] IT = new ZTS_MAT_QTY_S[0]; // Кол-во по материалу
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
    IT = new ZTS_MAT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_MAT_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_OPISK3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_OPISK3:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  TDDEST=" + TDDEST);
      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_OPISK3:");
        System.out.println("  KOROB=" + KOROB);
        System.out.println("  QTY_TOT=" + QTY_TOT);
        System.out.println("  ERR2=" + ERR2);
        System.out.println("  KOROB_SSCC=" + KOROB_SSCC);
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

      System.err.println("Error calling SAP procedure Z_TS_OPISK3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_OPISK3: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_OPISK3 params) {
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
      tabParams.clear();

      JCoTable IT_t = tabParams.getTable("IT");

      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("TDDEST", params.TDDEST);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("QTY", params.IT[i].QTY);
        IT_t.setValue("SHK", params.IT[i].SHK);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.KOROB = expParams.getString("KOROB");
        params.QTY_TOT = expParams.getBigDecimal("QTY_TOT");
        params.ERR2 = expParams.getString("ERR2");
        params.KOROB_SSCC = expParams.getString("KOROB_SSCC");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_MAT_QTY_S[IT_t.getNumRows()];
        ZTS_MAT_QTY_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_MAT_QTY_S();
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          IT_r.SHK = IT_t.getString("SHK");
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
      function = SAPconn.getFunction("Z_TS_OPISK3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_OPISK3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
