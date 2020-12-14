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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    JSONObject jsonObject;
    StorageReference mStorageRef;
    ProgressBar progressBar;
    private static String serverTimestamp;


    // 22050 -> 0.5s  4410 -> 0.1s
    int bufferSize = 4410;
    int sampleRate = 44100;
    ProcessState processState = ProcessState.initial;
    private static final int initialNum = 20;
    private static int initialCount = 0;

    // change sample time  6000=10min  18000=30min  600=1min
    private static final int runNum = 60;
    private static int runCount = 0;
    private static final int uploadNum = 100;
    private static int uploadCount = 0;

    private static final int stopNum = 1;
    private static final String soundDataName = "SoundData";
    private static final String userIDName = "UserID.json";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

//        try {
//            InputStream inputStream = MainActivity.this.openFileInput(userIDName);
//
//            if (inputStream != null) {
////                Toast.makeText(MainActivity.this, "Ok", Toast.LENGTH_SHORT).show();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//                String receiveString;
//                StringBuilder stringBuilder = new StringBuilder();
//
//                while ((receiveString = bufferedReader.readLine()) != null) {
//                    stringBuilder.append(receiveString);
//                }
//
//                inputStream.close();
//                JSONObject userObj = new JSONObject(stringBuilder.toString());
//                userID = userObj.getString("ID");
//
//                Toast.makeText(MainActivity.this, "UID: " + userID, Toast.LENGTH_SHORT).show();
//
//
//            }
//        } catch (FileNotFoundException e) {
//            Log.e("login activity", "File not found: " + e.toString());
//            db.collection("Ver.1").document("Users").get()
//                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                        @Override
//                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                            if (task.isSuccessful()) {
//                                userID = Integer.toString(Integer.parseInt(task.getResult().get("ID").toString()) + 1);
//
//                                Toast.makeText(MainActivity.this, "First UID: " + userID, Toast.LENGTH_SHORT).show();
//
//                                Log.d("", userID);
//                                // Update an existing document
//                                DocumentReference docRef = db.collection("Ver.1").document("Users");
//
//                                // (async) Update one field
//                                docRef.update("ID", userID);
//
//                                // Convert JsonObject to String Format
//                                JSONObject userObj = new JSONObject();
//                                try {
//                                    userObj.put("ID", userID);
//                                } catch (JSONException ex) {
//                                    ex.printStackTrace();
//                                }
//                                String userString = userObj.toString();
//                                // Define the File Path and its Name
//                                File writeFile = new File(MainActivity.this.getFilesDir(), userIDName);
//                                FileWriter fileWriter;
//                                BufferedWriter bufferedWriter;
//                                try {
//                                    fileWriter = new FileWriter(writeFile);
//                                    bufferedWriter = new BufferedWriter(fileWriter);
//                                    try {
//                                        bufferedWriter.write(userString);
//                                        bufferedWriter.close();
//
//                                    } catch (IOException ex) {
//                                        ex.printStackTrace();
//                                    }
//                                } catch (IOException ex) {
//                                    ex.printStackTrace();
//                                }
//                            } else {
//                                Log.w("", "Error getting documents.", task.getException());
//                            }
//                        }
//                    });
//        } catch (IOException e) {
//            Log.e("login activity", "Can not read file: " + e.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                JSONArray array = new JSONObject(loadJSONFromAsset(this)).getJSONArray("A");
//                for (int i = 0; i < array.length(); i++) {
//                    A.add(Double.parseDouble(array.getString(i)));
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } finally {
//                //        Toast.makeText(this, Double.toString(A.get(0)), Toast.LENGTH_SHORT).show();
//                startButton.setEnabled(true);
//                addListenerOnStartButton();
//            }
//        }

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


//        HttpURLConnection_Post_Task task = new HttpURLConnection_Post_Task();
//        task.execute("http://35.201.170.127/update/raw_data");


    }

    void addListenerOnStartButton() {
        final Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(
                new View.OnClickListener() {
                    @SuppressLint({"SetTextI18n", "HardwareIds", "CommitPrefEdits"})
                    @Override
                    public void onClick(View view) {
                        if (startButton.getText().toString().equals("START！")) {
                            startButton.setEnabled(false);
                            startButton.setText("Running");
                            jsonObject = new JSONObject();
                            lineChart.setData(new LineData());
                            serverTimestamp = Long.toString(System.currentTimeMillis());

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
//
//
////                            try {
////                                @SuppressLint("SdCardPath") FileOutputStream output = new FileOutputStream("/sdcard/output.json");
////                                writeJsonStream(output, soundSignal.get("1"));
////                            } catch (FileNotFoundException e) {
////                                e.printStackTrace();
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }

                            final Map<String, Object> data = new HashMap<>();

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

                            // write soundData to json, upload json file
                            writeSoundDataToJson(null);

//                            db.collection("Ver.1")
//                                    .document("Manufacturers")
//                                    .collection(Build.MANUFACTURER)
//                                    .document(Build.MODEL)
//                                    .collection(userID)
//                                    .document(serverTimestamp)
//                                    .set(data, SetOptions.merge())
//                                    .addOnCompleteListener(
//                                            new OnCompleteListener<Void>() {
//                                                @Override
//                                                public void onComplete(@NonNull Task<Void> task) {
//                                                    if (task.isSuccessful()) {
//                                                        Toast.makeText(MainActivity.this, "Add!", Toast.LENGTH_SHORT).show();
//
//                                                        startButton.setText("START！");
//                                                        startButton.setEnabled(true);
//
//                                                        Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//                                                        MediaPlayer mediaPlayer = new MediaPlayer();
//
//                                                        try {
//                                                            mediaPlayer.setDataSource(MainActivity.this, defaultRingtoneUri);
//                                                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
//                                                            mediaPlayer.prepare();
//                                                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//
//                                                                @Override
//                                                                public void onCompletion(MediaPlayer mp) {
//                                                                    mp.release();
//                                                                }
//                                                            });
//                                                            mediaPlayer.start();
//                                                        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    } else if (task.isCanceled()) {
//                                                        Toast.makeText(MainActivity.this, "Cancel!", Toast.LENGTH_SHORT).show();
//                                                    }
//                                                }
//                                            }
//                                    );

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
        final List<Double> dBSPLlist = new ArrayList<>();
        final List<Double> dBAlist = new ArrayList<>();
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
                final double dBA = estimateLevel(amplitudes, SPL.dBA);
                final double dBSPL = estimateLevel(amplitudes, SPL.dBSPL);

                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        addEntry((int) dBA, 1);
                        TextView text = findViewById(R.id.dBAView);
                        text.setText("" + (int) dBA + "dBA");

                        addEntry((int) dBSPL, 0);
//                        TextView text1 = findViewById(R.id.dBSPLView);
//                        text1.setText("" + (int) dBSPL + "dB SPL");
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
                            dBSPLlist.add(dBSPL);
                            dBAlist.add(dBA);
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
        if (lineChart.getData().getEntryCount() > 400) {
            lineChart.setData(new LineData());
        }
        LineData data = lineChart.getData();
        ILineDataSet set = data.getDataSetByIndex(0);
        ILineDataSet set1 = data.getDataSetByIndex(1);
        // set.addEntry(...); // can be called as well
        if (set == null) {
            set = createLineDataSet(0);
            data.addDataSet(set);
        }
        if (num == 0) {
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
        }
        if (num == 1) {
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
    double estimateLevel(float[] amplitudes, SPL type) {
        double dB;
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

//        for(float v : amplitudes){
//            System.out.print(v);
//            System.out.print(" ");
//
//        }
//        System.out.println();

//        Log.e("1000Hz", String.valueOf(amplitudes));
//        Log.e("1000Hz", String.valueOf(amplitudes[100]));
//        Log.e("1000Hz", String.valueOf(amplitudes[101]));


//        totalEnergy = totalEnergy/((1/(double)44100)*amplitudes.length*2);
        dB = (10 * Math.log10(totalEnergy * 2500000000.0));
        return dB;
    }

    String loadJSONFromAsset(Context context) {
        String json;
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

    void writeSoundDataToJson(JSONObject tJsonObject) {
        // Convert JsonObject to String Format
        String userString = (tJsonObject != null) ? tJsonObject.toString() : jsonObject.toString();
//        File writeFile = new File(MainActivity.this.getFilesDir(), soundDataName + fileNum + ".json");
//        FileWriter fileWriter;
//        BufferedWriter bufferedWriter;
//        try {
//            fileWriter = new FileWriter(writeFile);
//            bufferedWriter = new BufferedWriter(fileWriter);
//            try {
//                bufferedWriter.write(userString);
//                bufferedWriter.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        File rootFolder = MainActivity.this.getExternalFilesDir(null);
        File jsonFile = new File(rootFolder, soundDataName + ".json");
        try {
            Toast.makeText(MainActivity.this, jsonFile.toString(), Toast.LENGTH_LONG).show();
            System.out.println(jsonFile);
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(userString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //or IOUtils.closeQuietly(writer);
//        uploadFileToFirebaseStorage();
    }

    void uploadFileToFirebaseStorage() {

        Uri readFile = Uri.fromFile(new File(MainActivity.this.getExternalFilesDir(null), soundDataName + ".json"));

        final String storageServerTimestamp = Long.toString(System.currentTimeMillis());

        final StorageReference soundDataRef = mStorageRef.child("SoundData/" + userID + "/" + storageServerTimestamp + ".json");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setMax(uploadNum);
                progressBar.setProgress(uploadCount);
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.CYAN));
            }
        });

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

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setMax(initialNum);
                                progressBar.setProgress(initialCount);
                                progressBar.setProgressTintList(ColorStateList.valueOf(Color.BLUE));
                            }
                        });

                        uploadCount = 0;

                        // Get a URL to the uploaded content
                        soundDataRef.getDownloadUrl().addOnSuccessListener(
                                new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Toast.makeText(getBaseContext(), "Upload success!", Toast.LENGTH_SHORT).show();

                                        Map<String, Object> jsonURL = new HashMap<>();
                                        jsonURL.put(soundDataName, uri.toString());
                                        db.collection("Ver.1")
                                                .document("Manufacturers")
                                                .collection(Build.MANUFACTURER)
                                                .document(Build.MODEL)
                                                .collection(userID)
                                                .document(serverTimestamp)
                                                .set(jsonURL, SetOptions.merge())
                                                .addOnCompleteListener(
                                                        new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Toast.makeText(MainActivity.this, "Add!", Toast.LENGTH_SHORT).show();
                                                                } else if (task.isCanceled()) {
                                                                    Toast.makeText(MainActivity.this, "Cancel!", Toast.LENGTH_SHORT).show();
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
                        Log.e("Upload Fail!", "");
                    }
                });
    }

    public JSONObject shallowCopy(JSONObject original) {
        JSONObject copy = new JSONObject();
        for (Iterator<String> iterator = original.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            JSONArray value = original.optJSONArray(key);
            try {
                copy.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return copy;
    }

    public static class HttpURLConnection_Post_Task extends AsyncTask<String, Void, String> {
        private final static String TAG = "HTTPURLCONNECTION test";
        private String parameter1 = "2020-07-31";
        private String parameter2 = "Redmi";
        private String parameter3 = "Xiaomi";
        private double[] parameter4 = {66.43916939577134, 67.03164334849625};
        private String parameter5 = "mid1qqq";




        @Override
        protected String doInBackground(String... urls) {
            return POST(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG,"onPostExecute");
        }
        private String POST(String APIUrl) {
            StringBuilder result = new StringBuilder();
            HttpURLConnection connection;
            try {
                URL url = new URL(APIUrl.replaceAll(" ", "%20"));
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Content-Type", "application/json");
//                connection.setRequestProperty("authentication", MainActivity.this);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

//                Pattern p = Pattern.compile("[\\[\\]]");
//                Matcher m = p.matcher(Arrays.toString(parameter4));
//                System.out.println(m.replaceAll(""));
                StringBuilder stringBuilder =
                        new StringBuilder("date=" + URLEncoder.encode(parameter1, "UTF-8") + "&" +
                                "model=" + URLEncoder.encode(parameter2, "UTF-8") + "&" +
                                "brand=" + URLEncoder.encode(parameter3, "UTF-8") + "&" +
                                "stamp=" + URLEncoder.encode(parameter5, "UTF-8") + "&");

                for(double p : parameter4){
                    stringBuilder.append("dBA=").append(URLEncoder.encode(String.valueOf(p), "UTF-8")).append("&");
                }

                System.out.println(stringBuilder);

                outputStream.writeBytes(stringBuilder.toString());
                outputStream.flush();
                outputStream.close();


                InputStream inputStream = connection.getInputStream();
                int status = connection.getResponseCode();
                Log.d(TAG, String.valueOf(status));
                if(inputStream != null){
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader in = new BufferedReader(reader);

                    String line="";
                    while ((line = in.readLine()) != null) {
                        result.append(line).append("\n");
                    }
                } else{
                    result = new StringBuilder("Did not work!");
                }
                return result.toString();
            } catch (Exception e) {
//                Log.d("ATask InputStream", e.getLocalizedMessage());
                e.printStackTrace();
                return result.toString();
            }
        }
    }

}