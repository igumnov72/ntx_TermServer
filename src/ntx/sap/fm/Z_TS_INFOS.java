package ntx.sap.fm;

import ntx.ts.srv.TSparams;
import ntx.sap.sys.*;
import com.sap.conn.jco.*;
import ntx.sap.struct.*;

/**
 * Получение инструкций и сообщений для пользователей терминалов
 */
public class Z_TS_INFOS {

  // table params
  public ZTS_I_PTYP_S[] IT_PTYP = new ZTS_I_PTYP_S[0]; // Типы задач и инфа по из инструкциям
  public ZTS_I_MAN_S[] IT_MAN = new ZTS_I_MAN_S[0]; // Инструкции/сообщения-
  public ZTS_I_DEL_MAN_S[] IT_DEL_MAN = new ZTS_I_DEL_MAN_S[0]; // Удаленные инструкции/сообщения
  public ZTS_I_TEXT_S[] IT_TEXT = new ZTS_I_TEXT_S[0]; // Тексты (строки)
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

  public void IT_PTYP_create(int n) {
    IT_PTYP = new ZTS_I_PTYP_S[n];
    for (int i = 0; i < n; i++) {
      IT_PTYP[i] = new ZTS_I_PTYP_S();
    }
  }

  public void IT_MAN_create(int n) {
    IT_MAN = new ZTS_I_MAN_S[n];
    for (int i = 0; i < n; i++) {
      IT_MAN[i] = new ZTS_I_MAN_S();
    }
  }

  public void IT_DEL_MAN_create(int n) {
    IT_DEL_MAN = new ZTS_I_DEL_MAN_S[n];
    for (int i = 0; i < n; i++) {
      IT_DEL_MAN[i] = new ZTS_I_DEL_MAN_S();
    }
  }

  public void IT_TEXT_create(int n) {
    IT_TEXT = new ZTS_I_TEXT_S[n];
    for (int i = 0; i < n; i++) {
      IT_TEXT[i] = new ZTS_I_TEXT_S();
    }
  }

  public void execute() {
    isErr = false;
    err = "";
    errFull = "";

    if (TSparams.logDocLevel == 1) {
      System.out.println("Вызов ФМ Z_TS_INFOS");
    }

    // вызов САПовской процедуры
    Exception e = execute(this);

    if (e == null) {
      if (TSparams.logDocLevel >= 2) {
        System.out.println("Возврат из ФМ Z_TS_INFOS:");
        System.out.println("  IT_PTYP.length=" + IT_PTYP.length);
        System.out.println("  IT_MAN.length=" + IT_MAN.length);
        System.out.println("  IT_DEL_MAN.length=" + IT_DEL_MAN.length);
        System.out.println("  IT_TEXT.length=" + IT_TEXT.length);
      }
    } else {
      // обработка ошибки
      isErr = true;
      errFull = e.toString();
      ErrDescr ed = SAPconn.describeErr(errFull);
      err = ed.err;

      System.err.println("Error calling SAP procedure Z_TS_INFOS:");
      if (ed.isShort || !err.equals(errFull)) {
        System.err.println(err);
      }
      if (!ed.isShort) {
        e.printStackTrace();
      }
      System.err.flush();

      if (errFull.startsWith("com.sap.conn.jco.JCoException: (104) JCO_ERROR_SYSTEM_FAILURE:")) {
        System.out.println("!!! Error in Z_TS_INFOS: " + err);
      }
    }
  }

  private static synchronized Exception execute(Z_TS_INFOS params) {
    Exception ret = null;

    try {
      if (!isInit) {
        ret = init();
        if (ret != null) {
          return ret;
        }
      }

      tabParams.clear();

      JCoTable IT_PTYP_t = tabParams.getTable("IT_PTYP");
      JCoTable IT_MAN_t = tabParams.getTable("IT_MAN");
      JCoTable IT_DEL_MAN_t = tabParams.getTable("IT_DEL_MAN");
      JCoTable IT_TEXT_t = tabParams.getTable("IT_TEXT");

      IT_PTYP_t.appendRows(params.IT_PTYP.length);
      for (int i = 0; i < params.IT_PTYP.length; i++) {
        IT_PTYP_t.setRow(i);
        IT_PTYP_t.setValue("PTYP_ID", params.IT_PTYP[i].PTYP_ID);
        IT_PTYP_t.setValue("PTYP", params.IT_PTYP[i].PTYP);
        IT_PTYP_t.setValue("PTYP_NAME", params.IT_PTYP[i].PTYP_NAME);
        IT_PTYP_t.setValue("HAVE_MANUAL", params.IT_PTYP[i].HAVE_MANUAL);
        IT_PTYP_t.setValue("MAX_VER", params.IT_PTYP[i].MAX_VER);
        IT_PTYP_t.setValue("MAX_INFO_ID", params.IT_PTYP[i].MAX_INFO_ID);
      }

      IT_MAN_t.appendRows(params.IT_MAN.length);
      for (int i = 0; i < params.IT_MAN.length; i++) {
        IT_MAN_t.setRow(i);
        IT_MAN_t.setValue("PTYP_ID", params.IT_MAN[i].PTYP_ID);
        IT_MAN_t.setValue("INFO_NO", params.IT_MAN[i].INFO_NO);
        IT_MAN_t.setValue("VER", params.IT_MAN[i].VER);
        IT_MAN_t.setValue("NAME", params.IT_MAN[i].NAME);
        IT_MAN_t.setValue("TXT_ID", params.IT_MAN[i].TXT_ID);
      }

      IT_DEL_MAN_t.appendRows(params.IT_DEL_MAN.length);
      for (int i = 0; i < params.IT_DEL_MAN.length; i++) {
        IT_DEL_MAN_t.setRow(i);
        IT_DEL_MAN_t.setValue("PTYP_ID", params.IT_DEL_MAN[i].PTYP_ID);
        IT_DEL_MAN_t.setValue("INFO_NO", params.IT_DEL_MAN[i].INFO_NO);
        IT_DEL_MAN_t.setValue("VER", params.IT_DEL_MAN[i].VER);
      }

      IT_TEXT_t.appendRows(params.IT_TEXT.length);
      for (int i = 0; i < params.IT_TEXT.length; i++) {
        IT_TEXT_t.setRow(i);
        IT_TEXT_t.setValue("TXT_ID", params.IT_TEXT[i].TXT_ID);
        IT_TEXT_t.setValue("LINE_NO", params.IT_TEXT[i].LINE_NO);
        IT_TEXT_t.setValue("TXT", params.IT_TEXT[i].TXT);
        IT_TEXT_t.setValue("CONT_LIN", params.IT_TEXT[i].CONT_LIN);
      }

      ret = SAPconn.executeFunction(function);

      if (ret == null) {
        params.IT_PTYP = new ZTS_I_PTYP_S[IT_PTYP_t.getNumRows()];
        ZTS_I_PTYP_S IT_PTYP_r;
        for (int i = 0; i < params.IT_PTYP.length; i++) {
          IT_PTYP_t.setRow(i);
          IT_PTYP_r = new ZTS_I_PTYP_S();
          IT_PTYP_r.PTYP_ID = IT_PTYP_t.getInt("PTYP_ID");
          IT_PTYP_r.PTYP = IT_PTYP_t.getString("PTYP");
          IT_PTYP_r.PTYP_NAME = IT_PTYP_t.getString("PTYP_NAME");
          IT_PTYP_r.HAVE_MANUAL = IT_PTYP_t.getString("HAVE_MANUAL");
          IT_PTYP_r.MAX_VER = IT_PTYP_t.getInt("MAX_VER");
          IT_PTYP_r.MAX_INFO_ID = IT_PTYP_t.getInt("MAX_INFO_ID");
          params.IT_PTYP[i] = IT_PTYP_r;
        }

        params.IT_MAN = new ZTS_I_MAN_S[IT_MAN_t.getNumRows()];
        ZTS_I_MAN_S IT_MAN_r;
        for (int i = 0; i < params.IT_MAN.length; i++) {
          IT_MAN_t.setRow(i);
          IT_MAN_r = new ZTS_I_MAN_S();
          IT_MAN_r.PTYP_ID = IT_MAN_t.getInt("PTYP_ID");
          IT_MAN_r.INFO_NO = IT_MAN_t.getInt("INFO_NO");
          IT_MAN_r.VER = IT_MAN_t.getInt("VER");
          IT_MAN_r.NAME = IT_MAN_t.getString("NAME");
          IT_MAN_r.TXT_ID = IT_MAN_t.getInt("TXT_ID");
          params.IT_MAN[i] = IT_MAN_r;
        }

        params.IT_DEL_MAN = new ZTS_I_DEL_MAN_S[IT_DEL_MAN_t.getNumRows()];
        ZTS_I_DEL_MAN_S IT_DEL_MAN_r;
        for (int i = 0; i < params.IT_DEL_MAN.length; i++) {
          IT_DEL_MAN_t.setRow(i);
          IT_DEL_MAN_r = new ZTS_I_DEL_MAN_S();
          IT_DEL_MAN_r.PTYP_ID = IT_DEL_MAN_t.getInt("PTYP_ID");
          IT_DEL_MAN_r.INFO_NO = IT_DEL_MAN_t.getInt("INFO_NO");
          IT_DEL_MAN_r.VER = IT_DEL_MAN_t.getInt("VER");
          params.IT_DEL_MAN[i] = IT_DEL_MAN_r;
        }

        params.IT_TEXT = new ZTS_I_TEXT_S[IT_TEXT_t.getNumRows()];
        ZTS_I_TEXT_S IT_TEXT_r;
        for (int i = 0; i < params.IT_TEXT.length; i++) {
          IT_TEXT_t.setRow(i);
          IT_TEXT_r = new ZTS_I_TEXT_S();
          IT_TEXT_r.TXT_ID = IT_TEXT_t.getInt("TXT_ID");
          IT_TEXT_r.LINE_NO = IT_TEXT_t.getInt("LINE_NO");
          IT_TEXT_r.TXT = IT_TEXT_t.getString("TXT");
          IT_TEXT_r.CONT_LIN = IT_TEXT_t.getString("CONT_LIN");
          params.IT_TEXT[i] = IT_TEXT_r;
        }
      }
    } catch (Exception e) {
      return e;
    }

    return ret;
  }

  private static JCoException init() {
    try {
      function = SAPconn.getFunction("Z_TS_INFOS");
    } catch (JCoException e) {
      return e;
    }

    if (function == null) {
      return new JCoException(0, "Z_TS_INFOS not found in SAP.");
    }

    tabParams = function.getTableParameterList();

    isInit = true;

    return null;
  }
}
