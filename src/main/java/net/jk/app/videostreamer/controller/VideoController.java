package net.jk.app.videostreamer.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VideoController {
  @Value("${video.location}")
  private String videoLocation;

  private final long ChunkSize = 1000000L;

  @GetMapping("/videos/{name}/full")
  public ResponseEntity<UrlResource> getFullVideo(
      @PathVariable String name, @RequestHeader HttpHeaders headers) throws MalformedURLException {
    UrlResource video = new UrlResource(Paths.get(name).toUri());
    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .contentType(
            MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
        .body(video);
  }

  @GetMapping("/videos/{name}")
  public ResponseEntity<ResourceRegion> getVideo(
      @PathVariable String name, @RequestHeader HttpHeaders headers) throws IOException {
    var video = new UrlResource(Paths.get(name).toUri());
    var region = resourceRegion(video, headers);
    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .contentType(
            MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
        .body(region);
  }

  private ResourceRegion resourceRegion(UrlResource video, HttpHeaders headers) throws IOException {
    var contentLength = video.contentLength();
    var range = headers.getRange().stream().findFirst().orElse(null);
    if (range != null) {
      var start = range.getRangeStart(contentLength);
      var end = range.getRangeEnd(contentLength);
      var rangeLength = Long.min(ChunkSize, end - start + 1);
      return new ResourceRegion(video, start, rangeLength);
    } else {
      var rangeLength = Long.min(ChunkSize, contentLength);
      return new ResourceRegion(video, 0, rangeLength);
    }
  }
}
