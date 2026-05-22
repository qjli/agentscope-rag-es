package io.agentscope.rag.kb.ops;

public enum MaterialType {
    TEXT("TextReader"),
    WORD("WordReader"),
    PDF("PdfReader");

    private final String readerLabel;

    MaterialType(String readerLabel) {
        this.readerLabel = readerLabel;
    }

    public String getReaderLabel() {
        return readerLabel;
    }
}
