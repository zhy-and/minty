package com.rapid.prominence.minty.bean;

public class Song {
    public Song() {
        super();
    }

    public  String title;
    public  String artist;
    public  String filePath;
    public  String album;
    public  long duration;
    public  long albumId;
    public  String uri;
    public  long uriId;
    public long id;


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int index;

    public boolean isCollect() {
        return isCollect;
    }

    public void setCollect(boolean collect) {
        isCollect = collect;
    }

    public boolean isCollect;

    public Song(long id,String title, String artist, long duration, String uri,long uriId, String filePath, String album, long albumId) {
        this.title = title;
        this.id = id;
        this.artist = artist;
        this.duration = duration;
        this.album = album;
        this.albumId = albumId;
        this.filePath = filePath;
        this.uri = uri;
        this.uriId = uriId;
    }
}


