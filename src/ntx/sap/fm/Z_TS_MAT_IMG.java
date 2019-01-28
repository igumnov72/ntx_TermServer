package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение фотки материала (бинарные данные)
 */
public class Z_TS_MAT_IMG {

  // importing params
  public String MATNR = ""; // Номер материала
  //
  // exporting params
  public SDOKCNTBIN[] IT = new SDOKCNTBIN[0]; // Бинарные данные картинки
  public int COMP_SIZE = 0; // размер картинки в байтах
  public int DT_MOD = 0; // Дата/время изменения (секунды)
  public String NO_FOTO = ""; // признак отсутствия фото
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
      System.out.println("Вызов ФМ Z_TS_MAT_IMG");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_MAT_IMG:");
      System.out.println("  MATNR=" + MATNR);
    }

    // вызов САПовской процедуры
    JCoException e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_MAT_IMG:");
        System.out.println("  IT.length=" + IT.length);
        System.out.println("  COMP_SIZE=" + COMP_SIZE);
        System.out.println("  DT_MOD=" + DT_MOD);
        System.out.println("  NO_FOTO=" + NO_FOTO);
        System.out.println("  err=" + err);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_MAT_IMG:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_MAT_IMG: " + err);
      }
    }
  }

  private static synchronized JCoException execute(Z_TS_MAT_IMG params) {
    JCoException ret = null;

    if (!isInit) {
      ret = init();
      if (ret != null) {
        return ret;
      }
    }

    impParams.clear();
    expParams.clear();

    JCoTable IT_t = expParams.getTable("IT");

    impParams.setValue("MATNR", params.MATNR);

    ret = SAPconn.executeFunction(function);

    if (ret == null) {
      params.COMP_SIZE = expParams.getInt("COMP_SIZE");
      params.DT_MOD = expParams.getInt("DT_MOD");
      params.NO_FOTO = expParams.getString("NO_FOTO");
      params.err = expParams.getString("ERR");
      if (!params.err.isEmpty()) {
        params.isErr = true;
        params.errFull = params.err;
      }

      params.IT = new SDOKCNTBIN[IT_t.getNumRows()];
      SDOKCNTBIN IT_r;
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_r = new SDOKCNTBIN();
        IT_r.LINE = IT_t.getByteArray("LINE");
        params.IT[i] = IT_r;
      }
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_MAT_IMG");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_MAT_IMG not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}
