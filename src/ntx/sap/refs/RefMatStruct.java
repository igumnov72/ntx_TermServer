package ntx.sap.refs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import ntx.sap.fm.Z_TS_MAT_IMG;
import ntx.sap.sys.SAPconn;
import ntx.ts.sysproc.ProcessTask;

/**
 * Структура записи о материале
 */
public class RefMatStruct {

  public String matnr;
  public String name;
  public String fullName;
  public boolean haveFoto;
  public byte[] foto = null;
  public long dtMod = 0;

  public RefMatStruct(String matnr, String name, String fullName, String haveFoto) {
    this.matnr = matnr;
    this.name = name;
    this.fullName = fullName;
    if ((this.fullName != null) && this.fullName.isEmpty()) {
      this.fullName = null;
    }
    if (haveFoto.equals("X")) {
      this.haveFoto = true;
    }
  }

  public boolean equals(RefMatStruct m) {
    if (ProcessTask.strEq(name, m.name) && ProcessTask.strEq(fullName, m.fullName)) {
      return true;
    } else {
      return false;
    }
  }

  private static byte[] readLocalFile(String fname) {
    File f = new File(fname);

    if (!f.exists() || !f.isFile()) {
      return new byte[0];
    }
    String l_fileName = f.getName();
    long l_fileTimeStamp = f.lastModified();
    int l_fileSize = (int) f.length();
    byte[] l_fileData = new byte[l_fileSize];

    FileInputStream is = null;

    try {
      is = new FileInputStream(f);
      int r = 0;
      int off = 0;

      while (r != -1) {
        r = is.read(l_fileData, off, l_fileSize - off);
        if (r > 0) {
          off += r;
        } else if (r == 0) {
          break;
        }
      }
      if (off != l_fileSize) {
        return new byte[0];
      }
    } catch (IOException e) {
      return new byte[0];
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception ee) {
        }
      }
    }

    return l_fileData;
  }

  public byte[] getFoto() {
    if (!haveFoto) {
      return readLocalFile("no_foto.jpg");
    } else if (foto != null) {
      return foto;
    }

    Z_TS_MAT_IMG f = new Z_TS_MAT_IMG();
    f.MATNR = SAPconn.fillZeros(matnr, 18);
    f.execute();
    if (f.isErr) {
      return readLocalFile("err_foto.jpg");
    }

    if (f.NO_FOTO.equals("X")) {
      haveFoto = false;
      return readLocalFile("no_foto.jpg");
    }

    byte[] ret = new byte[f.COMP_SIZE];
    int j = 0;
    int n;

    for (int i = 0; i < f.IT.length; i++) {
      n = f.IT[i].LINE.length;
      if ((j + n) > f.COMP_SIZE) {
        n = f.COMP_SIZE - j;
      }
      if (n == 0) {
        return readLocalFile("err_foto.jpg");
      }
      System.arraycopy(f.IT[i].LINE, 0, ret, j, n);
      j += n;
    }

    if (j != f.COMP_SIZE) {
      return readLocalFile("err_foto.jpg");
    }

    dtMod = f.DT_MOD;
    dtMod *= 1000;

    foto = ret;
    return foto;
  }
}
