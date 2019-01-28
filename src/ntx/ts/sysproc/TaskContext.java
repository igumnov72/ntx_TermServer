package ntx.ts.sysproc;

/**
 * контекст выполнения задачи пользователя (доступ к глобальным переменным)
 */
public class TaskContext extends UserContext {

  public ProcessTask task;

  public TaskContext(UserContext ctx, ProcessTask task) {
    super(ctx, ctx.user);
    this.task = task;
  }
}
