package com.rapid.prominence.minty.ei.scan;

public class Album {
    public final String name;
    public final String artist;
    public final int songCount;
    public final long albumid;

    public Album(long albumid,String name, String artist, int songCount) {
        this.name = name;
        this.albumid = albumid;
        this.artist = artist;
        this.songCount = songCount;
    }
}
