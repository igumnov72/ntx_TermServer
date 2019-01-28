package ntx.ts.admin;

import ntx.ts.srv.ProcType;

/**
 * Описание задачи пользователя
 */
public class TaskDescr {

  public ProcType procType;
  public String taskName = null;
  public long finTime = 0;
  public long recCount = 1;
  public boolean userTask = false;

  public TaskDescr(ProcType procType) {
    this.procType = procType;
  }
}
