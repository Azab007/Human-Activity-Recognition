// For data export
// C:\Users\Oskars\Downloads\adb>adb -d shell "run-as com.example.oskars.xyzregister_v2 cat /data/data/com.example.oskars.xyzregister_v2/files/accData.txt" > accData.txt


package io.github.introml.activityrecognition;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {
    private float[] results;
    private static final int N_SAMPLES = 100;
    private static List<Float> xA, yA, zA, xG, yG, zG;
    private static List<Long> timestamps;
    private static String filename;
    private static String[] labels = {"Walking","Jogging","Sitting","Standing"};
    private static String[] labels1 = {"Walking","Jogging","Stairs","Sitting or standing"};
    private TextView walkingTextView, joggingTextView, sittingTextView, standingTextView;
    private TextView firstTextView, secondTextView, thirdTextView, forthTextView;
    private TextView logTextView;
    private TextToSpeech textToSpeech;
    private TensorFlowClassifier classifier;
    private Switch ClassifierSwitch;
    private Switch SoundSwitch;
    private Switch modelSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ClassifierSwitch = (Switch) findViewById(R.id.classifierSwitch);
        SoundSwitch = (Switch) findViewById(R.id.soundSwitch);
        modelSwitch = (Switch) findViewById(R.id.modelSwitch);
        ClassifierSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked){
                    onPause();
                }
                else {
                    onResume();
                }
            }
        });

        modelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSensorManager().unregisterListener(MainActivity.this);

                if (!isChecked){
                    xA.clear();
                    yA.clear();
                    zA.clear();
                    getSensorManager().registerListener(MainActivity.this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);


                }
                else {
                    xG.clear();
                    yG.clear();
                    zG.clear();
                    getSensorManager().registerListener(MainActivity.this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

                }
            }
        });

        prepareTextviews();
        setTextviews();
        xA = new ArrayList<>();
        yA = new ArrayList<>();
        zA = new ArrayList<>();
        xG = new ArrayList<>();
        yG = new ArrayList<>();
        zG = new ArrayList<>();
        timestamps = new ArrayList<>();
        classifier = new TensorFlowClassifier(getApplicationContext());
        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
        cleanFile(getBaseContext());
    }

    @Override
    public void onInit(int status) {
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        if(modelSwitch.isChecked()) {
            getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        }
        else {
            getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if ( !modelSwitch.isChecked() && sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            activityPrediction();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            xG.add(event.values[0]);
            yG.add(event.values[1]);
            zG.add(event.values[2]);
            String dataLine = String.format("%d,%s,%s,%s%n",
                    timestamp.getTime(), xG.get(xG.size() - 1), yG.get(yG.size() - 1), zG.get(zG.size() - 1));
            writeToFile(dataLine, getBaseContext());
            timestamps.add(timestamp.getTime());
        }
        if ( modelSwitch.isChecked() && sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            activityPrediction();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            xA.add(event.values[0]);
            yA.add(event.values[1]);
            zA.add(event.values[2]);
            String dataLine = String.format("%d,%s,%s,%s%n",
                    timestamp.getTime(), xA.get(xA.size() - 1), yA.get(yA.size() - 1), zA.get(zA.size() - 1));
            writeToFile(dataLine, getBaseContext());
            timestamps.add(timestamp.getTime());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {
        if(modelSwitch.isChecked()==true){
            if (xA.size() == N_SAMPLES && yA.size() == N_SAMPLES && zA.size() == N_SAMPLES) {
                new Thread(new secondThread(xA, yA, zA, timestamps)).start();
            }
        }
        else{
            if (xG.size() == N_SAMPLES && yG.size() == N_SAMPLES && zG.size() == N_SAMPLES) {
                new Thread(new secondThread(xG, yG, zG, timestamps)).start();
            }
        }

    }

    private void cleanFile(Context context) {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
            filename = "accData-" + dateFormat.format(date) + ".txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write("");
            outputStreamWriter.close();
            logTextView.setText(String.format("File (%s) cleaned", filename));
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void prepareTextviews() {
        joggingTextView = (TextView) findViewById(R.id.jogging_prob);
        sittingTextView = (TextView) findViewById(R.id.sitting_prob);
        standingTextView = (TextView) findViewById(R.id.standing_prob);
        walkingTextView = (TextView) findViewById(R.id.walking_prob);
        firstTextView = (TextView) findViewById(R.id.walking_title);
        secondTextView = (TextView) findViewById(R.id.jogging_title);
        thirdTextView = (TextView) findViewById(R.id.sitting_title);
        forthTextView = (TextView) findViewById(R.id.standing_title);
        logTextView = (TextView) findViewById(R.id.log);
    }

    private void setTextviews() {
        firstTextView.setText(labels[0]);
        secondTextView.setText(labels[1]);
        thirdTextView.setText(labels[2]);
        forthTextView.setText(labels[3]);
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];
        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private void writeToFile(String data, Context context) {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd");
            filename = "Data-" + dateFormat.format(date) + ".txt";
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_APPEND));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    class secondThread implements Runnable {
        List<Float> data = new ArrayList<>();
        private List<Float> x;
        private List<Float> y;
        private List<Float> z;
        private List<Long> timestamps;

        secondThread(List<Float> x, List<Float> y, List<Float> z, List<Long> timestamps) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamps = timestamps;
            data.addAll(x);
            data.addAll(y);
            data.addAll(z);
        }

        @Override
        public void run() {
            List<Float> new_data = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                new_data.add(data.get(j));
                new_data.add(data.get(100 + j));
                new_data.add(data.get(200 + j));
            }

            results = classifier.predictProbabilities(toFloatArray(new_data),modelSwitch.isChecked());
            timestamps.clear();
            x.clear();
            y.clear();
            z.clear();
            float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }
                if(modelSwitch.isChecked() && SoundSwitch.isChecked() == true){
                    textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, null);
                }
                else if (!modelSwitch.isChecked() && SoundSwitch.isChecked() == true){
                    textToSpeech.speak(labels1[idx], TextToSpeech.QUEUE_ADD, null, null);

            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Date date = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("mm-ss");
                    if(modelSwitch.isChecked()){
                        forthTextView.setText(labels[3]);
                        thirdTextView.setText(labels[2]);
                        joggingTextView.setText(Float.toString(round(results[1], 2)));
                        sittingTextView.setText(Float.toString(round(results[2], 2)));
                        standingTextView.setText(Float.toString(round(results[3], 2)));
                        walkingTextView.setText(Float.toString(round(results[0], 2)));
                    }
                    else{
                        forthTextView.setText(labels1[2]);
                        thirdTextView.setText(labels1[3]);
                        joggingTextView.setText(Float.toString(round(results[1], 2)));
                        sittingTextView.setText(Float.toString(round(results[3], 2)));
                        standingTextView.setText(Float.toString(round(results[2], 2)));
                        walkingTextView.setText(Float.toString(round(results[0], 2)));
                    }

                    logTextView.setText(dateFormat.format(date));
                }
            });
        }
    }
}
