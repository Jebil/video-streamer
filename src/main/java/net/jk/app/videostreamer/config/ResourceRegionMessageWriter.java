package net.jk.app.videostreamer.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

@Slf4j
public class ResourceRegionMessageWriter implements HttpMessageWriter<ResourceRegion> {

  private final ResourceRegionEncoder regionEncoder = new ResourceRegionEncoder();
  private final ResolvableType regionType = ResolvableType.forClass(ResourceRegion.class);
  private final List<MediaType> mediaTypes =
      MediaType.asMediaTypes(regionEncoder.getEncodableMimeTypes(regionType));

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return mediaTypes;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    return regionEncoder.canEncode(elementType, mediaType);
  }

  @Override
  public Mono<Void> write(
      Publisher<? extends ResourceRegion> inputStream,
      ResolvableType elementType,
      @Nullable MediaType mediaType,
      ReactiveHttpOutputMessage message,
      Map<String, Object> hints) {

    return Mono.from(inputStream)
        .flatMap(resourceRegion -> {
          MediaType resourceMediaType =
              getResourceMediaType(mediaType, resourceRegion.getResource());
          var body =
              this.regionEncoder.encode(
                  Mono.just(resourceRegion),
                  message.bufferFactory(),
                  regionType,
                  resourceMediaType,
                  Collections.emptyMap());
          return message.writeWith(body);
        });
  }

  @Override
  public Mono<Void> write(
      Publisher<? extends ResourceRegion> inputStream,
      ResolvableType actualType,
      ResolvableType elementType,
      @Nullable MediaType mediaType,
      ServerHttpRequest request,
      ServerHttpResponse response,
      Map<String, Object> hints) {

    HttpHeaders headers = response.getHeaders();
    headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

    return Mono.from(inputStream)
        .flatMap(resourceRegion -> {
          response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
          MediaType resourceMediaType =
              getResourceMediaType(mediaType, resourceRegion.getResource());
          headers.setContentType(resourceMediaType);

          long contentLength = 0;
          try {
            contentLength = resourceRegion.getResource().contentLength();
          } catch (IOException e) {
            log.error("Could not obtain content length for resource region", e);
          }

          long start = resourceRegion.getPosition();
          long end = Math.min(start + resourceRegion.getCount() - 1, contentLength - 1);
          headers.add(
              HttpHeaders.CONTENT_RANGE, "bytes " + start + '-' + end + '/' + contentLength);
          headers.setContentLength(end - start + 1);

          var body =
              this.regionEncoder.encode(
                  Mono.just(resourceRegion),
                  response.bufferFactory(),
                  regionType,
                  resourceMediaType,
                  Collections.emptyMap());
          return response.writeWith(body);
        });
  }

  private MediaType getResourceMediaType(@Nullable MediaType mediaType, Resource resource) {
    return (mediaType != null
            && mediaType.isConcrete()
            && mediaType != MediaType.APPLICATION_OCTET_STREAM)
        ? mediaType
        : MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
  }
}
