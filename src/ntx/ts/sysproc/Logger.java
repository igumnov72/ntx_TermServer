package ntx.ts.sysproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import ntx.sap.fm.Z_TS_LOG_DATA;
import ntx.sap.fm.Z_TS_LOG_MAX_DT;
import ntx.sap.sys.SAPconn;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.LogType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;

/**
 *
 * Регистрация некоторых событий, сохранение в САП
 */
public class Logger {

  private long lastSavedDt = 0;
  private final HashMap<Long, Proc> procs = new HashMap<Long, Proc>(100);
  private ArrayList<Rec> recs = new ArrayList<Rec>(100);

  private static final LogType[] LOG_TYPES = LogType.values();
  private static final ProcType[] PROC_TYPES = ProcType.values();

  synchronized public void add(DataRecord dr) {
    // добавление записи в лог (только необходимые)
    // а так же поддержание справочника задач

    if (lastSavedDt == 0) {
      Z_TS_LOG_MAX_DT f = new Z_TS_LOG_MAX_DT();
      f.SRV_NAME = TSparams.srvName;

      f.execute();
      if (f.isErr) {
        System.err.println("Ошибка вызова Z_TS_LOG_MAX_DT:");
        System.err.println(f.err);
        return;
      }
      lastSavedDt = Long.parseLong(f.MAX_DT);
    }

    Proc p;

    if (dr.recType == 1) {
      p = new Proc(dr.procId, PROC_TYPES[(Integer) dr.getVal(FieldType.PROC_TYPE)]);
      procs.put(p.procId, p);
    } else {
      p = procs.get(dr.procId);
      if (p == null) {
        return;
      }
    }

    if (dr.recType == 2) {
      procs.remove(dr.procId);
    } else {
      if ((p.procType == ProcType.USER) && (dr.haveVal(FieldType.SHK))) {
        p.sotr = dr.getValStr(FieldType.SHK);
        if (p.sotr.length() > 10) 
            p.sotr = p.sotr.substring(0, 10);
      }

      if (dr.haveVal(FieldType.PARENT)) {
        p.parentId = (Long) dr.getVal(FieldType.PARENT);
        Proc u = procs.get(p.parentId);
        if (u != null) {
          p.sotr = u.sotr;
        }
      }
    }

    if (dr.recDt > lastSavedDt) {
      doAdd(dr, p);
    }
  }

  private void doAdd(DataRecord dr, Proc p) {
    // добавление записи в лог (только необходимые)

    if ((dr.recType == 1) && (p.procType != ProcType.USER)) {
      addRec((new Rec(dr.recDt, dr.procId, "NEW")).setTaskTyp(p.procType.name()));
    }

    if (dr.recType == 2) {
      if (p.procType != ProcType.USER) {
        addRec(new Rec(dr.recDt, dr.procId, "DEL"));
      }
      return;
    }

    if (p.procType == ProcType.USER) {
      if (dr.haveVal(FieldType.LOG)) {
        if (dr.haveVal(FieldType.TASK_ADD)) {
          addRec(new Rec(dr.recDt, (Long) dr.getVal(FieldType.TASK_ADD), LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)].name()));
        } else if (dr.haveVal(FieldType.TASK_NO)) {
          addRec(new Rec(dr.recDt, (Long) dr.getVal(FieldType.TASK_NO), LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)].name()));
        } else if (dr.haveVal(FieldType.TASK_DEL)) {
          addRec(new Rec(dr.recDt, (Long) dr.getVal(FieldType.TASK_DEL), LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)].name()));
        } else {
          addRec((new Rec(dr.recDt, dr.procId, LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)].name())).setSotr(p.sotr));
        }
      } else if (dr.haveVal(FieldType.TERM)) {
        if (((Long) dr.getVal(FieldType.TERM)) == 0) {
          addRec((new Rec(dr.recDt, dr.procId, "USER_OUT")).setSotr(p.sotr));
        } else {
          addRec((new Rec(dr.recDt, dr.procId, "USER_IN")).setSotr(p.sotr));
        }
      }
      return;
    }

    if ((dr.haveVal(FieldType.PARENT)) && !p.sotr.isEmpty()) {
      addRec((new Rec(dr.recDt, dr.procId, "SOTR")).setSotr(p.sotr));
    }

    if (dr.haveVal(FieldType.LOG) && (LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)] != LogType.ADD_MOD_SGM_TOV)) {
      Rec r = new Rec(dr.recDt, dr.procId, LOG_TYPES[(Integer) dr.getVal(FieldType.LOG)].name());

      if (dr.haveStrVal(FieldType.VBELN)) {
        r.setVbeln(dr.getValStr(FieldType.VBELN));
      }
      if (dr.haveStrVal(FieldType.LGORT)) {
        r.setLgort(dr.getValStr(FieldType.LGORT));
      }
      if (dr.haveStrVal(FieldType.CELL)) {
        r.setCell(dr.getValStr(FieldType.CELL));
      }
      if (dr.haveStrVal(FieldType.PAL)) {
        r.setPal(dr.getValStr(FieldType.PAL));
      }
      if (dr.haveStrVal(FieldType.MATNR)) {
        r.setMatnr(dr.getValStr(FieldType.MATNR));
      }
      if (dr.haveStrVal(FieldType.CHARG)) {
        r.setCharg(dr.getValStr(FieldType.CHARG));
      }
      if (dr.haveVal(FieldType.QTY)) {
        r.setQty((BigDecimal) dr.getVal(FieldType.QTY));
      }

      addRec(r);
    }
  }

  private void addRec(Rec r) {
    recs.add(r);
  }

  synchronized public void save() {
    if ((lastSavedDt == 0) || recs.isEmpty()) {
      return;
    }

    Z_TS_LOG_DATA f = new Z_TS_LOG_DATA();
    f.SRV_NAME = TSparams.srvName;

    int nn = 0;
    long maxDt = recs.get(recs.size() - 1).dt;
    for (Rec r : recs) {
      if ((r.dt > lastSavedDt) && (r.dt < maxDt)) {
        nn++;
      }
    }

    if (nn == 0) {
      return;
    }

    f.IT_create(nn);
    int i = 0;
    for (Rec r : recs) {
      if ((r.dt > lastSavedDt) && (r.dt < maxDt)) {
        f.IT[i].LOG_TYP = r.oper;
        f.IT[i].LOG_DT = String.valueOf(r.dt).trim();
        f.IT[i].TASK_ID = String.valueOf(r.task).trim();
        f.IT[i].PTYP = r.taskTyp;
        f.IT[i].SOTR = r.sotr;
        f.IT[i].MATNR = SAPconn.fillZeros(r.matnr, 18);
        f.IT[i].CHARG = SAPconn.fillZeros(r.charg, 10);
        f.IT[i].QTY = r.qty;
        f.IT[i].LENUM = SAPconn.fillZeros(r.pal, 20);
        f.IT[i].VBELN = SAPconn.fillZeros(r.vbeln, 10);
        f.IT[i].LGPLA = r.cell;
        f.IT[i].LGORT = r.lgort;
        i++;
      }
    }

    f.execute();
    if (f.isErr) {
      System.err.println("Ошибка вызова Z_TS_LOG_DATA:");
      System.err.println(f.err);
      return;
    }
    lastSavedDt = f.MAX_DT.isEmpty() ? 0 : Long.parseLong(f.MAX_DT);

    ArrayList<Rec> recs2 = new ArrayList<Rec>(100);
    for (Rec r : recs) {
      if (r.dt > lastSavedDt) {
        recs2.add(r);
      }
    }

    recs = recs2;
  }
}

class Proc { // описание процесса (пользователь, задача)

  public long parentId = 0; // ид процесса пользователя
  public final long procId; // ID процесса
  public final ProcType procType;
  public String sotr = "";

  public Proc(long procId, ProcType procType) {
    this.procId = procId;
    this.procType = procType;
  }
}

class Rec { // запись в логе

  public final long dt;
  public final long task;
  public final String oper;
  public String taskTyp = "";
  public String sotr = "";
  public String lgort = "";
  public String vbeln = "";
  public String matnr = "";
  public String charg = "";
  public String cell = "";
  public String pal = "";
  public BigDecimal qty = BigDecimal.ZERO;

  public Rec(long dt, long task, String oper) {
    this.dt = dt;
    this.task = task;
    this.oper = oper;
  }

  public Rec setTaskTyp(String taskTyp) {
    this.taskTyp = taskTyp;
    return this;
  }

  public Rec setSotr(String sotr) {
    this.sotr = sotr;
    return this;
  }

  public Rec setLgort(String lgort) {
    this.lgort = lgort;
    return this;
  }

  public Rec setVbeln(String vbeln) {
    this.vbeln = vbeln;
    return this;
  }

  public Rec setMatnr(String matnr) {
    this.matnr = matnr;
    return this;
  }

  public Rec setCharg(String charg) {
    this.charg = charg;
    return this;
  }

  public Rec setCell(String cell) {
    this.cell = cell;
    return this;
  }

  public Rec setPal(String pal) {
    this.pal = pal;
    return this;
  }

  public Rec setQty(BigDecimal qty) {
    this.qty = qty;
    return this;
  }
}
