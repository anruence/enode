/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.enodeframework.amqp.message;

import com.rabbitmq.client.Channel;
import org.enodeframework.common.exception.IORuntimeException;
import org.enodeframework.common.extensions.SysProperties;
import org.enodeframework.queue.MessageHandlerHolder;
import org.enodeframework.queue.QueueMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.io.IOException;

/**
 * @author anruence@gmail.com
 */
public class AmqpMessageListener implements ChannelAwareMessageListener {
    private final MessageHandlerHolder messageHandlerMap;

    public AmqpMessageListener(MessageHandlerHolder messageHandlerMap) {
        this.messageHandlerMap = messageHandlerMap;
    }

    @Override
    public void onMessage(Message message, Channel channel) {
        QueueMessage queueMessage = this.covertToQueueMessage(message);
        messageHandlerMap.chooseMessageHandler(queueMessage.getType()).handle(queueMessage, context -> {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        });
    }

    private QueueMessage covertToQueueMessage(Message record) {
        MessageProperties props = record.getMessageProperties();
        QueueMessage queueMessage = new QueueMessage();
        queueMessage.setBody(record.getBody());
        queueMessage.setTag(props.getConsumerTag());
        queueMessage.setKey(props.getMessageId());
        queueMessage.setTopic(props.getConsumerQueue());
        queueMessage.setType(props.getHeader(SysProperties.MESSAGE_TYPE_KEY));
        return queueMessage;
    }
}
