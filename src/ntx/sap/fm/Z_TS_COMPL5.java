package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение ведомости на комплектацию (по поставке и ячейке)
 */
public class Z_TS_COMPL5 {

  // importing params
  public String LGORT = ""; // Склад
  public String VBELN = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  public String INF_COMPL = ""; // Признак информационной комплектации
  //
  // exporting params
  public String ZCOMP_CLIENT = ""; // Комплектация под клиента
  //
  // table params
  public ZTS_COMPL_S[] IT = new ZTS_COMPL_S[0]; // Данные ведомости на комплектацию
  public ZDC_MM_SPT_PRT_S[] IT_STRICT_PRT = new ZDC_MM_SPT_PRT_S[0]; // Список партий под особым запасом
  public ZDC_MM_SPT_PRT_S[] IT_ALL_PRT = new ZDC_MM_SPT_PRT_S[0]; // Список всех партий на складе (по материалам в вед по ячейке)
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
    IT = new ZTS_COMPL_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_COMPL_S();
    }
  }

  public void IT_STRICT_PRT_create(int n) {
    IT_STRICT_PRT = new ZDC_MM_SPT_PRT_S[n];
    for (int i = 0; i < n; i++) {
      IT_STRICT_PRT[i] = new ZDC_MM_SPT_PRT_S();
    }
  }

  public void IT_ALL_PRT_create(int n) {
    IT_ALL_PRT = new ZDC_MM_SPT_PRT_S[n];
    for (int i = 0; i < n; i++) {
      IT_ALL_PRT[i] = new ZDC_MM_SPT_PRT_S();
    }
  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_COMPL5");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL5:");
      System.out.println("  LGORT=" + LGORT);
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  LGPLA=" + LGPLA);
      System.out.println("  INF_COMPL=" + INF_COMPL);
      System.out.println("  IT.length=" + IT.length);
      System.out.println("  IT_STRICT_PRT.length=" + IT_STRICT_PRT.length);
      System.out.println("  IT_ALL_PRT.length=" + IT_ALL_PRT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL5:");
        System.out.println("  ZCOMP_CLIENT=" + ZCOMP_CLIENT);
        System.out.println("  err=" + err);
        System.out.println("  IT.length=" + IT.length);
        System.out.println("  IT_STRICT_PRT.length=" + IT_STRICT_PRT.length);
        System.out.println("  IT_ALL_PRT.length=" + IT_ALL_PRT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL5:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL5: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL5 params) {
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
      JCoTable IT_STRICT_PRT_t = tabParams.getTable("IT_STRICT_PRT");
      JCoTable IT_ALL_PRT_t = tabParams.getTable("IT_ALL_PRT");

      impParams.setValue("LGORT", params.LGORT);
      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("LGPLA", params.LGPLA);
      impParams.setValue("INF_COMPL", params.INF_COMPL);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("LGNUM", params.IT[i].LGNUM);
        IT_t.setValue("TANUM", params.IT[i].TANUM);
        IT_t.setValue("TAPOS", params.IT[i].TAPOS);
        IT_t.setValue("LGTYP", params.IT[i].LGTYP);
        IT_t.setValue("LGPLA", params.IT[i].LGPLA);
        IT_t.setValue("LENUM", params.IT[i].LENUM);
        IT_t.setValue("MATNR", params.IT[i].MATNR);
        IT_t.setValue("CHARG", params.IT[i].CHARG);
        IT_t.setValue("ONLY_PRT", params.IT[i].ONLY_PRT);
        IT_t.setValue("SOBKZ", params.IT[i].SOBKZ);
        IT_t.setValue("SONUM", params.IT[i].SONUM);
        IT_t.setValue("QTY", params.IT[i].QTY);
      }

      IT_STRICT_PRT_t.appendRows(params.IT_STRICT_PRT.length);
      for (int i = 0; i < params.IT_STRICT_PRT.length; i++) {
        IT_STRICT_PRT_t.setRow(i);
        IT_STRICT_PRT_t.setValue("CHARG", params.IT_STRICT_PRT[i].CHARG);
      }

      IT_ALL_PRT_t.appendRows(params.IT_ALL_PRT.length);
      for (int i = 0; i < params.IT_ALL_PRT.length; i++) {
        IT_ALL_PRT_t.setRow(i);
        IT_ALL_PRT_t.setValue("CHARG", params.IT_ALL_PRT[i].CHARG);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.ZCOMP_CLIENT = expParams.getString("ZCOMP_CLIENT");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_COMPL_S[IT_t.getNumRows()];
        ZTS_COMPL_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_COMPL_S();
          IT_r.LGNUM = IT_t.getString("LGNUM");
          IT_r.TANUM = IT_t.getString("TANUM");
          IT_r.TAPOS = IT_t.getString("TAPOS");
          IT_r.LGTYP = IT_t.getString("LGTYP");
          IT_r.LGPLA = IT_t.getString("LGPLA");
          IT_r.LENUM = IT_t.getString("LENUM");
          IT_r.MATNR = IT_t.getString("MATNR");
          IT_r.CHARG = IT_t.getString("CHARG");
          IT_r.ONLY_PRT = IT_t.getString("ONLY_PRT");
          IT_r.SOBKZ = IT_t.getString("SOBKZ");
          IT_r.SONUM = IT_t.getString("SONUM");
          IT_r.QTY = IT_t.getBigDecimal("QTY");
          params.IT[i] = IT_r;
        }

        params.IT_STRICT_PRT = new ZDC_MM_SPT_PRT_S[IT_STRICT_PRT_t.getNumRows()];
        ZDC_MM_SPT_PRT_S IT_STRICT_PRT_r;
        for (int i = 0; i < params.IT_STRICT_PRT.length; i++) {
          IT_STRICT_PRT_t.setRow(i);
          IT_STRICT_PRT_r = new ZDC_MM_SPT_PRT_S();
          IT_STRICT_PRT_r.CHARG = IT_STRICT_PRT_t.getString("CHARG");
          params.IT_STRICT_PRT[i] = IT_STRICT_PRT_r;
        }

        params.IT_ALL_PRT = new ZDC_MM_SPT_PRT_S[IT_ALL_PRT_t.getNumRows()];
        ZDC_MM_SPT_PRT_S IT_ALL_PRT_r;
        for (int i = 0; i < params.IT_ALL_PRT.length; i++) {
          IT_ALL_PRT_t.setRow(i);
          IT_ALL_PRT_r = new ZDC_MM_SPT_PRT_S();
          IT_ALL_PRT_r.CHARG = IT_ALL_PRT_t.getString("CHARG");
          params.IT_ALL_PRT[i] = IT_ALL_PRT_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_COMPL5");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL5 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
