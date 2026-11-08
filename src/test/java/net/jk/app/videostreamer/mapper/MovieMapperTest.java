package net.jk.app.videostreamer.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.jk.app.videostreamer.dto.MovieResponseDto;
import net.jk.app.videostreamer.model.Movie;
import org.junit.jupiter.api.Test;

class MovieMapperTest {

  private final MovieMapper mapper = new MovieMapper();

  @Test
  void shouldMapMovieToDto() {
    Movie movie = Movie.builder().title("Inception.mp4").path("Inception.mp4").build();
    MovieResponseDto dto = mapper.apply(movie);

    assertNotNull(dto);
    assertEquals("Inception.mp4", dto.getTitle());
    assertEquals("Inception.mp4", dto.getPath());
  }

  @Test
  void shouldMapListOfMovies() {
    List<Movie> movies =
        List.of(
            Movie.builder().title("Movie 1").path("path1").build(),
            Movie.builder().title("Movie 2").path("path2").build());

    List<MovieResponseDto> dtos = mapper.map(movies);

    assertEquals(2, dtos.size());
    assertEquals("Movie 1", dtos.get(0).getTitle());
    assertEquals("Movie 2", dtos.get(1).getTitle());
  }

  @Test
  void shouldHandleNullInput() {
    assertNull(mapper.apply(null));
  }
}
