package com.wess.main;
              
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.input.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.EqualizerBand;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.animation.AnimationTimer;

public class MusicPlayerApp extends Application {

    private TableView<Song> libraryTable;
    private ListView<Song> playlistView;
    private ComboBox<String> playlistComboBox;
    private TextField searchBar;
    private Button playButton, pauseButton,shuffleButton,repeatButton,loopButton, nextButton, prevButton, loadButton;
    private Slider volumeSlider;
    private MediaPlayer mediaPlayer;
    private final DatabaseManager dbManager = new DatabaseManager();
    private File lastAccessedFolder;
    private int currentPlaylistId = -1;
    private boolean shuffle = false;
    private boolean repeat = false;
    private List<Song> currentPlaylist = new ArrayList<>();
    private int currentSongIndex = -1;
    private Slider songProgressBar;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private HBox controlsBox;
    private static final int MAX_RECENT_FILES = 7;
    private final List<String> recentFiles = new ArrayList<>();
    private Menu recentFilesMenu;
    private static final String RECENT_FILES_FILENAME = "recent_files.txt";
    private Duration pausePosition;
    private float[] magnitudes = new float[128];
    
    @Override
    public void start(Stage primaryStage) throws SQLException {
        BorderPane mainLayout = new BorderPane();

        // Add menu bar
        MenuBar menuBar = createMenuBar();
        mainLayout.setTop(menuBar);

        VBox topControls = new VBox(10);
        topControls.getChildren().addAll(createTopControls(), createPlaylistControls());
        mainLayout.setTop(new VBox(menuBar, topControls));

        VBox centerContent = new VBox(10);
        SplitPane centerSplitPane = createCenterView();
        centerContent.getChildren().addAll(centerSplitPane);
        mainLayout.setCenter(centerContent);

        VBox bottomControls = new VBox(10);
        bottomControls.getChildren().addAll(createProgressControls(),createPlaybackControls());
        mainLayout.setBottom(bottomControls);

        Scene scene = new Scene(mainLayout, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/com/wess/styles/style.css").toExternalForm());
        primaryStage.setTitle("Music Player App");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            saveRecentFiles();
        });
        updatePlaylistComboBox();
        loadRecentFiles();
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu songMenu = new Menu("Song");
        MenuItem loadSongItem = new MenuItem("Load Song");
        loadSongItem.setOnAction(e -> loadAudioFiles());

        recentFilesMenu = new Menu("Recently opened");
        updateRecentFilesMenu();

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(e -> quit());

        songMenu.getItems().addAll(loadSongItem, recentFilesMenu, new SeparatorMenuItem(), quitItem);

        Menu playlistMenu = new Menu("Playlist");
        MenuItem createPlaylistItem = new MenuItem("Create Playlist");
        createPlaylistItem.setOnAction(e -> createNewPlaylist());
        MenuItem loadPlaylistItem = new MenuItem("Load Playlist");
        loadPlaylistItem.setOnAction(e -> loadSelectedPlaylist());
        MenuItem importPlaylistItem = new MenuItem("Import Playlist");
        importPlaylistItem.setOnAction(e -> importPlaylist());
        MenuItem exportPlaylistItem = new MenuItem("Export Playlist");
        exportPlaylistItem.setOnAction(e -> exportPlaylist());
        playlistMenu.getItems().addAll(createPlaylistItem, loadPlaylistItem, importPlaylistItem, exportPlaylistItem);
        
        Menu effectsMenu = new Menu("Effects and Adjustments");
        MenuItem equalizerItem = new MenuItem("Equalizer");
        equalizerItem.setOnAction(e -> showEqualizer());
        MenuItem speedItem = new MenuItem("Playback Speed");
        speedItem.setOnAction(e -> adjustPlaybackSpeed());
        MenuItem visualizationItem = new MenuItem("Visualisation audio");
        visualizationItem.setOnAction(e -> showAudioVisualization());
        
        effectsMenu.getItems().addAll(equalizerItem, speedItem, visualizationItem);

        Menu aboutMenu = new Menu("About");
        MenuItem infoItem = new MenuItem("Informations");
        infoItem.setOnAction(e -> showAboutInfo());
        aboutMenu.getItems().add(infoItem);

        menuBar.getMenus().addAll(songMenu, playlistMenu, effectsMenu, aboutMenu);
        return menuBar;
}
    
    private HBox createProgressControls() {
        HBox progressControls = new HBox(10);
        progressControls.setAlignment(Pos.CENTER);

        currentTimeLabel = new Label("0:00");
        totalTimeLabel = new Label("0:00");
        songProgressBar = new Slider(0, 100, 0);
        songProgressBar.setPrefWidth(800);
        songProgressBar.setPadding(new Insets(20, 5, 20, 5));
        songProgressBar.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && songProgressBar.isValueChanging()) {
                mediaPlayer.seek(Duration.seconds(newVal.doubleValue()));
            }
        });
        progressControls.getChildren().addAll(currentTimeLabel, songProgressBar, totalTimeLabel);
        return progressControls;
    }
    
    private void updateProgressBar() {
        if (mediaPlayer != null) {
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!songProgressBar.isValueChanging()) {
                    songProgressBar.setValue(newTime.toSeconds());
                }
                currentTimeLabel.setText(formatTime(newTime));
            });
            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getTotalDuration();
                songProgressBar.setMax(total.toSeconds());
                totalTimeLabel.setText(formatTime(total));
            });
        }
    }
    
    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private HBox createTopControls() {
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));

        loadButton = new Button("Load");
        loadButton.setOnAction(e -> loadAudioFiles());

        searchBar = new TextField();
        searchBar.setPromptText("Search for songs...");
        searchBar.setPrefWidth(300);

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());

        topBox.getChildren().addAll(loadButton, searchBar, searchButton);
        return topBox;
    }
    
    private HBox createPlaylistControls() {
        HBox playlistControls = new HBox(10);
        playlistComboBox = new ComboBox<>();
        Button createPlaylistButton = new Button("Create Playlist");
        Button addToPlaylistButton = new Button("Add to Playlist");
        Button removeFromPlaylistButton = new Button("Remove from Playlist");

        createPlaylistButton.setOnAction(e -> createNewPlaylist());
        addToPlaylistButton.setOnAction(e -> addSelectedSongToCurrentPlaylist());
        removeFromPlaylistButton.setOnAction(e -> removeSelectedSongFromCurrentPlaylist());

        playlistControls.getChildren().addAll(createPlaylistButton, addToPlaylistButton, removeFromPlaylistButton, playlistComboBox);
        return playlistControls;
    }
    
    private void createNewPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Playlist");
        dialog.setHeaderText("Enter the name for the new playlist:");
        dialog.setContentText("Playlist name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            dbManager.createPlaylist(name);
            updatePlaylistComboBox();
        });
    }

    private void updatePlaylistComboBox() {
        List<String> playlists = dbManager.getAllPlaylists();
        playlistComboBox.setItems(FXCollections.observableArrayList(playlists));
    }
    
    private void loadSelectedPlaylist() {
    String selectedPlaylist = playlistComboBox.getValue();
    if (selectedPlaylist != null) {
        currentPlaylistId = dbManager.getPlaylistId(selectedPlaylist);
        updatePlaylistView();
        enablePauseButtonDisablePlayButton();
        }
    }

    private void addSelectedSongToCurrentPlaylist() {
        Song selectedSong = libraryTable.getSelectionModel().getSelectedItem();
        String selectedPlaylist = playlistComboBox.getValue();
        if (selectedSong != null && selectedPlaylist != null) {
            int playlistId = dbManager.getPlaylistId(selectedPlaylist);
            dbManager.addSongToPlaylist(playlistId, selectedSong.getId(), dbManager.getPlaylistSize(playlistId) + 1);
            updatePlaylistView();
        }
    }
    
    private void removeSelectedSongFromCurrentPlaylist() {
        Song selectedSong = libraryTable.getSelectionModel().getSelectedItem();
    if (selectedSong != null && currentPlaylistId != -1) {
        try {
            dbManager.removeSongFromPlaylist(currentPlaylistId, selectedSong.getId());
            updatePlaylistView();
            AlertUtil.showInfoAlert("Song Removed", "The song has been removed from the playlist.");
        } catch (Exception e) {
            AlertUtil.showErrorAlert("Database Error", "Failed to remove song from playlist: " + e.getMessage());
        }
    }
    }

    private void updatePlaylistView(){
    if (currentPlaylistId != -1) {
        currentPlaylist = dbManager.getPlaylistSongs(currentPlaylistId);
        playlistView.setItems(FXCollections.observableArrayList(currentPlaylist));
        currentSongIndex = -1;
    }
}

    private SplitPane createCenterView() {
        SplitPane centerSplitPane = new SplitPane();

        // Music Library View
        VBox libraryBox = new VBox(10);
        libraryBox.setPadding(new Insets(10));
        Label libraryLabel = new Label("Music Library");
        libraryTable = new TableView<>();
        libraryTable.setPrefHeight(400);
        
        TableColumn<Song, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(cellData -> cellData.getValue().artistProperty());
        
        TableColumn<Song, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        
        TableColumn<Song, String> albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(cellData -> cellData.getValue().albumProperty());
        
        libraryTable.getColumns().addAll(artistCol, titleCol, albumCol);
        libraryBox.getChildren().addAll(libraryLabel, libraryTable);

        // Playlist View
        VBox playlistBox = new VBox(10);
        playlistBox.setPadding(new Insets(10));
        Label playlistLabel = new Label("Current Playlist");
        playlistView = new ListView<>();
        playlistView.setPrefHeight(400);
        playlistBox.getChildren().addAll(playlistLabel, playlistView);

        centerSplitPane.getItems().addAll(libraryBox, playlistBox);
        return centerSplitPane;
    }

    public HBox createPlaybackControls() {
        controlsBox = new HBox(10);
        controlsBox.setPadding(new Insets(10));
        controlsBox.setLayoutY(90);

        loopButton = createButtonWithIcon("loop.png", e -> {
            toggleRepeat();
            enableRepeatButtonDisableLoopButton();
        });
        repeatButton = createButtonWithIcon("repeat.png", e -> {
            toggleShuffle();
            enableShuffleButtonDisableRepeatButton();
        });
        shuffleButton = createButtonWithIcon("shuffle.png", e -> enableLoopButtonDisableShuffleButton());
        prevButton = createButtonWithIcon("back.png", e -> playPreviousSong());
        playButton = createButtonWithIcon("play.png", e -> {
            enablePauseButtonDisablePlayButton();
            playCurrentSong();
        });
        pauseButton = createButtonWithIcon("pause.png", e -> {
            enablePlayButtonDisablePauseButton();
            pauseCurrentSong();
        });
        nextButton = createButtonWithIcon("next.png", e -> playNextSong());
        volumeSlider = new Slider(0, 100, 0);
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newValue.doubleValue() / 100.0);
            }
        });
        controlsBox.getChildren().addAll(loopButton, prevButton, playButton, nextButton, new Label("Volume:"), volumeSlider);
        return controlsBox;
    }
    
    private Button createButtonWithIcon(String iconName, EventHandler<ActionEvent> action) {
        Button button = new Button();
        Image icon = new Image(getClass().getResourceAsStream("/com/wess/icon/" + iconName));
        ImageView imageView = new ImageView(icon);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        button.setGraphic(imageView);
        button.setOnAction(action);
        return button;
}
    
    public void enableRepeatButtonDisableLoopButton(){
        controlsBox.getChildren().remove(loopButton);
        controlsBox.getChildren().add(0, repeatButton);
        loopButton.setVisible(false);
        loopButton.setDisable(true);
        repeatButton.setVisible(true);
        repeatButton.setDisable(false);
}

    public void enableShuffleButtonDisableRepeatButton(){
        controlsBox.getChildren().remove(repeatButton);
        controlsBox.getChildren().add(0, shuffleButton);
        repeatButton.setVisible(false);
        repeatButton.setDisable(true);
        shuffleButton.setVisible(true);
        shuffleButton.setDisable(false);
    }

    public void enableLoopButtonDisableShuffleButton(){
        controlsBox.getChildren().remove(shuffleButton);
        controlsBox.getChildren().add(0, loopButton);
        shuffleButton.setVisible(false);
        shuffleButton.setDisable(true);
        loopButton.setVisible(true);
        loopButton.setDisable(false);
    }
    public void enablePauseButtonDisablePlayButton(){
        controlsBox.getChildren().remove(playButton);
        controlsBox.getChildren().remove(pauseButton);
        controlsBox.getChildren().add(2, pauseButton);
        playButton.setVisible(false);
        playButton.setDisable(true);
        pauseButton.setVisible(true);
        pauseButton.setDisable(false);
    }

    public void enablePlayButtonDisablePauseButton(){
        controlsBox.getChildren().remove(pauseButton);
        controlsBox.getChildren().remove(playButton);
        controlsBox.getChildren().add(2, playButton);
        pauseButton.setVisible(false);
        pauseButton.setDisable(true);
        playButton.setVisible(true);
        playButton.setDisable(false);
    }
    
    private void playCurrentSong() {
        if (currentPlaylist.isEmpty()) return;
        if (currentSongIndex == -1) currentSongIndex = 0;

        if (mediaPlayer != null && pausePosition != null) {
            mediaPlayer.seek(pausePosition);
            mediaPlayer.play();
            pausePosition = null;
        } else {
            playSong(currentPlaylist.get(currentSongIndex));
        }
    }

    private void pauseCurrentSong() {
        if (mediaPlayer != null) {
            pausePosition = mediaPlayer.getCurrentTime();
            mediaPlayer.pause();
        }
    }

    private void performSearch() {
        String searchTerm = searchBar.getText().toLowerCase();
        libraryTable.getItems().stream()
                .filter(song -> song.getTitle().toLowerCase().contains(searchTerm) ||
                                song.getArtist().toLowerCase().contains(searchTerm) ||
                                song.getAlbum().toLowerCase().contains(searchTerm))
                .forEach(song -> {
                    libraryTable.getSelectionModel().select(song);
                });
    }

    private void addSelectedSongToPlaylist() {
        Song selectedSong = libraryTable.getSelectionModel().getSelectedItem();
        if (selectedSong != null) {
            playlistView.getItems().add(selectedSong);
        }
    }
    
    private void loadAudioFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio Files");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.3gpp")
        );
        if (lastAccessedFolder != null) {
            fileChooser.setInitialDirectory(lastAccessedFolder);
        }
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            lastAccessedFolder = selectedFiles.get(0).getParentFile();
            for (File file : selectedFiles) {
                Song song = createSongFromFile(file);
                libraryTable.getItems().add(song);
                enablePauseButtonDisablePlayButton();
                playSong(song);
                addRecentFile(file.getAbsolutePath());
            }
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
    
    private void playSong(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        pausePosition = null;
        try {
            Media media = new Media(new File(song.getFilePath()).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
            updateProgressBar();
        } catch (Exception e) {
            showAlert("Error playing file: " + e.getMessage());
        }
        mediaPlayer.setOnEndOfMedia(() -> {
            if (repeat) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else {
                playNextSong();
            }
        });
}

    private void playNextSong() {
        if (currentPlaylist.isEmpty()) return;
    
        if (shuffle) {
            currentSongIndex = (int) (Math.random() * currentPlaylist.size());
        } else {
            currentSongIndex = (currentSongIndex + 1) % currentPlaylist.size();
        }
        playSong(currentPlaylist.get(currentSongIndex));
    }

    private void playPreviousSong() {
        if (currentPlaylist.isEmpty()) return;
    
        if (shuffle) {
            currentSongIndex = (int) (Math.random() * currentPlaylist.size());
        } else {
            currentSongIndex = (currentSongIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        }
        playSong(currentPlaylist.get(currentSongIndex));
        enablePauseButtonDisablePlayButton();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public class AlertUtil {
        public static void showErrorAlert(String title, String content) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        }

        public static void showInfoAlert(String title, String content) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(title);
            alert.setContentText(content);
            alert.showAndWait();
        }
}
    
    // Add this method to load songs from the database when the app starts
    private void loadSongsFromDatabase() throws SQLException {
        List<Song> songs = dbManager.getAllSongs();
        libraryTable.getItems().addAll(songs);
    }
    
    private void toggleShuffle() {
        shuffle = !shuffle;
        if (shuffle) {
            Collections.shuffle(currentPlaylist);
        } else {
        // Restore original order
        currentPlaylist = new ArrayList<>(playlistView.getItems());
        }
        enablePauseButtonDisablePlayButton();
    }

    private void toggleRepeat() {
        repeat = !repeat;
    }
    
    private void setupPlaylistDragAndDrop() {
        playlistView.setCellFactory(lv -> {
            ListCell<Song> cell = new ListCell<Song>() {
                @Override
                protected void updateItem(Song item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.toString());
                }
            };
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(cell.getIndex()));
                    db.setContent(content);
                    event.consume();
                }
            });
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    ObservableList<Song> items = playlistView.getItems();
                    int draggedIdx = Integer.parseInt(db.getString());
                    int thisIdx = cell.getIndex();
                    Song draggedItem = items.remove(draggedIdx);
                    items.add(thisIdx, draggedItem);
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
            return cell;
        });
        playlistView.setOnDragDone(event -> {
            updatePlaylistOrder();
        });
    }

    private void updatePlaylistOrder() {
        try {
            dbManager.updatePlaylistOrder(currentPlaylistId, playlistView.getItems());
        } catch (Exception e) {
            AlertUtil.showErrorAlert("Database Error", "Failed to update playlist order: " + e.getMessage());
        }
    }
    
    private void importPlaylist() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Playlist");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Playlist Files", "*.m3u"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                dbManager.importPlaylist(file.getAbsolutePath());
                updatePlaylistComboBox();
                AlertUtil.showInfoAlert("Import Successful", "Playlist imported successfully.");
            } catch (Exception e) {
                AlertUtil.showErrorAlert("Import Error", "Failed to import playlist: " + e.getMessage());
            }
        }
    }

    private void exportPlaylist() {
        String selectedPlaylist = playlistComboBox.getValue();
        if (selectedPlaylist != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Playlist");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Playlist Files", "*.m3u"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    dbManager.exportPlaylist(selectedPlaylist, file.getAbsolutePath());
                    AlertUtil.showInfoAlert("Export Successful", "Playlist exported successfully.");
                } catch (Exception e) {
                    AlertUtil.showErrorAlert("Export Error", "Failed to export playlist: " + e.getMessage());
                }
            }
        } else {
            AlertUtil.showErrorAlert("Export Error", "Please select a playlist to export.");
        }
    }
    
    private void updateRecentFilesMenu() {
        recentFilesMenu.getItems().clear();
        for (String filePath : recentFiles) {
            MenuItem item = new MenuItem(new File(filePath).getName());
            item.setOnAction(e -> loadRecentFile(filePath));
            recentFilesMenu.getItems().add(item);
        }
}

    private void addRecentFile(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        updateRecentFilesMenu();
    }

    private void loadRecentFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            Song song = createSongFromFile(file);
            libraryTable.getItems().add(song);
            enablePauseButtonDisablePlayButton();
            playSong(song);
            addRecentFile(filePath);
        } else {
            AlertUtil.showErrorAlert("File not found", "The file " + filePath + " don't exist.");
            recentFiles.remove(filePath);
            updateRecentFilesMenu();
        }
    }
    
    private void saveRecentFiles() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(RECENT_FILES_FILENAME))) {
            for (String filePath : recentFiles) {
                writer.write(filePath);
                writer.newLine();
            }
        } catch (IOException e) {
            AlertUtil.showErrorAlert("Error", "Impossible to save the list of recents files.");
        }
    }
    
    private void loadRecentFiles() {
        recentFiles.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(RECENT_FILES_FILENAME))) {
            String line;
            while ((line = reader.readLine()) != null && recentFiles.size() < MAX_RECENT_FILES) {
                recentFiles.add(line);
            }
        } catch (IOException e) {
            // Le fichier n'existe pas encore ou ne peut pas être lu, ce n'est pas une erreur critique
        }
        updateRecentFilesMenu();
    }
    
    private void adjustPlaybackSpeed() {
        Slider speedSlider = new Slider(0.5, 2.0, 1.0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);

        speedSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setRate(newValue.doubleValue());
            }
        });

        VBox dialogVbox = new VBox(10);
        dialogVbox.getChildren().add(new Label("Adjust playback speed"));
        dialogVbox.getChildren().add(speedSlider);

        Dialog<Void> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(dialogVbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

        private void showEqualizer() {
            Stage equalizerStage = new Stage();
            equalizerStage.setTitle("Égaliseur");

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(10, 10, 10, 10));
            grid.setVgap(10);
            grid.setHgap(10);

            String[] bands = {"60Hz", "170Hz", "310Hz", "600Hz", "1kHz", "3kHz", "6kHz", "12kHz", "14kHz", "16kHz"};
            List<Slider> sliders = new ArrayList<>();

            for (int i = 0; i < bands.length; i++) {
                Label bandLabel = new Label(bands[i]);
                Slider slider = new Slider(-12, 12, 0);
                slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                slider.setMajorTickUnit(6);

                int bandIndex = i;
                slider.valueProperty().addListener((obs, oldVal, newVal) -> 
                    adjustEqualizerBand(bandIndex, newVal.doubleValue()));

                grid.add(bandLabel, i, 0);
                grid.add(slider, i, 1);
                sliders.add(slider);
            }

            Button resetButton = new Button("Reset");
            resetButton.setOnAction(e -> sliders.forEach(slider -> slider.setValue(0)));

            Button saveButton = new Button("Save");
            saveButton.setOnAction(e -> saveEqualizerSettings(sliders));

            Button okButton = new Button("OK");
            okButton.setOnAction(e -> equalizerStage.close());

            HBox buttonBox = new HBox(10);
            buttonBox.getChildren().addAll(resetButton, saveButton, okButton);
            buttonBox.setAlignment(Pos.CENTER);

            VBox mainLayout = new VBox(10);
            mainLayout.getChildren().addAll(grid, buttonBox);

            Scene scene = new Scene(mainLayout);
            equalizerStage.setScene(scene);
            equalizerStage.show();
        }

    private void saveEqualizerSettings(List<Slider> sliders) {
        Properties properties = new Properties();

        // Save each slider value
        for (int i = 0; i < sliders.size(); i++) {
            properties.setProperty("equalizer.band." + i, String.valueOf(sliders.get(i).getValue()));
        }
        // Define the file path
        String userHome = System.getProperty("user.home");
        String configFilePath = userHome + File.separator + "musicplayer_equalizer.properties";
        try (FileOutputStream out = new FileOutputStream(configFilePath)) {
            properties.store(out, "Equalizer Settings");
            System.out.println("Equalizer settings saved to: " + configFilePath);
        } catch (IOException e) {
            System.err.println("Error saving equalizer settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void adjustEqualizerBand(int bandIndex, double value) {
        if (mediaPlayer != null) {
            AudioEqualizer equalizer = mediaPlayer.getAudioEqualizer();
            EqualizerBand band = equalizer.getBands().get(bandIndex);
            band.setGain(value);
            System.out.println("Ajustement de la bande " + bandIndex + " à " + value + " dB");
        }
    }
    
    private void showAudioVisualization() {
    Stage visualizationStage = new Stage();
    visualizationStage.setTitle("Visualisation Audio");

    final int BANDS = 128;
    final AudioSpectrumListener spectrumListener = (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
        // Store the magnitudes
        System.arraycopy(magnitudes, 0, this.magnitudes, 0, Math.min(magnitudes.length, this.magnitudes.length));
};

    if (mediaPlayer != null) {
        mediaPlayer.setAudioSpectrumListener(spectrumListener);
        mediaPlayer.setAudioSpectrumInterval(0.1);
        mediaPlayer.setAudioSpectrumNumBands(BANDS);
        mediaPlayer.setAudioSpectrumThreshold(-100);
    }

    Rectangle[] rectangles = new Rectangle[BANDS];
    Group group = new Group();
    for (int i = 0; i < BANDS; i++) {
        Rectangle rect = new Rectangle(5, 10, Color.RED);
        rect.setX(i * 6);
        rect.setY(150);
        group.getChildren().add(rect);
        rectangles[i] = rect;
    }

    Scene scene = new Scene(group, BANDS * 6, 200);
    visualizationStage.setScene(scene);
    visualizationStage.show();

    visualizationStage.setOnCloseRequest(event -> {
        if (mediaPlayer != null) {
            mediaPlayer.setAudioSpectrumListener(null);
        }
    });
    
    AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            for (int i = 0; i < BANDS; i++) {
                double magnitude = magnitudes[i];
                rectangles[i].setHeight(Math.max(10, magnitude + 60));
                rectangles[i].setY(150 - rectangles[i].getHeight());
            }
        }
    };
    timer.start();
}
    
    private void showAboutInfo() {
    AlertUtil.showInfoAlert("About", "Music Player App\nVersion 1.0\nDeveloped by Wess");
}
    
    private void quit() {
        saveRecentFiles();
        Platform.exit();
        System.exit(0);
}

    public static void main(String[] args) {
        launch(args);
    }
}