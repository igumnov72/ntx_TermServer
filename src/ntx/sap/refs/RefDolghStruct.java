package ntx.sap.refs;

import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о должности
 */
public class RefDolghStruct {

  public String name;

  public RefDolghStruct(String name) {
    this.name = name;
  }

  public boolean equals(RefDolghStruct m) {
    return ProcessTask.strEq(name, m.name);
  }
}
