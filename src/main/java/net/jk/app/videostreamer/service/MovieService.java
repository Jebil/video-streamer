package net.jk.app.videostreamer.service;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import net.jk.app.videostreamer.model.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MovieService {

  private final String videoLocation;
  private final Path basePath;

  private static final Escaper ESCAPER = UrlEscapers.urlPathSegmentEscaper();
  private static final long CACHE_TTL_MS = 60_000L; // 60-second in-memory cache

  private volatile List<Movie> cachedMovies = null;
  private volatile long lastCacheTime = 0L;

  private static final Set<String> SUPPORTED_EXTENSIONS =
      Set.of(".mp4", ".mkv", ".avi", ".wmv", ".vob", ".mov", ".webm", ".flv", ".m4v");

  private static final Set<MediaType> SUPPORTED_MEDIA_TYPES =
      Set.of(
          MediaType.valueOf("video/mp4"),
          MediaType.valueOf("video/x-matroska"),
          MediaType.valueOf("video/x-msvideo"),
          MediaType.valueOf("video/x-ms-wmv"),
          MediaType.valueOf("video/x-ms-vob"),
          MediaType.valueOf("video/webm"),
          MediaType.valueOf("video/quicktime"));

  public MovieService(@Value("${video.location}") String videoLocation) {
    this.videoLocation = videoLocation;
    this.basePath = Paths.get(videoLocation).toAbsolutePath().normalize();
  }

  public List<Movie> getMoviesList() {
    long now = System.currentTimeMillis();
    List<Movie> currentCache = cachedMovies;
    if (currentCache != null && (now - lastCacheTime) < CACHE_TTL_MS) {
      log.debug("Returning cached movie list ({} movies)", currentCache.size());
      return currentCache;
    }

    synchronized (this) {
      if (cachedMovies != null && (System.currentTimeMillis() - lastCacheTime) < CACHE_TTL_MS) {
        return cachedMovies;
      }
      cachedMovies = scanMoviesFromDisk();
      lastCacheTime = System.currentTimeMillis();
      return cachedMovies;
    }
  }

  public void refreshCache() {
    synchronized (this) {
      cachedMovies = scanMoviesFromDisk();
      lastCacheTime = System.currentTimeMillis();
      log.info("Movie cache manually refreshed: {} movies found", cachedMovies.size());
    }
  }

  private List<Movie> scanMoviesFromDisk() {
    log.info("Scanning for videos in: {}", basePath);
    if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
      log.warn("Configured video location does not exist or is not a directory: {}", basePath);
      return Collections.emptyList();
    }

    try (Stream<Path> stream = Files.walk(basePath)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(this::isVideoFile)
          .map(this::createMovie)
          .sorted((m1, m2) -> m1.getTitle().compareToIgnoreCase(m2.getTitle()))
          .toList();
    } catch (IOException e) {
      log.error("Failed to read video directory: {}", basePath, e);
      return Collections.emptyList();
    }
  }

  public Optional<Path> resolveVideoPath(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return Optional.empty();
    }

    String sanitized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
    Path resolved = basePath.resolve(sanitized).normalize();

    // Security check: Guard against Path Traversal attacks
    if (!resolved.startsWith(basePath)) {
      log.warn("Blocked potential path traversal attempt: requested '{}'", relativePath);
      throw new SecurityException("Path traversal attempt detected");
    }

    if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
      log.debug("Video file not found or not readable: {}", resolved);
      return Optional.empty();
    }

    return Optional.of(resolved);
  }

  public String escapeUrlPath(String videoName) {
    return ESCAPER.escape(videoName);
  }

  private boolean isVideoFile(Path path) {
    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    boolean hasVideoExt = SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    if (hasVideoExt) {
      return true;
    }

    return MediaTypeFactory.getMediaType(path.getFileName().toString())
        .map(SUPPORTED_MEDIA_TYPES::contains)
        .orElse(false);
  }

  private Movie createMovie(Path path) {
    Path relative = basePath.relativize(path.toAbsolutePath().normalize());
    String relativePathString = relative.toString().replace('\\', '/');
    String fileName = path.getFileName().toString();
    String extension = extractExtension(fileName);

    long size = 0L;
    try {
      size = Files.size(path);
    } catch (IOException ignored) {
    }

    return Movie.builder()
        .title(fileName)
        .path(relativePathString)
        .extension(extension)
        .sizeBytes(size)
        .formattedSize(formatFileSize(size))
        .build();
  }

  private String extractExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
      return fileName.substring(dotIndex + 1).toUpperCase(Locale.ROOT);
    }
    return "UNKNOWN";
  }

  private String formatFileSize(long bytes) {
    if (bytes <= 0) {
      return "0 B";
    }
    final String[] units = new String[] {"B", "KB", "MB", "GB", "TB"};
    int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
    digitGroups = Math.min(digitGroups, units.length - 1);
    return String.format(
        Locale.ROOT, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
  }
}
