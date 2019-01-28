package ntx.ts.sysproc;

import ntx.sap.fm.Z_TS_PRINTERS;
import ntx.sap.refs.RefLgort;
import ntx.ts.html.HtmlPageMenu;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.http.FileData;
import ntx.ts.srv.DataRecord;
import ntx.ts.srv.FieldType;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.Track;

/**
 * предок для всех процессов, выполняемых пользователем на терминале
 */
public abstract class ProcessTask extends ProcessUtil {

  private long userId = 0; // ид процесса пользователя

  public ProcessTask(ProcType procType, long procId) throws Exception {
    super(procType, procId);
  }

  public long getUserId() {
    return userId;
  }

  @Override
  public final String getAddTaskName(ProcessContext ctx) throws Exception {
    return getAddTaskName((UserContext) ctx);
  }

  public String getAddTaskName(UserContext ctx) throws Exception {
    // дополнительное описание задачи (номер и название склада, номер поставки и т.п.)
    // может быть быть переопределена
    return null;
  }

  public void callSetUserId(long user, String taskName, UserContext ctx) throws Exception {
    if (user == userId) {
      return;
    }
    if (userId != 0) {
      throw new Exception("callSetUserId: изменение пользователя задачи запрещено");
    }
    ctx.user.tasks.callTaskAdd(getProcId(), taskName, ctx);

    DataRecord dr = new DataRecord();
    dr.procId = getProcId();
    dr.setL(FieldType.PARENT, user);
    Track.saveProcessChange(dr, this, ctx);
  }

  public void callTaskDeactivate(UserContext ctx) throws Exception {
    if (userId != 0) {
      ctx.user.tasks.callTaskDeactivate(ctx);
    }
  }

  public void callTaskFinish(UserContext ctx) throws Exception {
    if (userId != 0) {
      ctx.user.tasks.callTaskDel(getProcId(), ctx);
    }
    Track.saveProcessFinish(getProcId(), this, ctx);
  }

  @Override
  public void callTaskFinish(ProcessContext ctx) throws Exception {
    callTaskFinish((UserContext) ctx);
  }

  public void callTaskNameChange(UserContext ctx) throws Exception {
    if (userId != 0) {
      ctx.user.tasks.callTaskNameChange(getProcId(), procName(), ctx);
    }
  }

  @Override
  public void handleData(DataRecord dr, ProcessContext ctx) throws Exception {
    // обработка записи трака (в т.ч. в реальном времени)
    // обязательно должна вызываться из handleData дочернего класса
    super.handleData(dr, ctx);

    switch (dr.recType) {
      case 0:
      case 1:
        if (dr.haveVal(FieldType.PARENT)) {
          userId = (Long) dr.getVal(FieldType.PARENT);
        }
        break;
    }
  }

  public String[] getLgorts(UserContext ctx) throws Exception {
    // эта ф-ия возвращает не то же самое, что ProcessUser.getUserLgorts()
    // если в пользователе настройка не указана

    String ss = ctx.user.getUserLgorts();
    if ((ss == null) || ss.isEmpty()) {
      ss = TSparams.lgorts;
    }
    return ss.split(",");
  }

  public FileData htmlSelLgort(String[] lgorts, boolean showErr, UserContext ctx) throws Exception {
    // страница выбора склада

    String[] lgs = lgorts;
    if (lgs == null) {
      lgs = getLgorts(ctx);
    }

    String def = null;
    for (int i = 0; i < lgs.length; i++) {
      if (def == null) {
        def = "sellgort" + lgs[i] + ":" + lgs[i] + " " + RefLgort.getNoNull(lgs[i]).name;
      } else {
        def = def + ";sellgort" + lgs[i] + ":" + lgs[i] + " " + RefLgort.getNoNull(lgs[i]).name;
      }
    }

    HtmlPageMenu p = new HtmlPageMenu("Выбор склада", "Выберите склад:", def,
            "sellgort" + ctx.user.getLastLgort(),
            showErr ? getLastErr() : null, null,
            (showErr && (getLastErr() != null)));
    return p.getPage();
  }

  public FileData htmlSelIsSgm(boolean showErr, UserContext ctx) throws Exception {
    // страница выбора сканирования СГМ
    HtmlPageMenu p = new HtmlPageMenu("Сканирование СГМ", "Сканировать коробки (СГМ - сборные грузовые места)?",
            "selsgm0:нет;selsgm1:да", "selsgm1",
            showErr ? getLastErr() : null, null,
            (showErr && (getLastErr() != null)));
    return p.getPage();
  }

  public boolean rightsLgort(String lgort, UserContext ctx) throws Exception {
    if ((lgort == null) || lgort.isEmpty()) {
      return false;
    }

    return ctx.user.rightsLgort(lgort);
  }

  @Override
  public final FileData handleQuery(TermQuery tq, ProcessContext ctx) throws Exception {
    return handleQuery(tq, (UserContext) ctx);
  }

  public abstract FileData handleQuery(TermQuery tq, UserContext ctx) throws Exception;

  public FileData htmlSelPrinter(String prefix, String title, UserContext ctx) throws Exception {
    // страница выбора принтера

    Z_TS_PRINTERS f = new Z_TS_PRINTERS();
    f.PRINTERS = TSparams.printers;

    f.execute();

    if (f.isErr) {
      HtmlPageMessage p = new HtmlPageMessage(f.err, null, null, null);
      return p.getPage();
    } else {
      String def = "no:Отмена";
      for (int i = 0; i < f.IT.length; i++) {
        def = def + ";" + (prefix + "_" + f.IT[i].PADEST + ":" + f.IT[i].PASTANDORT + " (" + f.IT[i].PADEST + ")").toLowerCase();
      }

      HtmlPageMenu p = new HtmlPageMenu("Выбор принтера", title, def,
              (prefix + "_" + ctx.user.getLastPrinter()).toLowerCase(),
              null, null, false);
      return p.getPage();
    }
  }
}
