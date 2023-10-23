/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author amolchanov
 */

package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Сохранить список ШК
 */
public class Z_TS_SHKLIST_OPIS_CHECK {

  // importing params
  public String USER_SHK = "";  // Штрих-код
  public String W_OPIS  = "";   // номер описи
  
  // exporting params
  public String ISERR = ""; // Признак ошибки 
  
//  public String SHKLIST = ""; // Список ШК
  //
  // table params
  public ZTS_SHK_S[] IT  = new ZTS_SHK_S[0]; // Table of zts_shk
  public ZTS_INFO_S[] IT_INFO = new ZTS_INFO_S[0]; // Table of zts_info

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
    IT = new ZTS_SHK_S[n];
    for (int i = 0; i < n; i++) {
      IT[i] = new ZTS_SHK_S();
    }
  }

  public void IT_INFO_create(int n) {
    IT_INFO = new ZTS_INFO_S[n];
    for (int i = 0; i < n; i++) {
      IT_INFO[i] = new ZTS_INFO_S();
    }
  }  
  
  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SHKLIST_OPIS_CHECK");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SHKLIST_OPIS_CHECK:");
      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  W_OPIS=" + W_OPIS);
      System.out.println("  IT.length=" + IT.length);
      System.out.println("  IT_INFO.length=" + IT_INFO.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SHKLIST_OPIS_CHECK:");
        System.out.println("  ISERR=" + isErr);        
        System.out.println("  err=" + err);
        System.out.println("  IT.length=" + IT.length);
        System.out.println("  IT_INFO.length=" + IT_INFO.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SHKLIST_OPIS_CHECK:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SHKLIST_OPIS_CHECK: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SHKLIST_OPIS_CHECK params) {
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
      JCoTable IT_INFO_t = tabParams.getTable("IT_INFO");

      impParams.setValue("USER_SHK", params.USER_SHK);
      impParams.setValue("W_OPIS", params.W_OPIS);

      IT_t.appendRows(params.IT.length);
      for (int i = 0; i < params.IT.length; i++) {
        IT_t.setRow(i);
        IT_t.setValue("SHK", params.IT[i].SHK);
        IT_t.setValue("SHK_NAME", params.IT[i].SHK_NAME);
      }

      IT_INFO_t.appendRows(params.IT_INFO.length);
      for (int i = 0; i < params.IT_INFO.length; i++) {
        IT_INFO_t.setRow(i);
        IT_INFO_t.setValue("ZINFO", params.IT_INFO[i].ZINFO);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
//        params.SHKLIST = expParams.getString("SHKLIST");
        params.ISERR = expParams.getString("ISERR");
        params.err = expParams.getString("ERR");
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

        params.IT = new ZTS_SHK_S[IT_t.getNumRows()];
        ZTS_SHK_S IT_r;
        for (int i = 0; i < params.IT.length; i++) {
          IT_t.setRow(i);
          IT_r = new ZTS_SHK_S();
          IT_r.SHK = IT_t.getString("SHK");
          IT_r.SHK_NAME = IT_t.getString("SHK_NAME");
          params.IT[i] = IT_r;
        }

        params.IT_INFO = new ZTS_INFO_S[IT_INFO_t.getNumRows()];
        ZTS_INFO_S IT_INFO_r;
        for (int i = 0; i < params.IT_INFO.length; i++) {
          IT_INFO_t.setRow(i);
          IT_INFO_r = new ZTS_INFO_S();
          IT_INFO_r.ZINFO = IT_INFO_t.getString("ZINFO");
          params.IT_INFO[i] = IT_INFO_r;
        }
       
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_SHKLIST_OPIS_CHECK");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SHKLIST_OPIS_CHECK not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}

