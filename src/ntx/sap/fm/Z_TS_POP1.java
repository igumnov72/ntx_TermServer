package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Проверка наличия пополнения
 */
public class Z_TS_POP1 {

  // importing params
  public String LGORT = ""; // Склад
  //
  // exporting params
  public String LGTYP1 = ""; // Тип склада откуда
  public String LGTYP2 = ""; // Тип склада куда
  public String DT = ""; // Дата/время создания отчета о пополнении
  public String LGNUM = ""; // Номер склада/комплекс
  public String NEXT_CELL = ""; // Следующая ячейка
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
      System.out.println("Вызов ФМ Z_TS_POP1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_POP1:");
      System.out.println("  LGORT=" + LGORT);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_POP1:");
        System.out.println("  LGTYP1=" + LGTYP1);
        System.out.println("  LGTYP2=" + LGTYP2);
        System.out.println("  DT=" + DT);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  NEXT_CELL=" + NEXT_CELL);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_POP1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_POP1: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_POP1 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    impParams.setValue("LGORT", params.LGORT);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.LGTYP1 = expParams.getString("LGTYP1");
      params.LGTYP2 = expParams.getString("LGTYP2");
      params.DT = expParams.getString("DT");
      params.LGNUM = expParams.getString("LGNUM");
      params.NEXT_CELL = expParams.getString("NEXT_CELL");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_POP1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_POP1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
