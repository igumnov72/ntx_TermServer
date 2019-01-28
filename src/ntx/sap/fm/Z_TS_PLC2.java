package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;
import ntx.sap.struct.*;

/**
 * Размещение - создание тр заказа
 */
public class Z_TS_PLC2 {

  // importing params
  public String PAL = ""; // Номер паллеты
  public String CELL = ""; // Ячейка
  public String USER_CODE = ""; // Штрих-код
  public String DO_CNF = ""; // X - подтверждение сразу
  public String MERGE_CNF = ""; // Наличие подтверждения объединения товара
  public String DO_PM = ""; // X - создать ПМ
  public String DO_PM_MAIL = ""; // X - отправить письмо о необходимости создания ПМ
  //
  // exporting params
  public String LGPLA = ""; // Складское место
  public int NERAZM = 0; // Число неразмещенных паллет
  public String IS_PNP = ""; // Приемка по ПНП
  public String NEED_MERGE_CNF = ""; // Признак необходимости подтверждения объединения товара
  public BigDecimal QTY0 = new BigDecimal(0); // Имеющееся в ячейке кол-во
  public String OLD_PAL = ""; // В ячейке оставлена старая паллета (из-за блокировок)
  public String PLC_DONE = ""; // Считаем размещение выполненным
  //
  // table params
  public ZTS_TANUM_S[] TANUMS = new ZTS_TANUM_S[0]; // Список трансп заказов
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

  public void TANUMS_create(int n) {
    TANUMS = new ZTS_TANUM_S[n];
    for (int i = 0; i < n; i++) {
      TANUMS[i] = new ZTS_TANUM_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_PLC2");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_PLC2:");
      System.out.println("  PAL=" + PAL);
      System.out.println("  CELL=" + CELL);
      System.out.println("  USER_CODE=" + USER_CODE);
      System.out.println("  DO_CNF=" + DO_CNF);
      System.out.println("  MERGE_CNF=" + MERGE_CNF);
      System.out.println("  DO_PM=" + DO_PM);
      System.out.println("  DO_PM_MAIL=" + DO_PM_MAIL);
      System.out.println("  TANUMS.length=" + TANUMS.length);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_PLC2:");
        System.out.println("  LGPLA=" + LGPLA);
        System.out.println("  NERAZM=" + NERAZM);
        System.out.println("  IS_PNP=" + IS_PNP);
        System.out.println("  NEED_MERGE_CNF=" + NEED_MERGE_CNF);
        System.out.println("  QTY0=" + QTY0);
        System.out.println("  OLD_PAL=" + OLD_PAL);
        System.out.println("  PLC_DONE=" + PLC_DONE);
        System.out.println("  err=" + err);
        System.out.println("  TANUMS.length=" + TANUMS.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_PLC2:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_PLC2: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_PLC2 params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();
    tabParams.clear();

    JCoTable TANUMS_t = tabParams.getTable("TANUMS");

    impParams.setValue("PAL", params.PAL);
    impParams.setValue("CELL", params.CELL);
    impParams.setValue("USER_CODE", params.USER_CODE);
    impParams.setValue("DO_CNF", params.DO_CNF);
    impParams.setValue("MERGE_CNF", params.MERGE_CNF);
    impParams.setValue("DO_PM", params.DO_PM);
    impParams.setValue("DO_PM_MAIL", params.DO_PM_MAIL);

    TANUMS_t.appendRows(params.TANUMS.length);
    for (int i = 0; i < params.TANUMS.length; i++) {
      TANUMS_t.setRow(i);
      TANUMS_t.setValue("LGNUM", params.TANUMS[i].LGNUM);
      TANUMS_t.setValue("TANUM", params.TANUMS[i].TANUM);
    }

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.LGPLA = expParams.getString("LGPLA");
      params.NERAZM = expParams.getInt("NERAZM");
      params.IS_PNP = expParams.getString("IS_PNP");
      params.NEED_MERGE_CNF = expParams.getString("NEED_MERGE_CNF");
      params.QTY0 = expParams.getBigDecimal("QTY0");
      params.OLD_PAL = expParams.getString("OLD_PAL");
      params.PLC_DONE = expParams.getString("PLC_DONE");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.TANUMS = new ZTS_TANUM_S[TANUMS_t.getNumRows()];
      ZTS_TANUM_S TANUMS_r;
      for (int i = 0; i < params.TANUMS.length; i++) {
        TANUMS_t.setRow(i);
        TANUMS_r = new ZTS_TANUM_S();
        TANUMS_r.LGNUM = TANUMS_t.getString("LGNUM");
        TANUMS_r.TANUM = TANUMS_t.getString("TANUM");
        params.TANUMS[i] = TANUMS_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_PLC2");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_PLC2 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
