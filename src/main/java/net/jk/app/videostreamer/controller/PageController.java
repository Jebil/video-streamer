package net.jk.app.videostreamer.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.jk.app.videostreamer.mapper.MovieMapper;
import net.jk.app.videostreamer.model.Movie;
import net.jk.app.videostreamer.service.MovieService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class PageController {

  private final MovieService movieService;
  private final MovieMapper movieMapper;

  @GetMapping("/")
  public String index(Model model) {
    List<Movie> movies = movieService.getMoviesList();
    model.addAttribute("videos", movieMapper.map(movies));
    return "index";
  }

  @GetMapping("/refresh")
  public String refresh() {
    movieService.refreshCache();
    return "redirect:/";
  }

  @GetMapping("/watch/{*videoPath}")
  public String watch(@PathVariable String videoPath, Model model) {
    String sanitized = videoPath.startsWith("/") ? videoPath.substring(1) : videoPath;
    model.addAttribute("videoPath", sanitized);
    return "video";
  }

  @GetMapping(value = "/playlist.m3u", produces = "audio/x-mpegurl;charset=UTF-8")
  @ResponseBody
  public String fullPlaylist(ServerHttpRequest request) {
    String hostHeader = request.getHeaders().getFirst(HttpHeaders.HOST);
    String baseUrl = (request.getSslInfo() != null ? "https://" : "http://") + hostHeader;
    StringBuilder sb = new StringBuilder("#EXTM3U\n");
    for (Movie movie : movieService.getMoviesList()) {
      sb.append("#EXTINF:-1,").append(movie.getTitle()).append("\n");
      sb.append(baseUrl).append("/videos/").append(movie.getPath()).append("\n");
    }
    return sb.toString();
  }

  @GetMapping(value = "/videos/m3u/{*videoPath}", produces = "audio/x-mpegurl;charset=UTF-8")
  @ResponseBody
  public ResponseEntity<String> moviePlaylist(
      @PathVariable String videoPath, ServerHttpRequest request) {
    String sanitized = videoPath.startsWith("/") ? videoPath.substring(1) : videoPath;
    String hostHeader = request.getHeaders().getFirst(HttpHeaders.HOST);
    String baseUrl = (request.getSslInfo() != null ? "https://" : "http://") + hostHeader;
    String content =
        "#EXTM3U\n#EXTINF:-1," + sanitized + "\n" + baseUrl + "/videos/" + sanitized + "\n";

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + sanitized.replace('/', '_') + ".m3u\"")
        .body(content);
  }
}
