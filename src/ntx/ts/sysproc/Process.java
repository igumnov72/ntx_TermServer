package ntx.ts.sysproc;

import java.text.*;
import java.util.Date;
import ntx.ts.http.FileData;
import ntx.ts.srv.*;

/**
 * Этот класс является базовым для всех процессов в системе.
 * К процессам относятся как задачи на терминале (типа приемки или размещения),
 * так и пользователи и системные настройки.
 * Изменение состояния процесса всегда (кроме интерфейсных вещей, типа вход в меню)
 * выполняется через трак (Track). То есть подготавливается объект DataRecord,
 * записывается на трак, и уже после этого с трака вызывается метод
 * HandleData, который информацию из DataRecord распихивает по переменным,
 * определяющим состояние процесса. Напрямую должны меняться только переменные,
 * ответственные за врЕменные (интерфейсные) изменения состояния.
 * Все взаимные влияния процессов друг на друга так же должны делаться через трак.
 * Примером является изменение списка дочерних процессов при создании нового процесса
 * с этим родителем.
 * Создание, изменение и завершение процессов должно выполняться только
 * через соответствующие методы класса Track. Загрузка и выгрузка процессов
 * так же должна выполняться через Track.
 * Конструктор процесса не должен выполнять никаких действий кроме присвоения типа
 * и идентификатора процесса; вся инициализация должна быть в обработке записи CREATE.
 * Рекомендуется при написании кода процесса создавать специальные процедуры
 * для изменения данных, в которых будут заполняться и передаваться в Track
 * соответствующие DataRecord. Имена этих процедур рекомендуется начинать с "call".
 * В процедуре handleData описывается изменение состояния объекта по данным с трака.
 * В handleData должны использоваться только данные о состоянии самого объекта
 * и запись с трака. Внешние данные не должны использоваться. Например, не должны
 * использоваться данные пользователя и не должно быть вызовов САПа. Все внешние
 * данные, влияющие на состояние объекта, должны анализироваться в процедурах call...
 * при создании записи трака. По возможности handleData не должна генерировать ошибки.
 * Задачи, выполняемые пользователем на терминале, нужно наследовать от ProcessTask.
 *
 * Написание кода выполнения задач на терминале требует определенной дисциплины,
 * поскольку для изменения состояния объекта (задачи) вместо простого присвоения
 * нового значения переменной (полю объекта) требуется выполнить сразу три действия:
 *   1. Написать процедуру call запуска изменения значения поля (при необходимости
 *      создать новый тип поля).
 *   2. Доработать процедуру handleData, чтобы она отрабатывала новые запросы
 *      на изменение состояния объекта.
 *   3. Вызвать процедуру call там где требуется изменить состояние объекта.
 */
public abstract class Process {

  private boolean unloaded = false;
  private final long procId; // ID процесса
  private final ProcType procType;
  protected static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
  protected static final DateFormat df2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  public abstract FileData handleQuery(TermQuery tq, ProcessContext ctx) throws Exception; // обработка инф с терминала
  public abstract void handleData(DataRecord dr, ProcessContext ctx) throws Exception;

  public final boolean getUnloaded() {
    // с выгруженными объектами не должно выполняться никаких операций
    return unloaded;
  }

  public final void setUnloaded() {
    unloaded = true;
  }

  protected Process(ProcType procType, long procId) throws Exception {
    this.procType = procType;
    this.procId = procId;
    if (procId == 0) {
      throw new Exception("Не указан ид процесса");
    }
    if (procType == ProcType.VOID) {
      throw new Exception("Не указан тип процесса");
    }
  }

  public final long getProcId() {
    return procId;
  }

  public final ProcType getProcType() {
    return procType;
  }

  public String procName() {
    // текстовое описание процесса, может быть переопределена
    return procType.text + " " + df2.format(new Date(procId));
  }
}
