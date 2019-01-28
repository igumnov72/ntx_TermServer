package ntx.sap.refs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import ntx.sap.fm.Z_TS_INFOS;
import ntx.ts.html.HtmlPage;
import ntx.ts.html.HtmlPageMenu;
import ntx.ts.html.HtmlPageMessage;
import ntx.ts.http.FileData;
import ntx.ts.srv.ProcType;
import ntx.ts.srv.TSparams;

/**
 * Информационные сообщения пользователю
 */
public class RefInfo {

  private static final ProcType[] procTypes = ProcType.values();
  private static final HashMap<Integer, RefInfoStruct> taskInfos = new HashMap<Integer, RefInfoStruct>();
  private static Date lastChecked = null; // дата/время последней проверки обновлений

  public static synchronized boolean haveInfo(ProcType ptyp) throws Exception {
    // проверка наличия сообщений по типу задачи
    return taskInfos.get(ptyp.ordinal()) != null;
  }

  public static synchronized boolean haveNewInfo(ProcType ptyp, Integer infoId) throws Exception {
    // проверка наличия новых сообщений по пользователю и типу задачи

    checkNewInfo();

    RefInfoStruct taskInfo = taskInfos.get(ptyp.ordinal());

    if (taskInfo == null) {
      return false;
    }

    if (infoId == null) {
      return true;
    }

    if (taskInfo.maxInfoId > infoId) {
      return true;
    } else {
      return false;
    }
  }

  public static synchronized FileData htmlNewInfo(int ptyp, Integer infoId) throws Exception {
    // выдача новых сообщений по пользователю и типу задачи

    String err = checkNewInfo();

    if (err != null) {
      return (new HtmlPageMessage(err, null, null, null)).getPage();
    }

    RefInfoStruct taskInfo = taskInfos.get(ptyp);

    if (taskInfo == null) {
      return null;
    }

    if (infoId == null) {
      if (taskInfo.haveManual) {
        return htmlNewInfo0(taskInfo); // выдача инструкции
      } else {
        return htmlNewInfo1(taskInfo, 0); // выдача всех сообщений
      }
    }

    if (taskInfo.maxInfoId > infoId) {
      return htmlNewInfo1(taskInfo, infoId); // выдача новых сообщений
    } else {
      return null;
    }
  }

  public static FileData htmlManualMenu(int taskType) throws Exception {
    // возвращаем меню выбора инструкции

    ConcurrentSkipListMap<Integer, String> lst = getInfoList(taskType);

    if (lst.size() == 1) {
      // только одно сообщение, выдаем его
      for (Entry<Integer, String> i : lst.entrySet()) {
        return htmlManualShow(taskType, i.getKey());
      }
    }

    String def = "cont:Назад";
    for (Entry<Integer, String> i : lst.entrySet()) {
      def = def + ";manual" + i.getKey() + ":" + i.getValue();
    }

    HtmlPageMenu p = new HtmlPageMenu("Выбор инструкции", "Выберите инструкцию", def, null, null, null);
    return p.getPage();
  }

  public static FileData htmlManualShow(int taskType, int infoId) throws Exception {
    // отображаем выбранную инструкцию
    if (infoId == 0) {
      return htmlNewInfo(taskType, null);
    } else {
      return htmlNewInfo(taskType, infoId - 1);
    }
  }

  private static FileData htmlNewInfo0(RefInfoStruct taskInfo) throws Exception {
    // выдача инструкции

    InfoData idata = taskInfo.infos.get(0);

    HtmlPage p = new HtmlPage();
    p.title = "Инструкция";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;
    p.addFormStart("work.html", "f");

    p.addLine("<b>" + idata.name + "</b>");
    p.addNewLine();

    for (int i = 0; i < idata.txt.length; i++) {
      p.addLine(idata.txt[i]);
    }

    p.addFormButtonSubmitGo("Продолжить");
    p.addFormFieldHidden("htext1", 1, "shownmsg" + taskInfo.procTyp + "." + taskInfo.maxInfoId);
    p.addFormEnd();

    return p.getPage();
  }

  private static FileData htmlNewInfo1(RefInfoStruct taskInfo, int infoId) throws Exception {
    // выдача новых сообщений

    // сортируем сообщения
    ConcurrentSkipListMap<Integer, InfoData> srt = new ConcurrentSkipListMap<Integer, InfoData>();
    InfoData idata;
    for (Entry<Integer, InfoData> i : taskInfo.infos.entrySet()) {
      idata = i.getValue();
      if (idata.infoId > infoId) {
        srt.put(idata.infoId, idata);
      }
    }

    HtmlPage p = new HtmlPage();
    p.title = "Сообщения";
    p.sound = "ask.wav";
    p.fontSize = TSparams.fontSize2;
    p.scrollToTop = true;
    p.addFormStart("work.html", "f");

    boolean isFirstMsg = true;
    for (Entry<Integer, InfoData> i : srt.entrySet()) {
      idata = i.getValue();

      if (isFirstMsg) {
        isFirstMsg = false;
      } else {
        p.addText("<HR>\r\n");
      }

      if (idata.name != null) {
        p.addLine("<b>" + idata.name + "</b>");
      }
      for (int j = 0; j < idata.txt.length; j++) {
        p.addLine(idata.txt[j]);
      }
    }

    p.addFormButtonSubmitGo("Продолжить");
    p.addFormFieldHidden("htext1", 1, "shownmsg" + taskInfo.procTyp + "." + taskInfo.maxInfoId);
    p.addFormEnd();

    return p.getPage();
  }

  public static synchronized ConcurrentSkipListMap<Integer, String> getInfoList(int ptyp) {
    // возвращает отсортированный список всех сообщений по типу задачи
    RefInfoStruct taskInfo = taskInfos.get(ptyp);
    if (taskInfo == null) {
      return new ConcurrentSkipListMap<Integer, String>();
    }
    ConcurrentSkipListMap<Integer, String> ret = new ConcurrentSkipListMap<Integer, String>();
    InfoData idata;
    for (Entry<Integer, InfoData> i : taskInfo.infos.entrySet()) {
      idata = i.getValue();
      ret.put(idata.infoId, idata.name);
    }
    return ret;
  }

  public static synchronized String checkNewInfo() throws Exception {
    // проверка наличия новых сообщений
    // возвращает ошибку обращения к САПу

    Date dt = new Date();
    if ((lastChecked != null) && ((dt.getTime() - lastChecked.getTime()) < 60000)) {
      // с момента последней проверки прошло менее минуты
      return null;
    }

    String ret = loadInfo();
    if (ret == null) {
      lastChecked = dt;
    }
    return ret;
  }

  private static String loadInfo() throws Exception {
    // загрузка сообщений
    // возвращает ошибку обращения к САПу

    Z_TS_INFOS f = new Z_TS_INFOS();
    int nt = 0; // вычисляем число типов задач пользователя
    for (int i = 0; i < procTypes.length; i++) {
      if (procTypes[i].isUserProc) {
        nt++;
      }
    }
    f.IT_PTYP_create(nt);
    RefInfoStruct taskInfo;
    int j = 0;
    for (int i = 0; i < procTypes.length; i++) {
      if (procTypes[i].isUserProc) {
        f.IT_PTYP[j].PTYP_ID = i;
        f.IT_PTYP[j].PTYP = procTypes[i].name();
        f.IT_PTYP[j].PTYP_NAME = procTypes[i].text;

        taskInfo = taskInfos.get(i);
        if (taskInfo == null) {
          f.IT_PTYP[j].MAX_INFO_ID = -1;
        } else {
          f.IT_PTYP[j].MAX_INFO_ID = taskInfo.maxInfoId;
          f.IT_PTYP[j].MAX_VER = taskInfo.maxVer;
        }
        j++;
      }
    }

    f.execute();

    if (!f.isErr) {
      InfoData idata;
      int txtId;
      ArrayList<String> ss = new ArrayList<String>();
      String s;
      for (int i = 0; i < f.IT_PTYP.length; i++) {
        if (f.IT_PTYP[i].MAX_INFO_ID >= 0) {
          taskInfo = taskInfos.get(f.IT_PTYP[i].PTYP_ID);
          if (taskInfo == null) {
            taskInfo = new RefInfoStruct();
            taskInfo.procTyp = f.IT_PTYP[i].PTYP_ID;
            taskInfos.put(taskInfo.procTyp, taskInfo);
          }
          // общая информация
          taskInfo.maxInfoId = f.IT_PTYP[i].MAX_INFO_ID;
          taskInfo.maxVer = f.IT_PTYP[i].MAX_VER;
          taskInfo.haveManual = f.IT_PTYP[i].HAVE_MANUAL.equals("X");
          // удаление инструкций
          for (int ii = 0; ii < f.IT_DEL_MAN.length; ii++) {
            if (f.IT_DEL_MAN[ii].PTYP_ID == taskInfo.procTyp) {
              taskInfo.infos.remove(f.IT_DEL_MAN[ii].INFO_NO);
            }
          }
          // добавление инструкций
          for (int ii = 0; ii < f.IT_MAN.length; ii++) {
            if (f.IT_MAN[ii].PTYP_ID == taskInfo.procTyp) {
              idata = taskInfo.infos.get(f.IT_MAN[ii].INFO_NO);
              if (idata == null) {
                idata = new InfoData();
                idata.infoId = f.IT_MAN[ii].INFO_NO;
                taskInfo.infos.put(idata.infoId, idata);
              }
              idata.name = f.IT_MAN[ii].NAME;
              idata.ver = f.IT_MAN[ii].VER;
              txtId = f.IT_MAN[ii].TXT_ID;
              // запись текста в idata
              ss.clear();
              s = null;
              for (int jj = 0; jj < f.IT_TEXT.length; jj++) {
                if (f.IT_TEXT[jj].TXT_ID == txtId) {
                  if (s == null) {
                    s = f.IT_TEXT[jj].TXT;
                  } else {
                    s = s + " " + f.IT_TEXT[jj].TXT;
                  }
                  if (f.IT_TEXT[jj].CONT_LIN.isEmpty()) {
                    ss.add(s);
                    s = null;
                  }
                }
              }
              if (s != null) {
                ss.add(s);
                s = null;
              }
              Object[] oo = ss.toArray();
              if (ss.isEmpty()) {
                idata.txt = new String[0];
              } else {
                idata.txt = ss.toArray(new String[0]);
              }
              ss.clear();
            }
          }
        }
      }
      return null;
    } else {
      return f.err;
    }
  }
}

class InfoData {
  // единица информации (текст)

  public String[] txt; // текст сообщения
  public int ver; // номер версии
  public int infoId; // последовательная нумерация сообщений (0 - инструкция)
  public String name; // название сообщения (м.б. просто дата)
}

class RefInfoStruct {
  // структура записи справочника сообщений

  public int procTyp; // тип задачи
  public boolean haveManual; // признак наличия инструкции
  public int maxVer; // максимальный номер версии (для запросов изменений)
  public int maxInfoId; // максимальный номер сообщения (для обработки запросов пользователей)
  public HashMap<Integer, InfoData> infos = new HashMap<Integer, InfoData>(); // инструкции (сообщения)
}
