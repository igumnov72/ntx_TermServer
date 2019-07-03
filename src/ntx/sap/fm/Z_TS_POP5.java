package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Сканирование ячейки-получателя
 */
public class Z_TS_POP5 {

  // importing params
  public String DT = ""; // Дата/время создания
  public String LGPLA = ""; // Складское место (отсканированное)
  public String LGPLA1 = ""; // Складское место (исходное)
  public String LGPLA2 = ""; // Складское место (получатель)
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP1 = ""; // Тип склада
  public String PAL = ""; // № единицы складирования
  public String LGORT = ""; // Склад
  public String TSD_USER = ""; // Пользователь ТСД
  //
  // exporting params
  public int KEY1 = 0; // 1-й ключ таблицы
  public String TR_ZAK_CREATED = ""; // Признак созданного тр заказа
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
      System.out.println("Вызов ФМ Z_TS_POP5");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_POP5:");
      System.out.println("  DT=" + DT);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  LGPLA1=" + LGPLA1);
      System.out.println("  LGPLA2=" + LGPLA2);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP1=" + LGTYP1);
      System.out.println("  PAL=" + PAL);
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  TSD_USER=" + TSD_USER);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_POP5:");
        System.out.println("  KEY1=" + KEY1);
        System.out.println("  TR_ZAK_CREATED=" + TR_ZAK_CREATED);
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

      System.err.println("Error calling SAP procedure Z_TS_POP5:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_POP5: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_POP5 params) {
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

      impParams.setValue("DT", params.DT);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("LGPLA1", params.LGPLA1);
      impParams.setValue("LGPLA2", params.LGPLA2);
      impParams.setValue("LGNUM", params.LGNUM);
      impParams.setValue("LGTYP1", params.LGTYP1);
      impParams.setValue("PAL", params.PAL);
      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("TSD_USER", params.TSD_USER);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.KEY1 = expParams.getInt("KEY1");
        params.TR_ZAK_CREATED = expParams.getString("TR_ZAK_CREATED");
        params.NEXT_CELL = expParams.getString("NEXT_CELL");
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
      function = SAPconn.getFunction("Z_TS_POP5");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_POP5 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
