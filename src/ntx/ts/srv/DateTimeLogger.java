package ntx.ts.srv;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeLogger extends OutputStream {

  private OutputStream out;
  private boolean flushed = true;
  private boolean newLine = false;
  private long lastWritenDT = 0;
  private long lastThread = 0;
  public static DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

  public DateTimeLogger(OutputStream os) throws IOException {
    if (os == null) {
      throw new NullPointerException("Null output stream");
    }
    out = os;
    writeDT("Log opened: ");
  }

  @Override
  public void close() throws IOException {
    synchronized (this) {
      if (out != null) {
        out.close();
      }
      out = null;
    }
  }

  @Override
  public void flush() throws IOException {
    synchronized (this) {
      checkOut();
      out.flush();
      flushed = true;
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    if (b == null) {
      return;
    }

    synchronized (this) {
      checkOut();
      writeDT(null);
      out.write(b);
      newLine = (b[b.length - 1] == '\n');
      flushed = false;
    }
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (b == null) {
      return;
    }

    synchronized (this) {
      checkOut();
      writeDT(null);
      out.write(b, off, len);
      newLine = (b[off + len - 1] == '\n');
      flushed = false;
    }
  }

  @Override
  public void write(int b) throws IOException {
    synchronized (this) {
      checkOut();
      writeDT(null);
      out.write(b);
      newLine = (b == '\n');
      flushed = false;
    }
  }

  private void checkOut() {
    if (out == null) {
      throw new NullPointerException("Null output stream");
    }
  }

  private void writeDT(String str) throws IOException {
    long curThread = Thread.currentThread().getId();

    if (!flushed && (curThread == lastThread)) {
      return;
    }

    long dt = (new Date()).getTime();

    if ((dt < (lastWritenDT + 100)) && (curThread == lastThread)) {
      return;
    }

    lastWritenDT = dt;

    String title = (newLine ? "" : "\r\n")
            + ((str == null) || (str.isEmpty()) ? "----------- " : str)
            + df.format(new Date()) + " "
            + Thread.currentThread().getName() + " (" + curThread + ")" + "\r\n";

    out.write(title.getBytes("windows-1251"));
    newLine = true;
    lastThread = curThread;
  }
}
