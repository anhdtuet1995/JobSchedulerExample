package com.supersoft.servicestudy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.List;
import java.util.Random;

public class MusicService extends JobService implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static MusicService instance;

    private static final String TAG = "anh.dt2_Service";
    private boolean jobCancelled = false;
    private MediaPlayer mMediaPlayer;

    //song list
    private List<Song> songs;
    //current position
    private int songPosn;

    //title of current song
    private String songTitle = "";
    //notification id
    private static final int NOTIFY_ID = 1;
    //shuffle flag and random
    private boolean shuffle=false;
    private Random rand;

    public static MusicService getInstance() {
        if (instance == null) {
            Log.d(TAG, "instance is null!!!!!!!!");
        }
        return instance;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started!!!!!!");

        instance = this;

        initMedia();

        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service on created!!!!!!");
    }

    //pass song list
    public void setList(List<Song> theSongs){
        songs = theSongs;
    }

    private void initMedia() {
        songPosn = 0;
        rand = new Random();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);

    }

    //play a song
    public void playSong(){
        //play
        mMediaPlayer.reset();
        //get song
        Song playSong = songs.get(songPosn);
        //get title
        songTitle = playSong.getTitle();
        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);
        //set the data source
        try{
            mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        mMediaPlayer.prepareAsync();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before completion!!!!!!");
        jobCancelled = true;
        mMediaPlayer.stop();
        mMediaPlayer.release();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        //check if playback has reached the end of a track
        if(mediaPlayer.getCurrentPosition() > 0){
            mediaPlayer.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.v(TAG, "Playback Error");
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        //notification
        showNotification();
    }

    private void showNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("PLAY_MUSIC",
                    "Music player",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("");
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "PLAY_MUSIC")
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendInt);
        mNotificationManager.notify(0, mBuilder.build());
    }

    //set the song
    public void setSong(int songIndex){
        songPosn = songIndex;
    }

    //playback methods
    public int getPosn(){
        return mMediaPlayer.getCurrentPosition();
    }

    public int getDur(){
        return mMediaPlayer.getDuration();
    }

    public boolean isPng(){
        return mMediaPlayer.isPlaying();
    }

    public void pausePlayer(){
        mMediaPlayer.pause();
    }

    public void seek(int posn){
        mMediaPlayer.seekTo(posn);
    }

    public void go(){
        mMediaPlayer.start();
    }

    //skip to previous track
    public void playPrev(){
        songPosn--;
        if(songPosn < 0) songPosn = songs.size()-1;
        playSong();
    }

    //skip to next
    public void playNext(){
        if(shuffle){
            int newSong = songPosn;
            while(newSong == songPosn){
                newSong = rand.nextInt(songs.size());
            }
            songPosn = newSong;
        }
        else{
            songPosn++;
            if(songPosn >= songs.size()) songPosn = 0;
        }
        playSong();
    }

    //toggle shuffle
    public void setShuffle(){
        if(shuffle) shuffle = false;
        else shuffle = true;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        instance = null;
    }
}
