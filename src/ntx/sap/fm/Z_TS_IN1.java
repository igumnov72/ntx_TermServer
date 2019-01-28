package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Выбор поставки
 */
public class Z_TS_IN1 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public String TASK_ID = ""; // ID задачи
  public String TASK_DT = ""; // Дата/время задачи
  public String TASK_USER = ""; // Пользователь, выполняющий задачу
  //
  // exporting params
  public String VBELN2 = ""; // Номер документа сбыта
  public String LGORT = ""; // Ракурс ОснЗапМтрл: выбор склада и партии
  public String VBELNS = ""; // Поставки ПНП
  public String CHECK_DP = ""; // С контролем даты пр-ва
  public ZTS_QTY_DIF_S QTY_DIF = new ZTS_QTY_DIF_S(); // Расхождения по кол-ву при приемке
  public String IS_RET = ""; // признак возврата
  public String IS_1_MAT = ""; // один материал в ячейке
  public String ZCOMP_CLIENT = ""; // Комплектация под клиента
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
      System.out.println("Вызов ФМ Z_TS_IN1");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_IN1:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  TASK_ID=" + TASK_ID);
      System.out.println("  TASK_DT=" + TASK_DT);
      System.out.println("  TASK_USER=" + TASK_USER);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_IN1:");
        System.out.println("  VBELN2=" + VBELN2);
        System.out.println("  LGORT=" + LGORT);
        System.out.println("  VBELNS=" + VBELNS);
        System.out.println("  CHECK_DP=" + CHECK_DP);
        System.out.println("  QTY_DIF.QTY_VBEL=" + QTY_DIF.QTY_VBEL);
        System.out.println("  QTY_DIF.QTY_SCAN=" + QTY_DIF.QTY_SCAN);
        System.out.println("  QTY_DIF.QTY_NEDOST=" + QTY_DIF.QTY_NEDOST);
        System.out.println("  QTY_DIF.QTY_IZL=" + QTY_DIF.QTY_IZL);
        System.out.println("  QTY_DIF.QTY_PRT=" + QTY_DIF.QTY_PRT);
        System.out.println("  IS_RET=" + IS_RET);
        System.out.println("  IS_1_MAT=" + IS_1_MAT);
        System.out.println("  ZCOMP_CLIENT=" + ZCOMP_CLIENT);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_IN1:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_IN1: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_IN1 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    JCoStructure QTY_DIF_s = expParams.getStructure("QTY_DIF");

    impParams.setValue("VBELN", params.VBELN);
    impParams.setValue("TASK_ID", params.TASK_ID);
    impParams.setValue("TASK_DT", params.TASK_DT);
    impParams.setValue("TASK_USER", params.TASK_USER);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.VBELN2 = expParams.getString("VBELN2");
      params.LGORT = expParams.getString("LGORT");
      params.VBELNS = expParams.getString("VBELNS");
      params.CHECK_DP = expParams.getString("CHECK_DP");
      params.IS_RET = expParams.getString("IS_RET");
      params.IS_1_MAT = expParams.getString("IS_1_MAT");
      params.ZCOMP_CLIENT = expParams.getString("ZCOMP_CLIENT");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }
      params.QTY_DIF.QTY_VBEL = QTY_DIF_s.getBigDecimal("QTY_VBEL");
      params.QTY_DIF.QTY_SCAN = QTY_DIF_s.getBigDecimal("QTY_SCAN");
      params.QTY_DIF.QTY_NEDOST = QTY_DIF_s.getBigDecimal("QTY_NEDOST");
      params.QTY_DIF.QTY_IZL = QTY_DIF_s.getBigDecimal("QTY_IZL");
      params.QTY_DIF.QTY_PRT = QTY_DIF_s.getBigDecimal("QTY_PRT");
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_IN1");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_IN1 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
