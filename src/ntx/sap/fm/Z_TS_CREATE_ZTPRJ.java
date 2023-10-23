/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntx.sap.fm;

import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import ntx.sap.sys.ErrDescr;
import ntx.sap.sys.SAPconn;
import ntx.ts.srv.TSparams;

/**
 *
 * @author amolchanov
 */
//public class Z_TS_CREATE_ZTPRJ {
//    
//}


/**
 * Определение имеется ли станок
 */
public class Z_TS_CREATE_ZTPRJ {

  // importing params
//  public String USER_SHK = ""; // Штрих-код
  //
  // exporting params

    /**
     *
     */
  public String W_SHK_ZEQ   = "";      // ШК станка
  public String W_SHK_CHARG = "";      // ШК Навоя
//  public String SHKLIST = ""; // Список ШК
  //
  // table params
//  public ZTS_SHK_S[] IT = new ZTS_SHK_S[0]; // Table of zts_shk
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  public boolean isSapErr;
  public String ZTPRJ;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
//  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

//  public void IT_create(int n) {
//    IT = new ZTS_SHK_S[n];
//    for (int i = 0; i < n; i++) {
//      IT[i] = new ZTS_SHK_S();
//    }
//  }

  public void execute() {
    isErr = false;
    isSapErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_CREATE_ZTPRJ");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_CREATE_ZTPRJ:");
//      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  W_SHK_ZEQ=" + W_SHK_ZEQ);
      System.out.println("  W_SHK_CHARG=" + W_SHK_CHARG);
//      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_CREATE_ZTPRJ:");
//        System.out.println("  SHKLIST=" + SHKLIST);
        System.out.println("  err=" + err);
        System.out.println("  ZTPRJ=" + ZTPRJ);        
//        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_CREATE_ZTPRJ:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_CREATE_ZTPRJ: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_CREATE_ZTPRJ params) {
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
//      tabParams.clear();

//      JCoTable IT_t = tabParams.getTable("IT");

//      impParams.setValue("USER_SHK", params.USER_SHK);
      impParams.setValue("W_SHK_ZEQ", params.W_SHK_ZEQ);
      impParams.setValue("W_SHK_CHARG", params.W_SHK_CHARG);

//      IT_t.appendRows(params.IT.length);
//      for (int i = 0; i < params.IT.length; i++) {
//        IT_t.setRow(i);
//        IT_t.setValue("SHK", params.IT[i].SHK);
//        IT_t.setValue("SHK_NAME", params.IT[i].SHK_NAME);
//      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
//        params.SHKLIST = expParams.getString("SHKLIST");
        params.err = expParams.getString("ERR");
        params.ZTPRJ = expParams.getString("ZTPRJ");        
        if (!params.err.isEmpty()) {
          params.isErr = true;
          params.errFull = params.err;
        }

//        params.IT = new ZTS_SHK_S[IT_t.getNumRows()];
//        ZTS_SHK_S IT_r;
//        for (int i = 0; i < params.IT.length; i++) {
//          IT_t.setRow(i);
//          IT_r = new ZTS_SHK_S();
//          IT_r.SHK = IT_t.getString("SHK");
//          IT_r.SHK_NAME = IT_t.getString("SHK_NAME");
//          params.IT[i] = IT_r;
//        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_CREATE_ZTPRJ");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_CREATE_ZTPRJ not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();
//    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}



/**
 *
 * @author amolchanov
 */
//public class Z_TS_IS_OPIS {
    
//}

