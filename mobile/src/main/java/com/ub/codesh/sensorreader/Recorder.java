package com.ub.codesh.sensorreader;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class Recorder {
    private final String mComma = ",";
    private StringBuilder mStringBuilder = null;
    private String mFileName = null;
    private SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    public void open(String filename, List<String> valueNames) {
        String folderName = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path != null) {
                folderName = path + "/CSV/";
            }
        }

        File fileRobo = new File(folderName);
        if (!fileRobo.exists()) {
            fileRobo.mkdir();
        }

        String deviceModel = Build.MODEL;
        mFileName = folderName +deviceModel+"-"+ filename +"-" + format.format(System.currentTimeMillis()) + ".csv";
        mStringBuilder = new StringBuilder();
        mStringBuilder.append("Time");
        mStringBuilder.append(mComma);
        for(int i=0;i<valueNames.size();i++){
            if(i!=valueNames.size()-1){
                mStringBuilder.append(valueNames.get(i));
                mStringBuilder.append(mComma);
            }else{
                mStringBuilder.append(valueNames.get(i));
                mStringBuilder.append("\n");
            }
        }
    }

    public void writeCsv(List<String> values){
        mStringBuilder.append(System.currentTimeMillis());
        mStringBuilder.append(mComma);
        for(int i=0;i<values.size();i++){
            if(i!=values.size()-1){
                mStringBuilder.append(values.get(i));
                mStringBuilder.append(mComma);
            }else{
                mStringBuilder.append(values.get(i));
                mStringBuilder.append("\n");
            }
        }
    }


    public void flush() {
        if (mFileName != null) {
            try {
                File file = new File(mFileName);
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write(mStringBuilder.toString().getBytes());
                fos.flush();
                fos.close();
                mStringBuilder.delete(0,mStringBuilder.length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("You should call open() before flush()");
        }
    }

}
