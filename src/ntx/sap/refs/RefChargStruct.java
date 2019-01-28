package ntx.sap.refs;

import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о партии
 */
public class RefChargStruct {

  public String matnr;

  public RefChargStruct(String matnr) {
    this.matnr = matnr;
  }

  public boolean equals(RefChargStruct m) throws Exception {
    return ProcessTask.strEq(matnr, m.matnr);
  }
}
