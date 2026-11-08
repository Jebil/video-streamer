package net.jk.app.videostreamer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import net.jk.app.videostreamer.dto.MovieResponseDto;
import net.jk.app.videostreamer.mapper.MovieMapper;
import net.jk.app.videostreamer.model.Movie;
import net.jk.app.videostreamer.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

class PageControllerTest {

  private MovieService movieService;
  private MovieMapper movieMapper;
  private PageController pageController;

  @BeforeEach
  void setUp() {
    movieService = mock(MovieService.class);
    movieMapper = mock(MovieMapper.class);
    pageController = new PageController(movieService, movieMapper);
  }

  @Test
  void shouldPopulateModelAndReturnIndexView() {
    List<Movie> movies = List.of(Movie.builder().title("Inception").path("Inception.mp4").build());
    List<MovieResponseDto> dtos =
        List.of(MovieResponseDto.builder().title("Inception").path("Inception.mp4").build());

    when(movieService.getMoviesList()).thenReturn(movies);
    when(movieMapper.map(movies)).thenReturn(dtos);

    Model model = new ConcurrentModel();
    String viewName = pageController.index(model);

    assertEquals("index", viewName);
    assertEquals(dtos, model.getAttribute("videos"));
  }

  @Test
  void shouldPopulateModelAndReturnWatchView() {
    Model model = new ConcurrentModel();
    String viewName = pageController.watch("/action/fast.mp4", model);

    assertEquals("video", viewName);
    assertEquals("action/fast.mp4", model.getAttribute("videoPath"));
  }

  @Test
  void shouldRefreshCacheAndRedirectToIndex() {
    String viewName = pageController.refresh();

    verify(movieService).refreshCache();
    assertEquals("redirect:/", viewName);
  }
}
