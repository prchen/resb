package test.classes;

import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.interfaces.Protocol;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import pub.resb.reactor.utils.ModelUtils;
import reactor.core.publisher.Mono;

import java.net.URI;

@SuppressWarnings("unchecked")
public class RemoteEchoProtocol implements Protocol<String> {
    private ServiceBus serverBus;

    public void setServerBus(ServiceBus serverBus) {
        this.serverBus = serverBus;
    }

    @Override
    public boolean accept(URI entry) {
        return entry.getScheme().equals("process");
    }

    @Override
    public <C extends Command<R>, R> Mono<Reply<R>> exchange(ServiceBus serviceBus, URI entry, C command) {
        return serverBus.exchange(ModelUtils.getCellName(entry), ((RemoteEchoCmd) command).getContent());
    }

    @Override
    public <C extends Command<R>, R> Mono<C> deserialize(Class<C> commandType, String payload) {
        RemoteEchoCmd cmd = new RemoteEchoCmd();
        cmd.setContent(payload);
        return Mono.just((C) cmd);
    }
}
