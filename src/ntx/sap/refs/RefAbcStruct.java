package ntx.sap.refs;

import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о признаке ABC
 */
public class RefAbcStruct {

  public String abc;
  private long tim;

  public RefAbcStruct(String abc) {
    this.abc = abc;
    this.tim = System.currentTimeMillis();
  }
  
  public boolean isValid() {
    return System.currentTimeMillis() < tim + 28800; // значение считается правильным в течении 8 часов после получения
  }

  public boolean equals(RefAbcStruct m) throws Exception {
    return ProcessTask.strEq(abc, m.abc);
  }
}
