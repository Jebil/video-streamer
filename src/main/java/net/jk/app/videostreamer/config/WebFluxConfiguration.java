package net.jk.app.videostreamer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfiguration implements WebFluxConfigurer {

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    configurer.customCodecs().register(new ResourceRegionMessageWriter());
  }
}
