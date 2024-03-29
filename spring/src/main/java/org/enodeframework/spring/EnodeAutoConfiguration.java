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
package org.enodeframework.spring;

import com.google.common.collect.Maps;
import kotlinx.coroutines.Dispatchers;
import org.enodeframework.commanding.CommandHandlerProvider;
import org.enodeframework.commanding.CommandOptions;
import org.enodeframework.commanding.CommandProcessor;
import org.enodeframework.commanding.ProcessingCommandHandler;
import org.enodeframework.commanding.impl.DefaultCommandHandlerProvider;
import org.enodeframework.commanding.impl.DefaultCommandProcessor;
import org.enodeframework.commanding.impl.DefaultProcessingCommandHandler;
import org.enodeframework.common.scheduling.DefaultScheduleService;
import org.enodeframework.common.scheduling.ScheduleService;
import org.enodeframework.common.serializing.DefaultSerializeService;
import org.enodeframework.common.serializing.SerializeService;
import org.enodeframework.domain.AggregateRepositoryProvider;
import org.enodeframework.domain.AggregateRootFactory;
import org.enodeframework.domain.AggregateSnapshotter;
import org.enodeframework.domain.AggregateStorage;
import org.enodeframework.domain.DomainExceptionMessage;
import org.enodeframework.domain.MemoryCache;
import org.enodeframework.domain.Repository;
import org.enodeframework.domain.impl.DefaultAggregateRepositoryProvider;
import org.enodeframework.domain.impl.DefaultAggregateRootFactory;
import org.enodeframework.domain.impl.DefaultAggregateRootInternalHandlerProvider;
import org.enodeframework.domain.impl.DefaultAggregateSnapshotter;
import org.enodeframework.domain.impl.DefaultMemoryCache;
import org.enodeframework.domain.impl.DefaultRepository;
import org.enodeframework.domain.impl.EventSourcingAggregateStorage;
import org.enodeframework.domain.impl.SnapshotOnlyAggregateStorage;
import org.enodeframework.eventing.DomainEventStream;
import org.enodeframework.eventing.EventCommittingService;
import org.enodeframework.eventing.EventSerializer;
import org.enodeframework.eventing.EventStore;
import org.enodeframework.eventing.ProcessingEventProcessor;
import org.enodeframework.eventing.PublishedVersionStore;
import org.enodeframework.eventing.impl.DefaultEventCommittingService;
import org.enodeframework.eventing.impl.DefaultEventSerializer;
import org.enodeframework.eventing.impl.DefaultProcessingEventProcessor;
import org.enodeframework.infrastructure.TypeNameProvider;
import org.enodeframework.infrastructure.impl.DefaultTypeNameProvider;
import org.enodeframework.messaging.ApplicationMessage;
import org.enodeframework.messaging.MessageDispatcher;
import org.enodeframework.messaging.MessageHandlerProvider;
import org.enodeframework.messaging.MessagePublisher;
import org.enodeframework.messaging.ThreeMessageHandlerProvider;
import org.enodeframework.messaging.TwoMessageHandlerProvider;
import org.enodeframework.messaging.impl.DefaultMessageDispatcher;
import org.enodeframework.messaging.impl.DefaultMessageHandlerProvider;
import org.enodeframework.messaging.impl.DefaultThreeMessageHandlerProvider;
import org.enodeframework.messaging.impl.DefaultTwoMessageHandlerProvider;
import org.enodeframework.queue.MessageHandler;
import org.enodeframework.queue.MessageHandlerHolder;
import org.enodeframework.queue.MessageTypeCode;
import org.enodeframework.queue.SendMessageService;
import org.enodeframework.queue.SendReplyService;
import org.enodeframework.queue.applicationmessage.DefaultApplicationMessageHandler;
import org.enodeframework.queue.applicationmessage.DefaultApplicationMessagePublisher;
import org.enodeframework.queue.command.CommandResultProcessor;
import org.enodeframework.queue.command.DefaultCommandBus;
import org.enodeframework.queue.command.DefaultCommandMessageHandler;
import org.enodeframework.queue.command.DefaultCommandResultProcessor;
import org.enodeframework.queue.domainevent.DefaultDomainEventMessageHandler;
import org.enodeframework.queue.domainevent.DefaultDomainEventPublisher;
import org.enodeframework.queue.publishableexceptions.DefaultPublishableExceptionMessageHandler;
import org.enodeframework.queue.publishableexceptions.DefaultPublishableExceptionPublisher;
import org.enodeframework.queue.reply.DefaultReplyMessageHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.net.InetAddress;

/**
 * @author anruence@gmail.com
 */
public class EnodeAutoConfiguration {

    @Value("${spring.enode.mq.topic.command:}")
    private String commandTopic;

    @Value("${spring.enode.mq.topic.event:}")
    private String eventTopic;

    @Value("${spring.enode.reply.topic:}")
    private String replyTopic;

    @Value("${spring.enode.server.wait.timeout:5000}")
    private int timeout;

    @Value("${spring.enode.reply.server.port:8929}")
    private int port;

    @Bean(name = "defaultCommandResultProcessor", initMethod = "start", destroyMethod = "stop")
    public DefaultCommandResultProcessor defaultCommandResultProcessor(CommandOptions commandOptions, ScheduleService scheduleService, SerializeService serializeService) throws Exception {
        return new DefaultCommandResultProcessor(scheduleService, serializeService, commandOptions, commandOptions.getTimeoutMs());
    }

    @Bean(name = "defaultCommandOptions")
    public DefaultCommandOptions defaultCommandOptions() throws Exception {
        return new DefaultCommandOptions(InetAddress.getLocalHost().getHostAddress(), port, timeout, replyTopic);
    }

    @Bean(name = "defaultScheduleService")
    public DefaultScheduleService defaultScheduleService() {
        return new DefaultScheduleService();
    }

    @Bean(name = "defaultTypeNameProvider")
    public DefaultTypeNameProvider defaultTypeNameProvider() {
        return new DefaultTypeNameProvider(Maps.newHashMap());
    }

    @Bean(name = "defaultProcessingEventProcessor", initMethod = "start", destroyMethod = "stop")
    public DefaultProcessingEventProcessor defaultProcessingEventProcessor(ScheduleService scheduleService, SerializeService serializeService, MessageDispatcher messageDispatcher, PublishedVersionStore publishedVersionStore) {
        return new DefaultProcessingEventProcessor(scheduleService, serializeService, messageDispatcher, publishedVersionStore, Dispatchers.getIO());
    }

    @Bean(name = "defaultEventSerializer")
    public DefaultEventSerializer defaultEventSerializer(TypeNameProvider typeNameProvider, SerializeService serializeService) {
        return new DefaultEventSerializer(typeNameProvider, serializeService);
    }

    @Bean(name = "defaultAggregateRootInternalHandlerProvider")
    public DefaultAggregateRootInternalHandlerProvider defaultAggregateRootInternalHandlerProvider() {
        return new DefaultAggregateRootInternalHandlerProvider();
    }

    @Bean(name = "defaultMessageDispatcher")
    public DefaultMessageDispatcher defaultMessageDispatcher(TypeNameProvider typeNameProvider, MessageHandlerProvider messageHandlerProvider, TwoMessageHandlerProvider twoMessageHandlerProvider, ThreeMessageHandlerProvider threeMessageHandlerProvider, SerializeService serializeService) {
        return new DefaultMessageDispatcher(typeNameProvider, messageHandlerProvider, twoMessageHandlerProvider, threeMessageHandlerProvider, serializeService, Dispatchers.getIO());
    }

    @Bean(name = "defaultRepository")
    public DefaultRepository defaultRepository(MemoryCache memoryCache) {
        return new DefaultRepository(memoryCache);
    }

    @Bean(name = "defaultMemoryCache", initMethod = "start", destroyMethod = "stop")
    public DefaultMemoryCache defaultMemoryCache(AggregateStorage aggregateStorage, ScheduleService scheduleService, TypeNameProvider typeNameProvider) {
        return new DefaultMemoryCache(aggregateStorage, scheduleService, typeNameProvider);
    }

    @Bean(name = "defaultAggregateRepositoryProvider")
    public DefaultAggregateRepositoryProvider defaultAggregateRepositoryProvider() {
        return new DefaultAggregateRepositoryProvider();
    }

    @Bean(name = "defaultThreeMessageHandlerProvider")
    public DefaultThreeMessageHandlerProvider defaultThreeMessageHandlerProvider() {
        return new DefaultThreeMessageHandlerProvider();
    }

    @Bean(name = "defaultTwoMessageHandlerProvider")
    public DefaultTwoMessageHandlerProvider defaultTwoMessageHandlerProvider() {
        return new DefaultTwoMessageHandlerProvider();
    }

    @Bean(name = "defaultMessageHandlerProvider")
    public DefaultMessageHandlerProvider defaultMessageHandlerProvider() {
        return new DefaultMessageHandlerProvider();
    }

    @Bean(name = "defaultCommandHandlerProvider")
    public DefaultCommandHandlerProvider defaultCommandHandlerProvider() {
        return new DefaultCommandHandlerProvider();
    }

    @Bean(name = "defaultAggregateRootFactory")
    public DefaultAggregateRootFactory defaultAggregateRootFactory() {
        return new DefaultAggregateRootFactory();
    }

    @Bean(name = "defaultAggregateSnapshotter")
    public DefaultAggregateSnapshotter defaultAggregateSnapshotter(AggregateRepositoryProvider aggregateRepositoryProvider) {
        return new DefaultAggregateSnapshotter(aggregateRepositoryProvider);
    }

    @Bean(name = "defaultProcessingCommandHandler")
    public DefaultProcessingCommandHandler defaultProcessingCommandHandler(EventStore eventStore, CommandHandlerProvider commandHandlerProvider, TypeNameProvider typeNameProvider, EventCommittingService eventService, MemoryCache memoryCache, @Qualifier(value = "defaultApplicationMessagePublisher") MessagePublisher<ApplicationMessage> applicationMessagePublisher, @Qualifier(value = "defaultPublishableExceptionPublisher") MessagePublisher<DomainExceptionMessage> publishableExceptionPublisher, SerializeService serializeService) {
        return new DefaultProcessingCommandHandler(eventStore, commandHandlerProvider, typeNameProvider, eventService, memoryCache, applicationMessagePublisher, publishableExceptionPublisher, serializeService, Dispatchers.getIO());
    }

    @Bean(name = "defaultEventCommittingService")
    public DefaultEventCommittingService defaultEventCommittingService(MemoryCache memoryCache, EventStore eventStore, SerializeService serializeService, @Qualifier("defaultDomainEventPublisher") MessagePublisher<DomainEventStream> domainEventPublisher) {
        return new DefaultEventCommittingService(memoryCache, eventStore, serializeService, domainEventPublisher, Dispatchers.getIO());
    }

    @Bean(name = "defaultSerializeService")
    @ConditionalOnProperty(prefix = "spring.enode", name = "serialize", havingValue = "jackson", matchIfMissing = true)
    public DefaultSerializeService defaultSerializeService() {
        return new DefaultSerializeService();
    }

    @Bean(name = "defaultCommandProcessor", initMethod = "start", destroyMethod = "stop")
    public DefaultCommandProcessor defaultCommandProcessor(ProcessingCommandHandler processingCommandHandler, ScheduleService scheduleService) {
        return new DefaultCommandProcessor(processingCommandHandler, scheduleService, Dispatchers.getIO());
    }

    @Bean(name = "snapshotOnlyAggregateStorage")
    @ConditionalOnProperty(prefix = "spring.enode", name = "aggregatestorage", havingValue = "snapshot", matchIfMissing = false)
    public SnapshotOnlyAggregateStorage snapshotOnlyAggregateStorage(AggregateSnapshotter aggregateSnapshotter) {
        return new SnapshotOnlyAggregateStorage(aggregateSnapshotter);
    }

    @Bean(name = "eventSourcingAggregateStorage")
    @ConditionalOnProperty(prefix = "spring.enode", name = "aggregatestorage", havingValue = "eventsourcing", matchIfMissing = true)
    public EventSourcingAggregateStorage eventSourcingAggregateStorage(AggregateRootFactory aggregateRootFactory, EventStore eventStore, AggregateSnapshotter aggregateSnapshotter, TypeNameProvider typeNameProvider) {
        return new EventSourcingAggregateStorage(eventStore, aggregateRootFactory, aggregateSnapshotter, typeNameProvider);
    }

    @Bean(name = "defaultCommandService")
    public DefaultCommandBus defaultCommandService(CommandResultProcessor commandResultProcessor, SendMessageService sendMessageService, SerializeService serializeService) {
        return new DefaultCommandBus(commandTopic, "", commandResultProcessor, sendMessageService, serializeService);
    }

    @Bean(name = "defaultDomainEventPublisher")
    public DefaultDomainEventPublisher defaultDomainEventPublisher(EventSerializer eventSerializer, SendMessageService sendMessageService, SerializeService serializeService) {
        return new DefaultDomainEventPublisher(eventTopic, "", eventSerializer, sendMessageService, serializeService);
    }

    @Bean(name = "defaultApplicationMessagePublisher")
    public DefaultApplicationMessagePublisher defaultApplicationMessagePublisher(SendMessageService sendMessageService, SerializeService serializeService, TypeNameProvider typeNameProvider) {
        return new DefaultApplicationMessagePublisher(eventTopic, "", sendMessageService, serializeService, typeNameProvider);
    }

    @Bean(name = "defaultPublishableExceptionPublisher")
    public DefaultPublishableExceptionPublisher defaultPublishableExceptionPublisher(SendMessageService sendMessageService, SerializeService serializeService, TypeNameProvider typeNameProvider) {
        return new DefaultPublishableExceptionPublisher(eventTopic, "", sendMessageService, serializeService, typeNameProvider);
    }

    @Bean(name = "messageHandlerHolder")
    public MessageHandlerHolder messageHandlerHolder(
        @Qualifier(value = "defaultPublishableExceptionMessageHandler") MessageHandler defaultPublishableExceptionMessageHandler,
        @Qualifier(value = "defaultApplicationMessageHandler") MessageHandler defaultApplicationMessageHandler,
        @Qualifier(value = "defaultDomainEventMessageHandler") MessageHandler defaultDomainEventMessageHandler,
        @Qualifier(value = "defaultCommandMessageHandler") MessageHandler defaultCommandMessageHandler,
        @Qualifier(value = "defaultReplyMessageHandler") MessageHandler defaultReplyMessageHandler
    ) {
        MessageHandlerHolder messageHandlerHolder = new MessageHandlerHolder();
        messageHandlerHolder.put(MessageTypeCode.DomainEventMessage.getValue(), defaultDomainEventMessageHandler);
        messageHandlerHolder.put(MessageTypeCode.ApplicationMessage.getValue(), defaultApplicationMessageHandler);
        messageHandlerHolder.put(MessageTypeCode.ExceptionMessage.getValue(), defaultPublishableExceptionMessageHandler);
        messageHandlerHolder.put(MessageTypeCode.CommandMessage.getValue(), defaultCommandMessageHandler);
        messageHandlerHolder.put(MessageTypeCode.ReplyMessage.getValue(), defaultReplyMessageHandler);
        return messageHandlerHolder;
    }

    @Bean(name = "defaultCommandMessageHandler")
    public DefaultCommandMessageHandler defaultCommandMessageHandler(SendReplyService sendReplyService, TypeNameProvider typeNameProvider, CommandProcessor commandProcessor, Repository repository, AggregateStorage aggregateRootStorage, SerializeService serializeService) {
        return new DefaultCommandMessageHandler(sendReplyService, typeNameProvider, commandProcessor, repository, aggregateRootStorage, serializeService);
    }

    @Bean(name = "defaultDomainEventMessageHandler")
    public DefaultDomainEventMessageHandler defaultDomainEventMessageHandler(SendReplyService sendReplyService, ProcessingEventProcessor domainEventMessageProcessor, EventSerializer eventSerializer, SerializeService serializeService) {
        return new DefaultDomainEventMessageHandler(sendReplyService, domainEventMessageProcessor, eventSerializer, serializeService);
    }

    @Bean(name = "defaultPublishableExceptionMessageHandler")
    public DefaultPublishableExceptionMessageHandler defaultPublishableExceptionMessageHandler(TypeNameProvider typeNameProvider, MessageDispatcher messageDispatcher, SerializeService serializeService) {
        return new DefaultPublishableExceptionMessageHandler(typeNameProvider, messageDispatcher, serializeService);
    }

    @Bean(name = "defaultReplyMessageHandler")
    public DefaultReplyMessageHandler defaultReplyMessageHandler(CommandResultProcessor commandResultProcessor, SerializeService serializeService) {
        return new DefaultReplyMessageHandler(commandResultProcessor, serializeService);
    }

    @Bean(name = "defaultApplicationMessageHandler")
    public DefaultApplicationMessageHandler defaultApplicationMessageHandler(TypeNameProvider typeNameProvider, MessageDispatcher messageDispatcher, SerializeService serializeService) {
        return new DefaultApplicationMessageHandler(typeNameProvider, messageDispatcher, serializeService);
    }
}


