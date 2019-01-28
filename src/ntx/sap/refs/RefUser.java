package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.fm.Z_TS_USER;

/**
 * Справочник пользователей
 */
public class RefUser {

  private static final Map<String, RefUserStruct> ref = Collections.synchronizedMap(new HashMap<String, RefUserStruct>(100));

  public static RefUserStruct get(String user) throws Exception {
    RefUserStruct ret = ref.get(user);
    if (ret != null) {
      return ret;
    }

    return getFromSAP(user);
  }

  public static RefUserStruct getNoNull(String user) throws Exception {
    RefUserStruct ret = null;
    try {
      ret = get(user);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefUserStruct("???");
    }
    return ret;
  }

  public static RefUserStruct set(String user, RefUserStruct s) {
    // сохранение данных о складе
    synchronized (ref) {
      RefUserStruct s1 = ref.get(user);
      if (s1 == null || !s1.equals(s)) {
        ref.put(user, s);
        return s;
      } else {
        return s1;
      }
    }
  }

  private static RefUserStruct getFromSAP(String user) throws Exception {
    // получение данных о складе из САП (с помещением в справочник)

    Z_TS_USER f = new Z_TS_USER();
    f.LIFNR = user;
    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    if ((f.NAME1 == null) || f.NAME1.isEmpty()) {
      return null;
    }

    RefUserStruct s = new RefUserStruct(f.NAME1);
    return set(user, s);
  }
}
