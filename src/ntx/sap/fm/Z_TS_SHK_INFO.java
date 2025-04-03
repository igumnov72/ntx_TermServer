package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получить информацию по ШК куска
 */
public class Z_TS_SHK_INFO {

  // importing params
  public String SHK = ""; // Штрих-код
  //
  // exporting params
  public String INF1 = "";
  public String INF2 = "";
  public String INF3 = "";
  public String INF4 = "";
  public String INF5 = "";
  public String INF6 = "";
  public String INF7 = "";
  public String INF8 = "";
  public String INF9 = "";
  public String INF10 = "";
  public String INF11 = "";
  public String INF12 = "";
  public String INFO = "";
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SHK_INFO");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SHK_INFO:");
      System.out.println("  SHK=" + SHK);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SHK_INFO:");
        System.out.println("  INF1=" + INF1);
        System.out.println("  INF2=" + INF2);
        System.out.println("  INF3=" + INF3);
        System.out.println("  INF4=" + INF4);
        System.out.println("  INF5=" + INF5);
        System.out.println("  INF6=" + INF6);
        System.out.println("  INF7=" + INF7);
        System.out.println("  INF8=" + INF8);
        System.out.println("  INF9=" + INF9);
        System.out.println("  INF10=" + INF10);
        System.out.println("  INF11=" + INF11);
        System.out.println("  INF12=" + INF12);
        System.out.println("  INFO=" + INFO);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SHK_INFO:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SHK_INFO: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SHK_INFO params) {
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

      impParams.setValue("SHK", params.SHK);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INF1 = expParams.getString("INF1");
        params.INF2 = expParams.getString("INF2");
        params.INF3 = expParams.getString("INF3");
        params.INF4 = expParams.getString("INF4");
        params.INF5 = expParams.getString("INF5");
        params.INF6 = expParams.getString("INF6");
        params.INF7 = expParams.getString("INF7");
        params.INF8 = expParams.getString("INF8");
        params.INF9 = expParams.getString("INF9");
        params.INF10 = expParams.getString("INF10");
        params.INF11 = expParams.getString("INF11");
        params.INF12 = expParams.getString("INF12");
        params.INFO = expParams.getString("INFO");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_SHK_INFO");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SHK_INFO not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
