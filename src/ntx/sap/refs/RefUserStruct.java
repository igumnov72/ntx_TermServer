package ntx.sap.refs;

import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о пользователе
 */
public class RefUserStruct {

  public String name;

  public RefUserStruct(String name) {
    this.name = name;
  }

  public boolean equals(RefUserStruct m) {
    return ProcessTask.strEq(name, m.name);
  }
}
