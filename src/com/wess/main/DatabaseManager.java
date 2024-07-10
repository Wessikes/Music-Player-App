package com.wess.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
/**
 *
 * @author Wess
 */
public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/mapp";
    private static final String USER = "root";
    private static final String PASSWORD = "Wessikes06031998";
    
    private Connection connection;
    
    public void connect() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    public void addSong(Song song) throws SQLException {
        String sql = "INSERT INTO songs (title, artist, album, duration, file_path) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setString(4, song.getFilePath());
            pstmt.executeUpdate();
        }
    }
    
    public void saveSong(Song song) {
        String sql = "INSERT INTO songs (title, artist, album, file_path) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, song.getTitle());
            pstmt.setString(2, song.getArtist());
            pstmt.setString(3, song.getAlbum());
            pstmt.setString(4, song.getFilePath());
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    song.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
        }
    }
    
    public List<Song> getAllSongs() throws SQLException {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT * FROM songs";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Song song = new Song(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("songLength"),
                    rs.getString("album"),
                    rs.getString("file_path")
                );
                songs.add(song);
            }
        }
        return songs;
    }
    
    public void createPlaylist(String name) {
        String sql = "INSERT INTO playlists (name) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
        }
    }
    public void addSongToPlaylist(int playlistId, int songId, int order) {
        String sql = "INSERT INTO playlist_songs (playlist_id, song_id, song_order) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            pstmt.setInt(2, songId);
            pstmt.setInt(3, order);
            pstmt.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public List<Song> getPlaylistSongs(int playlistId) {
        List<Song> songs = new ArrayList<>();
        String sql = "SELECT s.* FROM songs s JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.song_order";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                songs.add(new Song(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("artist"),
                    rs.getString("songLength"),
                    rs.getString("album"),
                    rs.getString("file_path")
                ));
            }
        } catch (SQLException e) {
        }
        return songs;
    }
    
    public List<String> getAllPlaylists() {
    List<String> playlists = new ArrayList<>();
    String sql = "SELECT name FROM playlists";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            playlists.add(rs.getString("name"));
        }
    } catch (SQLException e) {
    }
    return playlists;
}

    public int getPlaylistId(String playlistName) {
    String sql = "SELECT id FROM playlists WHERE name = ?";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
         pstmt.setString(1, playlistName);
         ResultSet rs = pstmt.executeQuery();
         if (rs.next()) {
             return rs.getInt("id");
         }
        } catch (SQLException e) {
        }
        return -1;
    }

    public int getPlaylistSize(int playlistId) {
        String sql = "SELECT COUNT(*) as count FROM playlist_songs WHERE playlist_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playlistId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    
    public void removeSongFromPlaylist(int playlistId, int songId)  {
    String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, playlistId);
        pstmt.setInt(2, songId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
    }
}
    
    public void updatePlaylistOrder(int playlistId, List<Song> songs) throws Exception {
    String sql = "UPDATE playlist_songs SET song_order = ? WHERE playlist_id = ? AND song_id = ?";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        conn.setAutoCommit(false);
        for (int i = 0; i < songs.size(); i++) {
            pstmt.setInt(1, i);
            pstmt.setInt(2, playlistId);
            pstmt.setInt(3, songs.get(i).getId());
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        conn.commit();
    } catch (SQLException e) {
        throw new Exception("Error updating playlist order", e);
    }
}
    
    public void importPlaylist(String filePath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            String playlistName = new File(filePath).getName().replaceFirst("[.][^.]+$", "");
            createPlaylist(playlistName);
            int playlistId = getPlaylistId(playlistName);
            int order = 1;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    File songFile = new File(line);
                    Song song = createSongFromFile(songFile);
                    saveSong(song);
                    addSongToPlaylist(playlistId, song.getId(), order++);
                }
            }
        } catch (IOException e) {
            throw new Exception("Error importing playlist", e);
        }
}

    public void exportPlaylist(String playlistName, String filePath) throws Exception {
        int playlistId = getPlaylistId(playlistName);
        List<Song> songs = getPlaylistSongs(playlistId);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("#EXTM3U");
            writer.newLine();
            for (Song song : songs) {
                writer.write("#EXTINF:" + song.getSongLength() + "," + song.getArtist() + " - " + song.getTitle());
                writer.newLine();
                writer.write(song.getFilePath());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new Exception("Error exporting playlist", e);
        }
    }
    
    public Song createSongFromFile(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            String artist = tag.getFirst(FieldKey.ARTIST);
            String title = tag.getFirst(FieldKey.TITLE);
            int songLengthInt = audioFile.getAudioHeader().getTrackLength();
            String songLength = String.valueOf(songLengthInt); 
            String album = tag.getFirst(FieldKey.ALBUM);

            if (artist.isEmpty()) artist = file.getName();
            if (title.isEmpty()) title = "Unknown Artist";
            if (album.isEmpty()) album = "Unknown Album";

            return new Song(0, title, artist, songLength, album, file.getAbsolutePath());
        } catch (IOException | CannotReadException | InvalidAudioFrameException | ReadOnlyFileException | KeyNotFoundException | TagException e) {
            return new Song(0, file.getName(), "Unknown Artist", "Empty file", "Unknown Album", file.getAbsolutePath());
        }
    }
}
