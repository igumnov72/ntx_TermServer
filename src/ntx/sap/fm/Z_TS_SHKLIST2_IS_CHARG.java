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
/*public class Z_TS_SHKLIST2_IS_CHARG {
    
}*/

package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;

/**
 * Инф по ШК
 */
public class Z_TS_SHKLIST2_IS_CHARG {

  // importing params
  public String SHK = ""; // Штрих-код
  //
  // exporting params
  public String INF = "";
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList impParams;
  private static volatile JCoParameterList expParams;
  private static volatile boolean isInit = false;

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_SHKLIST2_IS_CHARG");
    } else if (TSparams.logDocLevel >= 2) {
      System.out.println("Вызов ФМ Z_TS_SHKLIST2_IS_CHARG:");
      System.out.println("  SHK=" + SHK);
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_SHKLIST2_IS_CHARG:");
        System.out.println("  INF=" + INF);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_SHKLIST2_IS_CHARG:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_SHKLIST2_IS_CHARG: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_SHKLIST2_IS_CHARG params) {
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

      impParams.setValue("SHK", params.SHK);

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.INF = expParams.getString("INF");
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
      function = SAPconn.getFunction("Z_TS_SHKLIST2_IS_CHARG");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_SHKLIST2_IS_CHARG not found in SAP.");
    }

    impParams = function.getImportParameterList();
    expParams = function.getExportParameterList();

    isInit = true;

    return null;
  }
}

