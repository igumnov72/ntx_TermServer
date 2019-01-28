package ntx.ts.http;

import java.io.UnsupportedEncodingException;
import java.util.Date;

public class FileData {
  public byte[] ba = null;
  public boolean haveDate = false;
  public Date dt;
  public String redirect = null;
  public long formTime = 0;
  
  public FileData(byte[] a_ba){
    haveDate = false;
    ba = a_ba;
  }
  
  public FileData(byte[] a_ba, Date a_dt){
    haveDate = true;
    ba = a_ba;
    dt = a_dt;
  }

  public FileData(String a_str) throws UnsupportedEncodingException {
    haveDate = false;
    ba = a_str.getBytes("utf-8");
  }
}
