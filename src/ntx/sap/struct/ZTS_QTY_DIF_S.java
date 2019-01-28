package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_QTY_DIF_S {

  public BigDecimal QTY_VBEL = new BigDecimal(0); // Количество поставки
  public BigDecimal QTY_SCAN = new BigDecimal(0); // Количество со сканера
  public BigDecimal QTY_NEDOST = new BigDecimal(0); // Кол-во недостача
  public BigDecimal QTY_IZL = new BigDecimal(0); // Кол-во излишки
  public BigDecimal QTY_PRT = new BigDecimal(0); // Кол-во пересорт партий
}
