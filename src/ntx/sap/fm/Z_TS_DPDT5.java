package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * сохранение данных по перемещению в журнал zts28
 */
public class Z_TS_DPDT5 {

  // importing params
  public String LGORT1 = ""; // Склад отпускающий
  public String LGORT2 = ""; // Склад принимающий
  public String LENUM = ""; // № единицы складирования
  public String LGPLA = ""; // Складское место
  public String TASK_ID = ""; // ID задачи
  public String TASK_DT = ""; // Дата/время задачи
  public String USER_SHK = ""; // Штрих-код пользователя
  //
  // exporting params
  public int ID = 0; // Номер записи в журнале
  public String IS_DOC_ERR = ""; // Ошибка при создании документов (данные сохранены)
  //
  // table params
  public ZTS_PRT_QTY_S[] IT_DONE = new ZTS_PRT_QTY_S[0]; // Кол-во по партии
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

  public void IT_DONE_create(int n) {
    IT_DONE = new ZTS_PRT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_DONE[i] = new ZTS_PRT_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_DPDT5");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_DPDT5:");
      System.out.println("  LGORT1=" + LGORT1);
      System.out.println("  LGORT2=" + LGORT2);
      System.out.println("  LENUM=" + LENUM);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  TASK_ID=" + TASK_ID);
      System.out.println("  TASK_DT=" + TASK_DT);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  IT_DONE.length=" + IT_DONE.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_DPDT5:");
        System.out.println("  ID=" + ID);
        System.out.println("  IS_DOC_ERR=" + IS_DOC_ERR);
        System.out.println("  err=" + err);
        System.out.println("  IT_DONE.length=" + IT_DONE.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_DPDT5:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_DPDT5: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_DPDT5 params) {
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

      JCoTable IT_DONE_t = tabParams.getTable("IT_DONE");

      impParams.setValue("LGORT1", params.LGORT1);
      impParams.setValue("LGORT2", params.LGORT2);
      impParams.setValue("LENUM", params.LENUM);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("TASK_ID", params.TASK_ID);
      impParams.setValue("TASK_DT", params.TASK_DT);
      impParams.setValue("USER_SHK", params.USER_SHK);

      IT_DONE_t.appendRows(params.IT_DONE.length);
      for (int i = 0; i < params.IT_DONE.length; i++) {
        IT_DONE_t.setRow(i);
        IT_DONE_t.setValue("CHARG", params.IT_DONE[i].CHARG);
        IT_DONE_t.setValue("QTY", params.IT_DONE[i].QTY);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ID = expParams.getInt("ID");
        params.IS_DOC_ERR = expParams.getString("IS_DOC_ERR");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT_DONE = new ZTS_PRT_QTY_S[IT_DONE_t.getNumRows()];
        ZTS_PRT_QTY_S IT_DONE_r;
        for (int i = 0; i < params.IT_DONE.length; i++) {
          IT_DONE_t.setRow(i);
          IT_DONE_r = new ZTS_PRT_QTY_S();
          IT_DONE_r.CHARG = IT_DONE_t.getString("CHARG");
          IT_DONE_r.QTY = IT_DONE_t.getBigDecimal("QTY");
          params.IT_DONE[i] = IT_DONE_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_DPDT5");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_DPDT5 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
