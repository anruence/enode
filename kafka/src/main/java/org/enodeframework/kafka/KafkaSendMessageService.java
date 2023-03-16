package org.enodeframework.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.enodeframework.common.exception.IORuntimeException;
import org.enodeframework.queue.QueueMessage;
import org.enodeframework.queue.SendMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

/**
 * @author anruence@gmail.com
 */
public class KafkaSendMessageService implements SendMessageService {

    private final static Logger logger = LoggerFactory.getLogger(KafkaSendMessageService.class);

    private final KafkaTemplate<String, String> producer;


    public KafkaSendMessageService(KafkaTemplate<String, String> producer) {
        this.producer = producer;
    }

    @Override
    public CompletableFuture<Boolean> sendMessageAsync(QueueMessage queueMessage) {
        ProducerRecord<String, String> message = this.covertToProducerRecord(queueMessage);
        return producer.send(message).handle((result, throwable) -> {
            if (throwable != null) {
                logger.error("Async send message has exception, message: {}", queueMessage, throwable);
                throw new IORuntimeException(throwable);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Async send message success, sendResult: {}, message: {}", result, queueMessage);
            }
            return true;
        });
    }

    private ProducerRecord<String, String> covertToProducerRecord(QueueMessage queueMessage) {
        return new ProducerRecord<>(queueMessage.getTopic(), queueMessage.getRouteKey(), queueMessage.getBodyAndType());
    }
}
