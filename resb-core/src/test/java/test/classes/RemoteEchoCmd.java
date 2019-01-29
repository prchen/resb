package test.classes;

import pub.resb.api.annotations.Entry;
import pub.resb.api.models.Command;

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
