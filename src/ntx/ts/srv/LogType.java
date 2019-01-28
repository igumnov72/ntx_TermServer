package ntx.ts.srv;

/**
 * Типы регистрируемых (в САПе) событий !!! ДОБАВЛЕНИЕ НОВЫХ ТИПОВ - ТОЛЬКО В
 * КОНЕЦ !!!
 */
public enum LogType {
  VOID, // не должно использоваться
  TASK_ADD, // добавление задачи (с переключением на нее)
  TASK_ACTIVATE, // переключение на задачу
  TASK_DEACTIVATE, // временный выход из задачи
  ADD_VBELN,
  SET_LGORT,
  SET_CELL,
  SET_VBELN,
  SET_MOD_SGM,
  ADD_TOV,
  ADD_MOD_SGM_TOV,
  ADD_SGM,
  SET_PAL_VBELN_LGORT,
  SET_CELL_VBELN,
  USER_IN,
  USER_OUT,
  SET_PAL;
}
