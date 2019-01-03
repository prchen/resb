package pub.resb.starter;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import pub.resb.reactor.implementations.BlockedSystemResolver;
import pub.resb.reactor.implementations.HostAndPortExplorer;

@ComponentScan
@SpringBootConfiguration
@AutoConfigureAfter(WebFluxAutoConfiguration.class)
public class ResbAutoConfiguration {

    @Bean
    public BlockedSystemResolver blockedSystemResolver() {
        return new BlockedSystemResolver();
    }

    @Bean
    public HostAndPortExplorer hostAndPortExplorer() {
        return new HostAndPortExplorer();
    }
}
