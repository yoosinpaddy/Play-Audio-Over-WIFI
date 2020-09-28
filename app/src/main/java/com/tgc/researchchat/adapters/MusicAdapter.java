package com.tgc.researchchat.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.tgc.researchchat.ChatClient;
import com.tgc.researchchat.R;
import com.tgc.researchchat.models.MySongs;

import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    ArrayList<MySongs> mySongs=new ArrayList<>();
    Context c;
    private static final String TAG = "MusicAdapter";


    public MusicAdapter(ArrayList<MySongs> mySongs, Context c) {
        Log.e(TAG, "MusicAdapter: " );
        this.mySongs = mySongs;
        this.c = c;
    }


    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicViewHolder(LayoutInflater.from(c).inflate(R.layout.item_song,null));
    }

    @Override
    public void onBindViewHolder(@NonNull final MusicViewHolder holder, int position) {
        Log.e(TAG, "onBindViewHolder: " );
        final MySongs mySong=mySongs.get(position);
        holder.musicName.setText(mySong.getDisplayName());
        holder.playPause.setImageDrawable(c.getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
        holder.playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ChatClient)c).playMusic(mySong,holder.playPause);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mySongs.size();
    }

    class MusicViewHolder extends RecyclerView.ViewHolder{
        ImageView playPause,musicImage;
        TextView musicName;
        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            playPause=itemView.findViewById(R.id.play_pause);
            musicImage=itemView.findViewById(R.id.music_image);
            musicName=itemView.findViewById(R.id.music_name);

        }
    }
}
