/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package ntx.sap.fm;

/**
 *
 * @author amolchanov
 */
//public class Z_TS_IS_SUPPLY {
//    
//}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
 * Определение имеется ли опись
 */
public class Z_TS_IS_SUPPLY {

  // importing params
//  public String USER_SHK = ""; // Штрих-код
  //
  // exporting params

    /**
     *
     */
  public String W_SUPPLY  = "";      // номер описи
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
      System.out.println("Вызов ФМ Z_TS_IS_SUPPLY");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_IS_SUPPLY:");
//      System.out.println("  USER_SHK=" + USER_SHK);
      System.out.println("  W_SUPPLY=" + W_SUPPLY);
//      System.out.println("  IT.length=" + IT.length);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_IS_SUPPLY:");
//        System.out.println("  SHKLIST=" + SHKLIST);
        System.out.println("  err=" + err);
//        System.out.println("  IT.length=" + IT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      isSapErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_IS_SUPPLY:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_IS_SUPPLY: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_IS_SUPPLY params) {
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
      impParams.setValue("W_SUPPLY", params.W_SUPPLY);

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
      function = SAPconn.getFunction("Z_TS_IS_SUPPLY");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_IS_SUPPLY not found in SAP.");
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
