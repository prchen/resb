package test.classes;

import pub.resb.reactor.annotations.Entry;
import pub.resb.reactor.models.Command;

@Entry("resb+rest://test/Echo")
public class EchoCmd implements Command<String> {
    private String content;

    public EchoCmd() {
    }

    public EchoCmd(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
