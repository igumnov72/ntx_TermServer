package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_LOG_S {

  public String LOG_TYP = ""; // Тип записи (log)
  public String LOG_DT = ""; // Дата/время из Java (число миллисекунд)
  public String TASK_ID = ""; // Id задачи пользователя на терминале
  public String PTYP = ""; // Тип задачи пользователя терминала
  public String SOTR = ""; // Сотрудник
  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY = new BigDecimal(0); // Количество
  public String LENUM = ""; // № единицы складирования
  public String VBELN = ""; // Номер документа сбыта
  public String LGPLA = ""; // Складское место
  public String LGORT = ""; // Склад
}
