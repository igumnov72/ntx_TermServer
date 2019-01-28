package ntx.sap.refs;

import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о складе
 */
public class RefLgortStruct {

  public String name;
  public String lgnum;
  public String werks;

  public RefLgortStruct(String name, String lgnum, String werks) {
    this.name = name;
    this.lgnum = lgnum;
    this.werks = werks;
  }

  public boolean equals(RefLgortStruct m) {
    return ProcessTask.strEq(name, m.name) && ProcessTask.strEq(lgnum, m.lgnum) && ProcessTask.strEq(werks, m.werks);
  }
}
