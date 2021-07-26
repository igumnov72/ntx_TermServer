package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_LG_STOCK_S {

  public String LGNUM = ""; // Номер склада/комплекс
  public String LGTYP = ""; // Тип склада
  public String LGPLA = ""; // Складское место
  public String LENUM = ""; // № единицы складирования
  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public String SOBKZ = ""; // Код особого запаса
  public String SONUM = ""; // Номер особого запаса
  public BigDecimal GESME = new BigDecimal(0); // Общее количество
  public BigDecimal VERME = new BigDecimal(0); // Доступный запас
  public BigDecimal EINME = new BigDecimal(0); // Принимаемый запас
  public BigDecimal AUSME = new BigDecimal(0); // Отпускаемое количество
}
