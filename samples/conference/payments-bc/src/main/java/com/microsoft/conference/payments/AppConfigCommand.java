package com.microsoft.conference.payments;

import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.enodeframework.ENodeBootstrap;
import com.enodeframework.queue.TopicData;
import com.enodeframework.queue.command.CommandResultProcessor;
import com.enodeframework.rocketmq.message.RocketMQCommandService;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.microsoft.common.Constant.COMMAND_PRODUCER_GROUP;
import static com.microsoft.common.Constant.COMMAND_TOPIC;
import static com.microsoft.common.Constant.NAMESRVADDR;

@Configuration
public class AppConfigCommand {

    @Bean
    public RocketMQCommandService rocketMQCommandService(DefaultMQProducer producer) {
        RocketMQCommandService rocketMQCommandService = new RocketMQCommandService();
        rocketMQCommandService.setDefaultMQProducer(producer);
        TopicData topicData = new TopicData(COMMAND_TOPIC, "*");
        rocketMQCommandService.setTopicData(topicData);
        return rocketMQCommandService;
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public CommandResultProcessor commandResultProcessor() {
        CommandResultProcessor processor = new CommandResultProcessor();
        processor.setPort(6000);
        return processor;
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DefaultMQProducer commandProducer() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(NAMESRVADDR);
        producer.setProducerGroup(COMMAND_PRODUCER_GROUP);
        return producer;
    }

    @Bean(initMethod = "init")
    public ENodeBootstrap eNodeBootstrap() {
        ENodeBootstrap bootstrap = new ENodeBootstrap();
        bootstrap.setPackages(Lists.newArrayList("com.enodeframework.samples"));
        return bootstrap;
    }

}
