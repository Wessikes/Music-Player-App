package com.wess.main;

/**
 *
 * @author Wess
 */
public class Song {
    private int id;
    private String title;
    private String artist;
    private String songLength;
    private String album;
    private String filePath;

    public Song(int id, String title, String artist, String songLength, String album,String filePath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.songLength = songLength;
        this.album = album;
        this.filePath = filePath;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getSongLength() {return songLength;}
    public void setSongLength(String songLength) { this.songLength = songLength; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
     @Override
    public String toString() {
        return title + " - " + artist;
    }

    public javafx.beans.property.StringProperty titleProperty() {
        return new javafx.beans.property.SimpleStringProperty(title);
    }

    public javafx.beans.property.StringProperty artistProperty() {
        return new javafx.beans.property.SimpleStringProperty(artist);
    }

    public javafx.beans.property.StringProperty albumProperty() {
        return new javafx.beans.property.SimpleStringProperty(album);
    }
}