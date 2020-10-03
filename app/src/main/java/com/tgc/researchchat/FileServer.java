package com.tgc.researchchat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.tgc.researchchat.adapters.MusicAdapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;
import static android.media.AudioTrack.getMinBufferSize;

public class FileServer extends Thread {

    private Context context;
    private String serverIpAddress;
    private String TAG = "FILE SERVER";
    private RecyclerView messageList;
    private ArrayList<Message> messageArray;
    private ChatAdapterRecycler mAdapter;
    ChatClient.MyFilesInteface myFilesInteface;
    private int port;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

    static final int sampleFreq = 8000;
    static final int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_8BIT;
    static final int streamType = STREAM_MUSIC;
    static final int audioMode = MODE_STREAM;
//    static final int bufferSize = getMinBufferSize(sampleFreq, channelConfig, audioMode);

    FileServer(Context context, ChatAdapterRecycler mAdapter, RecyclerView messageList, ArrayList<Message> messageArray, int port, String serverIpAddress, RecyclerView mSongRecycler, MusicAdapter musicAdapter, ChatClient.MyFilesInteface myFilesInteface) {
        this.messageArray = messageArray;
        this.messageList = messageList;
        this.mAdapter = mAdapter;
        this.port = port;
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        this.myFilesInteface = myFilesInteface;
    }

    public void run() {
        try {
            ServerSocket fileSocket = new ServerSocket(port + 1);
            Log.d(TAG, "run: " + fileSocket.getLocalPort());
            fileSocket.setReuseAddress(true);
            System.out.println(TAG + "started");
            while (!Thread.interrupted()) {
                Socket connectFileSocket = fileSocket.accept();
                Log.d(TAG, "run: File Opened");
                ReceiveFiles handleFile = new ReceiveFiles();

//                ((ChatClient)context).progressBar.setVisibility(View.VISIBLE);
                handleFile.execute(connectFileSocket);
            }
            fileSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class ReceiveFiles extends AsyncTask<Socket, Void, String> {
        String text;
        File outputFile;


        @Override
        protected String doInBackground(Socket... sockets) {
            try {
                File testDirectory = Environment.getExternalStorageDirectory();
                boolean aa=false,ab = false;
                if (!testDirectory.exists()){
                    aa=testDirectory.mkdirs();
                }
                if (!testDirectory.exists()){
                    ab =testDirectory.mkdir();
                }
                if (!aa||!ab){
                    Log.e(TAG, "doInBackground: mkdis,mkdir"+aa+ab );
                }

                try {
                    InputStream inputStream = sockets[0].getInputStream();
                    DataInputStream dataInputStream = new DataInputStream(inputStream);

                    String fileName = dataInputStream.readUTF();
                    outputFile = new File(testDirectory+"/Download/", fileName);
                    text = fileName;
                    //TODO start of stream
                    byte [] audioBuffer = new byte[4096];
                    //creates input stream readers to read incoming data
                    BufferedInputStream myBis = new BufferedInputStream(sockets[0].getInputStream());
                    DataInputStream myDis = new DataInputStream(myBis);
                    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
                    int BytesPerElement = 2; // 2 bytes in 16bit format
                    Log.e(TAG, "Input created, listener");
                    AudioTrack myAudioTrack = new AudioTrack(streamType, RECORDER_SAMPLERATE, channelConfig, RECORDER_AUDIO_ENCODING, BufferElements2Rec*BytesPerElement, audioMode);
//                    AudioTrack myAudioTrack = new AudioTrack(streamType, sampleFreq, channelConfig, audioEncoding, bufferSize, audioMode);
                    //Log.d(debugStr, String.valueOf(mySocket.getInputStream().read(audioBuffer)));

                    Log.e(TAG, "track made");
                    // Read the file into the music array.
                    int i = 0;
                    //TODO unsure of while loop condition
                    while (sockets[0].getInputStream().read(audioBuffer) != -1) {
                        Log.e(TAG, "doInBackground: looping");
                        audioBuffer[audioBuffer.length-1-i] = myDis.readByte();
                        myAudioTrack.play();
                        myAudioTrack.write(audioBuffer, 0, audioBuffer.length);
                        i++;
                    }

                    /*OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                    long fileSize = dataInputStream.readLong();
                    int bytesRead;
                    byte[] byteArray = new byte[8192 * 16];

                    while (fileSize > 0 && (bytesRead = dataInputStream.read(byteArray, 0, (int) Math.min(byteArray.length, fileSize))) != -1) {
                        outputStream.write(byteArray, 0, bytesRead);
                        fileSize -= bytesRead;
                    }*/
                    inputStream.close();
                    dataInputStream.close();
//                    outputStream.flush();
//                    outputStream.close();

                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: "+e.getMessage() );
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute: Result" + result);
//                ((ChatClient)context).progressBar.setVisibility(View.GONE);
            if (result != null) {
                messageArray.add(new Message("New File Received: " + result, 1, Calendar.getInstance().getTime()));
                messageList.setAdapter(mAdapter);
                if (outputFile!=null){
//                    myFilesInteface.gotFiles(outputFile);
                }
            }
        }
    }


}
