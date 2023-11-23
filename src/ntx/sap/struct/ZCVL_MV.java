package ntx.sap.struct;

import java.math.BigDecimal;

public class ZCVL_MV {

  public String IDX = "0"; // Numc3, внутр. использование
  public String MATNR = ""; // Номер материала
  public String MAKTX = ""; // Краткий текст материала
  public String CHARG = ""; // Номер партии
  public String LGPLA = ""; // Складское место
  public BigDecimal QTY = new BigDecimal(0); // Количество
  public String TANUM = "0"; // Номер транспортного заказа
  public BigDecimal PICKED = new BigDecimal(0); // Количество
  public String KQUIT = ""; // Индикатор: утверждение
  public BigDecimal TO_PICK = new BigDecimal(0); // Количество
}
