package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение ячеек для комплектации (с кол-вом)
 */
public class Z_TS_COMPL4 {

  // importing params
  public String LGORT = ""; // Склад
  public String VBELN = ""; // Номер документа сбыта
  public String INF_COMPL1 = ""; // Признак информационной комплектации
  public String CHECK_COMPL1 = ""; // Контроль возможности комплектации
  public String USER_SHK = ""; // Штрих-код
  //
  // exporting params
  public String INF_COMPL = ""; // Признак информационной комплектации
  public String LGNUM = ""; // Номер склада/комплекс
  public String CHECK_COMPL = ""; // Контроль возможности комплектации
  public String ZCOMP_CLIENT = ""; // Комплектация под клиента
  public String ASK_MESH = ""; // Общий флаг
  public String TREB_SBOR = "";
  public String BOX_QTIES = "";
  //
  // table params
  public ZTS_COMPL_CELL_S[] IT = new ZTS_COMPL_CELL_S[0]; // Ячейки для комплектации
  public ZTS_COMPL_FP_S[] IT_FP = new ZTS_COMPL_FP_S[0]; // Признак комплектации с паллеты
  public ZTS_CHARG_PROPS_S[] IT_CH = new ZTS_CHARG_PROPS_S[0]; // Свойства партий
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

  public void IT_create(int n) {
    IT = new ZTS_COMPL_CELL_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_COMPL_CELL_S();
    }
  }

  public void IT_FP_create(int n) {
    IT_FP = new ZTS_COMPL_FP_S[n];
    for (int i = 0; i < n; i++) {
      IT_FP[i] = new ZTS_COMPL_FP_S();
    }
  }

  public void IT_CH_create(int n) {
    IT_CH = new ZTS_CHARG_PROPS_S[n];
    for (int i = 0; i < n; i++) {
      IT_CH[i] = new ZTS_CHARG_PROPS_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL4");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL4:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  INF_COMPL1=" + INF_COMPL1);
      System.out.println("  CHECK_COMPL1=" + CHECK_COMPL1);
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  IT.length=" + IT.length);
      System.out.println("  IT_FP.length=" + IT_FP.length);
      System.out.println("  IT_CH.length=" + IT_CH.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL4:");
        System.out.println("  INF_COMPL=" + INF_COMPL);
        System.out.println("  LGNUM=" + LGNUM);
        System.out.println("  CHECK_COMPL=" + CHECK_COMPL);
        System.out.println("  ZCOMP_CLIENT=" + ZCOMP_CLIENT);
        System.out.println("  ASK_MESH=" + ASK_MESH);
        System.out.println("  TREB_SBOR=" + TREB_SBOR);
        System.out.println("  BOX_QTIES=" + BOX_QTIES);
        System.out.println("  err=" + err);
        System.out.println("  IT.length=" + IT.length);
        System.out.println("  IT_FP.length=" + IT_FP.length);
        System.out.println("  IT_CH.length=" + IT_CH.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL4:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL4: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL4 params) {
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

      JCoTable IT_t = tabParams.getTable("IT");
      JCoTable IT_FP_t = tabParams.getTable("IT_FP");
      JCoTable IT_CH_t = tabParams.getTable("IT_CH");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("INF_COMPL1", params.INF_COMPL1);
      impParams.setValue("CHECK_COMPL1", params.CHECK_COMPL1);
      impParams.setValue("USER_SHK", params.USER_SHK);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("LGTYP", params.IT[i].LGTYP);
        IT_t.setValue("LGPLA", params.IT[i].LGPLA);
        IT_t.setValue("QTY", params.IT[i].QTY);
      }

      IT_FP_t.appendRows(params.IT_FP.length);
      for (int i = 0; i < params.IT_FP.length; i++) {
        IT_FP_t.setRow(i);
        IT_FP_t.setValue("LGPLA", params.IT_FP[i].LGPLA);
        IT_FP_t.setValue("COMPL_FROM", params.IT_FP[i].COMPL_FROM);
      }

      IT_CH_t.appendRows(params.IT_CH.length);
      for (int i = 0; i < params.IT_CH.length; i++) {
        IT_CH_t.setRow(i);
        IT_CH_t.setValue("CHARG", params.IT_CH[i].CHARG);
        IT_CH_t.setValue("NO_CORP_SHK_BOX", params.IT_CH[i].NO_CORP_SHK_BOX);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INF_COMPL = expParams.getString("INF_COMPL");
        params.LGNUM = expParams.getString("LGNUM");
        params.CHECK_COMPL = expParams.getString("CHECK_COMPL");
        params.ZCOMP_CLIENT = expParams.getString("ZCOMP_CLIENT");
        params.ASK_MESH = expParams.getString("ASK_MESH");
        params.TREB_SBOR = expParams.getString("TREB_SBOR");
        params.BOX_QTIES = expParams.getString("BOX_QTIES");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_COMPL_CELL_S[IT_t.getNumRows()];
        ZTS_COMPL_CELL_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_COMPL_CELL_S();
          IT_r.LGTYP = IT_t.getString("LGTYP");
          IT_r.LGPLA = IT_t.getString("LGPLA");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          params.IT[i] = IT_r;
        }

        params.IT_FP = new ZTS_COMPL_FP_S[IT_FP_t.getNumRows()];
        ZTS_COMPL_FP_S IT_FP_r;
        for (int i = 0; i < params.IT_FP.length; i++) {
          IT_FP_t.setRow(i);
          IT_FP_r = new ZTS_COMPL_FP_S();
          IT_FP_r.LGPLA = IT_FP_t.getString("LGPLA");
          IT_FP_r.COMPL_FROM = IT_FP_t.getString("COMPL_FROM");
          params.IT_FP[i] = IT_FP_r;
        }

        params.IT_CH = new ZTS_CHARG_PROPS_S[IT_CH_t.getNumRows()];
        ZTS_CHARG_PROPS_S IT_CH_r;
        for (int i = 0; i < params.IT_CH.length; i++) {
          IT_CH_t.setRow(i);
          IT_CH_r = new ZTS_CHARG_PROPS_S();
          IT_CH_r.CHARG = IT_CH_t.getString("CHARG");
          IT_CH_r.NO_CORP_SHK_BOX = IT_CH_t.getString("NO_CORP_SHK_BOX");
          params.IT_CH[i] = IT_CH_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL4");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL4 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
