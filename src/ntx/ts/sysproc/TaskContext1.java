package ntx.ts.sysproc;

/**
 * контекст выполнения задачи (доступ к глобальным переменным)
 */
public class TaskContext1 extends ProcessContext {

  public ProcessUtil task;

  public TaskContext1(ProcessContext ctx, ProcessUtil task) {
    super(ctx.track);
    this.task = task;
  }
}
