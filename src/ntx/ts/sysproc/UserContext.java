package ntx.ts.sysproc;

import ntx.ts.srv.Track;

/**
 * контекст выполнения задачи пользователя (доступ к глобальным переменным)
 */
public class UserContext extends ProcessContext {

  public ProcessUser user;

  public UserContext(ProcessContext ctx, ProcessUser user) {
    super(ctx.track);
    this.user = user;
  }
}
