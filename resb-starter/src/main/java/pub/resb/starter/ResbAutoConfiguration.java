package pub.resb.starter;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import pub.resb.core.implementations.SystemResolver;
import pub.resb.core.implementations.HostAndPortExplorer;

@ComponentScan
@SpringBootConfiguration
@AutoConfigureAfter(WebFluxAutoConfiguration.class)
public class ResbAutoConfiguration {

    @Bean
    public SystemResolver blockedSystemResolver() {
        return new SystemResolver();
    }

    @Bean
    public HostAndPortExplorer hostAndPortExplorer() {
        return new HostAndPortExplorer();
    }
}
