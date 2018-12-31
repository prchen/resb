package test.classes;

import pub.resb.reactor.annotations.Entry;
import pub.resb.reactor.models.Command;

@Entry("process://unknown/RemoteEcho")
public class RemoteEchoCmd implements Command<String> {
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
