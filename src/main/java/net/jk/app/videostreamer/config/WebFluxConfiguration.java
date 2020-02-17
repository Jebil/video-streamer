package net.jk.app.videostreamer.config;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Component
public class WebFluxConfiguration implements WebFluxConfigurer {
  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    configurer.customCodecs().register(new ResourceRegionMessageWriter());
  }
}
