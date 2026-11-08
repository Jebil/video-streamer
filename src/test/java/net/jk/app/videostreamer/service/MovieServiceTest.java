package net.jk.app.videostreamer.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.jk.app.videostreamer.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MovieServiceTest {

  @TempDir Path tempDir;

  private MovieService movieService;

  @BeforeEach
  void setUp() {
    movieService = new MovieService(tempDir.toString());
  }

  @Test
  void shouldReturnEmptyListWhenDirectoryIsEmpty() {
    List<Movie> movies = movieService.getMoviesList();
    assertTrue(movies.isEmpty());
  }

  @Test
  void shouldFindVideoFilesAndIgnoreNonVideoFiles() throws IOException {
    Files.createFile(tempDir.resolve("movie1.mp4"));
    Files.createFile(tempDir.resolve("document.pdf"));
    Files.createFile(tempDir.resolve("movie2.mkv"));
    Files.createFile(tempDir.resolve("song.mp3"));

    Path subDir = Files.createDirectory(tempDir.resolve("subfolder"));
    Files.createFile(subDir.resolve("sub_movie.avi"));

    List<Movie> movies = movieService.getMoviesList();

    assertEquals(3, movies.size());
    List<String> titles = movies.stream().map(Movie::getTitle).toList();
    assertTrue(titles.contains("movie1.mp4"));
    assertTrue(titles.contains("movie2.mkv"));
    assertTrue(titles.contains("sub_movie.avi"));

    Optional<Movie> subMovie =
        movies.stream().filter(m -> m.getTitle().equals("sub_movie.avi")).findFirst();
    assertTrue(subMovie.isPresent());
    assertEquals("subfolder/sub_movie.avi", subMovie.get().getPath());
    assertEquals("AVI", subMovie.get().getExtension());
  }

  @Test
  void shouldCacheResultsAndSupportManualRefresh() throws IOException {
    Files.createFile(tempDir.resolve("initial.mp4"));
    List<Movie> firstCall = movieService.getMoviesList();
    assertEquals(1, firstCall.size());

    // Add another file without refresh; cache should still return 1
    Files.createFile(tempDir.resolve("second.mp4"));
    List<Movie> cachedCall = movieService.getMoviesList();
    assertEquals(1, cachedCall.size());

    // Call refreshCache
    movieService.refreshCache();
    List<Movie> refreshedCall = movieService.getMoviesList();
    assertEquals(2, refreshedCall.size());
  }

  @Test
  void shouldHandleNonExistentDirectoryGracefully() {
    MovieService invalidService =
        new MovieService(tempDir.resolve("non_existent_folder").toString());
    List<Movie> movies = invalidService.getMoviesList();
    assertNotNull(movies);
    assertTrue(movies.isEmpty());
  }

  @Test
  void shouldResolveExistingVideoPath() throws IOException {
    Path video = Files.createFile(tempDir.resolve("sample.mp4"));

    Optional<Path> resolved = movieService.resolveVideoPath("sample.mp4");
    assertTrue(resolved.isPresent());
    assertEquals(video.toAbsolutePath().normalize(), resolved.get());
  }

  @Test
  void shouldReturnEmptyForNonExistentVideo() {
    Optional<Path> resolved = movieService.resolveVideoPath("missing.mp4");
    assertTrue(resolved.isEmpty());
  }

  @Test
  void shouldPreventPathTraversalAttack() {
    assertThrows(
        SecurityException.class,
        () -> movieService.resolveVideoPath("../../sensitive_system_file.txt"));
  }
}
