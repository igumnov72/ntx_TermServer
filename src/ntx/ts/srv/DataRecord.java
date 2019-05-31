package ntx.ts.srv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * сохранение данных в файле (и извлечение)
 */
public class DataRecord {

  public long procId = 0; // ID процесса
  public long recDt = 0; // дата-время записи
  public byte recType = 0; // 0 - изменение процесса, 1 - создание, 2 - удаление
  public int no = 0; // последовательная нумерация
  private Object[] fld = new Object[FieldType.values().length]; // массив всех переменных полей записи
  private static long maxDt = 0; // максимальный уникальный номер
  private static FieldType[] fldTypes = FieldType.values();
  private boolean changed = false; // признак изменения массива полей
  public int fieldCount = 0; // число полей, устанавливается при чтении с трака

  public void setI(FieldType tp, int val) throws Exception {
    if (tp.valType != ValType.INT) {
      throw new Exception("Ошибка программы: DataRecord.setI меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    changed = true;
  }

  public void setL(FieldType tp, long val) throws Exception {
    if (tp.valType != ValType.LONG) {
      throw new Exception("Ошибка программы: DataRecord.setL меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    changed = true;
  }

  public void setS(FieldType tp, String val) throws Exception {
    if (tp.valType != ValType.STRING) {
      throw new Exception("Ошибка программы: DataRecord.setS меняет поле другого типа");
    }
    fld[tp.ordinal()] = (val == null ? "" : val);
    changed = true;
  }

  public void setN(FieldType tp, BigDecimal val) throws Exception {
    if (tp.valType != ValType.DEC) {
      throw new Exception("Ошибка программы: DataRecord.setN меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setD(FieldType tp, Date val) throws Exception {
    if (tp.valType != ValType.DATE) {
      throw new Exception("Ошибка программы: DataRecord.setD меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setB(FieldType tp, boolean val) throws Exception {
    if (tp.valType != ValType.BOOL) {
      throw new Exception("Ошибка программы: DataRecord.setB меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    changed = true;
  }

  public void setV(FieldType tp) throws Exception {
    if (tp.valType != ValType.VOID) {
      throw new Exception("Ошибка программы: DataRecord.setV меняет поле другого типа");
    }
    fld[tp.ordinal()] = new Object();
    changed = true;
  }

  public void setIa(FieldType tp, int[] val) throws Exception {
    if (tp.valType != ValType.INT_AR) {
      throw new Exception("Ошибка программы: DataRecord.setIa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setLa(FieldType tp, long[] val) throws Exception {
    if (tp.valType != ValType.LONG_AR) {
      throw new Exception("Ошибка программы: DataRecord.setLa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setSa(FieldType tp, String[] val) throws Exception {
    if (tp.valType != ValType.STRING_AR) {
      throw new Exception("Ошибка программы: DataRecord.setSa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setNa(FieldType tp, BigDecimal[] val) throws Exception {
    if (tp.valType != ValType.DEC_AR) {
      throw new Exception("Ошибка программы: DataRecord.setNa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setDa(FieldType tp, Date[] val) throws Exception {
    if (tp.valType != ValType.DATE_AR) {
      throw new Exception("Ошибка программы: DataRecord.setDa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public void setBa(FieldType tp, boolean[] val) throws Exception {
    if (tp.valType != ValType.BOOL_AR) {
      throw new Exception("Ошибка программы: DataRecord.setBa меняет поле другого типа");
    }
    fld[tp.ordinal()] = val;
    if (val != null) {
      changed = true;
    }
  }

  public boolean isChanged() {
    return changed;
  }

  public boolean haveVal(FieldType tp) {
    return (fld[tp.ordinal()] != null);
  }

  public boolean haveStrVal(FieldType tp) {
    return (fld[tp.ordinal()] != null) && !((String) fld[tp.ordinal()]).isEmpty();
  }

  public Object getVal(FieldType tp) {
    return fld[tp.ordinal()];
  }

  public String getValStr(FieldType tp) {
    String s = (String) fld[tp.ordinal()];
    return (s == null) || s.isEmpty() ? null : s;
  }

  public void clearAll() {
    procId = 0;
    recDt = 0;
    recType = 0;
    changed = false;
    fieldCount = 0;
    for (int i = 0; i < fld.length; i++) {
      fld[i] = null;
    }
  }

  private static String readString(DataInputStream ds) throws Exception {
    int n = ds.readInt(); // длина массива символов

    char[] ar = new char[n];
    for (int i = 0; i < n; i++) {
      ar[i] = ds.readChar();
    }
    return new String(ar);
  }

  private static void writeString(DataOutputStream ds, String txt) throws Exception {
    char[] ar = txt.toCharArray();
    int n = ar.length;
    ds.writeInt(n); // длина массива символов

    for (int i = 0; i < n; i++) {
      ds.writeChar(ar[i]);
    }
  }

  private static void writeBigDecimal(DataOutputStream ds, BigDecimal n) throws Exception {
    byte[] ar = n.unscaledValue().toByteArray();
    ds.writeInt(ar.length); // длина массива

    for (int i = 0; i < ar.length; i++) {
      ds.writeByte(ar[i]);
    }

    ds.writeInt(n.scale());
  }

  private static BigDecimal readBigDecimal(DataInputStream ds) throws Exception {
    int len = ds.readInt();
    byte[] ba = new byte[len];
    for (int i = 0; i < len; i++) {
      ba[i] = ds.readByte();
    }

    BigInteger bigInt = new BigInteger(ba);
    int scale = ds.readInt();
    return new BigDecimal(bigInt, scale);
  }

  public static synchronized long correctDt(long dt) {
    long ret = dt;
    if (ret <= maxDt) {
      ret = maxDt + 1;
    }
    maxDt = ret;
    return ret;
  }

  public static synchronized long correctDt2(long dt) {
    if (dt < maxDt) {
      return maxDt;
    }
    maxDt = dt;
    return dt;
  }

  public static void clearMaxDt() {
    maxDt = 0;
  }

  public void write(DataOutputStream ds, Track track) throws Exception {
    write2(ds, false, track);
    track.log.add(this);
  }

  public void write2(DataOutputStream ds, boolean oldRec, Track track) throws Exception {
    if ((recType == 0) && !changed) {
      return;
    }

//    synchronized (Track.lockWrite) {
    // проверка типа записи
    if ((recType < 0) || (recType > 3)) {
      throw new Exception("Ошибка программы: неверная recType");
    }
    if (recType == 1) {
      if (fld[FieldType.PROC_TYPE.ordinal()] == null) {
        throw new Exception("Ошибка программы: не указан тип создаваемого процесса");
      }
    } else {
      if (fld[FieldType.PROC_TYPE.ordinal()] != null) {
        throw new Exception("Ошибка программы: указан тип изменяемого процесса");
      }
      if (recType == 3) {
        if (procId != 0) {
          throw new Exception("Ошибка программы: указан ид для системного процесса");
        }
      } else if (procId == 0) {
        throw new Exception("Ошибка программы: не указан ид изменяемого процесса");
      }
    }

    if (!oldRec) {
      recDt = correctDt2((new Date()).getTime());
      if ((recType == 1) && (procId == 0)) {
        procId = correctDt(recDt);
        recDt = procId;
      }
    }

    track.incRecCount();

    // контрольное число
    ds.writeInt(12101970);

    ds.writeLong(recDt);
    ds.writeLong(procId);
    ds.writeByte(recType);
    if (recType == 2) { // при завершении процесса никакие поля в файл не пишем
      ds.writeShort(-1);
      ds.flush();
      return;
    }

    ValType vtp;
    int[] ia;
    long[] la;
    String[] sa;
    BigDecimal[] na;
    Date[] da;
    boolean[] ba;

    for (int i = 0; i < fld.length; i++) {
      if (fld[i] != null) {
        ds.writeShort(i);
        vtp = fldTypes[i].valType;
        switch (vtp) {
          case INT:
            ds.writeInt((Integer) fld[i]);
            break;
          case LONG:
            ds.writeLong((Long) fld[i]);
            break;
          case STRING:
            writeString(ds, (String) fld[i]);
            break;
          case DEC:
            writeBigDecimal(ds, (BigDecimal) fld[i]);
            break;
          case DATE:
            ds.writeLong(((Date) fld[i]).getTime());
            break;
          case BOOL:
            ds.writeBoolean((Boolean) fld[i]);
            break;
          case VOID:
            break;
          case INT_AR:
            ia = (int[]) fld[i];
            ds.writeInt(ia.length);
            for (int j = 0; j < ia.length; j++) {
              ds.writeInt(ia[j]);
            }
            break;
          case LONG_AR:
            la = (long[]) fld[i];
            ds.writeInt(la.length);
            for (int j = 0; j < la.length; j++) {
              ds.writeLong(la[j]);
            }
            break;
          case STRING_AR:
            sa = (String[]) fld[i];
            ds.writeInt(sa.length);
            for (int j = 0; j < sa.length; j++) {
              writeString(ds, sa[j]);
            }
            break;
          case DEC_AR:
            na = (BigDecimal[]) fld[i];
            ds.writeInt(na.length);
            for (int j = 0; j < na.length; j++) {
              writeBigDecimal(ds, na[j]);
            }
            break;
          case DATE_AR:
            da = (Date[]) fld[i];
            ds.writeInt(da.length);
            for (int j = 0; j < da.length; j++) {
              ds.writeLong(da[j].getTime());
            }
            break;
          case BOOL_AR:
            ba = (boolean[]) fld[i];
            ds.writeInt(ba.length);
            for (int j = 0; j < ba.length; j++) {
              ds.writeBoolean(ba[j]);
            }
            break;
          default:
            throw new Exception("Ошибка программы: неизвестный тип значения " + vtp.name());
        }
      }
    }

    ds.writeShort(-1);
    ds.flush();
//    }
  }

  public boolean readDataRecord(DataInputStream ds) throws Exception {
    clearAll();
    no++;

    // контрольное число
    int checkNum;
    try {
      checkNum = ds.readInt();
    } catch (EOFException e) {
      return false;
    }

    if (checkNum != 12101970) {
      throw new Exception("read DataRecord: invalid check number");
    }

    recDt = ds.readLong();
    procId = ds.readLong();
    recType = ds.readByte();
    changed = true;

    int prevFld = -1;
    int nextFld = ds.readShort();
    ValType vtp;
    int[] ia;
    long[] la;
    String[] sa;
    BigDecimal[] na;
    Date[] da;
    boolean[] ba;
    int len;

    while (nextFld != -1) {
      if (nextFld <= prevFld) {
        throw new Exception("Ошибка программы: нарушение порядка полей в записи");
      }
      prevFld = nextFld;

      vtp = fldTypes[nextFld].valType;
      fieldCount++;

      switch (vtp) {
        case INT:
          fld[nextFld] = ds.readInt();
          break;
        case LONG:
          fld[nextFld] = ds.readLong();
          break;
        case STRING:
          fld[nextFld] = readString(ds);
          break;
        case DEC:
          fld[nextFld] = readBigDecimal(ds);
          break;
        case DATE:
          fld[nextFld] = new Date(ds.readLong());
          break;
        case BOOL:
          fld[nextFld] = ds.readBoolean();
          break;
        case VOID:
          fld[nextFld] = new Object();
          break;
        case INT_AR:
          len = ds.readInt();
          ia = new int[len];
          for (int i = 0; i < len; i++) {
            ia[i] = ds.readInt();
          }
          fld[nextFld] = ia;
          break;
        case LONG_AR:
          len = ds.readInt();
          la = new long[len];
          for (int i = 0; i < len; i++) {
            la[i] = ds.readLong();
          }
          fld[nextFld] = la;
          break;
        case STRING_AR:
          len = ds.readInt();
          sa = new String[len];
          for (int i = 0; i < len; i++) {
            sa[i] = readString(ds);
          }
          fld[nextFld] = sa;
          break;
        case DEC_AR:
          len = ds.readInt();
          na = new BigDecimal[len];
          for (int i = 0; i < len; i++) {
            na[i] = readBigDecimal(ds);
          }
          fld[nextFld] = na;
          break;
        case DATE_AR:
          len = ds.readInt();
          da = new Date[len];
          for (int i = 0; i < len; i++) {
            da[i] = new Date(ds.readLong());
          }
          fld[nextFld] = da;
          break;
        case BOOL_AR:
          len = ds.readInt();
          ba = new boolean[len];
          for (int i = 0; i < len; i++) {
            ba[i] = ds.readBoolean();
          }
          fld[nextFld] = ba;
          break;
        default:
          throw new Exception("Ошибка программы: неизвестный тип значения " + vtp.name());
      }
      nextFld = ds.readShort();
    }

    return true;
  }
}
