package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import java.math.BigDecimal;

/**
 * Сохранение кол-ва паллет и коробов по ИП
 */
public class Z_TS_COMPL24 {

  // importing params
  public String VBELN = ""; // Номер документа сбыта
  public BigDecimal PAL_QTY = new BigDecimal(0); // 5 знаков
  public BigDecimal BOX_QTY = new BigDecimal(0); // 5 знаков
  public String PAL_BOX_QTIES = "";
  public BigDecimal MESH_QTY = new BigDecimal(0); // 5 знаков
  public String ZDC_NK = ""; // Общий флаг
  public String USER_SHK = ""; // Штрих-код
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
      System.out.println("Вызов ФМ Z_TS_COMPL24");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_COMPL24:");
      System.out.println("  VBELN=" + VBELN);
      System.out.println("  PAL_QTY=" + PAL_QTY);
      System.out.println("  BOX_QTY=" + BOX_QTY);
      System.out.println("  PAL_BOX_QTIES=" + PAL_BOX_QTIES);
      System.out.println("  MESH_QTY=" + MESH_QTY);
      System.out.println("  ZDC_NK=" + ZDC_NK);
      System.out.println("  USER_SHK=" + USER_SHK);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_COMPL24:");
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_COMPL24:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_COMPL24: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_COMPL24 params) {
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

      impParams.setValue("VBELN", params.VBELN);
      impParams.setValue("PAL_QTY", params.PAL_QTY);
      impParams.setValue("BOX_QTY", params.BOX_QTY);
      impParams.setValue("PAL_BOX_QTIES", params.PAL_BOX_QTIES);
      impParams.setValue("MESH_QTY", params.MESH_QTY);
      impParams.setValue("ZDC_NK", params.ZDC_NK);
      impParams.setValue("USER_SHK", params.USER_SHK);

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
      function = SAPconn.getFunction("Z_TS_COMPL24");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_COMPL24 not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
