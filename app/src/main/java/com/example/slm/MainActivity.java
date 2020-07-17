package com.example.slm;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {
    String UUIDFileName = "myUUID";
    LineChart lineChart;
    AudioDispatcher dispatcher;
    FirebaseFirestore db;
    List<Double> A = new ArrayList<>();

    int bufferSize = 22050;
    int sampleRate = 44100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lineChart = findViewById(R.id.chart);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setData(new LineData());
        lineChart.getLegend().setForm(Legend.LegendForm.LINE);
        lineChart.invalidate();
        addListenerOnStartButton();
        JSONObject obj;
        JSONArray array;
        try {
            obj = new JSONObject(loadJSONFromAsset(this));
//            Toast.makeText(this, obj.toString(), Toast.LENGTH_SHORT).show();
            array = obj.getJSONArray("A");
            for (int i = 0; i < array.length(); i++) {
                A.add(Double.parseDouble(array.getString(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        ArrayList<JSONObject> arrays = new ArrayList<>();
//        for (int i = 0; i < size; i++) {
//            JSONObject another_json_object = null;
//            try {
//                another_json_object = a.getJSONObject(i);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            //Blah blah blah...
//            arrays.add(another_json_object);
//        }
//
//        //Finally
//        JSONObject[] jsons = new JSONObject[arrays.size()];
//        arrays.toArray(jsons);
        Toast.makeText(this, Double.toString(A.get(0)), Toast.LENGTH_SHORT).show();
    }

    void addListenerOnStartButton() {
        final Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(
                new View.OnClickListener() {
                    @SuppressLint({"SetTextI18n", "HardwareIds"})
                    @Override
                    public void onClick(View view) {
                        if (startButton.getText().toString().equals("Start")) {
                            startButton.setText("Stop");
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {
//                            Toast.makeText(MainActivity.this, "Ok", Toast.LENGTH_SHORT).show();
                                // Permission is granted
                                soundDetection();
                            } else {
                                Toast.makeText(MainActivity.this, "False", Toast.LENGTH_SHORT).show();
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.RECORD_AUDIO},
                                        1);
                            }
                        } else if (startButton.getText().toString().equals("Stop")) {
                            dispatcher.stop();
                            startButton.setText("Start");
                            Map<String, Object> data = new HashMap<>();
                            data.put("Brand", Build.BRAND);
                            data.put("Product", Build.PRODUCT);
                            data.put("Board", Build.BOARD);
                            data.put("Bootloader", Build.BOOTLOADER);
                            data.put("Device", Build.DEVICE);
                            data.put("Display", Build.DISPLAY);
                            data.put("FingerPrint", Build.FINGERPRINT);
                            data.put("Hareware", Build.HARDWARE);
                            data.put("Host", Build.HOST);
                            data.put("ID", Build.ID);
                            data.put("Manufacturer", Build.MANUFACTURER);
                            data.put("Model", Build.MODEL);
                            data.put("Tags", Build.TAGS);
                            data.put("Time", Build.TIME);
                            data.put("Type", Build.TYPE);
                            data.put("Unknow", Build.UNKNOWN);
                            data.put("User", Build.USER);
                            data.put("CPUABI", Build.CPU_ABI);
                            data.put("CPUABI2", Build.CPU_ABI2);
                            data.put("Radio", Build.RADIO);
                            data.put("Serial", Build.SERIAL);
                            data.put("SDK_INT", Build.VERSION.SDK_INT);
                            data.put("dB", 50);

                            ContentResolver resolver = getContentResolver();
                            String UUID = "";
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                                UUID = GUID.checkUUIDFileByUri(UUIDFileName, resolver);
                                Toast.makeText(MainActivity.this, UUID, Toast.LENGTH_SHORT).show();
                                if (UUID.equals("")) {
//                                    Toast.makeText(MainActivity.this, "UUIDDD!", Toast.LENGTH_SHORT).show();
                                    UUID = GUID.creatUUIDFile(UUIDFileName, resolver);
                                }
                            }
                            data.put("UUID", UUID);

                            db = FirebaseFirestore.getInstance();
                            Toast.makeText(MainActivity.this, "Here!", Toast.LENGTH_SHORT).show();

                            db.collection("Redmitest").add(data).addOnCompleteListener(
                                    new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(MainActivity.this, "add!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //這個"CUSTOM_NUMBER"就是上述的自訂意義的請求代碼
        final int CUSTOM_NUMBER = 1;
        if (requestCode == CUSTOM_NUMBER) {
            //假如允許了
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //do something
                Toast.makeText(this, "已經拿到CAMERA權限囉!", Toast.LENGTH_SHORT).show();
                soundDetection();
            }
            //假如拒絕了
            else {
                //do something
                Toast.makeText(this, "CAMERA權限FAIL", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

//    void showPitch() {
//        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
//
//        PitchDetectionHandler pdh = new PitchDetectionHandler() {
//            @Override
//            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
//                final float pitchInHz = result.getPitch();
//                runOnUiThread(new Runnable() {
//                    @SuppressLint("SetTextI18n")
//                    @Override
//                    public void run() {
//                        TextView text = (TextView) findViewById(R.id.dBSPLView);
//                        text.setText("" + pitchInHz);
//                    }
//
//                });
//            }
//        };
//        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pdh);
//        dispatcher.addAudioProcessor(p);
//        new Thread(dispatcher, "Audio Dispatcher").start();
//    }

    void soundDetection() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, 0);
//        AudioDispatcher dispatcher2 = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
//        Toast.makeText(MainActivity.this, "YYYYY", Toast.LENGTH_SHORT).show();
//        Toast.makeText(MainActivity.this, "YYYYY", Toast.LENGTH_SHORT).show();

        AudioProcessor d = new AudioProcessor() {
            @Override
            public boolean process(final AudioEvent audioEvent) {
                final double dBSPL = 20 * Math.log10(AudioEvent.calculateRMS(audioEvent.getFloatBuffer()) / 0.00002);
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if (dBSPL > 0) {
                            addEntry((int) dBSPL, 0);
                            TextView text = findViewById(R.id.dBSPLView);
                            text.setText("" + (int) dBSPL + "dB SPL");
                        }
                    }
                });
                return true;
            }

            @Override
            public void processingFinished() {

            }
        };

        AudioProcessor fftProcessor = new AudioProcessor() {

            FFT fft = new FFT(bufferSize);
            float[] amplitudes = new float[bufferSize / 2];

            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                float[] transformbuffer = new float[bufferSize];
                System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
                fft.forwardTransform(transformbuffer);
                fft.modulus(transformbuffer, amplitudes);
                final int dBA = estimateLevel(amplitudes);
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        addEntry((int) dBA, 1);
                        TextView text = findViewById(R.id.dBAView);
                        text.setText("" + dBA + "dBA");
                    }
                });
                return true;
            }

            @Override
            public void processingFinished() {

            }
        };
//        new Thread(dispatcher, "Audio Dispatcher1").start();

//        PitchDetectionHandler pdh = new PitchDetectionHandler() {
//            @Override
//            public void handlePitch(PitchDetectionResult result, AudioEvent e) {
//                final float pitchInHz = result.getPitch();
//                runOnUiThread(new Runnable() {
//                    @SuppressLint("SetTextI18n")
//                    @Override
//                    public void run() {
//                        TextView text = findViewById(R.id.HzView);
//                        text.setText("" + (int) pitchInHz + "Hz");
//                    }
//                });
//            }
//        };
//
//        AudioProcessor p = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET, 22050, 22050, pdh);
//        dispatcher.addAudioProcessor(p);

        dispatcher.addAudioProcessor(d);
        dispatcher.addAudioProcessor(fftProcessor);

        new

                Thread(dispatcher, "Audio Dispatcher").

                start();

    }

    void addEntry(double newData, int num) {
        LineData data = lineChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);
        ILineDataSet set1 = data.getDataSetByIndex(1);
        // set.addEntry(...); // can be called as well
        if (set == null) {
            set = createLineDataSet(0);
            data.addDataSet(set);
        }else if(num == 0){
            set.addEntry(new Entry(set.getEntryCount(), (float) newData));
            data.notifyDataChanged();
            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();
//        lineChart.invalidate();
            lineChart.setVisibleXRangeMaximum(100);
//        lineChart.setVisibleYRangeMaximum(15, YAxis.AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
//        lineChart.moveViewToX(data.getEntryCount() - 5);
            lineChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }
        if (set1 == null) {
            set1 = createLineDataSet(1);
            data.addDataSet(set1);
        } else if(num == 1){
            set1.addEntry(new Entry(set1.getEntryCount(), (float) newData));
            data.notifyDataChanged();
            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();
//        lineChart.invalidate();
            lineChart.setVisibleXRangeMaximum(100);
//        lineChart.setVisibleYRangeMaximum(15, YAxis.AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
//        lineChart.moveViewToX(data.getEntryCount() - 5);
            lineChart.moveViewTo(data.getEntryCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }


    }

    LineDataSet createLineDataSet(int num) {
        LineDataSet set = new LineDataSet(null, num==0?"SPL":"dBA");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        // 折線的顏色
        set.setColor(num==0?Color.RED:Color.BLUE);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }

    //    float dBA(float[] buffer) {
//        for (float f : buffer) {
//            float value = SPLA(Math.pow(f, 2));
//        }
//    }
//
//    float SPLA(float fSquare) {
//        float value = 0D;
//        value += 148693636 * Math.pow(fSquare, 2) / ((fSquare + 424.36) * Math.sqrt((fSquare + 11599.29) * (fSquare + 544496.41)) * (fSquare + 148693636));
//        return value;
//    }

    //amplitudes.length = bufferSize/2  -> HzPerBin = sampleRate/(2*amplitudes.length)
    int estimateLevel(float[] amplitudes) {
        int dBA;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] == 0) amplitudes[i] = (float) Math.pow(10, -17);
        }
        float totalEnergy = 0;
        for (int i = 10; i < amplitudes.length; i++) {
//            double sum = 0;
//            for (double v : A) {
//                sum += Math.pow(amplitudes[i] * v, 2);
//            }
            //dBA
            totalEnergy  += Math.pow(amplitudes[i]/amplitudes.length*A.get(i), 2);

            //dB SPL
//            totalEnergy  += Math.pow(amplitudes[i]/amplitudes.length, 2);
//            totalEnergy += (sum / amplitudes.length * 2);
        }

//        totalEnergy = totalEnergy/((1/(double)44100)*amplitudes.length*2);
        dBA = (int) (10 * Math.log10(totalEnergy*2500000000.0));
        return dBA;
    }

//    double[] weightingFilter(double[] fsBand) {
//        double[] A = new double[fsBand.length];
//        double c1 = 148693636;
//        double c2 = 424.318677406009;
//        double c3 = 11589.0930520225;
//        double c4 = 544440.6704605729;
//        double c5 = 148693636;
//
//        for (int i = 0; i < fsBand.length; i++) {
//            if (fsBand[i] == 0) fsBand[i] = Math.pow(10, -17);
//            fsBand[i] = Math.pow(fsBand[i], 2);
//            A[i] = (c1 + Math.pow(fsBand[i], 2)) / ((fsBand[i] + c2) * Math.sqrt((fsBand[i] + c3) * (fsBand[i] + c4)) * (fsBand[i] + c5));
//        }
//        return A;
//    }

    String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("Aweighting.json");

            int size = is.available();

            byte[] buffer = new byte[size];
            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;

    }

}