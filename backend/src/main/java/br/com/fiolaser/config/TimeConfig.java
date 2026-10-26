package br.com.fiolaser.config;

import java.time.*;
import org.springframework.context.annotation.*;

@Configuration
public class TimeConfig {
  @Bean
  public Clock bookingClock() {
    return Clock.system(ZoneId.of("America/Bahia"));
  }
}
