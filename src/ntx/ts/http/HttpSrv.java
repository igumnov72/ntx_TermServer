package ntx.ts.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import ntx.ts.srv.DateTimeLogger;
import ntx.ts.srv.TSparams;
import ntx.ts.srv.TermQuery;
import ntx.ts.srv.TermServer;

public class HttpSrv implements Runnable {

  private int querySource; // 0 - терминалы склада, 1 - планшет в цехе, 2 - сканер в цехе
  private boolean isUDP;
  private int port;
//  protected static final Vector<Worker> threads = new Vector<Worker>(); //Where worker threads stand idle
  protected static final List<Worker> threads = Collections.synchronizedList(new ArrayList<Worker>());

  ; //Where worker threads stand idle
  private static int workerNo = 0;

  public HttpSrv(int querySource, boolean isUDP, int port) {
    this.querySource = querySource;
    this.isUDP = isUDP;
    this.port = port;
  }

  public static int getThreadsSize() {
    return threads.size();
  }

  public static int getWorkerNo() {
    return workerNo;
  }

  public static void Start() throws Exception {
    /* start worker threads */
    for (int i = 0; i < TSparams.workers; ++i) {
      Worker w = new Worker();
      workerNo++;
      (new Thread(w, "worker #" + workerNo)).start();
//      threads.addElement(w);
      threads.add(w);
    }
  }

  public void run() {
    try {
      ServerSocket ss = null;
      if (isUDP) {
        // ...
      } else {
        ss = new ServerSocket(port);
      }

      while (true) {
        try {
          Socket s = ss.accept();
          if (TSparams.logDocLevel >= 1) {
            System.out.println("new socket accepted");
          }
          Worker w = null;
          synchronized (threads) {
            if (threads.isEmpty()) {
              Worker ws = new Worker();
              ws.setSocket(s, querySource, isUDP);
              workerNo++;
              (new Thread(ws, "additional worker #" + workerNo)).start();
            } else {
//              w = (Worker) threads.elementAt(0);
//              threads.removeElementAt(0);
              w = threads.get(0);
              threads.remove(0);
              w.setSocket(s, querySource, isUDP);
            }
          }
        } catch (Exception e) {
          System.err.println("Error in main http loop:");
          e.printStackTrace();
          System.err.flush();
        }
      }
    } catch (Exception e) {
      System.err.println("Error entering main http loop:");
      e.printStackTrace();
      System.err.flush();
    }
  }
}

class Worker implements HttpConstants, Runnable {

  private Socket s;
  private int querySource; // 0 - терминалы склада, 1 - планшет в цехе, 2 - сканер в цехе
  private boolean isUDP;
  private static DateFormat gmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.UK);
//  private static java.util.Hashtable<String, String> map = new java.util.Hashtable<String, String>();
  private static java.util.HashMap<String, String> map = new java.util.HashMap<String, String>();

//  Worker() {
//    s = null;
//  }
  synchronized void setSocket(Socket s, int querySource, boolean isUDP) {
    this.s = s;
    this.querySource = querySource;
    this.isUDP = isUDP;
    notify();
  }

  public synchronized void run() {
    while (true) {
      if (s == null) {
        /* nothing to do */
        try {
          wait();
        } catch (InterruptedException e) {
          /* should not happen */
          continue;
        }
      }
      if (TSparams.logDocLevel >= 1) {
        System.out.println("starting handling connection");
      }
      try {
        if (isUDP) {
          // ...
        } else {
          handleClientTCP();
        }
      } catch (Exception e) {
        System.err.println("Error handling client request:");
        e.printStackTrace();
        System.err.flush();
      }
      if (TSparams.logDocLevel >= 1) {
        System.out.println("finished handling connection");
      }
      System.out.flush();
      /* go back in wait queue if there's1 fewer
       * than numHandler connections.
       */
      s = null;
      synchronized (HttpSrv.threads) {
        if (HttpSrv.threads.size() > TSparams.workers) {
          System.out.println("too many threads (" + HttpSrv.threads.size() + "), exit this one");
          System.out.flush();
          return;
        } else {
          HttpSrv.threads.add(this);
        }
      }
    }
  }

  private void handleClientTCP() throws Exception {
    InputStream is = new BufferedInputStream(s.getInputStream());
    PrintStream ps = new PrintStream(s.getOutputStream());
    /* we will only block in read for this many milliseconds
     * before we fail with java.io.InterruptedIOException,
     * at which point we will abandon the connection.
     */
    s.setSoTimeout(TSparams.timeout);
    s.setTcpNoDelay(true);

    String[] sa = getRequestData(is);

    if (TSparams.logDocLevel >= 3) {
      if ((sa == null) || (sa.length == 0)) {
        System.out.println("NO REQUEST DATA");
      } else {
        System.out.println(">>>>>>>>>> REQUEST DATA:");
        for (int i = 0; i < sa.length; i++) {
          System.out.println(sa[i]);
        }
        System.out.println("<<<<<<<<<< END OF REQUEST DATA");
      }
    }

    try {
      /* are we doing a GET or just a HEAD */
      TermQuery tq = new TermQuery();

      tq.querySource = querySource;
      tq.isUDP = isUDP;

      /* beginning of file name */
      int index;
      int n = sa.length;

      if ((n > 0) && sa[0].startsWith("GET ")) {
        tq.doingGet = true;
        tq.doingPost = false;
        index = 4;
      } else if ((n > 0) && sa[0].startsWith("HEAD ")) {
        tq.doingGet = false;
        tq.doingPost = false;
        index = 5;
      } else if ((n > 0) && sa[0].startsWith("POST ")) {
        tq.doingGet = false;
        tq.doingPost = true;
        index = 5;
      } else {
        /* we don't support this method */
        ps.print("HTTP/1.0 " + HTTP_BAD_METHOD
                + " unsupported method type: ");
        if ((sa[0] != null) && (sa[0].length() > 5)) {
          ps.print(sa[0].substring(0, 5));
        } else if (sa[0] != null) {
          ps.print(sa[0]);
        } else {
          ps.print("null");
        }
        ps.print("\r\n");
        ps.flush();
        System.out.println("HTTP: unsupported method type: " + sa[0]);
        System.out.flush();
        s.close();
        return;
      }

      int index2 = sa[0].indexOf(' ', index);
      if (index2 > 0) {
        tq.fname = sa[0].substring(index, index2);
      } else {
        tq.fname = "";
      }
      if (tq.fname.startsWith("/")) {
        tq.fname = tq.fname.substring(1);
      }

      if (tq.doingPost) {
        // обработка данных формы
        tq.params = new ParArray(sa[sa.length - 1], "&");
      } else if (tq.fname.isEmpty()) {
        // просто получение файла
        tq.fname = "index.html";
      }

      // получение кукисов
      for (int i = 0; i < sa.length; i++) {
        if ((sa[i].length() > 8) && (sa[i].substring(0, 8).equalsIgnoreCase("Cookie: "))) {
          tq.cookies = new ParArray(sa[i].substring(8), ";");
          break;
        }
      }

      if ((tq.cookies != null) && (tq.cookies.length() > 0)) {
        String s1 = tq.cookies.getPar("terminal");
        if ((s1 != null) && (!s1.isEmpty())) {
          tq.terminal = Long.parseLong(s1);
        }
      }

      tq.fromAddr = s.getInetAddress().getHostAddress();

      tq.sf = tq.fname.split("\\.");

      if (TSparams.logDocLevel >= 1) {
        tq.print();
      }

      FileData fd = null;
//      System.out.println("before handleQuery (waiting)");
      switch (tq.querySource) {
        case 0:
          fd = TermServer.handleQuery(tq);
          break;
        case 1:
          fd = TermServer.handleQueryWP(tq);
          break;
        case 2: // UDP
          // ...
          break;
      }

      if (fd == null) {
        printHeaders(tq.fname, null, new FileData(new byte[0]), ps, false, 0, null);
        send404(ps);
      } else {
        boolean OK = printHeaders(tq.fname, fd.redirect, fd, ps,
                (tq.terminal == 0) || tq.setTerminal, tq.terminal, tq.fromAddr);
        if (tq.doingGet || tq.doingPost) {
          if (OK) {
            sendFile(fd.ba, ps);
          } else {
            send404(ps);
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Exception while processing client request (handleClient):");
      e.printStackTrace();
      System.err.flush();
    } finally {
      s.close();
    }
  }

  public static String[] getRequestData(InputStream is) {
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    int nChars = 0;
    try {
      String line = null;
      while ((line = br.readLine()) != null) {
        if ((line.length() > 16) && (line.substring(0, 16).equalsIgnoreCase("Content-Length: "))) {
          nChars = Integer.parseInt(line.substring(16));
        }
        sb.append(line);
        sb.append("\n");
        if (line.isEmpty()) {
          break;
        }
      }

      if (nChars > 0) {
        for (int i = 0; i < nChars; i++) {
          sb.append((char) br.read());
        }
      }
    } catch (Exception e) {
      System.err.println("Error getting request data. Received:");
      String[] ss = sb.toString().split("\n");
      for (int i = 0; i < ss.length; i++) {
        System.err.println(ss[i]);
      }
//      e.printStackTrace();
      System.err.flush();
    }

    return sb.toString().split("\n");
  }

  boolean printHeaders(String fname, String redirect, FileData fd,
          PrintStream ps, boolean printCookie, long newTerminal,
          String fromAddress) throws IOException {

    boolean ret = false;

    if (redirect != null) {
      ps.print("HTTP/1.0 " + HTTP_SEE_OTHER + " Redirect\r\n");
      ret = true;
    } else if ((fd == null) || (fd.ba == null) || (fd.ba.length == 0)) {
      ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found\r\n");
      ret = false;
    } else {
      ps.print("HTTP/1.0 " + HTTP_OK + " OK\r\n");
      ret = true;
    }

    if (redirect != null) {
      ps.print("Location: " + redirect + "\r\n");
    }
    ps.print("Server: Terminal Server\r\n");
    if (!fname.endsWith(".wav")) {
      ps.print("Cache-Control: no-cache\r\n");
    }
    ps.print("Date: " + gmt.format(new Date()) + "\r\n");

    if (ret) {
      ps.print("Content-length: " + fd.ba.length + "\r\n");
      if (fd.haveDate) {
        ps.print("Last Modified: \"" + gmt.format(fd.dt) + "\"\r\n");
      }

      String fn = (redirect == null ? fname : redirect);
      int ind = fn.lastIndexOf('.');
      String ct = null;
      if (ind > 0) {
        ct = (String) map.get(fn.substring(ind));
      }
      if (ct == null) {
        ct = "unknown/unknown";
      }
      ps.print("Content-type: " + ct + "\r\n");
      if (printCookie) {
        long terminal = newTerminal;
        if (terminal == 0) {
          terminal = (new Date()).getTime();
        }
        ps.print("Set-Cookie: terminal=" + terminal
                + "; expires=Fri, 25-Dec-2099 00:00:01 GMT\r\n");
        System.out.println("Created terminal " + terminal + " ("
                + DateTimeLogger.df.format(new Date(terminal))
                + ") at " + (fromAddress == null ? "???" : fromAddress));
      }
//      ps.print("\r\n");
    }

    return ret;
  }

  void send404(PrintStream ps) throws IOException {
    ps.println("\r\n\r\nNot Found\r\n\r\nThe requested resource was not found.\r\n");
  }

  void sendFile(byte[] page, PrintStream ps) throws IOException {
    ps.print("\r\n");

    try {
      ps.write(page, 0, page.length);
    } finally {
    }
  }

  static {
    fillMap();
  }

  static void fillMap() {
    map.put("", "content/unknown");
    map.put(".uu", "application/octet-stream");
    map.put(".exe", "application/octet-stream");
    map.put(".ps", "application/postscript");
    map.put(".zip", "application/zip");
    map.put(".sh", "application/x-shar");
    map.put(".tar", "application/x-tar");
    map.put(".snd", "audio/basic");
    map.put(".au", "audio/basic");
    map.put(".wav", "audio/x-wav");
    map.put(".gif", "image/gif");
    map.put(".jpg", "image/jpeg");
    map.put(".jpeg", "image/jpeg");
    map.put(".htm", "text/html");
    map.put(".html", "text/html");
    map.put(".text", "text/plain");
    map.put(".c", "text/plain");
    map.put(".cc", "text/plain");
    map.put(".c++", "text/plain");
    map.put(".h", "text/plain");
    map.put(".pl", "text/plain");
    map.put(".txt", "text/plain");
    map.put(".log", "text/plain");
    map.put(".java", "text/plain");
  }
}

interface HttpConstants {

  /** 2XX: generally "OK" */
  public static final int HTTP_OK = 200;
  public static final int HTTP_CREATED = 201;
  public static final int HTTP_ACCEPTED = 202;
  public static final int HTTP_NOT_AUTHORITATIVE = 203;
  public static final int HTTP_NO_CONTENT = 204;
  public static final int HTTP_RESET = 205;
  public static final int HTTP_PARTIAL = 206;
  /** 3XX: relocation/redirect */
  public static final int HTTP_MULT_CHOICE = 300;
  public static final int HTTP_MOVED_PERM = 301;
  public static final int HTTP_MOVED_TEMP = 302;
  public static final int HTTP_SEE_OTHER = 303;
  public static final int HTTP_NOT_MODIFIED = 304;
  public static final int HTTP_USE_PROXY = 305;
  /** 4XX: client error */
  public static final int HTTP_BAD_REQUEST = 400;
  public static final int HTTP_UNAUTHORIZED = 401;
  public static final int HTTP_PAYMENT_REQUIRED = 402;
  public static final int HTTP_FORBIDDEN = 403;
  public static final int HTTP_NOT_FOUND = 404;
  public static final int HTTP_BAD_METHOD = 405;
  public static final int HTTP_NOT_ACCEPTABLE = 406;
  public static final int HTTP_PROXY_AUTH = 407;
  public static final int HTTP_CLIENT_TIMEOUT = 408;
  public static final int HTTP_CONFLICT = 409;
  public static final int HTTP_GONE = 410;
  public static final int HTTP_LENGTH_REQUIRED = 411;
  public static final int HTTP_PRECON_FAILED = 412;
  public static final int HTTP_ENTITY_TOO_LARGE = 413;
  public static final int HTTP_REQ_TOO_LONG = 414;
  public static final int HTTP_UNSUPPORTED_TYPE = 415;
  /** 5XX: server error */
  public static final int HTTP_SERVER_ERROR = 500;
  public static final int HTTP_INTERNAL_ERROR = 501;
  public static final int HTTP_BAD_GATEWAY = 502;
  public static final int HTTP_UNAVAILABLE = 503;
  public static final int HTTP_GATEWAY_TIMEOUT = 504;
  public static final int HTTP_VERSION = 505;
}
