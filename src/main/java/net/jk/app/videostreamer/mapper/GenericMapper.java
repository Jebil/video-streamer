package net.jk.app.videostreamer.mapper;

import java.util.List;
import java.util.function.Function;

public interface GenericMapper<T, R> extends Function<T, R> {

  default List<R> map(List<T> list) {
    return list.stream().map(this::apply).toList();
  }
}
