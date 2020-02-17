package net.jk.app.videostreamer.mapper;

import net.jk.app.videostreamer.dto.MovieResponseDto;
import net.jk.app.videostreamer.model.Movie;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper implements GenericMapper<Movie, MovieResponseDto> {

  @Override
  public MovieResponseDto apply(Movie source) {
    MovieResponseDto target = new MovieResponseDto();
    BeanUtils.copyProperties(source, target);
    return target;
  }
}
