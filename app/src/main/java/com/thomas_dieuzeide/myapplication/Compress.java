package com.thomas_dieuzeide.myapplication;

/**
 * Created by Thomas_Dieuzeide on 4/19/2016.
 */

import android.os.Environment;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Compress {
    private static final int BUFFER = 2048;

    private File[] _files;
    private String _zipFile;

    public Compress(File[] files, String zipFile) {
        _files = files;
        _zipFile = zipFile;
    }

    public File zip() {
        try  {
            BufferedInputStream origin = null;
            File zip = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Bons/"+_zipFile+".zip");
            zip.getParentFile().mkdirs();
            FileOutputStream dest = new FileOutputStream(zip);

            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

            byte data[] = new byte[BUFFER];

            for(int i=0; i < _files.length; i++) {
                Log.v("Compress", "Adding: " + _files[i].getAbsolutePath());
                FileInputStream fi = new FileInputStream(_files[i]);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(_files[i].getAbsolutePath().substring(_files[i].getAbsolutePath().lastIndexOf("/") + 1));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }

            out.close();
            return zip;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
