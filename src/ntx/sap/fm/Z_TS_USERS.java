package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение имен пользователей
 */
public class Z_TS_USERS {

  // table params
  public ZTS_USERS_S[] USERS = new ZTS_USERS_S[0]; // ШК и имя пользователя
  //
  // переменные для работы с ошибками
  public boolean isErr;
  public String err;
  public String errFull;
  //
  // вспомогательные переменные
  private static volatile JCoFunction function;
  private static volatile JCoParameterList tabParams;
  private static volatile boolean isInit = false;

  public void USERS_create(int n) {
    USERS = new ZTS_USERS_S[n];
    for (int i = 0; i < n; i++) {
      USERS[i] = new ZTS_USERS_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_USERS");
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_USERS:");
        System.out.println("  USERS.length=" + USERS.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_USERS:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_USERS: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_USERS params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      tabParams.clear();

      JCoTable USERS_t = tabParams.getTable("USERS");

      USERS_t.appendRows(params.USERS.length);
      for (int i = 0; i < params.USERS.length; i++) {
        USERS_t.setRow(i);
        USERS_t.setValue("SHK", params.USERS[i].SHK);
        USERS_t.setValue("NAME1", params.USERS[i].NAME1);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.USERS = new ZTS_USERS_S[USERS_t.getNumRows()];
        ZTS_USERS_S USERS_r;
        for (int i = 0; i < params.USERS.length; i++) {
          USERS_t.setRow(i);
          USERS_r = new ZTS_USERS_S();
          USERS_r.SHK = USERS_t.getString("SHK");
          USERS_r.NAME1 = USERS_t.getString("NAME1");
          params.USERS[i] = USERS_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_USERS");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_USERS not found in SAP.");
    }

    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
