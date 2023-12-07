package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;
import ntx.sap.struct.*;

/**
 * Товар
 */
public class Z_TS_SKL_MOVE3 {

  // importing params
  public String LGORT = ""; // Склад
  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  public String PAL = ""; // № единицы складирования
  public String SHK = ""; // Штрих-код
  public String A_CHARG = ""; // Номер партии
  public BigDecimal A_QTY = new BigDecimal(0); // Количество
  //
  // exporting params
  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY = new BigDecimal(0); // Количество
  public String MAKTX = ""; // Краткий текст материала
  public String MAT_CELL = ""; // Ячейки с материалом
  public String ABC = ""; // Признак материала на складе ABC
  public String EI = ""; // Внешняя ЕИ - техническое представление (6-значная)
  //
  // table params
  public ZTS_MAT_QTY_S[] IT_MQ = new ZTS_MAT_QTY_S[0]; // Кол-во по материалу
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

  public void IT_MQ_create(int n) {
    IT_MQ = new ZTS_MAT_QTY_S[n];
    for (int i = 0; i < n; i++) {
      IT_MQ[i] = new ZTS_MAT_QTY_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE3");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SKL_MOVE3:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  LGNUM=" + LGNUM);
      System.out.println("  LGTYP=" + LGTYP);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  PAL=" + PAL);
      System.out.println("  SHK=" + SHK);
      System.out.println("  A_CHARG=" + A_CHARG);
      System.out.println("  A_QTY=" + A_QTY);
      System.out.println("  IT_MQ.length=" + IT_MQ.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SKL_MOVE3:");
        System.out.println("  MATNR=" + MATNR);
        System.out.println("  CHARG=" + CHARG);
        System.out.println("  QTY=" + QTY);
        System.out.println("  MAKTX=" + MAKTX);
        System.out.println("  MAT_CELL=" + MAT_CELL);
        System.out.println("  ABC=" + ABC);
        System.out.println("  EI=" + EI);
        System.out.println("  err=" + err);
        System.out.println("  IT_MQ.length=" + IT_MQ.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SKL_MOVE3:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SKL_MOVE3: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SKL_MOVE3 params) {
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

      JCoTable IT_MQ_t = tabParams.getTable("IT_MQ");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("LGNUM", params.LGNUM);
      impParams.setValue("LGTYP", params.LGTYP);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("PAL", params.PAL);
      impParams.setValue("SHK", params.SHK);
      impParams.setValue("A_CHARG", params.A_CHARG);
      impParams.setValue("A_QTY", params.A_QTY);

      IT_MQ_t.appendRows(params.IT_MQ.length);
      for (int i = 0; i < params.IT_MQ.length; i++) {
        IT_MQ_t.setRow(i);
        IT_MQ_t.setValue("MATNR", params.IT_MQ[i].MATNR);
        IT_MQ_t.setValue("QTY", params.IT_MQ[i].QTY);
        IT_MQ_t.setValue("SHK", params.IT_MQ[i].SHK);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.MATNR = expParams.getString("MATNR");
        params.CHARG = expParams.getString("CHARG");
        params.QTY = expParams.getBigDecimal("QTY");
        params.MAKTX = expParams.getString("MAKTX");
        params.MAT_CELL = expParams.getString("MAT_CELL");
        params.ABC = expParams.getString("ABC");
        params.EI = expParams.getString("EI");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT_MQ = new ZTS_MAT_QTY_S[IT_MQ_t.getNumRows()];
        ZTS_MAT_QTY_S IT_MQ_r;
        for (int i = 0; i < params.IT_MQ.length; i++) {
          IT_MQ_t.setRow(i);
          IT_MQ_r = new ZTS_MAT_QTY_S();
          IT_MQ_r.MATNR = IT_MQ_t.getString("MATNR");
          IT_MQ_r.QTY = IT_MQ_t.getBigDecimal("QTY");
          IT_MQ_r.SHK = IT_MQ_t.getString("SHK");
          params.IT_MQ[i] = IT_MQ_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_SKL_MOVE3");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SKL_MOVE3 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
