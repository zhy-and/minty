package com.rapid.prominence.minty.ei.scan;

public class Artist {
    public final String name;
    public final String key;
    public final int albumCount;
    public final int songCount;

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public  boolean isSelect;

    public Artist(String key,String name, int albumCount, int songCount) {
        this.name = name;
        this.key = key;
        this.albumCount = albumCount;
        this.songCount = songCount;
    }
}
