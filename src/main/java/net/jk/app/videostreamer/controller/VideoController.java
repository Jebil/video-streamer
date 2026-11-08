package net.jk.app.videostreamer.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jk.app.videostreamer.service.MovieService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class VideoController {

  private final MovieService movieService;

  private static final long CHUNK_SIZE = 1_000_000L; // 1 MB chunk

  @GetMapping("/videos/full/{*name}")
  public ResponseEntity<Resource> getFullVideo(
      @PathVariable String name, @RequestHeader(required = false) HttpHeaders headers) {
    try {
      String sanitized = name != null && name.startsWith("/") ? name.substring(1) : name;
      Optional<Path> resolved = movieService.resolveVideoPath(sanitized);
      if (resolved.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      Path filePath = resolved.get();
      Resource video = new FileSystemResource(filePath);
      MediaType mediaType =
          MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);

      ContentDisposition disposition =
          ContentDisposition.attachment().filename(filePath.getFileName().toString()).build();

      return ResponseEntity.ok()
          .header(HttpHeaders.ACCEPT_RANGES, "bytes")
          .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
          .contentType(mediaType)
          .body(video);
    } catch (SecurityException e) {
      log.warn("Access denied during full video request for '{}': {}", name, e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @GetMapping("/videos/{*name}")
  public ResponseEntity<ResourceRegion> getVideo(
      @PathVariable String name, @RequestHeader(required = false) HttpHeaders headers)
      throws IOException {
    try {
      String sanitized = name != null && name.startsWith("/") ? name.substring(1) : name;
      Optional<Path> resolved = movieService.resolveVideoPath(sanitized);
      if (resolved.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      Resource video = new FileSystemResource(resolved.get());
      long contentLength = video.contentLength();
      HttpHeaders reqHeaders = headers != null ? headers : new HttpHeaders();
      HttpRange range = reqHeaders.getRange().stream().findFirst().orElse(null);

      if (range != null && range.getRangeStart(contentLength) >= contentLength) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
            .build();
      }

      ResourceRegion region = resourceRegion(video, contentLength, range);
      MediaType mediaType =
          MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);

      return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
          .header(HttpHeaders.ACCEPT_RANGES, "bytes")
          .contentType(mediaType)
          .body(region);
    } catch (SecurityException e) {
      log.warn("Access denied during chunked video request for '{}': {}", name, e.getMessage());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  private ResourceRegion resourceRegion(Resource video, long contentLength, HttpRange range) {
    if (range != null) {
      long start = range.getRangeStart(contentLength);
      long end = range.getRangeEnd(contentLength);
      long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
      return new ResourceRegion(video, start, rangeLength);
    } else {
      long rangeLength = Math.min(CHUNK_SIZE, contentLength);
      return new ResourceRegion(video, 0, rangeLength);
    }
  }
}
