package com.tgc.researchchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.tgc.researchchat.adapters.MusicAdapter;
import com.tgc.researchchat.models.MySongs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.ContentValues.TAG;
import static com.tgc.researchchat.FileServer.channelConfig;

public class ChatClient extends AppCompatActivity implements PickiTCallbacks {
    String TAG = "CLIENT ACTIVITY";

    EditText smessage;
    ImageButton sent;
    String serverIpAddress = "";
    public static int myport=8973;
    public static int myport_2=8922;
    public static int myport_3=6281;
    int sendPort;
    ArrayList<Message> messageArray;
    ImageButton fileUp;
    TextView textView;
    ChatServer s;
    FileServer f;
    String ownIp;
    Toolbar toolbar;
    ProgressBar progressBar;
    PickiT pickiT;
    private Boolean exit = false;
    private RecyclerView mMessageRecycler;
    private RecyclerView mSongRecycler;
    private ChatAdapterRecycler mMessageAdapter;
    public MusicAdapter musicAdapter;
    private int REQUEST_CODE = 200;
    View showMe;
    View getMedia;
    ImageView myQrCode;
    ImageView close;
    public ArrayList<MySongs> allSongs = new ArrayList<>();
    private ImageView currentlyPlaying;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    public MyFilesInteface myFilesInteface = new MyFilesInteface() {
        @Override
        public void gotFiles(File file) {
            Log.e(TAG, "gotFiles: "+file.getName() );
            if (file.getName().contains(".mp3")) {
                String filename = file.getName();
                filename = filename.trim();
                String path = Environment.getExternalStorageDirectory() + "/Download/";
                System.out.println(TAG + "path and filename => " + path + filename);
                Uri uri = Uri.parse(path + filename);
                if (mediaPlayer!=null&&mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    if (currentlyPlaying!=null){
                        currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));

                    }
                } else {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer = MediaPlayer.create(ChatClient.this, uri);
//                    try {
////                        mediaPlayer.prepareAsync();
////                        mediaPlayer.setDataSource(path+File.separator +filename);
////                        mediaPlayer.setDataSource(file.de);
////                        mediaPlayer.prepare();
//                    } catch (IOException e) {
//                        Log.e(TAG, "gotFiles: IOException", e);
//                        e.printStackTrace();
//                    }
                    //Log.d(TAG, "onClick: " + context.getObbDir() + "/downloadFolder/" + path);
                    if (mediaPlayer!=null){
                        mediaPlayer.start();

                    }
                    if (currentlyPlaying!=null){
                        currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                    }
                }
            }else {
                Log.e(TAG, "gotFiles: not an mp3" );
            }
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    if (currentlyPlaying!=null){
                        currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    }

                });
            }else {
                Log.e(TAG, "gotFiles: cant set oncomplete listener" );
            }
        }
    };

    interface MyFilesInteface {
        void gotFiles(File file);
    }
    public MyFileListInteface myFileListInteface=new MyFileListInteface() {
        @Override
        public void getSongs(ArrayList<MySongs> songs) {
            Log.e(TAG, "getSongs: "+songs.size() );
            allSongs.clear();
            allSongs.addAll(songs);
            musicAdapter = new MusicAdapter(allSongs, ChatClient.this);
            mSongRecycler.setLayoutManager(new LinearLayoutManager(ChatClient.this));
            mSongRecycler.setAdapter(musicAdapter);

        }
    };
    interface MyFileListInteface {
        void getSongs(ArrayList<MySongs> songs);
    }


    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);

        showMe = findViewById(R.id.showMe);
        getMedia = findViewById(R.id.getMedia);
        myQrCode = findViewById(R.id.myQrCode);

        generateMyQR();

        close = findViewById(R.id.close);
        pickiT = new PickiT(this, this);
        smessage = findViewById(R.id.edittext_chatbox);
        toolbar = findViewById(R.id.toolbar);
        sent = findViewById(R.id.button_chatbox_send);
        fileUp = findViewById(R.id.file_send);
        textView = findViewById(R.id.textView);
        progressBar = (ProgressBar) findViewById(R.id.pbHeaderProgress);
        setSupportActionBar(toolbar);

        messageArray = new ArrayList<>();
        mMessageRecycler = findViewById(R.id.message_list);
        mSongRecycler = findViewById(R.id.audioList);
        mMessageAdapter = new ChatAdapterRecycler(this, messageArray);
        musicAdapter = new MusicAdapter(allSongs, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        layoutManager.setSmoothScrollbarEnabled(true);

        mMessageRecycler.setLayoutManager(layoutManager);
        mSongRecycler.setLayoutManager(new LinearLayoutManager(this));
        mSongRecycler.setAdapter(musicAdapter);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String info = bundle.getString("ip&port");
            assert info != null;
            String[] infos = info.split(" ");
            serverIpAddress = infos[0];
            sendPort = Integer.parseInt(infos[1]);
            myport = Integer.parseInt(infos[2]);
        }
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ownIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        getSupportActionBar().setTitle("Connection to " + serverIpAddress);

        if (!serverIpAddress.equals("")) {
            s = new ChatServer(ownIp, this, getApplicationContext(), mMessageAdapter, mMessageRecycler, messageArray, myport, serverIpAddress, mSongRecycler, musicAdapter,allSongs,myFileListInteface);
            s.start();
            f = new FileServer(ChatClient.this, mMessageAdapter, mMessageRecycler, messageArray, myport, serverIpAddress, mSongRecycler, musicAdapter, myFilesInteface);
            f.start();
        }

        if (permissionAlreadyGranted()) {
            Toast.makeText(ChatClient.this, "Permission is already granted!", Toast.LENGTH_SHORT).show();

        }

        requestPermission();
        sent.setOnClickListener(v -> {
            if (!smessage.getText().toString().isEmpty()) {
                SendDataToRemote sendDataToRemote = new SendDataToRemote("1:" + smessage.getText().toString());
                sendDataToRemote.execute();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please write something", Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        getMedia.setOnClickListener(v -> {
            getMedia.setEnabled(false);
            SendDataToRemote sendDataToRemote = new SendDataToRemote("3:get media");
            sendDataToRemote.execute();
        });

        fileUp.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select file"), 1);
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMe.setVisibility(View.GONE);
            }
        });


    }

    public void sendMessage(String message) {
        Log.e(TAG, "sendMessage: "+message );
        if (message != null && !message.contentEquals("")) {
            SendDataToRemote sendDataToRemote = new SendDataToRemote( message);
            sendDataToRemote.execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            final Context context = ChatClient.this;
            ColorPickerDialogBuilder
                    .with(context)
                    .setTitle("Choose color")
                    .initialColor(0xffffffff)
                    .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
                    .density(12)
                    .setOnColorSelectedListener(selectedColor -> {
                    })
                    .setPositiveButton("ok", (dialog, selectedColor, allColors) -> {
                        changeBackgroundColor(selectedColor);
                        SendDataToRemote sendDataToRemote = new SendDataToRemote("2:" + Integer.toHexString(selectedColor));
                        sendDataToRemote.execute();
                        Log.d("ColorPicker", "onColorChanged: 0x" + Integer.toHexString(selectedColor));
                    })
                    .setNegativeButton("cancel", (dialog, which) -> {
                    })
                    .build()
                    .show();
        }
        if (item.getItemId() == R.id.action_history) {

            File path = Environment.getExternalStorageDirectory();
            Log.i(TAG, "FilesDir =>" + path + "\n");
            String fileName = new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + serverIpAddress + ".txt";
            File file = new File(path + "/Download/", fileName);

            for (int i = 0; i < messageArray.size(); i++) {
                String s = messageArray.get(i).getMessage();
                if (messageArray.get(i).isSent()) {
                    s = "Client:" + s + "\n";
                    System.out.println(s);
                } else {
                    s = "Serer : " + s + "\n";
                    System.out.println(s);
                }

                try {
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write(s.getBytes());
                    Toast.makeText(ChatClient.this, "Chat history has been saved in " + path + "/Download/  folder", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (item.getItemId() == R.id.action_QR) {
            generateMyQR();
            showMe.setVisibility(View.VISIBLE);
        }
        return super.onOptionsItemSelected(item);
    }

    private void generateMyQR() {
        String myiP = "";
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        myiP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(myiP, BarcodeFormat.QR_CODE, 400, 400);
            myQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean permissionAlreadyGranted() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
        int result3 = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);

        if (result == PackageManager.PERMISSION_GRANTED&&result2 == PackageManager.PERMISSION_GRANTED&&result3 == PackageManager.PERMISSION_GRANTED)
            return true;

        return false;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION}, REQUEST_CODE);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission is denied!", Toast.LENGTH_SHORT).show();
                boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (!showRationale) {
                    openSettingsDialog();
                }


            }
        }
    }


    private void openSettingsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ChatClient.this);
        builder.setTitle("Required Permissions");
        builder.setMessage("This app require permission to use awesome feature. Grant them in app settings.");
        builder.setPositiveButton("Take Me To SETTINGS", (dialog, which) -> {
            dialog.cancel();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 101);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
        }
    }

    public final void changeBackgroundColor(Integer selectedColor) {
        LayerDrawable layerDrawable = (LayerDrawable) mMessageRecycler.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.shapeColor);
        gradientDrawable.setColor(selectedColor);
    }

    @Override
    public void onBackPressed() {
        if (exit) {
            pickiT.deleteTemporaryFile();
            s.interrupt();
            f.interrupt();
            finish();
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(() -> exit = false, 3 * 1000);

        }
    }

    @Override
    public void PickiTonStartListener() {

    }

    @Override
    public void PickiTonProgressUpdate(int progress) {

    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        Log.d(TAG, "PickiTonCompleteListener: Directory was" + path);
        new fileTransfer(path).execute();

    }

    public void playMusic(MySongs mySong, ImageView imageView) {

        Log.e(TAG, "playMusic: playing" + mySong.getDisplayName());
        JSONObject a = new JSONObject();
        try {
            a.put("startStreaming", mySong.getJSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessage(a.toString());
        if (currentlyPlaying != null) {
            currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
        }
        //TODO Pause the player
        if (mediaPlayer!=null){
            if (mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                if (currentlyPlaying != null) {
                    currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                }
            }else{
                mediaPlayer.start();
                if (currentlyPlaying != null) {
                    currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                }
            }
        }else{

        }
        if (currentlyPlaying != null) {
            if (currentlyPlaying==imageView){
                if (currentlyPlaying.getDrawable()==ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24)){
                    currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                }else{
                    currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                }
            }else{
                currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                currentlyPlaying = imageView;
                currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
            }
        }else  {
            currentlyPlaying = imageView;
            currentlyPlaying.setImageDrawable(ChatClient.this.getResources().getDrawable(R.drawable.ic_baseline_pause_24));
        }
    }

    public void startStreaming(MySongs mySongs) {
        new fileTransfer(mySongs.getPath()).execute();
    }

    @SuppressLint("StaticFieldLeak")

    public class SendDataToRemote extends AsyncTask<Void, Void, String> {
        String msg;

        SendDataToRemote(String message) {
            msg = message;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String ipadd = serverIpAddress;
                int portr = sendPort;
                Socket clientSocket = new Socket(ipadd, myport);
                OutputStream outToServer = clientSocket.getOutputStream();
                PrintWriter output = new PrintWriter(outToServer);
                output.println(msg);
                output.flush();
                clientSocket.close();
                runOnUiThread(() -> sent.setEnabled(false)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            return msg;
        }

        protected void onPostExecute(String result) {
            runOnUiThread(() -> sent.setEnabled(true));

            Log.i(TAG, "on post execution result => " + result);
            StringBuilder stringBuilder = new StringBuilder(result);
            if (stringBuilder.charAt(0) == '1' && stringBuilder.charAt(1) == ':') {
                stringBuilder.deleteCharAt(0);
                stringBuilder.deleteCharAt(0);
                result = stringBuilder.toString();

                messageArray.add(new Message(result, 0, Calendar.getInstance().getTime()));
                mMessageRecycler.setAdapter(mMessageAdapter);
                smessage.setText("");
            } else if (stringBuilder.charAt(0) == '3' && stringBuilder.charAt(1) == ':') {
                getMedia.setEnabled(true);
            }
        }


    }

    @SuppressLint("StaticFieldLeak")
    class fileTransfer extends AsyncTask<Void, Integer, String> {
        String path;

        fileTransfer(String path) {
            this.path = path;
        }

        @Override
        protected String doInBackground(Void... voids) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
            });
            String filenameX = "";
            String ipadd = serverIpAddress;
            int portr = sendPort + 1;
            try {
                Socket clientSocket = new Socket(ipadd, portr);
                if (path.charAt(0) != '/') {
                    path = "/storage/emulated/0/" + path;
                }
                Log.d(TAG, "doInBackground: Storage Here " + path);
                File file = new File(path);
                if (path.isEmpty()) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Path is empty", Toast.LENGTH_SHORT);
                    toast.show();
                }
                Log.d(TAG, "doInBackground: " + path);

                FileInputStream fileInputStream = new FileInputStream(file);

                long fileSize = file.length();
                byte[] byteArray = new byte[(int) fileSize];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buff = new byte[4096];
                int i = Integer.MAX_VALUE;
                while ((i = fileInputStream.read(buff, 0, buff.length)) > 0) {
                    baos.write(buff, 0, i);
                }

                /////////////////////////////////////////

               /* try {
                    InputStream inStream = new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return 0;
                        }
                    };
                    AudioRecord recorder;

                    int sampleRate = 44100 ; // 44100 for music
                    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
                    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    boolean status = true;

                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS","Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(ipadd);
                    Log.d("VS", "Address retrieved");


                    recorder = new AudioRecord(MediaRecorder.AudioSource.,sampleRate,channelConfig,audioFormat,minBufSize*10);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording();


                    while(status == true) {


                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer,buffer.length,destination,portr);

                        socket.send(packet);
                        System.out.println("MinBufferSize: " +minBufSize);


                    }



                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                }*/
                ///////////////////////////////////////

//                DataInputStream dataInputStream = new DataInputStream(fileInputStream);
//                dataInputStream.readFully(buff, 0, buff.length);

                OutputStream outputStream = clientSocket.getOutputStream();

                DataOutputStream dataOutputStream = new DataOutputStream(baos);
                dataOutputStream.writeUTF(file.getName());
                dataOutputStream.writeLong(buff.length);

                filenameX = file.getName();


                dataOutputStream.write(buff, 0, buff.length);
                dataOutputStream.flush();

                outputStream.write(buff, 0, buff.length);
                outputStream.flush();

                outputStream.close();
                dataOutputStream.close();

                clientSocket.close();
//                DataInputStream dataInputStream = new DataInputStream(fileInputStream);
//                dataInputStream.readFully(byteArray, 0, byteArray.length);
//
//                OutputStream outputStream = clientSocket.getOutputStream();
//
//                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
//                dataOutputStream.writeUTF(file.getName());
//                dataOutputStream.writeLong(byteArray.length);
//
//                filenameX = file.getName();
//
//
//                dataOutputStream.write(byteArray, 0, byteArray.length);
//                dataOutputStream.flush();
//
//                outputStream.write(byteArray, 0, byteArray.length);
//                outputStream.flush();
//
//                outputStream.close();
//                dataOutputStream.close();
//
//                clientSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: "+e );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(ChatClient.this, "Connection was reset", Toast.LENGTH_SHORT).show();

                    }
                }); e.printStackTrace();
            }
            return filenameX;
        }

        @Override
        protected void onPostExecute(String name) {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
            });
            Log.d(TAG, "onPostExecute: " + name);

            if (!name.isEmpty()) {
                messageArray.add(new Message("New File Sent: " + name + ":" + path, 0, Calendar.getInstance().getTime()));
                mMessageRecycler.setAdapter(mMessageAdapter);
                smessage.setText("");
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "File Sending Error.", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }
}