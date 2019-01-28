package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_IN12_S {

  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY_SCAN = new BigDecimal(0); // Количество со сканера
  public BigDecimal QTY_V = new BigDecimal(0); // Количество поставки
  public BigDecimal QTY_DIF = new BigDecimal(0); // Разница кол-ва
  public String PALS = ""; // Список номеров паллет
}
