package ntx.ts.srv;

/**
 * типы процессов
 */
public enum ProcType {

  VOID("???", false), // не должен использоваться
  SYSTEM("Система", false), // системные параметры (сейчас не используется, на всякий случай)
  USER("Пользователь", false), // обеспечение работы пользователя
  PLACEMENT("Размещение", 2), // свободное размещение при приемке
  TEST("Тест", 9), // простой тестовый процесс
  SKL_MOVE("Перемещение", 3), // перемещение по складу
  PRIEMKA("Приемка", 1),
  INVENT("Инвентаризация", 6),
  COMPL("Комплектация", 5),
  PRODPRI("Приход с пр-ва", 7),
  DPDT("Отгрузка ДПДТ", 8),
  POPOLN("Пополнение зоны АК", 4),
  WP("Рабочее место", true), // рабочее место в пр-ве
  VOZVRAT("Возврат клиента", 1),
  OPIS("Опись паллеты", 1), // коробки на паллете
  VYGR("Выгрузка машины", 10), // ввод данных по выгрузке машины
  PEREUP1("В переупаковку", 11), // отправка паллеты в переупаковку
  PEREUP2("Из переупаковки", 12), // размещение товара из переупаковки
  OPISK("Покороб опись", 13), // покоробочная опись
  COMPL_MOVE("Перемещение скомпл. товара", 15), // перемещение скомплектованного товара
  PROGRES("Перемещение с Прогресса", 16), // перемещение товара с Прогресса
  SKOROB("Сборный короб", 14), // сборный короб
  SHK_LIST("Список ШК",17),
  PROGOPIS("Прогресс опись",18);
  //
  public final String text;
  public final boolean isUserProc;
  public final boolean canFinish;
  public final int order;

  ProcType(String text, int order) {
    this.text = text;
    this.order = order;
    this.isUserProc = true;
    this.canFinish = true;
  }

  ProcType(String text, boolean canFinish) {
    this.text = text;
    this.order = 0;
    this.isUserProc = false;
    this.canFinish = canFinish;
  }
}
