package net.jk.app.videostreamer.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.jk.app.videostreamer.mapper.MovieMapper;
import net.jk.app.videostreamer.model.Movie;
import net.jk.app.videostreamer.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
}
