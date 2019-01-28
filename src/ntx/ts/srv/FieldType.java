package ntx.ts.srv;

/**
 * Типы полей для DataRecord. !!! ДОБАВЛЕНИЕ НОВЫХ ТИПОВ - ТОЛЬКО В КОНЕЦ !!!
 */
public enum FieldType {

  PROC_TYPE(ValType.INT), // тип процесса (указывается только при создании)
  PARENT(ValType.LONG), // родительский процесс (для привязки задач к пользователям)
  NAME(ValType.STRING), // имя пользователя (и другие названия)
  PASSWORD(ValType.STRING), // пароль
  SHK(ValType.STRING), // штрих-код
  IS_ADMIN(ValType.BOOL), // права администратора
  LOCKED(ValType.BOOL), // наличие блокировки (пользователя)
  TERM(ValType.LONG), // терминал
  TASK_ADD(ValType.LONG), // добавляемая/активируемая задача пользователя
  TASK_NAME(ValType.STRING), // название задачи пользователя
  TASK_IS_ACTIVE(ValType.BOOL), // признак активности задачи пользователя
  TASK_DEL(ValType.LONG), // удаляемая задача пользователя
  SCAN(ValType.STRING), // описание сканирования
  DEL_LAST(ValType.VOID), // команда удаления последнего сканирования
  LAST_TASK_TYPE(ValType.STRING), // последний выбранный пользователем тип задачи
  ERR(ValType.STRING), // ошибка после последнего сканирования
  MSG(ValType.STRING), // сообщение после последнего сканирования
  TASK_STATE(ValType.INT), // состояние выполнения процесса
  ADD_HIST(ValType.STRING), // добавление сообщения в историю
  PAL(ValType.STRING), // паллета
  CELL(ValType.STRING), // ячейка
  LGNUMS(ValType.STRING_AR), // массив номеров склада
  TANUMS(ValType.STRING_AR), // массив номеров трансп заказов
  LGORTS(ValType.STRING), // список складов через запятую
  VBELN(ValType.STRING), // номер поставки
  LGORT(ValType.STRING), // номер склада
  PAL2(ValType.STRING), // паллета 2
  CELL2(ValType.STRING), // ячейка 2
  LGTYP1(ValType.STRING),
  LGTYP2(ValType.STRING),
  CLEAR_TOV_DATA(ValType.VOID), // удаление данных о товаре
  CHARG(ValType.STRING), // номер партии (без ведущих нулей)
  MATNR(ValType.STRING), // номер материала (без ведущих нулей)
  QTY(ValType.DEC), // кол-во
  KEY1(ValType.INT), // номер строки в журнале
  INPUT(ValType.STRING), // полученный скан или выбор меню
  CHECK_DP(ValType.BOOL), // признак необходимости ввода дат пр-ва
  LAST_SCAN(ValType.STRING), // последнее сканирование
  DAT_PR(ValType.STRING), // дата пр-ва в формате ГГГГММДД
  N_SCAN(ValType.INT), // число сканирований
  PTYP(ValType.INT), // тип процесса (задачи)
  INFO_ID(ValType.INT), // ид последней информации
  LGNUM(ValType.STRING), // номер адресного склада
  IS_RET(ValType.BOOL), // признак возврата
  MAT1(ValType.BOOL), // один материал на паллете
  IVNUM(ValType.STRING), // номер инвентаризации САП
  MISCH(ValType.STRING), // X - признак нескольких паллет в ячейке
  WERKS(ValType.STRING), // завод
  INV_ID(ValType.INT), // номер строки в журнале инвентаризаций
  MATNRS(ValType.STRING_AR), // массив номеров материалов
  CHARGS(ValType.STRING_AR), // массив номеров партий
  QTYS(ValType.DEC_AR), // массив с кол-вом
  ASK_QTY(ValType.BOOL), // запрос кол-ва
  DEL_ALL(ValType.VOID), // команда удаления всех данных
  DEL_PAL(ValType.STRING), // команда удаления данных по паллете
  LAST_COMPR_DATE(ValType.DATE), // последняя дата сжатия файла данных
  STATE_TEXT(ValType.STRING), // текстовое описание состояния
  TAB1(ValType.VOID), // признак таблицы N 1
  TAB2(ValType.VOID), // признак таблицы N 2
  TAPOSS(ValType.STRING_AR), //
  LGTYPS(ValType.STRING_AR), //
  LGPLAS(ValType.STRING_AR), //
  LENUMS(ValType.STRING_AR), //
  ONLY_PRTS(ValType.STRING_AR), //
  COMPL_FROMS(ValType.STRING_AR), //
  SAVED(ValType.VOID), // сохранено (сброс данных)
  NERAZM(ValType.INT), // число неразмещенных паллет
  INF_COMPL(ValType.STRING), // признак информационной комплектации (X)
  EBELNS(ValType.STRING), // список заказов на поставку
  QTY_MAX(ValType.DEC), // максимальное кол-во
  TTYPES(ValType.INT_AR), // список типов задач (для прав пользователя)
  PRINTER(ValType.STRING), // принтер
  LGPLAS_FP(ValType.STRING_AR), //
  CLEAR_VED(ValType.VOID), // удаление ведомости на комплектацию по ячейке/поставке
  SOBKZS(ValType.STRING_AR),
  SONUMS(ValType.STRING_AR),
  COMPL_FROM(ValType.STRING),
  LGPLA_FP(ValType.STRING),
  FREE_COMPL(ValType.BOOL), // признак состояния свободной комплектации
  TAB3(ValType.VOID), // признак таблицы N 3
  TAB4(ValType.VOID), // признак таблицы N 4
  TAB5(ValType.VOID), // признак таблицы N 5
  ADDR_SKL(ValType.BOOL), // признак адресного склада
  TANUM1(ValType.STRING), // мин номер трансп заказа
  TANUM2(ValType.STRING), // макс номер трансп заказа
  DT(ValType.STRING), // дата-время в формате САП (YYYYMMDD HHMMSS)
  LAST_CELL(ValType.STRING), // последняя отсканированная ячейка
  CHECK_COMPL(ValType.STRING), // признак проверки наличия под комплектацию (X)
  IS_SGM(ValType.BOOL), // признак сканирования СГМ
  SGM(ValType.INT), // номер СГМ (коробки)
  LAST_CHARG(ValType.STRING), // последнее сканирование партии
  LAST_QTY(ValType.DEC), // последнее сканирование кол-ва
  WP_NUM(ValType.INT), // ид рабочего места
  WP_NAME(ValType.STRING), // название рабочего места (производственной линии)
  CEH_ID(ValType.INT), // ид цеха
  CEH_NAME(ValType.STRING), // название цеха
  IS_SMENA(ValType.BOOL), // признак начатой смены
  IS_PLANSHET(ValType.BOOL), // признак графического интерфейса (брайзер на планшете)
  IP(ValType.STRING), // ip-адрес, с которого поступал запрос
  CAN_FINISH(ValType.BOOL), // признак возможности завершения процесса (нет незаконченных мелких циклов)
  USER(ValType.STRING), // код пользователя
  HAVE_USER_DATA(ValType.BOOL), // признак наличия несохраненных данных по пользователю
  LAST_DOLGH(ValType.INT), // должность
  DOLGH(ValType.INT), // должность (сопоставление с совмещением)
  SOVM(ValType.INT), // процент совмещения
  ERR_ON_CANCEL(ValType.VOID), // признак ошибки при отмене
  STRICT_CHARGS(ValType.STRING_AR), // массив номеров партий под особым запасом (по материалам в ведомости по ячейке)
  POS_COUNTS(ValType.INT_AR), // массив числа позиций
  CAN_MQ(ValType.BOOL), // разрешение ручного ввода кол-ва (при компл, если оно запрещено по складу)
  NO_FREE_COMPL(ValType.BOOL), // запрет свободной комплектации
  MOD_SGM(ValType.INT), // пересканируемая СГМ
  MOD_SGM_QTY(ValType.DEC), // кол-во в СГМ (при пересканировании)
  MOD_SGM_CLEAR(ValType.VOID), // команда удаления всех данных по изменению СГМ
  MOD_SGM_DEL_LAST(ValType.VOID), // команда удаления последнего сканирования
  PREV_TASK_STATE(ValType.INT), // предыдущее состояние выполнения процесса
  LOG(ValType.INT), // тип события (для регистрации в САПе)
  TASK_NO(ValType.LONG), // номер задачи
  DO_PM(ValType.BOOL), // признак создания ПМ после размещения в ячейку
  DO_PM_MAIL(ValType.BOOL), // признак отправки письма о создании ПМ после размещения в ячейку
  STARTED(ValType.BOOL), // признак начала
  M3_MACH(ValType.DEC), // кубатера машины
  M3_GR(ValType.DEC), // кубатера груза
  ALL_CHARGS(ValType.STRING_AR), // массив всех номеров партий (по материалам в ведомости по ячейке)
  NEXT_CELL(ValType.STRING), // следующая ячейка
  NEXT_VBELN(ValType.STRING); // следующая поставка
  //
  public final ValType valType;

  FieldType(ValType valType) {
    this.valType = valType;
  }
}
