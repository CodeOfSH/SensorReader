package com.ub.codesh.sensorreader;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private List<Float> list = new ArrayList<>();
    private List<String> names=new ArrayList<>();
    private List<String> stringValues=new ArrayList<>();
    private List<Integer> color=new ArrayList<>();

    private Recorder recorder_Accelerometer=new Recorder();
    private Recorder recorder_Gyroscope=new Recorder();
    private Recorder recorder_Magnetic=new Recorder();
    private Boolean isRecording=false;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private Button button;

    private Timer timer;
    private TimerTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // // Enables Always-on
        //setAmbientEnabled();

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        button= (Button) findViewById(R.id.button);

        verifyStoragePermissions(this);

        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);

        names.add("X");
        names.add("Y");
        names.add("Z");


        task = new TimerTask() {
            @Override
            public void run() {
                if(isRecording)
                {
                    recorder_Accelerometer.flush();
                    recorder_Gyroscope.flush();
                    recorder_Magnetic.flush();
                }            }
        };

        timer = new Timer();
        timer.schedule(task, 10000, 10000);
    }

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //check if have the permission
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // if no permission, ask for permission
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //__________________________________________________________________
        //__________________________________________________________________
        //__________________________________________________________________
        //The end of this line is where you need to modify the sensor rate!
        //__________________________________________________________________
        //__________________________________________________________________
        //__________________________________________________________________
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
        if(isRecording)
        {
            recorder_Accelerometer.flush();
            recorder_Gyroscope.flush();
            recorder_Magnetic.flush();
            isRecording=false;
            button.setText("Start");
            System.out.println("CSV saved");
        }
    }

    public void clickStart(View V){
        if(isRecording==false){
            recorder_Accelerometer.open("Acclerometer",names);
            recorder_Gyroscope.open("Gyroscpoe", names);
            recorder_Magnetic.open("Magnetic", names);
            System.out.println("Start recording");
            button.setText("Stop");
            isRecording=true;
        }else{
            recorder_Accelerometer.flush();
            recorder_Gyroscope.flush();
            recorder_Magnetic.flush();
            isRecording=false;
            button.setText("Start");
            System.out.println("CSV saved");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER && isRecording)
        {
                stringValues.add(Float.toString(values[0]));
                stringValues.add(Float.toString(values[1]));
                stringValues.add(Float.toString(values[2]));
                recorder_Accelerometer.writeCsv(stringValues);
                stringValues.clear();

        }else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE && isRecording)
        {
                stringValues.add(Float.toString(values[0]));
                stringValues.add(Float.toString(values[1]));
                stringValues.add(Float.toString(values[2]));
                recorder_Gyroscope.writeCsv(stringValues);
                stringValues.clear();
        }else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD && isRecording)
        {
                stringValues.add(Float.toString(values[0]));
                stringValues.add(Float.toString(values[1]));
                stringValues.add(Float.toString(values[2]));
                recorder_Magnetic.writeCsv(stringValues);
                stringValues.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
