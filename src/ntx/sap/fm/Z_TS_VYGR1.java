package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Выгрузка машины - сохранение данных
 */
public class Z_TS_VYGR1 {

  // importing params
  public String TASK_ID = ""; // Id задачи пользователя на терминале
  public String LGORT = ""; // Склад
  public BigDecimal M3_MACH = new BigDecimal(0); // Кубатура машины
  public BigDecimal M3_GR = new BigDecimal(0); // Кубатура груза
  public String USER_CODE = ""; // Штрих-код
  public String FINISHED = ""; // Признак завершения задачи
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
      System.out.println("Вызов ФМ Z_TS_VYGR1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_VYGR1:");
      System.out.println("  TASK_ID=" + TASK_ID);
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  M3_MACH=" + M3_MACH);
      System.out.println("  M3_GR=" + M3_GR);
      System.out.println("  USER_CODE=" + USER_CODE);
      System.out.println("  FINISHED=" + FINISHED);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_VYGR1:");
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_VYGR1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_VYGR1: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_VYGR1 params) {
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

      impParams.setValue("TASK_ID", params.TASK_ID);
      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("M3_MACH", params.M3_MACH);
      impParams.setValue("M3_GR", params.M3_GR);
      impParams.setValue("USER_CODE", params.USER_CODE);
      impParams.setValue("FINISHED", params.FINISHED);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
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
      function = SAPconn.getFunction("Z_TS_VYGR1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_VYGR1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
