package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.sys.SAPconn;
import ntx.sap.fm.Z_TS_CHARG;

/**
 * Справочник материалов. Номера материалов и партий - без ведущих нулей.
 */
public class RefCharg {

  private static final Map<String, RefChargStruct> ref = Collections.synchronizedMap(new HashMap<String, RefChargStruct>(3000));

  public static RefChargStruct get(String charg) throws Exception {
    return get(charg, null);
  }

  public static RefChargStruct get(String charg, String vbeln) throws Exception {
    RefChargStruct ret = ref.get(charg);
    if (ret != null) {
      return ret;
    }

    return getFromSAP(charg, vbeln);
  }

  public static RefChargStruct getNoNull(String charg) throws Exception {
    return getNoNull(charg, null);
  }

  public static RefChargStruct getNoNull(String charg, String vbeln) throws Exception {
    RefChargStruct ret = null;
    try {
      ret = get(charg, vbeln);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefChargStruct("???");
    }
    return ret;
  }

  public static RefChargStruct set(String charg, RefChargStruct s) throws Exception {
    // сохранение данных о материале
    synchronized (ref) {
      RefChargStruct s1 = ref.get(charg);
      if ((s1 == null) || !s1.equals(s)) {
        ref.put(charg, s);
        return s;
      } else {
        return s1;
      }
    }
  }

  private static RefChargStruct getFromSAP(String charg, String vbeln) throws Exception {
    // получение данных о материале из САП (с помещением в справочник)

    Z_TS_CHARG f = new Z_TS_CHARG();
    f.CHARG = SAPconn.fillZeros(charg, 10);
    if (vbeln != null) {
      f.VBELN = SAPconn.fillZeros(vbeln, 10);
    }

    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    if ((f.MATNR == null) || f.MATNR.isEmpty()) {
      return null;
    }

    String matnr = SAPconn.delZeros(f.MATNR);
    RefChargStruct s = new RefChargStruct(matnr);
    RefMat.set(matnr, new RefMatStruct(matnr, f.MAKTX, f.FULL_NAME, f.HAVE_FOTO));
    return set(charg, s);
  }
}
