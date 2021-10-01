package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.fm.Z_TS_ABC;
import ntx.sap.sys.SAPconn;

/**
 * Справочник признаков ABC. Номера материалов - без ведущих нулей.
 */
public class RefAbc {

  private static final Map<String, RefAbcStruct> 
          ref = Collections.synchronizedMap(new HashMap<String, RefAbcStruct>(3000));

  public static RefAbcStruct get(String lgort, String matnr) throws Exception {
    RefAbcStruct ret = ref.get(lgort + "-" + matnr);
    if ((ret != null) && ret.isValid()) {
      return ret;
    }

    return getFromSAP(lgort, matnr);
  }

  public static RefAbcStruct getNoNull(String lgort, String matnr) throws Exception {
    RefAbcStruct ret = null;
    try {
      ret = get(lgort, matnr);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefAbcStruct("???", "???");
    }
    return ret;
  }

  public static RefAbcStruct set(String lgort, String matnr, RefAbcStruct s) throws Exception {
    // сохранение данных о материале
    synchronized (ref) {
      RefAbcStruct s1 = ref.get(lgort + "-" + matnr);
      if ((s1 == null) || !s1.equals(s) || !s1.isValid()) {
        ref.put(lgort + "-" + matnr, s);
        return s;
      } else {
        return s1;
      }
    }
  }
  
  public static String appendAbcXyz(String s, String lgort, String matnr) throws Exception {
      String ret = s;
      RefAbcStruct a = get(lgort, matnr);
      if (a == null) return ret;
      if (!a.abc.isEmpty()) ret = ret + " (ABC: " + a.abc + ")";
      if (!a.xyz.isEmpty()) ret = ret + " (XYZ: " + a.xyz + ")";
      return ret;
  }

  private static RefAbcStruct getFromSAP(String lgort, String matnr) throws Exception {
    // получение данных о материале из САП (с помещением в справочник)

    Z_TS_ABC f = new Z_TS_ABC();
    f.LGORT = lgort;
    f.MATNR = SAPconn.fillZeros(matnr, 18);

    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    //if ((f.ABC == null) || f.ABC.isEmpty()) {
    //  return null;
    //}

    RefAbcStruct s = new RefAbcStruct(f.ABC, f.XYZ);
    return set(lgort, matnr, s);
  }
}
