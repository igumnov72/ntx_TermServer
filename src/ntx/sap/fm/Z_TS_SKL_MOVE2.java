package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Отсканирована паллета-источник при перемещении
 */
public class Z_TS_SKL_MOVE2 {

  // importing params
  public String LGPLA = "";
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String PAL = ""; // № единицы складирования
  //
  // exporting params
  public String NOT_WHOLE = ""; // Флаг невозм перем всего товара с паллеты
  public String ABC = ""; // Признак материала на складе ABC
  public String MAT_CELL = ""; // Ячейки с материалом
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
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE2:");
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP=" + LGTYP);
      System.out.println("  PAL=" + PAL);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKL_MOVE2:");
        System.out.println("  NOT_WHOLE=" + NOT_WHOLE);
        System.out.println("  ABC=" + ABC);
        System.out.println("  MAT_CELL=" + MAT_CELL);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SKL_MOVE2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKL_MOVE2: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SKL_MOVE2 params) {
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

      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("LGNUM", params.LGNUM);
      impParams.setValue("LGTYP", params.LGTYP);
      impParams.setValue("PAL", params.PAL);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.NOT_WHOLE = expParams.getString("NOT_WHOLE");
        params.ABC = expParams.getString("ABC");
        params.MAT_CELL = expParams.getString("MAT_CELL");
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
      function = SAPconn.getFunction("Z_TS_SKL_MOVE2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKL_MOVE2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
