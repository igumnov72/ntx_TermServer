package ntx.ts.srv;

/**
 * Типы значений полей для DataRecord (используется в FieldType)
 */
public enum ValType {

  INT(false),
  LONG(false),
  STRING(false),
  DEC(false),
  DATE(false),
  BOOL(false),
  VOID(false),
  INT_AR(true),
  LONG_AR(true),
  STRING_AR(true),
  DEC_AR(true),
  DATE_AR(true),
  BOOL_AR(true);
  //
  public final boolean isArray;

  ValType(boolean isArray) {
    this.isArray = isArray;
  }
}
