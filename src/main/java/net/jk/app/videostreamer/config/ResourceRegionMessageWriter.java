package net.jk.app.videostreamer.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

public class ResourceRegionMessageWriter implements HttpMessageWriter<ResourceRegion> {

  private ResourceRegionEncoder regionEncoder = new ResourceRegionEncoder();

  private ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);

  private List<MediaType> mediaTypes =
      MediaType.asMediaTypes(regionEncoder.getEncodableMimeTypes());

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return mediaTypes;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
    return regionEncoder.canEncode(elementType, mediaType);
  }

  @Override
  public Mono<Void> write(
      Publisher<? extends ResourceRegion> inputStream,
      ResolvableType elementType,
      MediaType mediaType,
      ReactiveHttpOutputMessage message,
      Map<String, Object> hints) {
    // TODO Auto-generated method stub
    return Mono.empty();
  }

  public Mono<Void> write(
      Publisher<? extends ResourceRegion> inputStream,
      ResolvableType actualType,
      ResolvableType elementType,
      @Nullable MediaType mediaType,
      ServerHttpRequest request,
      ServerHttpResponse response,
      Map<String, Object> hints) {

    var headers = response.getHeaders();
    headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
    return Mono.from(inputStream)
        .flatMap(
            resourceRegion -> {
              response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
              var resourceMediaType = getResourceMediaType(mediaType, resourceRegion.getResource());
              headers.setContentType(resourceMediaType);
              long contentLength = 0;
              try {
                contentLength = resourceRegion.getResource().contentLength();
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              var start = resourceRegion.getPosition();
              var end = Math.min(start + resourceRegion.getCount() - 1, contentLength - 1);
              headers.add("Content-Range", "bytes " + start + '-' + end + '/' + contentLength);
              headers.setContentLength(end - start + 1);

              return zeroCopy(resourceRegion.getResource(), resourceRegion, response)
                  .orElseGet(
                      () -> {
                        var input = Mono.just(resourceRegion);
                        var body =
                            this.regionEncoder.encode(
                                input,
                                response.bufferFactory(),
                                REGION_TYPE,
                                resourceMediaType,
                                Collections.emptyMap());
                        return response.writeWith(body);
                      });
            });
  }

  public MediaType getResourceMediaType(MediaType mediaType, Resource resource) {
    return (mediaType != null
            && mediaType.isConcrete()
            && mediaType != MediaType.APPLICATION_OCTET_STREAM)
        ? mediaType
        : MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
  }

  public Optional<Mono<Void>> zeroCopy(
      Resource resource, ResourceRegion region, ReactiveHttpOutputMessage message) {
    if (message instanceof ZeroCopyHttpOutputMessage && resource.isFile()) {
      try {
        var file = resource.getFile();
        var pos = region.getPosition();
        var count = region.getCount();
        ZeroCopyHttpOutputMessage msg = (ZeroCopyHttpOutputMessage) message;
        return Optional.of(msg.writeWith(file, pos, count));
      } catch (IOException ex) {
        // should not happen
      }
    }
    return Optional.empty();
  }
}
