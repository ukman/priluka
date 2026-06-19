package io.github.ukman.priluka;

/**
 * Optional base class for terminal instances that need source metadata.
 */
public class Token {
    private int start;
    private int len;
    private String text;
    private Class<? extends Token>[] tokenTypes;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Class<? extends Token>[] getTokenTypes() {
        return tokenTypes;
    }

    public void setTokenTypes(Class<? extends Token>[] tokenTypes) {
        this.tokenTypes = tokenTypes;
    }
}
