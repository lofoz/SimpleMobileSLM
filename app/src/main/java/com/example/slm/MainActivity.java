package com.example.slm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {
    public enum SPL {
        dBA, dBSPL
    }

    public enum ProcessState {
        initial, run, stop
    }

    String userID = "";
    LineChart lineChart;
    AudioDispatcher dispatcher;
    FirebaseFirestore db;
    List<Double> A;
    //    Map<String, ArrayList<Float>> soundSignal = new HashMap<>();
    JSONObject jsonObject;
    StorageReference mStorageRef;
    ProgressBar progressBar;

    int bufferSize = 22050;
    int sampleRate = 44100;
    ProcessState processState = ProcessState.initial;
    private static final int initialNum = 4;
    private static int initialCount = 0;
    private static final int runNum = 20;
    private static int runCount = 0;
    private static final int uploadNum = 100;
    private static int uploadCount = 0;
    private static final int stopNum = 1;
    private static final String soundDataName = "SoundData.json";
    private static final String userIDName = "UserID.json";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        db = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        A = new ArrayList<>();


        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(initialNum);
        progressBar.setProgress(initialCount);
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
        lineChart = findViewById(R.id.chart);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.getLegend().setForm(Legend.LegendForm.LINE);
        lineChart.invalidate();

        final Button startButton = findViewById(R.id.startButton);

        startButton.setEnabled(false);

        try {
            InputStream inputStream = MainActivity.this.openFileInput(userIDName);

            if (inputStream != null) {
//                Toast.makeText(MainActivity.this, "Ok", Toast.LENGTH_SHORT).show();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                JSONObject userObj = new JSONObject(stringBuilder.toString());
                userID = userObj.getString("ID");

                Toast.makeText(MainActivity.this, "UID: " + userID, Toast.LENGTH_SHORT).show();


            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            db.collection("Ver.1").document("Users").get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                userID = Integer.toString(Integer.parseInt(task.getResult().get("ID").toString()) + 1);

                                Toast.makeText(MainActivity.this, "First UID: " + userID, Toast.LENGTH_SHORT).show();

                                Log.d("", userID);
                                // Update an existing document
                                DocumentReference docRef = db.collection("Ver.1").document("Users");

                                // (async) Update one field
                                docRef.update("ID", userID);

                                // Convert JsonObject to String Format
                                JSONObject userObj = new JSONObject();
                                try {
                                    userObj.put("ID", userID);
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                String userString = userObj.toString();
                                // Define the File Path and its Name
                                File writeFile = new File(MainActivity.this.getFilesDir(), userIDName);
                                FileWriter fileWriter;
                                BufferedWriter bufferedWriter;
                                try {
                                    fileWriter = new FileWriter(writeFile);
                                    bufferedWriter = new BufferedWriter(fileWriter);
                                    try {
                                        bufferedWriter.write(userString);
                                        bufferedWriter.close();

                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                Log.w("", "Error getting documents.", task.getException());
                            }
                        }
                    });
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                JSONArray array = new JSONObject(loadJSONFromAsset(this)).getJSONArray("A");
                for (int i = 0; i < array.length(); i++) {
                    A.add(Double.parseDouble(array.getString(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                //        Toast.makeText(this, Double.toString(A.get(0)), Toast.LENGTH_SHORT).show();
                startButton.setEnabled(true);
                addListenerOnStartButton();
            }
        }
    }

    void addListenerOnStartButton() {
        final Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(
                new View.OnClickListener() {
                    @SuppressLint({"SetTextI18n", "HardwareIds", "CommitPrefEdits"})
                    @Override
                    public void onClick(View view) {
                        if (startButton.getText().toString().equals("Start")) {
                            startButton.setEnabled(false);
                            startButton.setText("Running");
                            jsonObject = new JSONObject();
                            lineChart.setData(new LineData());
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
                        } else if (startButton.getText().toString().equals("Running")) {
                            dispatcher.stop();
                            //                            Toast.makeText(MainActivity.this, "Ok", Toast.LENGTH_SHORT).show();


//                            try {
//                                @SuppressLint("SdCardPath") FileOutputStream output = new FileOutputStream("/sdcard/output.json");
//                                writeJsonStream(output, soundSignal.get("1"));
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }

                            // Convert JsonObject to String Format
                            String userString = jsonObject.toString();
                            // Define the File Path and its Name
                            File writeFile = new File(MainActivity.this.getFilesDir(), soundDataName);
                            FileWriter fileWriter;
                            BufferedWriter bufferedWriter;
                            try {
                                fileWriter = new FileWriter(writeFile);
                                bufferedWriter = new BufferedWriter(fileWriter);
                                try {
                                    bufferedWriter.write(userString);
                                    bufferedWriter.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Uri readFile = Uri.fromFile(new File(MainActivity.this.getFilesDir(), soundDataName));

                            final String serverTimestamp = Long.toString(System.currentTimeMillis());

                            final StorageReference soundDataRef = mStorageRef.child("SoundData/" + userID + "/" + serverTimestamp + ".json");

                            soundDataRef.putFile(readFile)
                                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                                            uploadCount = (int) ((double) taskSnapshot.getBytesTransferred() / (double) taskSnapshot.getTotalByteCount() * 100.0);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressBar.setProgress(uploadCount);
                                                }
                                            });
                                        }
                                    })
                                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                            uploadCount = 0;
                                            // Get a URL to the uploaded content
                                            soundDataRef.getDownloadUrl().addOnSuccessListener(
                                                    new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            final Map<String, Object> data = new HashMap<>();

                                                            Toast.makeText(getBaseContext(), "Upload success! URL - " + uri.toString(), Toast.LENGTH_SHORT).show();

                                                            data.put("SoundData", uri.toString());
                                                            data.put("Brand", Build.BRAND);
                                                            data.put("Product", Build.PRODUCT);
                                                            data.put("Board", Build.BOARD);
                                                            data.put("Device", Build.DEVICE);
                                                            data.put("Display", Build.DISPLAY);
                                                            data.put("FingerPrint", Build.FINGERPRINT);
                                                            data.put("Hareware", Build.HARDWARE);
                                                            data.put("Host", Build.HOST);
                                                            data.put("ID", Build.ID);
                                                            data.put("Model", Build.MODEL);
                                                            data.put("Tags", Build.TAGS);
                                                            data.put("CPUABI", Build.CPU_ABI);
                                                            data.put("CPUABI2", Build.CPU_ABI2);
                                                            data.put("SDK_INT", Build.VERSION.SDK_INT);

//                    Toast.makeText(MainActivity.this, "Here!", Toast.LENGTH_SHORT).show();

                                                            db.collection("Ver.1")
                                                                    .document("Manufacturers")
                                                                    .collection(Build.MANUFACTURER)
                                                                    .document(Build.MODEL)
                                                                    .collection(userID)
                                                                    .document(serverTimestamp)
                                                                    .set(data)
                                                                    .addOnCompleteListener(
                                                                            new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        Toast.makeText(MainActivity.this, "add!", Toast.LENGTH_SHORT).show();

                                                                                        startButton.setText("Start");
                                                                                        startButton.setEnabled(true);

                                                                                        runOnUiThread(new Runnable() {
                                                                                            @Override
                                                                                            public void run() {
                                                                                                progressBar.setMax(initialNum);
                                                                                                progressBar.setProgress(initialCount);
                                                                                                progressBar.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
                                                                                            }
                                                                                        });

                                                                                        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                                                                                        MediaPlayer mediaPlayer = new MediaPlayer();

                                                                                        try {
                                                                                            mediaPlayer.setDataSource(MainActivity.this, defaultRingtoneUri);
                                                                                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                                                                                            mediaPlayer.prepare();
                                                                                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                                                                                                @Override
                                                                                                public void onCompletion(MediaPlayer mp)
                                                                                                {
                                                                                                    mp.release();
                                                                                                }
                                                                                            });
                                                                                            mediaPlayer.start();
                                                                                        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
                                                                                            e.printStackTrace();
                                                                                        }
                                                                                    } else if (task.isCanceled()) {
                                                                                        Toast.makeText(MainActivity.this, "cancel!", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            }
                                                                    );
                                                        }
                                                    }
                                            );
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle unsuccessful uploads
                                            // ...
                                        }
                                    });
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
                Toast.makeText(this, "已經拿到MICROPHONE權限囉!", Toast.LENGTH_SHORT).show();
                soundDetection();
            }
            //假如拒絕了
            else {
                //do something
                Toast.makeText(this, "MICROPHONE權限FAIL", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    void soundDetection() {
        final List<Integer> dBSPLlist = new ArrayList<>();
        final List<Integer> dBAlist = new ArrayList<>();
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, 0);

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
                final int dBA = estimateLevel(amplitudes, SPL.dBA);
                final int dBSPL = estimateLevel(amplitudes, SPL.dBSPL);

                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        addEntry((int) dBA, 1);
                        TextView text = findViewById(R.id.dBAView);
                        text.setText("" + dBA + "dBA");

                        addEntry((int) dBSPL, 0);
                        TextView text1 = findViewById(R.id.dBSPLView);
                        text1.setText("" + dBSPL + "dB SPL");
                    }
                });

                switch (processState) {
                    case initial:
                        if (initialCount < initialNum) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(initialCount++ + 1);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setMax(runNum);
                                    progressBar.setProgress(runCount);
                                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                                }
                            });
                            processState = ProcessState.run;
                            initialCount = 0;
                        }
                        break;
                    case run:
                        if (runCount < runNum) {
                            JSONArray result = new JSONArray();
                            for (float v : audioFloatBuffer) {
                                try {
                                    result.put(v);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                dBSPLlist.add(dBSPL);
                                dBAlist.add(dBA);
                                jsonObject.put(Integer.toString(runCount), result);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(runCount++ + 1);
                                }
                            });
                        } else {
                            try {
                                jsonObject.put("dBSPL", new JSONArray(dBSPLlist));
                                jsonObject.put("dBA", new JSONArray(dBAlist));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setMax(stopNum);
                                    progressBar.setProgress(stopNum);
                                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                                }
                            });
                            processState = ProcessState.stop;
                            runCount = 0;
                        }
                        break;
                    case stop:
                        processState = ProcessState.initial;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Button button = findViewById(R.id.startButton);
                                progressBar.setMax(uploadNum);
                                progressBar.setProgress(uploadCount);
                                progressBar.setProgressTintList(ColorStateList.valueOf(Color.CYAN));
                                button.performClick();
                            }
                        });
                        break;
                }
                return true;
            }

            @Override
            public void processingFinished() {

            }
        };

        dispatcher.addAudioProcessor(fftProcessor);
        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    void addEntry(double newData, int num) {
        LineData data = lineChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);
        ILineDataSet set1 = data.getDataSetByIndex(1);
        // set.addEntry(...); // can be called as well
        if (set == null) {
            set = createLineDataSet(0);
            data.addDataSet(set);
        } else if (num == 0) {
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
        } else if (num == 1) {
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
        LineDataSet set = new LineDataSet(null, num == 0 ? "SPL" : "dBA");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        // 折線的顏色
        set.setColor(num == 0 ? Color.RED : Color.BLUE);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }

    //amplitudes.length = bufferSize/2  -> HzPerBin = sampleRate/(2*amplitudes.length)
    int estimateLevel(float[] amplitudes, SPL type) {
        int dB;
        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] == 0) amplitudes[i] = (float) Math.pow(10, -17);
        }
        float totalEnergy = 0;
        for (int i = 10; i < amplitudes.length; i++) {
//            double sum = 0;
//            for (double v : A) {
//                sum += Math.pow(amplitudes[i] * v, 2);
//            }
            switch (type) {
                case dBA:
                    totalEnergy += Math.pow(amplitudes[i] / amplitudes.length * A.get(i), 2);
                    break;
                case dBSPL:
                    totalEnergy += Math.pow(amplitudes[i] / amplitudes.length, 2);
                    break;
            }

//            totalEnergy += (sum / amplitudes.length * 2);
        }

//        totalEnergy = totalEnergy/((1/(double)44100)*amplitudes.length*2);
        dB = (int) (10 * Math.log10(totalEnergy * 2500000000.0));
        return dB;
    }

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