package net.jk.app.videostreamer.mapper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public interface GenericMapper<T, R> extends Function<T, R> {

  public default List<R> map(List<T> list) {
    return list.stream().map(this::apply).collect(Collectors.toList());
  }
}
