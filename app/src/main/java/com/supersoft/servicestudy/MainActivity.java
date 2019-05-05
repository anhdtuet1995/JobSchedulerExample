package com.supersoft.servicestudy;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.ListView;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private final String TAG = "anh.dt2_Main";

    private MusicController mMusicController;
    private List<Song> songs;
    private ListView songView;

    private boolean paused = false, playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Started");
        scheduleJob();

        Log.d(TAG, "init and get song list");
        songs = new ArrayList<>();
        getSongList();
        songView = (ListView) findViewById(R.id.song_list);

        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songs);
        songView.setAdapter(songAdt);

        //setup controller
        setController();


    }

    public void scheduleJob() {
        ComponentName componentName = new ComponentName(this, MusicService.class);
        JobInfo info = new JobInfo.Builder(123, componentName)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        Log.d(TAG, (resultCode == JobScheduler.RESULT_SUCCESS) ? "Job scheduled" : "Job scheduling failed");
    }

    public void cancelJob() {
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(123);
        Log.d(TAG, "Job cancelled");
    }

    //method to retrieve song info from device
    public void getSongList(){
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songs.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    //set the controller up
    private void setController(){
        mMusicController = new MusicController(this);
        //set previous and next button listeners
        mMusicController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        mMusicController.setMediaPlayer(this);
        mMusicController.setAnchorView(findViewById(R.id.song_list));
        mMusicController.setEnabled(true);
    }

    private void playNext(){
        MusicService.getInstance().playNext();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        mMusicController.show(0);
    }

    private void playPrev(){
        MusicService.getInstance().playPrev();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        mMusicController.show(0);
    }

    //user song select
    public void songPicked(View view){
        MusicService.getInstance().setSong(Integer.parseInt(view.getTag().toString()));
        MusicService.getInstance().playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        mMusicController.show(0);
    }


    @Override
    public void start() {
        MusicService.getInstance().go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        MusicService.getInstance().pausePlayer();
    }

    @Override
    public int getDuration() {
        if (MusicService.getInstance().isPng()) {
            return MusicService.getInstance().getDur();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (MusicService.getInstance().isPng()) {
            return MusicService.getInstance().getPosn();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        MusicService.getInstance().seek(i);
    }

    @Override
    public boolean isPlaying() {
        return MusicService.getInstance().isPng();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        mMusicController.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
