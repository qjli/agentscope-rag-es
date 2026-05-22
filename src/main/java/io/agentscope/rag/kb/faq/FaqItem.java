package io.agentscope.rag.kb.faq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FaqItem {

    private String id;
    private String category;
    private String question;
    private String answer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String toKnowledgeText() {
        return """
                [FAQ-%s]
                分类: %s
                问: %s
                答: %s
                """
                .formatted(id, category, question, answer)
                .strip();
    }
}
