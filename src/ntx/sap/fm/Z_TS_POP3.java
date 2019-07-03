package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Сканирование ячейки
 */
public class Z_TS_POP3 {

  // importing params
  public String LGORT = ""; // Склад
  public String LGPLA = ""; // Складское место
  //
  // exporting params
  public String LGTYP1 = ""; // Тип склада
  public String LGTYP2 = ""; // Тип склада
  public String DT = ""; // Дата/время создания
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGPLA2 = ""; // Складское место (получатель)
  public BigDecimal QTY = new BigDecimal(0); // Количество
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
      System.out.println("Вызов ФМ Z_TS_POP3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_POP3:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGPLA=" + LGPLA);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_POP3:");
        System.out.println("  LGTYP1=" + LGTYP1);
        System.out.println("  LGTYP2=" + LGTYP2);
        System.out.println("  DT=" + DT);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  LGPLA2=" + LGPLA2);
        System.out.println("  QTY=" + QTY);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_POP3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_POP3: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_POP3 params) {
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

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("LGPLA", params.LGPLA);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.LGTYP1 = expParams.getString("LGTYP1");
        params.LGTYP2 = expParams.getString("LGTYP2");
        params.DT = expParams.getString("DT");
        params.LGNUM = expParams.getString("LGNUM");
        params.LGPLA2 = expParams.getString("LGPLA2");
        params.QTY = expParams.getBigDecimal("QTY");
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
      function = SAPconn.getFunction("Z_TS_POP3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_POP3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
