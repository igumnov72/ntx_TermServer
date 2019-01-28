package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.fm.Z_TS_LGORT;

/**
 * Справочник складов.
 */
public class RefLgort {

  private static final Map<String, RefLgortStruct> ref = Collections.synchronizedMap(new HashMap<String, RefLgortStruct>(100));

  public static RefLgortStruct get(String lgort) throws Exception {
    RefLgortStruct ret = ref.get(lgort);
    if (ret != null) {
      return ret;
    }

    return getFromSAP(lgort);
  }

  public static RefLgortStruct getNoNull(String lgort) throws Exception {
    RefLgortStruct ret = null;
    try {
      ret = get(lgort);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefLgortStruct("???", "???", "???");
    }
    return ret;
  }

  public static RefLgortStruct set(String lgort, RefLgortStruct s) {
    // сохранение данных о складе
    synchronized (ref) {
      RefLgortStruct s1 = ref.get(lgort);
      if (s1 == null || !s1.equals(s)) {
        ref.put(lgort, s);
        return s;
      } else {
        return s1;
      }
    }
  }

  private static RefLgortStruct getFromSAP(String lgort) throws Exception {
    // получение данных о складе из САП (с помещением в справочник)

    Z_TS_LGORT f = new Z_TS_LGORT();
    f.LGORT = lgort;
    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    if ((f.LGOBE == null) || f.LGOBE.isEmpty()) {
      return null;
    }

    RefLgortStruct s = new RefLgortStruct(f.LGOBE, f.LGNUM, f.WERKS);
    return set(lgort, s);
  }
}
