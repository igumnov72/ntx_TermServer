package ntx.ts.sysproc;

import ntx.ts.srv.DataRecord;

/**
 * Предок для классов - контейнеров данных процессов
 */
public abstract class ProcData {

  public abstract void handleData(DataRecord dr, ProcessContext ctx) throws Exception;
  
  public static boolean strEq(String s1, String s2) {
    // сравнение строк
    // считаем null == ""
    if (((s1 == null) || s1.isEmpty()) && ((s2 == null) || s2.isEmpty())) {
      return true;
    } else if ((s1 == null) || s1.isEmpty() || (s2 == null) || s2.isEmpty()) {
      return false;
    } else {
      return s1.equals(s2);
    }
  }
}
