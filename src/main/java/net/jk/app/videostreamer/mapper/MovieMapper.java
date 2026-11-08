package net.jk.app.videostreamer.mapper;

import net.jk.app.videostreamer.dto.MovieResponseDto;
import net.jk.app.videostreamer.model.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper implements GenericMapper<Movie, MovieResponseDto> {

  @Override
  public MovieResponseDto apply(Movie source) {
    if (source == null) {
      return null;
    }
    return MovieResponseDto.builder()
        .title(source.getTitle())
        .path(source.getPath())
        .extension(source.getExtension())
        .formattedSize(source.getFormattedSize())
        .sizeBytes(source.getSizeBytes())
        .build();
  }
}
