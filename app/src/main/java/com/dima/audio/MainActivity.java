package com.dima.audio;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{

    Button start, play;
    TextView textV;
    StartTime STARTTIME;

    Boolean recording;
    private static final String TAG = "myLogs";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textV = (TextView) findViewById(R.id.textV);
        start = (Button)findViewById(R.id.start);
        play = (Button)findViewById(R.id.play);

        start.setOnClickListener(this);
        play.setOnClickListener(this);

    }


    private void startRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), "record.pcm");

        try {
            file.createNewFile();

            OutputStream outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            int minBufferSize = AudioRecord.getMinBufferSize(11025,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            short[] audioData = new short[minBufferSize];

            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    11025,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);

            audioRecord.startRecording();

            while(recording){
                int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
                for(int i = 0; i < numberOfShort; i++){
                    dataOutputStream.writeShort(audioData[i]);
                }
            }

            audioRecord.stop();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void playRecord(){

        File file = new File(Environment.getExternalStorageDirectory(), "record.pcm");

        int shortSizeInBytes = Short.SIZE/Byte.SIZE;

        int bufferSizeInBytes = (int)(file.length()/shortSizeInBytes);
        short[] audioData = new short[bufferSizeInBytes];

        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            int i = 0;
            while(dataInputStream.available() > 0){audioData[i] = dataInputStream.readShort(); i++;}

            //переворачиваем запись для воспроизведения с конца )
            short[]backAudio = new short[audioData.length];
            for (int j = 0; j<audioData.length;j++){backAudio[j]=audioData[j];}
            for (int k = 0; k<audioData.length;k++){audioData[k]=backAudio[backAudio.length-1-k];}

            dataInputStream.close();

            AudioTrack audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    11025,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            audioTrack.play();
            audioTrack.write(audioData, 0, bufferSizeInBytes);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start:
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB_MR1) {STARTTIME = new StartTime();STARTTIME.execute();}
                else {STARTTIME = new StartTime();	STARTTIME.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);}
                break;
            case R.id.play:
                playRecord();
                break;
        }
    }

    public class StartTime extends AsyncTask<Void, Integer, Void> {
        int startTime=10;
        @Override
        protected Void doInBackground(Void... params) {

            while (startTime > 0) {
                startTime = startTime - 1;
                publishProgress(startTime);
                SystemClock.sleep(1000);
                isCancelled();
                if (isCancelled()) {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
           textV.setText(String.valueOf(startTime));

        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            textV.setText("Audio Record");
            recording = false;
            startTime = 10;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            recording = true;
            Thread recordThread = new Thread(new Runnable(){

                @Override
                public void run() {
                    recording = true;
                    startRecord();
                }

            });
            recordThread.start();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TAG, "Cancel");
            recording = false;
            startTime = 10;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        try{STARTTIME.onCancelled();}catch (Exception e){e.printStackTrace();}

    }
}