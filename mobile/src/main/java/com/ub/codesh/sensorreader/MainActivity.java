package com.ub.codesh.sensorreader;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private RadioGroup mRadioGroup;
    private int SensorRate=0;
    private List<String> names=new ArrayList<>();
    private List<String> stringValues=new ArrayList<>();

    private Recorder recorder_Accelerometer=new Recorder();
    private Recorder recorder_Gyroscope=new Recorder();
    private Recorder recorder_Gravity=new Recorder();
    private Recorder recorder_Magnetic=new Recorder();
    private Recorder recorder_AccMagOrientation = new Recorder();
    private Recorder recorder_FinalValue =new Recorder();
    private Boolean isRecording=false;

    private float[] value_accelerometer = new float[3];
    private float[] value_magnetic = new float[3];
    private float[] value_gyroscope = new float[3];
    private float[] value_gravity = new float[3];


    private float[] rotationMatrix = new float[9];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    private static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private Button button;

    private Timer timer;
    private TimerTask task;
    private Timer fuseTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        button= (Button) findViewById(R.id.button);

        verifyStoragePermissions(this);

        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);

        // initialize orientation
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;
        // initialize gyroMatrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;

        names.add("X");
        names.add("Y");
        names.add("Z");

        SensorRate=100000;
        mRadioGroup=findViewById(R.id.radio_group);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.radiobutton1:
                        SensorRate=100000;
//                        System.out.println("Rate set to 100000");
                        break;
                    case R.id.radiobutton2:
                        SensorRate=50000;
//                        System.out.println("Rate set to 50000");
                        break;
                    case R.id.radiobutton3:
                        SensorRate=20000;
//                        System.out.println("Rate set to 20000");
                        break;
                    case R.id.radiobutton4:
                        SensorRate=10000;
//                        System.out.println("Rate set to 10000");
                        break;
                    default:
                        break;
                }
            }
        });


        task = new TimerTask() {
            @Override
            public void run() {
                if(isRecording)
                {
                    recorder_Accelerometer.flush();
                    recorder_Gyroscope.flush();
                    recorder_Gravity.flush();
                    recorder_Magnetic.flush();
                    recorder_AccMagOrientation.flush();
                    recorder_FinalValue.flush();
                }            }
        };

        timer = new Timer();
        timer.schedule(task, 10000, 10000);

        fuseTimer = new Timer();
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                1000, SensorRate/1000);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
        if(isRecording)
        {
            recorder_Accelerometer.flush();
            recorder_Gyroscope.flush();
            recorder_Gravity.flush();
            recorder_Magnetic.flush();
            recorder_AccMagOrientation.flush();
            recorder_FinalValue.flush();
            isRecording=false;
            button.setText("Start");
            System.out.println("CSV saved");
            for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                mRadioGroup.getChildAt(i).setEnabled(true);
            }
        }
    }

    public void clickStart(View V){
        if(isRecording==false){
            sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorRate);
            sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorRate);
            sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),SensorRate);
            sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorRate);

            recorder_Accelerometer.open("Acclerometer",names);
            recorder_Gyroscope.open("Gyroscpoe", names);
            recorder_Gravity.open("Gravity", names);
            recorder_Magnetic.open("Magnetic", names);
            recorder_AccMagOrientation.open("FinalValueUnmodified", names);
            recorder_FinalValue.open("FinalValueModifued",names);
            System.out.println("Start recording");
            button.setText("Stop");
            isRecording=true;

            for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                mRadioGroup.getChildAt(i).setEnabled(false);
            }
        }else{
            recorder_Accelerometer.flush();
            recorder_Gyroscope.flush();
            recorder_Gravity.flush();
            recorder_Magnetic.flush();
            recorder_AccMagOrientation.flush();
            recorder_FinalValue.flush();
            isRecording=false;
            button.setText("Start");
            System.out.println("CSV saved");

            for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                mRadioGroup.getChildAt(i).setEnabled(true);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER && isRecording)
        {
            for(int i=0;i<3;i++)
            {
                value_accelerometer[i]=values[i];
                stringValues.add(Float.toString(values[i]));
            }
            recorder_Accelerometer.writeCsv(stringValues);
            stringValues.clear();

            calculateAccMagOrientation();
        }else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE && isRecording)
        {
            for(int i=0;i<3;i++) {
                value_gyroscope[i] = values[i];
                stringValues.add(Float.toString(values[i]));
            }
            recorder_Gyroscope.writeCsv(stringValues);
            stringValues.clear();

            gyroFunction(event);
        }else if(event.sensor.getType()==Sensor.TYPE_GRAVITY && isRecording)
        {
            for(int i=0;i<3;i++) {
                value_gravity[i] = values[i];
                stringValues.add(Float.toString(values[i]));
            }
            recorder_Gravity.writeCsv(stringValues);
            stringValues.clear();
        }else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD && isRecording)
        {
            for(int i=0;i<3;i++) {
                value_magnetic[i] = values[i];
                stringValues.add(Float.toString(values[i]));
            }
            recorder_Magnetic.writeCsv(stringValues);
            stringValues.clear();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, value_accelerometer, value_magnetic)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    private void getRotationVectorFromGyro(float[] gyroValues,
                                           float[] deltaRotationVector,
                                           float timeFactor)
    {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, value_gyroscope, 0, 3);
            getRotationVectorFromGyro(value_gyroscope, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    private void matrixFrom9to16(float[] A, float[] B)
    {
        for(int i=0;i<4;i++) {
            for(int j=0;j<4;j++) {
                if(i!=3&&j!=3) {
                    A[i*4+j]=rotationMatrix[i*3+j];
                }else {
                    A[i*4+j]=0;
                }
            }
        }
        A[15]=1;

    }

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            if(isRecording)
            {
                float[] rotate = new float[16];
                float[] relative_value = new float[4];
                float[] inverse_rotate = new float[16];
                float[] values_earth_unmodified = new float[4];
                float[] values_earth_modified = new float[4];

                rotationMatrix=getRotationMatrixFromOrientation(accMagOrientation);
                matrixFrom9to16(rotate,rotationMatrix);
                relative_value[0]=value_accelerometer[0];
                relative_value[1]=value_accelerometer[1];
                relative_value[2]=value_accelerometer[2];
                relative_value[3]=0;
                android.opengl.Matrix.invertM(inverse_rotate, 0, rotate, 0);
                android.opengl.Matrix.multiplyMV(values_earth_unmodified, 0, inverse_rotate, 0, relative_value, 0);

                rotationMatrix=getRotationMatrixFromOrientation(fusedOrientation);
                matrixFrom9to16(rotate,rotationMatrix);
                relative_value[0]=value_accelerometer[0];
                relative_value[1]=value_accelerometer[1];
                relative_value[2]=value_accelerometer[2];
                relative_value[3]=0;
                android.opengl.Matrix.invertM(inverse_rotate, 0, rotate, 0);
                android.opengl.Matrix.multiplyMV(values_earth_modified, 0, inverse_rotate, 0, relative_value, 0);

                for(int i=0;i<3;i++)
                {
                    stringValues.add(Float.toString(values_earth_unmodified[i]));
                }
                recorder_AccMagOrientation.writeCsv(stringValues);
                stringValues.clear();

                for(int i=0;i<3;i++)
                {
                    stringValues.add(Float.toString(values_earth_modified[i]));
                }
                recorder_FinalValue.writeCsv(stringValues);
                stringValues.clear();
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }
}
