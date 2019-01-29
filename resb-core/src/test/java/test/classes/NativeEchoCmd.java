package test.classes;

import pub.resb.api.models.Command;

public class NativeEchoCmd implements Command<String> {
    private String content;

    public NativeEchoCmd() {
    }

    public NativeEchoCmd(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
