package ntx.ts.srv;

import java.util.ArrayList;
import java.util.Map;

/**
 * отметка элементов отображения (для вызова в цикле) для последующего их
 * удаления (вне цикла)
 */
public class MapItemArray {

  private final Map m;
  private final ArrayList<Object> a;

  public MapItemArray(Map m) {
    this.m = m;
    a = new ArrayList<Object>();
  }

  public void MarkItem(Object item) {
    a.add(item);
  }

  public void RemoveAll() {
    int n = a.size();
    for (int i = 0; i < n; i++) {
      m.remove(a.get(i));
    }
  }
}
