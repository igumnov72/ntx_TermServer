package ntx.sap.refs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ntx.sap.fm.Z_TS_DOLGH;

/**
 * Справочник должностей
 */
public class RefDolgh {

  private static final Map<Integer, RefDolghStruct> ref = Collections.synchronizedMap(new HashMap<Integer, RefDolghStruct>(100));

  public static RefDolghStruct get(int dolgh) throws Exception {
    RefDolghStruct ret = ref.get(dolgh);
    if (ret != null) {
      return ret;
    }

    return getFromSAP(dolgh);
  }

  public static RefDolghStruct getNoNull(int dolgh) throws Exception {
    RefDolghStruct ret = null;
    try {
      ret = get(dolgh);
    } catch (Exception e) {
    }
    if (ret == null) {
      ret = new RefDolghStruct("???");
    }
    return ret;
  }

  public static RefDolghStruct set(int dolgh, RefDolghStruct s) {
    // сохранение данных о складе
    synchronized (ref) {
      RefDolghStruct s1 = ref.get(dolgh);
      if (s1 == null || !s1.equals(s)) {
        ref.put(dolgh, s);
        return s;
      } else {
        return s1;
      }
    }
  }

  private static RefDolghStruct getFromSAP(int dolgh) throws Exception {
    // получение данных о складе из САП (с помещением в справочник)

    Z_TS_DOLGH f = new Z_TS_DOLGH();
    f.DOLGH_ID = dolgh;
    f.execute();
    if (f.isErr) {
      throw new Exception(f.err);
    }
    if ((f.DOLGH_NAME == null) || f.DOLGH_NAME.isEmpty()) {
      return null;
    }

    RefDolghStruct s = new RefDolghStruct(f.DOLGH_NAME);
    return set(dolgh, s);
  }
}
