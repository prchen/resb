package pub.resb.starter.factory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.ServiceBusBuilder;
import pub.resb.reactor.interfaces.*;

import java.util.Collections;
import java.util.List;

@Component
public class ServiceBusFactoryBean implements FactoryBean<ServiceBus> {
    @Autowired(required = false)
    private List<Cell<?, ?>> cells = Collections.emptyList();
    @Autowired(required = false)
    private List<Resolver> resolvers = Collections.emptyList();
    @Autowired(required = false)
    private List<Explorer> explorers = Collections.emptyList();
    @Autowired(required = false)
    private List<Protocol<?>> protocols = Collections.emptyList();
    @Autowired(required = false)
    private List<Interceptor> interceptors = Collections.emptyList();

    @Override
    public ServiceBus getObject() throws Exception {
        return ServiceBus.builder()
                .cells(cells)
                .resolvers(resolvers)
                .explorers(explorers)
                .protocols(protocols)
                .interceptors(interceptors)
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return ServiceBus.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
