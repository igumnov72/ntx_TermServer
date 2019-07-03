package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Получение данных о материале
 */
public class Z_TS_MAT {

  // importing params
  public String MATNR = ""; // Номер материала
  //
  // exporting params
  public String MAKTX = ""; // Краткий текст материала
  public String FULL_NAME = ""; // Полное название материала
  public String HAVE_FOTO = ""; // признак наличия фото материала
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
      System.out.println("Вызов ФМ Z_TS_MAT");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_MAT:");
      System.out.println("  MATNR=" + MATNR);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_MAT:");
        System.out.println("  MAKTX=" + MAKTX);
        System.out.println("  FULL_NAME=" + FULL_NAME);
        System.out.println("  HAVE_FOTO=" + HAVE_FOTO);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_MAT:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_MAT: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_MAT params) {
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

      impParams.setValue("MATNR", params.MATNR);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.MAKTX = expParams.getString("MAKTX");
        params.FULL_NAME = expParams.getString("FULL_NAME");
        params.HAVE_FOTO = expParams.getString("HAVE_FOTO");
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_MAT");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_MAT not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
