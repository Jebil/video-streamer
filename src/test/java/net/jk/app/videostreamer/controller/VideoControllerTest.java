package net.jk.app.videostreamer.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.jk.app.videostreamer.config.ResourceRegionMessageWriter;
import net.jk.app.videostreamer.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

class VideoControllerTest {

  @TempDir Path tempDir;

  private MovieService movieService;
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    movieService = mock(MovieService.class);
    VideoController controller = new VideoController(movieService);

    client =
        WebTestClient.bindToController(controller)
            .httpMessageCodecs(
                configurer -> configurer.customCodecs().register(new ResourceRegionMessageWriter()))
            .build();
  }

  @Test
  void shouldReturn404WhenVideoNotFound() {
    when(movieService.resolveVideoPath("missing.mp4")).thenReturn(Optional.empty());

    client.get().uri("/videos/missing.mp4").exchange().expectStatus().isNotFound();
  }

  @Test
  void shouldReturn403WhenPathTraversalDetected() {
    when(movieService.resolveVideoPath(anyString()))
        .thenThrow(new SecurityException("Path traversal attempt"));

    client.get().uri("/videos/../../etc/passwd").exchange().expectStatus().isForbidden();
  }

  @Test
  void shouldReturnPartialContentForRangeRequest() throws IOException {
    Path video = tempDir.resolve("test.mp4");
    byte[] data = new byte[2000];
    Files.write(video, data);

    when(movieService.resolveVideoPath("test.mp4")).thenReturn(Optional.of(video));

    client
        .get()
        .uri("/videos/test.mp4")
        .header(HttpHeaders.RANGE, "bytes=0-499")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.PARTIAL_CONTENT)
        .expectHeader()
        .valueEquals(HttpHeaders.ACCEPT_RANGES, "bytes")
        .expectHeader()
        .valueEquals(HttpHeaders.CONTENT_RANGE, "bytes 0-499/2000")
        .expectHeader()
        .contentLength(500L);
  }

  @Test
  void shouldReturn416WhenRangeExceedsContentLength() throws IOException {
    Path video = tempDir.resolve("test.mp4");
    byte[] data = new byte[100];
    Files.write(video, data);

    when(movieService.resolveVideoPath("test.mp4")).thenReturn(Optional.of(video));

    client
        .get()
        .uri("/videos/test.mp4")
        .header(HttpHeaders.RANGE, "bytes=500-600")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
        .expectHeader()
        .valueEquals(HttpHeaders.CONTENT_RANGE, "bytes */100");
  }

  @Test
  void shouldReturnFullVideoWithAttachmentHeader() throws IOException {
    Path video = tempDir.resolve("test.mp4");
    byte[] data = new byte[100];
    Files.write(video, data);

    when(movieService.resolveVideoPath("test.mp4")).thenReturn(Optional.of(video));

    client
        .get()
        .uri("/videos/full/test.mp4")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(HttpHeaders.ACCEPT_RANGES, "bytes")
        .expectHeader()
        .valueMatches(HttpHeaders.CONTENT_DISPOSITION, ".*filename=\"?test\\.mp4\"?.*");
  }
}
