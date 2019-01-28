package ntx.ts.sysproc;

import ntx.ts.srv.Track;

/**
 * контекст выполнения процесса (доступ к глобальным переменным)
 */
public class ProcessContext {

  public Track track;

  public ProcessContext(Track track) {
    this.track = track;
  }
}
