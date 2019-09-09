package org.enodeframework.queue;

/**
 * @author anruence@gmail.com
 */
public class QueueMessage {
    private String body;
    private String topic;
    private String tags;
    private int code;
    private int version;
    private String routeKey;
    private String key;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "QueueMessage{" +
                "body='" + body + '\'' +
                ", topic='" + topic + '\'' +
                ", tags='" + tags + '\'' +
                ", code=" + code +
                ", version=" + version +
                ", routeKey='" + routeKey + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
