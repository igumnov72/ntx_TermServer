package ntx.ts.srv;

import ntx.ts.http.HttpSrv;
import ntx.sap.sys.SAPconn;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import ntx.sap.refs.RefInfo;

public class Start {

  public static void main(String[] args) throws Exception {

    System.setErr(new PrintStream(
            new BufferedOutputStream(
                    new DateTimeLogger(
                            new FileOutputStream("ts.err.log", true))), false));

    System.setOut(new PrintStream(
            new BufferedOutputStream(
                    new DateTimeLogger(
                            new FileOutputStream("ts.out.log", true))), false));

    System.out.println("Server starting ...");
    System.out.flush();

    try {
      TSparams.load("ts.ini");
      SAPconn.init();
      TermServer.init();
      RefInfo.checkNewInfo();
      HttpSrv.Start();

      if (TSparams.port > 0) {
        HttpSrv httpSrv = new HttpSrv(0, false, TSparams.port);
        (new Thread(httpSrv, "main http server loop TCP " + TSparams.port)).start();
      }
      if (TSparams.port_pl > 0) {
        HttpSrv httpSrv = new HttpSrv(1, false, TSparams.port_pl);
        (new Thread(httpSrv, "main http server loop TCP " + TSparams.port_pl)).start();
      }
      if (TSparams.port_scan_udp > 0) {
        HttpSrv httpSrv = new HttpSrv(2, true, TSparams.port_scan_udp);
        (new Thread(httpSrv, "main http server loop UDP " + TSparams.port_scan_udp)).start();
      }

      TermServer.paramsToSAP();
    } catch (Exception e) {
      System.err.println("Error starting terminal server:");
      e.printStackTrace();
      System.err.flush();
    }

    System.out.println("... server started");
    System.out.flush();
  }
}
