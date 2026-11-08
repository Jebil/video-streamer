package net.jk.app.videostreamer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;
import net.jk.app.videostreamer.dto.MovieResponseDto;
import net.jk.app.videostreamer.mapper.MovieMapper;
import net.jk.app.videostreamer.model.Movie;
import net.jk.app.videostreamer.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
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

  @Test
  void shouldGenerateFullM3uPlaylist() {
    List<Movie> movies =
        List.of(
            Movie.builder().title("Movie 1").path("action/m1.mp4").build(),
            Movie.builder().title("Movie 2").path("comedy/m2.mkv").build());
    when(movieService.getMoviesList()).thenReturn(movies);

    MockServerHttpRequest request =
        MockServerHttpRequest.get("/playlist.m3u")
            .header(HttpHeaders.HOST, "192.168.0.17:8085")
            .build();

    String playlist = pageController.fullPlaylist(request);

    assertTrue(playlist.startsWith("#EXTM3U\n"));
    assertTrue(
        playlist.contains("#EXTINF:-1,Movie 1\nhttp://192.168.0.17:8085/videos/action/m1.mp4\n"));
    assertTrue(
        playlist.contains("#EXTINF:-1,Movie 2\nhttp://192.168.0.17:8085/videos/comedy/m2.mkv\n"));
  }

  @Test
  void shouldGenerateSingleMovieM3uPlaylist() {
    MockServerHttpRequest request =
        MockServerHttpRequest.get("/videos/m3u/action/m1.mp4")
            .header(HttpHeaders.HOST, "192.168.0.17:8085")
            .build();

    ResponseEntity<String> response = pageController.moviePlaylist("action/m1.mp4", request);

    assertEquals(200, response.getStatusCode().value());
    assertTrue(response.getBody().contains("http://192.168.0.17:8085/videos/action/m1.mp4"));
    assertTrue(
        response
            .getHeaders()
            .getFirst(HttpHeaders.CONTENT_DISPOSITION)
            .contains("action_m1.mp4.m3u"));
  }
}
