package ntx.sap.struct;

import java.math.BigDecimal;

public class ZTS_COMPL_DONE_TO_PAL_S {

  public int SGM = 0; // Номер СГМ
  public String LENUM = ""; // № единицы складирования
  public String TO_PAL = ""; // На какую паллету идет комплектация
  public String MATNR = ""; // Номер материала
  public String CHARG = ""; // Номер партии
  public BigDecimal QTY = new BigDecimal(0); // Количество
  public String SHK = ""; // Штрих-код
}
