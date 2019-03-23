package test.classes;

import pub.resb.api.interfaces.Protocol;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import pub.resb.core.utils.ResbApiUtils;
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
    public <C extends Command<R>, R> Mono<Reply<R>> clientExchange(ServiceBus serviceBus, URI entry, C command) {
        return serverBus.serverExchange(ResbApiUtils.getCellName(entry), ((RemoteEchoCmd) command).getContent());
    }

    @Override
    public <C extends Command<R>, R> Mono<C> serverDeserialize(ServiceBus serviceBus, Class<C> commandType, String payload) {
        RemoteEchoCmd cmd = new RemoteEchoCmd();
        cmd.setContent(payload);
        return Mono.just((C) cmd);
    }
}
