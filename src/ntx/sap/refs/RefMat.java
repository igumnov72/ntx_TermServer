package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.sys.SAPconn;
import ntx.sap.fm.Z_TS_MAT;

/**
 * Справочник материалов. Номер материала должен передаваться без ведущих нулей.
 */
public class RefMat {

  private static final Map<String, RefMatStruct> ref = Collections.synchronizedMap(new HashMap<String, RefMatStruct>(1000));

  public static String getName(String matnr) throws Exception {
    return getNoNull(matnr).name;
  }

  public static String getFullName(String matnr) throws Exception {
    RefMatStruct m = getNoNull(matnr);
    if (m.fullName == null) {
      return m.name;
    } else {
      return m.name + " / " + m.fullName;
    }
  }

  public static RefMatStruct get(String matnr) throws Exception {
    RefMatStruct ret = ref.get(matnr);
    if (ret != null) {
      return ret;
    }

    return getFromSAP(matnr);
  }

  public static RefMatStruct getNoNull(String matnr) throws Exception {
    RefMatStruct ret = null;
    try {
      ret = get(matnr);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefMatStruct("???", "???", null, "");
    }
    return ret;
  }

  public static RefMatStruct set(String matnr, RefMatStruct s) {
    // сохранение данных о материале
    synchronized (ref) {
      RefMatStruct s1 = ref.get(matnr);
      if (s1 == null || !s1.equals(s)) {
        ref.put(matnr, s);
        return s;
      } else {
        return s1;
      }
    }
  }

  private static RefMatStruct getFromSAP(String matnr) throws Exception {
    // получение данных о материале из САП (с помещением в справочник)

    Z_TS_MAT f = new Z_TS_MAT();
    f.MATNR = SAPconn.fillZeros(matnr, 18);
    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    if ((f.MAKTX == null) || f.MAKTX.isEmpty()) {
      return null;
    }

    RefMatStruct s = new RefMatStruct(matnr, f.MAKTX, f.FULL_NAME, f.HAVE_FOTO);
    return set(matnr, s);
  }
}
