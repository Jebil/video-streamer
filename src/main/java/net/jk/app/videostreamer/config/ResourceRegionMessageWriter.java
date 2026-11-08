package net.jk.app.videostreamer.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ResourceRegionMessageWriter implements HttpMessageWriter<ResourceRegion> {

  private static final int BUFFER_SIZE = 64 * 1024; // 64 KB
  private static final DataBufferFactory SAFE_BUFFER_FACTORY =
      DefaultDataBufferFactory.sharedInstance;
  private static final ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);

  private final List<MediaType> mediaTypes = List.of(MediaType.ALL);

  @Override
  public List<MediaType> getWritableMediaTypes() {
    return mediaTypes;
  }

  @Override
  public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
    return REGION_TYPE.isAssignableFrom(elementType);
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
          Flux<DataBuffer> body = encodeRegion(resourceRegion);
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

          Flux<DataBuffer> body = encodeRegion(resourceRegion);
          return response.writeWith(body);
        });
  }

  private Flux<DataBuffer> encodeRegion(ResourceRegion region) {
    Resource resource = region.getResource();

    try {
      if (resource.isFile()) {
        File file = resource.getFile();
        long position = region.getPosition();
        long count = region.getCount();

        return DataBufferUtils.readByteChannel(
            () -> {
              FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
              return new BoundedByteChannel(fileChannel, position, count);
            },
            SAFE_BUFFER_FACTORY,
            BUFFER_SIZE);
      }
    } catch (IOException e) {
      log.debug("Falling back to stream reader for resource: {}", resource);
    }

    return DataBufferUtils.readInputStream(
        () -> {
          InputStream is = resource.getInputStream();
          long toSkip = region.getPosition();
          while (toSkip > 0) {
            long skipped = is.skip(toSkip);
            if (skipped <= 0) {
              if (is.read() == -1) {
                break;
              }
              toSkip--;
            } else {
              toSkip -= skipped;
            }
          }
          return new BoundedInputStream(is, region.getCount());
        },
        SAFE_BUFFER_FACTORY,
        BUFFER_SIZE);
  }

  private MediaType getResourceMediaType(@Nullable MediaType mediaType, Resource resource) {
    return (mediaType != null
            && mediaType.isConcrete()
            && mediaType != MediaType.APPLICATION_OCTET_STREAM)
        ? mediaType
        : MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
  }

  private static class BoundedByteChannel implements ReadableByteChannel {
    private final FileChannel fileChannel;
    private long remaining;

    public BoundedByteChannel(FileChannel fileChannel, long position, long count)
        throws IOException {
      this.fileChannel = fileChannel;
      this.fileChannel.position(position);
      this.remaining = count;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
      if (remaining <= 0) {
        return -1;
      }
      int originalLimit = dst.limit();
      if (dst.remaining() > remaining) {
        dst.limit(dst.position() + (int) remaining);
      }
      int bytesRead = fileChannel.read(dst);
      dst.limit(originalLimit);
      if (bytesRead > 0) {
        remaining -= bytesRead;
      }
      return bytesRead;
    }

    @Override
    public boolean isOpen() {
      return fileChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
      fileChannel.close();
    }
  }

  private static class BoundedInputStream extends InputStream {
    private final InputStream in;
    private long remaining;

    public BoundedInputStream(InputStream in, long count) {
      this.in = in;
      this.remaining = count;
    }

    @Override
    public int read() throws IOException {
      if (remaining <= 0) {
        return -1;
      }
      int result = in.read();
      if (result != -1) {
        remaining--;
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (remaining <= 0) {
        return -1;
      }
      int toRead = (int) Math.min(len, remaining);
      int bytesRead = in.read(b, off, toRead);
      if (bytesRead > 0) {
        remaining -= bytesRead;
      }
      return bytesRead;
    }

    @Override
    public void close() throws IOException {
      in.close();
    }
  }
}
