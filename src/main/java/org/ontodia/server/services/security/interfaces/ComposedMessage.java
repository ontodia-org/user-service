package org.ontodia.server.services.security.interfaces;

public class ComposedMessage {
    private String subject;
    private String body;

    protected ComposedMessage(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private String subject;
        private String body;

        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public ComposedMessage compose() {
            return new ComposedMessage(subject, body);
        }
    }
}
