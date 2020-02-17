package net.jk.app.videostreamer.service;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.jk.app.videostreamer.model.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MovieService {

  @Value("${video.location}")
  private String videoLocation;

  private static final Escaper ESCAPER = UrlEscapers.urlPathSegmentEscaper();

  private static final List<MediaType> MEDIA_TYPES = new ArrayList<>();

  @PostConstruct
  private void init() {
    MEDIA_TYPES.add(MediaType.valueOf("video/x-matroska"));
    MEDIA_TYPES.add(MediaType.valueOf("video/x-msvideo"));
    MEDIA_TYPES.add(MediaType.valueOf("video/x-ms-wmv"));
    MEDIA_TYPES.add(MediaType.valueOf("video/mp4"));
    MEDIA_TYPES.add(MediaType.valueOf("video/x-ms-vob"));
  }

  public List<Movie> getMoviesList() {
    log.info("Video location {} ", videoLocation);
    try {
      return Files.walk(Paths.get(videoLocation))
          .filter(Files::isRegularFile)
          .filter(isVideoFile)
          .map(createMovie)
          .collect(Collectors.toList());
    } catch (IOException e) {
      // TODO throw custom exception
      return Collections.emptyList();
    }
  }

  public String escapeUrlPath(String videoName) {
    return ESCAPER.escape(videoName);
  }

  private Predicate<Path> isVideoFile =
      (path) -> {
        MediaType mediaType =
            MediaTypeFactory.getMediaType(path.getFileName().toString()).orElse(null);
        return MEDIA_TYPES.contains(mediaType);
      };

  private Function<Path, Movie> createMovie =
      (path) -> {
        return Movie.builder()
            .title(path.getFileName().toString())
            .path(ESCAPER.escape(path.toAbsolutePath().toString()))
            .build();
      };
}
