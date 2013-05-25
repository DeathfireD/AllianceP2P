package org.alliance.core.messageboard;

/**
 * Created by IntelliJ IDEA.
 * User: Pontus
 * Date: 2006-apr-18
 * Time: 21:36:46
 * Represents a topic in the message board.
 */
public class Topic extends Message {

    private String subject;

    public Topic(String subject, String text, int createdBy) {
        super(text, createdBy);
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
