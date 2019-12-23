package ntx.ts.srv;

import ntx.ts.sysproc.ProcessContext;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.ts.sysproc.Logger;
import ntx.ts.sysproc.ProcessUser;
import ntx.ts.sysproc.Process;
import ntx.ts.sysproc.ProcessWP;
import ntx.ts.userproc.*;

/**
 * управление записями данных
 */
public class Track {

  public static DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
  private static ProcType[] procTypes = ProcType.values();
  // данные по траку:
  private volatile DataOutputStream ds = null; // здесь сохраняются данные в процессе работы
  private final Map<Long, Process> loadedProcs = Collections.synchronizedMap(new HashMap<Long, Process>(10000)); // загруженные процессы
  private volatile long recCount = 0; // число записей в файле
  public final Object lockRead = new Object();
  public final Object lockWrite = new Object();
  public final Logger log = new Logger();
  private int fileNo; // номер файла данных (0 - рабочий, остальные - архивные)
  public Date lastComprDate = null; // дата последнего сжатия файла данных
  private Date nextComprDate = null; // дата следующего сжатия файла данных
  // данные по пользователям:
  public int adminCount = 0;
  public final Map<String, ProcessUser> tabSHK = Collections.synchronizedMap(new HashMap<String, ProcessUser>(300));
  public final Map<Long, ProcessUser> tabTerm = Collections.synchronizedMap(new HashMap<Long, ProcessUser>(300));
  public final Map<Long, ProcessWP> tabTermWP = Collections.synchronizedMap(new HashMap<Long, ProcessWP>(30));
  public final Map<Integer, ProcessWP> tabWP = Collections.synchronizedMap(new HashMap<Integer, ProcessWP>(30)); // соответствие процесса номеру рабочего места
  private static final DateFormat df2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  public static Date started = null; // время старта сервера

  public static Process createProcess(ProcType pType, long procId) throws Exception {
    // сюда должен быть помещен код создания пустых процессов всех типов
    Process ret = null;

    switch (pType) {
      case USER:
        ret = new ProcessUser(procId);
        break;
      case TEST:
        ret = new ProcessTest(procId);
        break;
      case PLACEMENT:
        ret = new ProcessPlacement(procId);
        break;
      case SKL_MOVE:
        ret = new ProcessSklMove(procId);
        break;
      case PRIEMKA:
        ret = new ProcessPriemka(procId);
        break;
      case INVENT:
        ret = new ProcessInvent(procId);
        break;
      case COMPL:
        ret = new ProcessCompl(procId);
        break;
      case PRODPRI:
        ret = new ProcessProdPriemka(procId);
        break;
      case DPDT:
        ret = new ProcessDpdt(procId);
        break;
      case POPOLN:
        ret = new ProcessPopoln(procId);
        break;
      case WP:
        ret = new ProcessWP(procId);
        break;
      case VOZVRAT:
        ret = new ProcessVozvrat(procId);
        break;
      case OPIS:
        ret = new ProcessOpis(procId);
        break;
      case VYGR:
        ret = new ProcessVygr(procId);
        break;
      case PEREUP1:
        ret = new ProcessToPereup(procId);
        break;
      case PEREUP2:
        ret = new ProcessFromPereup(procId);
        break;
      case OPISK:
        ret = new ProcessOpisK(procId);
        break;
      case COMPL_MOVE:
        ret = new ProcessComplMove(procId);
        break;
      case PROGRES:
        ret = new ProcessProgres(procId);
        break;
    }
    if (ret == null) {
      throw new Exception("Ошибка программы: не создан процесс типа " + pType.text);
    }
    if (ret.getProcType() != pType) {
      throw new Exception("Ошибка программы: созданный процесс имеет неверный тип");
    }

    return ret;
  }

  public Track(int fileNo) {
    this.fileNo = fileNo;
  }

  public Process getProcess(long procId, Process proc) throws Exception {
    // получение ссылки на ранее загруженный процесс

    Process p = proc;
    boolean loaded = false;
    if (p == null) {
      p = loadedProcs.get(procId);
      loaded = true;
    }
    if (p == null) {
      throw new Exception("Ошибка программы: не найден загруженный процесс "
              + procId + " (" + df.format(new Date(procId)) + ")");
    }
    if (p.getProcId() != procId) {
      throw new Exception("Ошибка программы: неверная ссылка на загруженный процесс "
              + procId + " (" + df.format(new Date(procId)) + ")");
    }
    if (p.getUnloaded() && loaded) {
      throw new Exception("Ошибка программы: выгруженный процесс в массиве loadedProcs "
              + procId + " (" + df.format(new Date(procId)) + ")");
    }
    return p;
  }

  public static void saveProcessChange(DataRecord dr, Process proc, ProcessContext ctx) throws Exception {
    // сохранение данных (об изменении состояния процесса) на траке
    // плюс запуск изменения состояния объекта по этим данным
    // proc - ссылка на загруженных процесс (может быть null, тогда он ищется в loadedProcs)

    if (!dr.isChanged()) {
      return;
    }

    Process p = ctx.track.getProcess(dr.procId, proc);
    dr.recType = 0;
    synchronized (ctx.track.lockWrite) {
      dr.write(ctx.track.ds, ctx.track);
    }
    p.handleData(dr, ctx);
  }

  public static Process saveProcessNew(ProcType pType, DataRecord dr, ProcessContext ctx) throws Exception {
    // сохранение данных о создании нового процесса
    // и фактическое создание этого процесса

    if ((pType == null) || (pType == ProcType.VOID)) {
      throw new Exception("Process type not specified");
    }

    dr.recType = 1;
    dr.procId = 0;
    dr.setI(FieldType.PROC_TYPE, pType.ordinal());
    synchronized (ctx.track.lockWrite) {
      dr.write(ctx.track.ds, ctx.track);
    }

    Process ret = createProcess(pType, dr.procId);
    ctx.track.loadedProcs.put(dr.procId, ret);
    ret.handleData(dr, ctx);

    return ret;
  }

  public static void saveProcessFinish(long procId, Process proc, ProcessContext ctx) throws Exception {
    // сохранение данных о завершении процесса
    // и фактическое завершение процесса

    Process p = ctx.track.getProcess(procId, proc);
    DataRecord dr = new DataRecord();
    dr.recType = 2;
    dr.procId = procId;
    synchronized (ctx.track.lockWrite) {
      dr.write(ctx.track.ds, ctx.track);
    }
    p.handleData(dr, ctx);
    p.setUnloaded();
    ctx.track.loadedProcs.remove(procId);
  }

  public static void saveParam(DataRecord dr, ProcessContext ctx) throws Exception {
    // сохранение параметров на траке

    if (!dr.isChanged()) {
      return;
    }

    dr.recType = 3;
    dr.procId = 0;
    synchronized (ctx.track.lockWrite) {
      dr.write(ctx.track.ds, ctx.track);
    }
  }

  public void incRecCount() {
    recCount++;
  }

  public long getRecCount(int fileNo) throws Exception {
    if (fileNo == this.fileNo) {
      return recCount;
    } else {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(dis);
      long rc = 0;
      while (haveData) {
        rc++;
        haveData = dr.readDataRecord(dis);
      }
      dis.close();
      return rc;
    }
  }

  private void createDs() throws Exception {
    if (fileNo != 0) {
      throw new Exception("Ошибка! Попытка создания DataOutputStream для архивного файла");
    }
    synchronized (lockWrite) {
      ds = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trackFileName(fileNo), true)));
    }
  }

  public void init() throws Exception {
    loadData(true);
    createDs();
    scheduleCompress();

    if (adminCount == 0) {
      ProcessUser user = getUserBySHK("1");
      if (user == null) {
        // создаем админа по умолчанию
        user = callCreateUser("Default admin", "1", "1");
      }

      DataRecord dr = new DataRecord();
      dr.recType = 0;
      dr.procId = user.getProcId();
      if (!user.getIsAdmin()) {
        dr.setB(FieldType.IS_ADMIN, true);
      }
      if (user.getLocked()) {
        dr.setB(FieldType.LOCKED, false);
      }
      saveProcessChange(dr, user, new ProcessContext(this));
    }

    // запоминаем время старта
    started = new Date();
  }

  public void loadData(boolean writeLog) throws Exception {
    synchronized (lockRead) {
      synchronized (lockWrite) {
        recCount = 0;

        File datFile = new File(trackFileName(fileNo));
        if (!datFile.exists() && (fileNo == 0)) {
          datFile.createNewFile();
          return;
        }
        if (!datFile.isFile()) {
          throw new Exception(trackFileName(fileNo) + " is not a file");
        }

        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(datFile), 524288));
        DataRecord dr = new DataRecord();
        Process p;
        boolean haveData = dr.readDataRecord(dis);
        ProcessContext ctx = new ProcessContext(this);

        while (haveData) {
          recCount++;

          if (writeLog) {
            log.add(dr);
          }

          if (dr.recType == 1) {
            // здесь логика определения того, какие процессы грузятся при старте программы
            switch (procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)]) {
              case USER:
                p = createProcess(ProcType.USER, dr.procId);
                p.handleData(dr, ctx);
                loadedProcs.put(p.getProcId(), p);
                break;
              case WP:
                p = createProcess(ProcType.WP, dr.procId);
                p.handleData(dr, ctx);
                loadedProcs.put(p.getProcId(), p);
                break;
            }
          } else if (dr.recType == 3) {
            // системный параметр
            if (dr.haveVal(FieldType.LAST_COMPR_DATE)) {
              lastComprDate = (Date) dr.getVal(FieldType.LAST_COMPR_DATE);
            }
          } else {
            // если процесс загружен - передаем данные ему на обработку
            p = loadedProcs.get(dr.procId);
            if (p != null) {
              p.handleData(dr, ctx);
              if (dr.recType == 2) {
                loadedProcs.remove(p.getProcId());
              }
            }
          }

          haveData = dr.readDataRecord(dis);
        }

        dis.close();
      }
    }
  }

  public Process loadProcess(long procId) throws Exception {
    // загрузка процесса с трака в память (если не загружен)

    Process ret = loadedProcs.get(procId);
    if (ret != null) {
      return ret;
    }

    synchronized (lockRead) {
      DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(trackFileName(fileNo)), 524288));
      DataRecord dr = new DataRecord();
      boolean haveData = dr.readDataRecord(dis);
      ProcessContext ctx = new ProcessContext(this);

      while (haveData) {
        if (dr.procId == procId) {
          if (dr.recType == 1) {
            if (ret != null) {
              throw new Exception("Ошибка программы: повторное создание процесса " + procId + " (" + df.format(new Date(procId)) + ")");
            }
            ret = createProcess(procTypes[(Integer) dr.getVal(FieldType.PROC_TYPE)], procId);
            ret.handleData(dr, ctx);
          } else {
            if (ret == null) {
              throw new Exception("Ошибка программы: изменение несозданного процесса " + procId + " (" + df.format(new Date(procId)) + ")");
            }
            ret.handleData(dr, ctx);
          }
        }
        haveData = dr.readDataRecord(dis);
      }

      dis.close();

      if (ret == null) {
        throw new Exception("Ошибка программы: на траке нет процесса " + procId + " (" + df.format(new Date(procId)) + ")");
      }
      loadedProcs.put(procId, ret);
      return ret;
    }
  }

  public void unloadProcess(long procId) throws Exception {
    // выгрузка процесса из памяти
    // все ссылки на этот процесс из других процессов
    // должны быть предварительно (или сразу после) удалены
    Process p = loadedProcs.get(procId);
    if (p == null) {
      return;
    }
    if (p.getUnloaded()) {
      throw new Exception("Ошибка программы: выгруженный процесс в массиве loadedProcs "
              + procId + " (" + df.format(new Date(procId)) + ")");
    }
    p.setUnloaded();
    loadedProcs.remove(procId);
  }

  public static String trackFileName(int idx) {
    // возвращает имя файла трака (0 - текущий, остальные - архивные)
    if (idx == 0) {
      return "ts.dat";
    } else {
      String s = Integer.toString(idx);
      switch (s.length()) {
        case 1:
          s = "00" + s;
          break;
        case 2:
          s = "0" + s;
          break;
      }
      return "ts." + s + ".dat";
    }
  }

  public String compressTrack(int saveDays) throws Exception {
    // сжатие трака (старый трак переименовывается в отдельный файл)
    // saveDays - сколько дней оставить на траке (0 - нисколько, 1 - с полуночи и т.д.)
    // возвращает сообщение об ошибке либо null

    if (fileNo != 0) {
      throw new Exception("Ошибка: попытка сжатия архивного файла");
    }

    try {
      log.save();

      synchronized (lockRead) {
        synchronized (lockWrite) {
          long savedRecCount = recCount;

          File newFile = new File(trackFileName(0) + ".new");
          if (newFile.exists()) {
            newFile.delete();
          }
          if (newFile.exists()) {
            return "Не могу удалить файл " + newFile.getAbsolutePath();
          }
          newFile.createNewFile(); // новый файл для трака создан

          // закрываем выходной поток
          ds.flush();
          ds.close();
          ds = null;

          Date minDate = new Date();
          if (saveDays > 0) {
            DateFormat df1 = new SimpleDateFormat("dd.MM.yyyy");
            minDate = df1.parse(df.format(minDate));
            if (saveDays > 1) {
              minDate = new Date(minDate.getTime() - (86400000l * (saveDays - 1)));
            }
          }
          long minDt = minDate.getTime(); // граница очистки трака

          HashMap<Long, Boolean> finProc = new HashMap<Long, Boolean>(1000); // завершенные (до minDt) процессы
          // последние записи определенных типов по пользователям (какую историю полностью не хранить):
          HashMap<Long, userLastFieldRec> users = new HashMap<Long, userLastFieldRec>(100);
          userLastFieldRec uu;
          DataInputStream ds1 = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(0)), 524288));
          DataRecord dr = new DataRecord();
          int userProcType = ProcType.USER.ordinal();
          boolean haveData = dr.readDataRecord(ds1);
          while (haveData) {
            uu = users.get(dr.procId);

            if (dr.recDt < minDt) { // после minDt берем все записи
              if (dr.recType == 2) {
                finProc.put(dr.procId, true);
              }

              // находим последние записи по полям, по которым не нужна полная история
              if ((dr.recType == 1) && dr.haveVal(FieldType.PROC_TYPE)
                      && (((Integer) dr.getVal(FieldType.PROC_TYPE)) == userProcType)) {
                uu = new userLastFieldRec();
                users.put(dr.procId, uu);
              }
              if (uu != null) {
                if (dr.haveVal(FieldType.LGORT)) {
                  uu.lgort = dr.recDt;
                }
                if (dr.haveVal(FieldType.ASK_QTY)) {
                  uu.askQty = dr.recDt;
                }
                if (dr.haveVal(FieldType.LAST_TASK_TYPE)) {
                  uu.tt = dr.recDt;
                }
                if (dr.haveVal(FieldType.TASK_IS_ACTIVE)) {
                  uu.isActive = dr.recDt;
                }
                if (dr.haveVal(FieldType.TERM)) {
                  uu.term = dr.recDt;
                }
              }
            } else {
              break;
            }

            haveData = dr.readDataRecord(ds1);
          }
          ds1.close();
          // finProc содержит завершенные процессы

          // копируем данные в новый файл
          ds1 = new DataInputStream(new BufferedInputStream(new FileInputStream(Track.trackFileName(0)), 524288));
          DataOutputStream ds2 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newFile, true)));
          haveData = dr.readDataRecord(ds1);
          Boolean finished;
          boolean doCopy;
          long taskAddDel;
          userLastFieldRec uu2;
          recCount = 0;
          while (haveData) {
            doCopy = true; // по умолчанию запись копируем

            // если процесс завершен (до minDt), то запись не копируем
            finished = finProc.get(dr.procId);
            if (finished == null) {
              finished = false;
            }
            if (finished) {
              doCopy = false;
            }

            if (doCopy) {
              // не копируем запись с информацией по завершенному процессу в списке задач пользователя
              taskAddDel = 0;
              if (dr.haveVal(FieldType.TASK_ADD)) {
                taskAddDel = (Long) dr.getVal(FieldType.TASK_ADD);
              } else if (dr.haveVal(FieldType.TASK_DEL)) {
                taskAddDel = (Long) dr.getVal(FieldType.TASK_DEL);
              }
              if (taskAddDel != 0) {
                finished = finProc.get(taskAddDel);
                if (finished == null) {
                  finished = false;
                }
                if (finished) {
                  doCopy = false;
                }
              }
            }

            if (doCopy) {
              if (dr.recDt < minDt) {
                uu = users.get(dr.procId);
                if (uu != null) {
                  if (dr.haveVal(FieldType.LGORT)
                          || dr.haveVal(FieldType.ASK_QTY)
                          || dr.haveVal(FieldType.LAST_TASK_TYPE)) {
                    // опускаем подробную историю по определенным полям (по пользователям)
                    if ((dr.recDt != uu.askQty) && (dr.recDt != uu.lgort) && (dr.recDt != uu.tt)) {
                      doCopy = false;
                    }
                  }
                  if ((dr.fieldCount == 1)
                          && (dr.haveVal(FieldType.TASK_IS_ACTIVE)
                          || dr.haveVal(FieldType.TERM))) {
                    // опускаем подробную историю по определенным полям (по пользователям)
                    if ((dr.recDt != uu.isActive) && (dr.recDt != uu.term)) {
                      doCopy = false;
                    }
                  }
                }
              }
            }

            if (doCopy) {
              // выполняем копирование
              dr.write2(ds2, true, this);

              if ((dr.recType != 2) && dr.haveVal(FieldType.PARENT)) {
                uu2 = users.get((Long) dr.getVal(FieldType.PARENT));
                if (uu2 != null) {
                  uu2.taskCount++;
                }
              }
            }

            haveData = dr.readDataRecord(ds1);
          }
          ds1.close();
          ds2.close();

          int j = 1;
          File afile = new File(trackFileName(j));
          while (afile.exists()) {
            j++;
            afile = new File(trackFileName(j));
          }
          File tfile = new File(trackFileName(0));
          if (!tfile.renameTo(afile)) {
            recCount = savedRecCount;
            return "Не могу переименовать " + tfile.getAbsolutePath()
                    + " в " + afile.getAbsolutePath();
          }
          tfile = new File(trackFileName(0));
          if (!newFile.renameTo(tfile)) {
            return "!!! КРИТИЧЕСКАЯ ОШИБКА: не могу переименовать "
                    + newFile.getAbsolutePath() + " в " + tfile.getAbsolutePath();
          }

          // сохраняем новые счетчики задач пользователей (в загруженных процессах)
          ProcessUser u;
          for (Entry<Long, userLastFieldRec> i : users.entrySet()) {
            try {
              u = (ProcessUser) getProcess(i.getKey(), null);
            } catch (Exception e) {
              u = null;
            }
            if (u != null) {
              u.tasks.setTaskCount(i.getValue().taskCount);
            }
          }

          createDs();

          // записываем дату сжатия
          setLastComprDate(new Date());

          System.out.println("СЖАТИЕ ФАЙЛА ДАННЫХ: выполнено успешно");
          System.out.flush();

          return null;
        }
      }
    } catch (Exception e) {
      return e.toString();
    } finally {
      if (ds == null) {
        createDs();
      }
    }
  }

  public int getAdminCount() {
    return adminCount;
  }

  public ProcessUser getUserBySHK(String scan) {
    // получение процесса "пользователь" по его ШК
    return tabSHK.get(scan);
  }

  public ProcessUser getUserByTerm(long terminal) {
    // получение процесса "пользователь" по номеру терминала
    return tabTerm.get(terminal);
  }

  public ProcessWP getWPbyTerm(long terminal) {
    // получение процесса WP по номеру терминала
    return tabTermWP.get(terminal);
  }

  public ProcessWP getWPbyNum(int wpNum) {
    // получение процесса WP по номеру WP
    return tabWP.get(wpNum);
  }

  public String[] getUsersSHK() {
    String[] ret = new String[tabSHK.size()];

    int i = 0;
    for (Entry<String, ProcessUser> s : tabSHK.entrySet()) {
      ret[i] = s.getKey();
      i++;
    }

    return ret;
  }

  public ConcurrentSkipListMap<String, ProcessUser> getUsersSorted() {
    ConcurrentSkipListMap<String, ProcessUser> ret = new ConcurrentSkipListMap<String, ProcessUser>();
    for (Entry<String, ProcessUser> i : tabSHK.entrySet()) {
      ret.put(i.getValue().getUserName(), i.getValue());
    }
    return ret;
  }

  public ProcessUser callCreateUser(String uName, String uSHK, String passw) throws Exception {
    if (uName.isEmpty()) {
      throw new Exception("Ошибка: не указано имя пользователя");
    }
    if (uSHK.isEmpty()) {
      throw new Exception("Ошибка: не указан ШК пользователя");
    }

    ProcessUser u1 = getUserBySHK(uSHK);
    if (u1 != null) {
      throw new Exception("Ошибка: ШК " + uSHK + " имеет другой пользователь: " + u1.getUserName());
    }

    DataRecord dr = new DataRecord();
    dr.setS(FieldType.NAME, uName);
    dr.setS(FieldType.SHK, uSHK);
    dr.setS(FieldType.PASSWORD, passw);

    ProcessUser ret = (ProcessUser) Track.saveProcessNew(ProcType.USER, dr, new ProcessContext(this));

    return ret;
  }

  public String nextComprDateStr(boolean doCalc) {
    long dt = nextComprDate(doCalc);
    if (dt == 0) {
      return "нет";
    } else {
      return df2.format(new Date(dt));
    }
  }

  public long nextComprDate(boolean doCalc) {
    if (!doCalc) {
      return nextComprDate == null ? 0 : nextComprDate.getTime();
    }
    if ((TSparams.comprDate1 == null) || (TSparams.comprWeeks <= 0)) {
      return 0;
    }
    long dt1 = TSparams.comprDate1.getTime();
    long dtDif = 604800000L * TSparams.comprWeeks;
    long dt2 = (new Date()).getTime();
    while (dt1 < dt2) {
      dt1 += dtDif;
    }
    return dt1;
  }

  public void setLastComprDate(Date dt) throws Exception {
    lastComprDate = dt;
    DataRecord dr = new DataRecord();
    dr.setD(FieldType.LAST_COMPR_DATE, lastComprDate);
    saveParam(dr, new ProcessContext(this));
  }

  private void scheduleCompress() {
    // планирование автоматического сжатия

    if (nextComprDate(true) == 0) {
      return;
    }

    new Thread() {

      public void run() {
        long dt1, dt2;
        try {
          while (true) {
            dt2 = nextComprDate(true);
            if (dt2 == 0) {
              System.err.println("ОШИБКА! Планироващик автосжатия не может получить дату следующего запуска");
              System.err.flush();
              return;
            }
            dt1 = (new Date()).getTime();
            if (dt1 >= (dt2 - 10000)) {
              System.err.println("ОШИБКА! Планироващик автосжатия получил неверную дату запуска: now="
                      + dt1 + " (" + df.format(new Date(dt1)) + "), dt2="
                      + dt2 + " (" + df.format(new Date(dt2)) + ")");
              System.err.flush();
              sleep(10000);
              continue;
            }
            nextComprDate = new Date(dt2);

            sleep(dt2 - dt1);

            nextComprDate = null;
            dt1 = (new Date()).getTime();
            if (dt1 < (dt2 - 200000)) {
              System.err.println("ОШИБКА! Планироващик автосжатия проснулся невовремя: now="
                      + dt1 + " (" + df.format(new Date(dt1)) + "), dt2="
                      + dt2 + " (" + df.format(new Date(dt2)) + ")");
              System.err.flush();
              continue;
            }
            if ((lastComprDate != null) && (dt1 < (lastComprDate.getTime() + TSparams.comprDays * 86400000))) {
              System.out.println("СЖАТИЕ ФАЙЛА ДАННЫХ: автосжатие пропущено, поскольку недавно проводилось сжатие");
              System.out.flush();
              continue;
            }
            while (dt1 < dt2) {
              sleep(1000);
              dt1 = (new Date()).getTime();
            }

            try {
              compressTrack(TSparams.comprDays);
            } catch (Exception e) {
              System.out.println("!!! СЖАТИЕ ФАЙЛА ДАННЫХ: не выполнено из-за ошибки");
              System.out.flush();
            }
          }
        } catch (Exception e) {
          nextComprDate = null;
          System.err.println("Error in auto compress scheduler (stopped):");
          e.printStackTrace();
          System.err.flush();
        }
      }
    }.start();
  }

  private static class userLastFieldRec {
    // ссылки на последние записи определенных типов (по пользователю)

    public long lgort = 0;
    public long askQty = 0;
    public long tt = 0;
    public long isActive = 0;
    public long term = 0;
    public int taskCount = 0;
  }
}
