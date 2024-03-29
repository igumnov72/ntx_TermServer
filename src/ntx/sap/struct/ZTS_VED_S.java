package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_VED_S {

  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  public String VBELN = ""; // Номер документа сбыта
  public String LENUM = ""; // № единицы складирования
  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY = new BigDecimal(0); // Количество
  public BigDecimal STOCK = new BigDecimal(0); // Количество
  public String FLOOR = ""; // Индикатор из одной позиции
  public String SRT = ""; // Текст длиной 20 знаков
}
