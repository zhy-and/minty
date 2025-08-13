package com.rapid.prominence.minty.ei.scan;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.rapid.prominence.minty.bean.Song;

import java.util.ArrayList;
import java.util.List;

public class EiScanTools {
    public static List<Song> getLocalSongs(Context context) {
        List<Song> songs = new ArrayList<>();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                null)) {
            
            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                int pathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                while (cursor.moveToNext()) {
                    songs.add(new Song(
                            cursor.getLong(idIndex),
                            cursor.getString(titleIndex),
                            cursor.getString(artistIndex),
                            cursor.getLong(durationIndex),
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
                            cursor.getLong(idIndex),
                            cursor.getString(pathIndex),
                            cursor.getString(albumColumn),
                            cursor.getLong(albumIdColumn)


                    ));
                }
            }
        }
        return songs;
    }

    public static List<Album> getLocalAlbums(Context context) {
        List<Album> albums = new ArrayList<>();
        String[] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS
        };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC")) {
            
            if (cursor != null) {
                int albumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);
                int albumIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
                int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
                int countIndex = cursor.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS);

                while (cursor.moveToNext()) {
                    albums.add(new Album(
                            cursor.getLong(albumIdIndex),
                            cursor.getString(albumIndex),
                            cursor.getString(artistIndex),
                            cursor.getInt(countIndex)
                    ));
                }
            }
        }
        return albums;
    }

    public static List<Artist> getLocalArtists(Context context) {
        List<Artist> artists = new ArrayList<>();
        String[] projection = {
                MediaStore.Audio.Artists.ARTIST_KEY,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Artists.ARTIST + " ASC")) {
            
            if (cursor != null) {
                int keyindex = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST_KEY); // 鏂板artistKey
                int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
                int albumsIndex = cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
                int tracksIndex = cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

                while (cursor.moveToNext()) {
                    artists.add(new Artist(
                            cursor.getString(keyindex),
                            cursor.getString(artistIndex),
                            cursor.getInt(albumsIndex),
                            cursor.getInt(tracksIndex)
                    ));
                }
            }
        }
        return artists;
    }
}
