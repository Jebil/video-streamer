package net.jk.app.videostreamer.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import net.jk.app.videostreamer.mapper.MovieMapper;
import net.jk.app.videostreamer.model.Movie;
import net.jk.app.videostreamer.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller(value = "/")
public class PageController {

  @Autowired private MovieService svc;

  @Autowired private MovieMapper mapper;

  @GetMapping("/")
  public String index(Model model) throws IOException {
    // getting all of the files in video folder
    List<Movie> movies = svc.getMoviesList();
    model.addAttribute("videos", mapper.map(movies));
    return "index";
  }

  @GetMapping("/{videoName}")
  public String video(@PathVariable String videoName, Model model)
      throws UnsupportedEncodingException {
    String encodedPath = svc.escapeUrlPath(videoName);
    model.addAttribute("videoName", encodedPath);
    return "video";
  }
}
