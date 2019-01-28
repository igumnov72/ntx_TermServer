package ntx.ts.sysproc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.Track;

/**
 * Поддержка списка задач в процессе пользователя
 */
public class HandlerTaskList {

  public final int initSize; // начальный размер массива задач
  private long[] taskIds; // иды задач пользователя
  private String[] taskNames; // описания задач пользователя
  private boolean taskIsActive = false;
  private ProcessTask activeTask = null; // ссылка на активную задачу
  private int activeTaskCount = 0;
  private int taskCount = 0;
  public boolean taskFinished = false;
  private long procId;
  protected static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
  protected static final DateFormat df2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  public void setTaskCount(int taskCount) {
    this.taskCount = taskCount;
  }

  public HandlerTaskList(long procId, int initSize) {
    this.procId = procId;
    this.initSize = initSize;
    taskIds = new long[initSize];
    taskNames = new String[initSize];
  }

  public ProcessTask getActiveTask(UserContext ctx) throws Exception {
    if (!taskIsActive || (taskIds[0] == 0)) {
      activeTask = null;
      return null;
    }

    if (activeTask == null) {
      activeTask = (ProcessTask) ctx.track.loadProcess(taskIds[0]);
    }

    return activeTask;
  }

  public int getActiveTaskCount() {
    return activeTaskCount;
  }

  public int getTaskCount() {
    return taskCount;
  }

  public long getTaskId(int i) {
    return taskIds[i];
  }

  public String getTaskName(int i) {
    return taskNames[i];
  }

  public void callTaskAdd(long taskId, String taskName, UserContext ctx) throws Exception {
    // добавление задачи в начало массива
    // задача автоматически активируется

    if (taskId == 0) {
      throw new Exception("Ошибка программы: не указан ид задачи");
    }
    if ((taskName == null) || taskName.isEmpty()) {
      throw new Exception("Ошибка программы: не указано название задачи");
    }
    for (int i = 0; i < taskIds.length; i++) {
      if (taskIds[i] == taskId) {
        throw new Exception("Ошибка программы: добавление существующей задачи");
      }
    }

    DataRecord dr = new DataRecord();
    dr.procId = procId;
    dr.setL(FieldType.TASK_ADD, taskId);
    dr.setS(FieldType.TASK_NAME, taskName);
    dr.setB(FieldType.TASK_IS_ACTIVE, true);
    dr.setI(FieldType.LOG, LogType.TASK_ADD.ordinal());
    Track.saveProcessChange(dr, null, ctx);
  }

  public void callTaskActivate(long taskId, UserContext ctx) throws Exception {
    if (taskId == 0) {
      throw new Exception("Ошибка программы: не указан ид задачи");
    }
    if (taskIsActive && (taskIds[0] == taskId)) {
      return;
    }

    for (int i = 0; i < taskIds.length; i++) {
      if (taskIds[i] == taskId) {
        DataRecord dr = new DataRecord();
        dr.procId = procId;
        if (i > 0) {
          dr.setL(FieldType.TASK_ADD, taskId);
        }
        if (!taskIsActive) {
          dr.setB(FieldType.TASK_IS_ACTIVE, true);
        }
        dr.setI(FieldType.LOG, LogType.TASK_ACTIVATE.ordinal());
        dr.setL(FieldType.TASK_NO, taskId);
        Track.saveProcessChange(dr, null, ctx);
        getActiveTask(ctx);
        return;
      }
    }

    throw new Exception("Ошибка программы: активация несуществующей задачи пользователя " + df2.format(new Date(taskId)));
  }

  public void callTaskDeactivate(UserContext ctx) throws Exception {
    DataRecord dr = new DataRecord();
    dr.procId = procId;
    if (taskIsActive) {
      dr.setB(FieldType.TASK_IS_ACTIVE, false);
      if ((taskIds != null) && (taskIds.length > 0)) {
        dr.setI(FieldType.LOG, LogType.TASK_DEACTIVATE.ordinal());
        dr.setL(FieldType.TASK_NO, taskIds[0]);
      }
    }
    Track.saveProcessChange(dr, null, ctx);

    ctx.track.log.save();
  }

  public void callTaskDel(long taskId, UserContext ctx) throws Exception {
    if (taskId == 0) {
      throw new Exception("Ошибка программы: не указан ид задачи");
    }
    for (int i = 0; i < taskIds.length; i++) {
      if (taskIds[i] == taskId) {
        DataRecord dr = new DataRecord();
        dr.procId = procId;
        dr.setL(FieldType.TASK_DEL, taskId);
        if (taskIsActive) {
          dr.setB(FieldType.TASK_IS_ACTIVE, false);
          dr.setI(FieldType.LOG, LogType.TASK_DEACTIVATE.ordinal());
//          dr.setL(FieldType.TASK_NO, taskId);
        }
        Track.saveProcessChange(dr, null, ctx);
        taskFinished = true;
      }
    }

    ctx.track.log.save();
  }

  public void callTaskCreate(ProcType ptype, UserContext ctx) throws Exception {
    ProcessTask p = (ProcessTask) Track.saveProcessNew(ptype, new DataRecord(), ctx);
    p.callSetUserId(procId, p.procName(), ctx);
    getActiveTask(ctx);
  }

  public void callTaskNameChange(long task, String name, UserContext ctx) throws Exception {
    if (task == 0) {
      throw new Exception("Ошибка программы: не указан ид задачи");
    }
    if ((name == null) || name.isEmpty()) {
      throw new Exception("Ошибка программы: не указано название задачи");
    }
    for (int i = 0; i < taskIds.length; i++) {
      if (taskIds[i] == task) {
        DataRecord dr = new DataRecord();
        dr.procId = procId;
        dr.setL(FieldType.TASK_ADD, task);
        dr.setS(FieldType.TASK_NAME, name);
        dr.setB(FieldType.TASK_IS_ACTIVE, true);
        if (!taskIsActive || (i != 0)) {
          dr.setI(FieldType.LOG, LogType.TASK_ACTIVATE.ordinal());
//          dr.setL(FieldType.TASK_NO, task);
        }
        Track.saveProcessChange(dr, null, ctx);
        return;
      }
    }

    throw new Exception("Ошибка программы: изменение названия несуществующей задачи");
  }

  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    if (dr.haveVal(FieldType.TASK_ADD)) {
      // добавление задачи в начало списка или её перемещение в начало списка
      long task = (Long) dr.getVal(FieldType.TASK_ADD);
      String name = (String) dr.getVal(FieldType.TASK_NAME);
      int taskIdx = -1;
      for (int i = 0; i < taskIds.length; i++) {
        if (taskIds[i] == task) {
          taskIdx = i;
          break;
        }
      }
      if (taskIdx == -1) {
        // добавление задачи
        if (taskIds[taskIds.length - 1] != 0) {
          // увеличиваем размер массивов
          long[] tIds = new long[taskIds.length + initSize];
          String[] tNames = new String[taskIds.length + initSize];
          for (int i = 0; i < taskIds.length; i++) {
            tIds[i] = taskIds[i];
            tNames[i] = taskNames[i];
          }
          taskIds = tIds;
          taskNames = tNames;
        }
        for (int i = taskIds.length - 1; i > 0; i--) {
          taskIds[i] = taskIds[i - 1];
          taskNames[i] = taskNames[i - 1];
        }
        taskIds[0] = task;
        taskNames[0] = (name == null ? "???" : name);
        taskCount++;
        activeTaskCount++;
      } else if (taskIdx > 0) {
        // перемещение задачи
        if (task == taskIds[0]) {
          if (name != null) {
            taskNames[0] = name;
          }
        } else {
          for (int i = 1; i < taskIds.length; i++) {
            if (task == taskIds[i]) {
              if (name == null) {
                name = taskNames[i];
              }
              for (int j = i; j > 0; j--) {
                taskIds[j] = taskIds[j - 1];
                taskNames[j] = taskNames[j - 1];
              }
              taskIds[0] = task;
              taskNames[0] = name;
              break;
            }
          }
        }
      } else if ((taskIdx == 0) && (name != null) && !name.isEmpty()) {
        // переименовываем первую задачу
        taskNames[0] = name;
      }
      if ((taskIdx != 0) && (activeTask != null)) {
        ctx.track.unloadProcess(activeTask.getProcId());
        activeTask = null;
      }
    }
    if (dr.haveVal(FieldType.TASK_DEL)) {
      long task = (Long) dr.getVal(FieldType.TASK_DEL);
      for (int i = 0; i < taskIds.length; i++) {
        if (taskIds[i] == task) {
          for (int j = i; j < taskIds.length - 1; j++) {
            taskIds[j] = taskIds[j + 1];
            taskNames[j] = taskNames[j + 1];
          }
          taskIds[taskIds.length - 1] = 0;
          taskNames[taskIds.length - 1] = null;
          activeTaskCount--;
          break;
        }
      }
      if (activeTask != null) {
        ctx.track.unloadProcess(activeTask.getProcId());
        activeTask = null;
      }
    }
    if (dr.haveVal(FieldType.TASK_IS_ACTIVE)) {
      taskIsActive = (Boolean) dr.getVal(FieldType.TASK_IS_ACTIVE);
      if (activeTask != null) {
        ctx.track.unloadProcess(activeTask.getProcId());
        activeTask = null;
      }
    }
  }
}
