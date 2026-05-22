package io.agentscope.rag.kb.web.dto;

public class ChatResponse {

    private String answer;
    private String sessionId;

    public ChatResponse(String answer, String sessionId) {
        this.answer = answer;
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
