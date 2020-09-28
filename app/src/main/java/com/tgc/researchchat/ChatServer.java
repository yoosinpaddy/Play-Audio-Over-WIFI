package com.tgc.researchchat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.tgc.researchchat.adapters.MusicAdapter;
import com.tgc.researchchat.models.MySongs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

public class ChatServer extends Thread {

    private Context context;
    private String serverIpAddress;
    private Activity activity;
    private String ownIp;
    private String TAG = "CHATSERVER";
    private RecyclerView messageList;
    private RecyclerView fileListRecycler;
    private ArrayList<Message> messageArray;
    private ChatAdapterRecycler mAdapter;
    private MusicAdapter musicAdapter;
    private int port;
    ArrayList<MySongs> allSongs;
    ArrayList<MySongs> allSongsToSend;
    ChatClient.MyFileListInteface myFileListInteface;

    ChatServer(String ownIp, Activity activity, Context context, ChatAdapterRecycler mAdapter, RecyclerView messageList, ArrayList<Message> messageArray, int port, String serverIpAddress, RecyclerView mSongRecycler, MusicAdapter musicAdapter, ArrayList<MySongs> allSongs, ChatClient.MyFileListInteface myFileListInteface) {
        this.ownIp = ownIp;
        this.messageArray = messageArray;
        this.messageList = messageList;
        this.fileListRecycler = mSongRecycler;
        this.mAdapter = mAdapter;
        this.port = port;
        this.context = context;
        this.serverIpAddress = serverIpAddress;
        this.activity = activity;
        this.allSongs = allSongs;
        this.musicAdapter = musicAdapter;
        this.myFileListInteface = myFileListInteface;
    }

    @SuppressLint("SetTextI18n")
    public void run() {
        try {
            ServerSocket initSocket = new ServerSocket(ChatClient.myport);
            initSocket.setReuseAddress(true);
            TextView textView;
            textView = activity.findViewById(R.id.textView);
            textView.setText("Server Socket Started at IP: " + ownIp + " and Port: " + ChatClient.myport);
            textView.setBackgroundColor(Color.parseColor("#39FF14"));
            System.out.println(TAG + "started");
            while (!Thread.interrupted()) {
                Socket connectSocket = initSocket.accept();
                ReceiveTexts handle = new ReceiveTexts();
                handle.execute(connectSocket);
            }
            initSocket.close();
        } catch (IOException e) {
            TextView textView;
            textView = activity.findViewById(R.id.textView);
            if (ChatClient.myport!=ChatClient.myport_2&&ChatClient.myport!=ChatClient.myport_3){
                ChatClient.myport=ChatClient.myport_2;
                Log.e(TAG, "run: retrying...." );
                this.run();
            }else if (ChatClient.myport!=ChatClient.myport_2){
                ChatClient.myport=ChatClient.myport_3;
                Log.e(TAG, "run: retrying...." );
                this.run();
            }
            textView.setText("Server Socket initialization failed. Port already in use. Retrying...");
            textView.setBackgroundColor(Color.parseColor("#FF0800"));
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class ReceiveTexts extends AsyncTask<Socket, Void, String> {
        String text;

        @Override
        protected String doInBackground(Socket... sockets) {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(sockets[0].getInputStream()));
                text = input.readLine();
                Log.i(TAG, "Received => " + text);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return text;
        }

        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute: Result" + result);
            if (result.charAt(0) == '1' && result.charAt(1) == ':') {
                StringBuilder stringBuilder = new StringBuilder(result);
                stringBuilder.deleteCharAt(0);
                stringBuilder.deleteCharAt(0);
                result = stringBuilder.toString();

                messageArray.add(new Message(result, 1, Calendar.getInstance().getTime()));
//                messageList.setAdapter(mAdapter);
            } else if (result.charAt(0) == '3' && result.charAt(1) == ':') {
                StringBuilder stringBuilder = new StringBuilder(result);
                stringBuilder.deleteCharAt(0);
                stringBuilder.deleteCharAt(0);
                result = stringBuilder.toString();

                    getMediaSongs();

                ((ChatClient)activity).sendMessage(getMediaArray().toString());
                messageArray.add(new Message(result, 1, Calendar.getInstance().getTime()));
//                messageList.setAdapter(mAdapter);
            }else if (isSongResults(result)) {
                ((ChatClient)activity).musicAdapter.notifyDataSetChanged();
                Log.e(TAG, "onPostExecute: songs received" );
            }else if (isOneSongResults(result)) {
                Log.e(TAG, "onPostExecute: one song received" );
//                fileListRecycler.setAdapter(musicAdapter);

                try {
                    JSONObject o = null;
                    o = new JSONObject(result);
                    JSONObject arr = o.getJSONObject("startStreaming");
                    ((ChatClient)activity).startStreaming(new MySongs(arr));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder(result);
                stringBuilder.deleteCharAt(0);
                stringBuilder.deleteCharAt(0);
                result = stringBuilder.toString();
                RecyclerView message_List;
                message_List = activity.findViewById(R.id.message_list);
                LayerDrawable layerDrawable = (LayerDrawable) message_List.getBackground();
                GradientDrawable gradientDrawable = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.shapeColor);
                gradientDrawable.setColor(Color.parseColor("#" + result));
            }
        }
    }

    private boolean isOneSongResults(String message) {
        try {
            JSONObject o = new JSONObject(message);
            o.get("startStreaming");
            if (message.contains("startStreaming")) {
                JSONObject arr = o.getJSONObject("startStreaming");
                return true;
            } else {
                Log.e(TAG, "isJSONObject: but doesnt contain songs");
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "isJSONObject: " + e.getMessage());
            return false;
        }
    }

    private boolean isSongResults(String message) {
        try {
            JSONObject o = new JSONObject(message);
            o.get("MySongs");
            if (message.contains("MySongs")) {
                ArrayList<MySongs> allSongs = new ArrayList<>();
                JSONArray arr = o.getJSONArray("MySongs");
                for (int i = 0; i < arr.length(); i++) {
                    allSongs.add(new MySongs(arr.getJSONObject(i)));
                }
                myFileListInteface.getSongs(allSongs);
                return true;
            } else {
                Log.e(TAG, "isJSONObject: but doesnt contain songs");
                return false;
            }
        } catch (JSONException e) {
            Log.e(TAG, "isJSONObject: " + e.getMessage());
            return false;
        }
    }
    private void getMediaSongs() {
        Log.e(TAG, "getMediaSongs: ");
        allSongsToSend = new ArrayList<>();
        ContentResolver cr =activity.getContentResolver();

        //Some audio may be explicitly marked as not being music
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };

        Cursor c = activity.managedQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

        while (c.moveToNext()) {
            allSongsToSend.add(new MySongs(c.getString(4), c.getString(3), c.getString(0)));
            Log.e(TAG, "getMediaSongs: " + c.getString(4));
        }
        Log.e(TAG, "getMediaSongs:  Total songs:" + allSongsToSend.size());
    }
    private JSONObject getMediaArray() {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (MySongs m : allSongsToSend) {
            jsonArray.put(m.getJSONObject());
        }
        try {
            jsonObject.put("MySongs", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
