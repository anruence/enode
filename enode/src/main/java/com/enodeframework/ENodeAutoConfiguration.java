package com.enodeframework;

import com.enodeframework.commanding.impl.CommandAsyncHandlerProxy;
import com.enodeframework.commanding.impl.CommandHandlerProxy;
import com.enodeframework.commanding.impl.DefaultCommandAsyncHandlerProvider;
import com.enodeframework.commanding.impl.DefaultCommandHandlerProvider;
import com.enodeframework.common.container.SpringObjectContainer;
import com.enodeframework.common.scheduling.ScheduleService;
import com.enodeframework.domain.impl.DefaultAggregateRepositoryProvider;
import com.enodeframework.domain.impl.DefaultAggregateRootFactory;
import com.enodeframework.domain.impl.DefaultAggregateRootInternalHandlerProvider;
import com.enodeframework.domain.impl.DefaultAggregateSnapshotter;
import com.enodeframework.domain.impl.DefaultMemoryCache;
import com.enodeframework.domain.impl.DefaultRepository;
import com.enodeframework.domain.impl.EventSourcingAggregateStorage;
import com.enodeframework.eventing.impl.DefaultEventSerializer;
import com.enodeframework.eventing.impl.DefaultProcessingDomainEventStreamMessageProcessor;
import com.enodeframework.messaging.impl.DefaultMessageDispatcher;
import com.enodeframework.messaging.impl.DefaultMessageHandlerProvider;
import com.enodeframework.messaging.impl.DefaultThreeMessageHandlerProvider;
import com.enodeframework.messaging.impl.DefaultTwoMessageHandlerProvider;
import com.enodeframework.infrastructure.impl.DefaultTypeNameProvider;
import com.enodeframework.messaging.impl.MessageHandlerProxy1;
import com.enodeframework.messaging.impl.MessageHandlerProxy2;
import com.enodeframework.messaging.impl.MessageHandlerProxy3;
import com.enodeframework.queue.SendReplyService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * @author anruence@gmail.com
 */
public class ENodeAutoConfiguration {
    @Bean
    public ScheduleService scheduleService() {
        return new ScheduleService();
    }

    @Bean
    public DefaultTypeNameProvider defaultTypeNameProvider() {
        return new DefaultTypeNameProvider();
    }

    @Bean
    public SpringObjectContainer springObjectContainer() {
        SpringObjectContainer objectContainer = new SpringObjectContainer();
        ObjectContainer.container = objectContainer;
        return objectContainer;
    }

    @Bean
    public DefaultProcessingDomainEventStreamMessageProcessor defaultProcessingDomainEventStreamMessageProcessor() {
        return new DefaultProcessingDomainEventStreamMessageProcessor();
    }

    /**
     * 原型模式获取bean，每次新建代理执行
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CommandHandlerProxy commandHandlerProxy() {
        return new CommandHandlerProxy();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CommandAsyncHandlerProxy commandAsyncHandlerProxy() {
        return new CommandAsyncHandlerProxy();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MessageHandlerProxy1 messageHandlerProxy1() {
        return new MessageHandlerProxy1();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MessageHandlerProxy2 messageHandlerProxy2() {
        return new MessageHandlerProxy2();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MessageHandlerProxy3 messageHandlerProxy3() {
        return new MessageHandlerProxy3();
    }

    @Bean
    public DefaultEventSerializer defaultEventSerializer() {
        return new DefaultEventSerializer();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SendReplyService sendReplyService() {
        return new SendReplyService();
    }

    @Bean
    public DefaultAggregateRootInternalHandlerProvider aggregateRootInternalHandlerProvider() {
        return new DefaultAggregateRootInternalHandlerProvider();
    }

    @Bean
    public DefaultMessageDispatcher defaultMessageDispatcher() {
        return new DefaultMessageDispatcher();
    }

    @Bean
    public DefaultRepository defaultRepository() {
        return new DefaultRepository();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public DefaultMemoryCache defaultMemoryCache() {
        return new DefaultMemoryCache();
    }

    @Bean
    public DefaultAggregateRepositoryProvider aggregateRepositoryProvider() {
        return new DefaultAggregateRepositoryProvider();
    }

    @Bean
    public DefaultThreeMessageHandlerProvider threeMessageHandlerProvider() {
        return new DefaultThreeMessageHandlerProvider();
    }

    @Bean
    public DefaultTwoMessageHandlerProvider twoMessageHandlerProvider() {
        return new DefaultTwoMessageHandlerProvider();
    }

    @Bean
    public DefaultMessageHandlerProvider messageHandlerProvider() {
        return new DefaultMessageHandlerProvider();
    }

    @Bean
    public DefaultCommandAsyncHandlerProvider commandAsyncHandlerProvider() {
        return new DefaultCommandAsyncHandlerProvider();
    }

    @Bean
    public DefaultCommandHandlerProvider commandHandlerProvider() {
        return new DefaultCommandHandlerProvider();
    }

    @Bean
    public DefaultAggregateRootFactory aggregateRootFactory() {
        return new DefaultAggregateRootFactory();
    }

    @Bean
    public DefaultAggregateSnapshotter aggregateSnapshotter() {
        return new DefaultAggregateSnapshotter();
    }

    @Bean
    public EventSourcingAggregateStorage eventSourcingAggregateStorage() {
        return new EventSourcingAggregateStorage();
    }
}
