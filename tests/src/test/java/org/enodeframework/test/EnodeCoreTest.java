package org.enodeframework.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.enodeframework.commanding.CommandMessage;
import org.enodeframework.commanding.CommandResult;
import org.enodeframework.commanding.CommandReturnType;
import org.enodeframework.commanding.CommandStatus;
import org.enodeframework.common.io.Task;
import org.enodeframework.common.utils.IdGenerator;
import org.enodeframework.domain.AggregateRoot;
import org.enodeframework.eventing.DomainEventMessage;
import org.enodeframework.eventing.DomainEventStream;
import org.enodeframework.eventing.EventAppendResult;
import org.enodeframework.eventing.ProcessingEvent;
import org.enodeframework.test.async.ManualResetEvent;
import org.enodeframework.test.command.AggregateThrowExceptionCommand;
import org.enodeframework.test.command.AsyncHandlerBaseCommand;
import org.enodeframework.test.command.AsyncHandlerChildCommand;
import org.enodeframework.test.command.AsyncHandlerCommand;
import org.enodeframework.test.command.BaseCommand;
import org.enodeframework.test.command.ChangeInheritTestAggregateTitleCommand;
import org.enodeframework.test.command.ChangeMultipleAggregatesCommand;
import org.enodeframework.test.command.ChangeNothingCommand;
import org.enodeframework.test.command.ChangeTestAggregateTitleCommand;
import org.enodeframework.test.command.ChangeTestAggregateTitleWhenDirtyCommand;
import org.enodeframework.test.command.ChildCommand;
import org.enodeframework.test.command.CreateInheritTestAggregateCommand;
import org.enodeframework.test.command.CreateTestAggregateCommand;
import org.enodeframework.test.command.NoHandlerCommand;
import org.enodeframework.test.command.NotCheckAsyncHandlerExistWithResultCommand;
import org.enodeframework.test.command.SetResultCommand;
import org.enodeframework.test.command.TestEventPriorityCommand;
import org.enodeframework.test.command.ThrowExceptionCommand;
import org.enodeframework.test.command.TwoAsyncHandlersCommand;
import org.enodeframework.test.command.TwoHandlersCommand;
import org.enodeframework.test.domain.InheritTestAggregate;
import org.enodeframework.test.domain.TestAggregate;
import org.enodeframework.test.domain.TestAggregateCreated;
import org.enodeframework.test.domain.TestAggregateTitleChanged;
import org.enodeframework.test.eventhandler.Handler1;
import org.enodeframework.test.eventhandler.Handler121;
import org.enodeframework.test.eventhandler.Handler122;
import org.enodeframework.test.eventhandler.Handler123;
import org.enodeframework.test.eventhandler.Handler1231;
import org.enodeframework.test.eventhandler.Handler1232;
import org.enodeframework.test.eventhandler.Handler1233;
import org.enodeframework.test.eventhandler.Handler2;
import org.enodeframework.test.eventhandler.Handler3;
import org.enodeframework.test.mock.DomainEventStreamProcessContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EnodeCoreTest extends AbstractTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnodeCoreTest.class);

    public static volatile ConcurrentHashMap<Integer, List<String>> HandlerTypes = new ConcurrentHashMap<>();

    @Test
    public void create_and_update_aggregate_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //执行修改聚合根的命令
        ChangeTestAggregateTitleCommand command2 = new ChangeTestAggregateTitleCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setTitle("Changed Note");
        commandResult = Task.await(commandService.executeAsync(command2));
        Assertions.assertNotNull(commandResult);


        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(2, note.getVersion());
    }

    @Test
    public void create_and_update_inherit_aggregate_test() {
        String aggregateId = IdGenerator.id();
        CreateInheritTestAggregateCommand command = new CreateInheritTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        InheritTestAggregate note = Task.await(memoryCache.getAsync(aggregateId, InheritTestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //执行修改聚合根的命令
        ChangeInheritTestAggregateTitleCommand command2 = new ChangeInheritTestAggregateTitleCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setTitle("Changed Note");
        commandResult = Task.await(commandService.executeAsync(command2));
        Assertions.assertNotNull(commandResult);


        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, InheritTestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(2, note.getVersion());
    }

    @Test
    public void command_sync_execute_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //执行修改聚合根的命令
        ChangeTestAggregateTitleCommand command2 = new ChangeTestAggregateTitleCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setTitle("Changed Note");
        commandResult = Task.await(commandService.executeAsync(command2));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(2, note.getVersion());
    }

    @Test
    public void duplicate_create_aggregate_command_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //用同一个命令再次执行创建聚合根的命令
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //用另一个命令再次执行创建相同聚合根的命令
        command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
    }

    @Test
    public void duplicate_update_aggregate_command_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command1 = new CreateTestAggregateCommand();
        command1.setAggregateRootId(aggregateId);
        command1.setTitle("Sample Note");
        //先创建一个聚合根
        CommandStatus status = Task.await(commandService.executeAsync(command1)).getStatus();
        Assertions.assertEquals(CommandStatus.Success, status);
        ChangeTestAggregateTitleCommand command2 = new ChangeTestAggregateTitleCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setTitle("Changed Note");
        //执行修改聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command2, CommandReturnType.EventHandled));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(2, note.getVersion());
        //在重复执行该命令
        commandResult = Task.await(commandService.executeAsync(command2, CommandReturnType.EventHandled));
        commandResult = Task.await(commandService.executeAsync(command2, CommandReturnType.EventHandled));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2));
        commandResult = Task.await(commandService.executeAsync(command2, CommandReturnType.EventHandled));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(2, note.getVersion());
    }

    //TODO 这个测试用例需要更新，会引发命令超时返回
    @Test
    public void create_and_concurrent_update_aggregate_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //并发执行修改聚合根的命令
        long totalCount = 100;
        AtomicLong finishedCount = new AtomicLong(0);
        ManualResetEvent waitHandle = new ManualResetEvent(false);
        for (int i = 0; i < totalCount; i++) {
            ChangeTestAggregateTitleCommand updateCommand = new ChangeTestAggregateTitleCommand();
            updateCommand.setAggregateRootId(aggregateId);
            updateCommand.setTitle("Changed Note");
            commandService.executeAsync(updateCommand).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                }
                try {
                    Assertions.assertNotNull(result);
                    Assertions.assertNotNull(result);
                    Assertions.assertEquals(CommandStatus.Success, result.getStatus());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    long current = finishedCount.incrementAndGet();
                    if (current == totalCount) {
                        TestAggregate note1 = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
                        Assertions.assertNotNull(note1);
                        Assertions.assertEquals("Changed Note", note1.getTitle());
                        Assertions.assertEquals(totalCount + 1, ((AggregateRoot) note1).getVersion());
                        waitHandle.set();
                    }
                }

            });
        }
        waitHandle.waitOne();
    }

    @Test
    public void change_nothing_test() {
        ChangeNothingCommand command = new ChangeNothingCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
    }

    @Test
    public void set_result_command_test() {
        SetResultCommand command = new SetResultCommand();
        command.setAggregateRootId(IdGenerator.id());
        command.setResult("CommandResult");
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        Assertions.assertEquals("CommandResult", commandResult.getResult());
    }

    @Test
    public void change_multiple_aggregates_test() {
        CreateTestAggregateCommand command1 = new CreateTestAggregateCommand();
        command1.setAggregateRootId(IdGenerator.id());
        command1.setTitle("Sample Note1");
        Task.await(commandService.executeAsync(command1));
        CreateTestAggregateCommand command2 = new CreateTestAggregateCommand();
        command2.setAggregateRootId(IdGenerator.id());
        command2.setTitle("Sample Note2");
        Task.await(commandService.executeAsync(command2));
        ChangeMultipleAggregatesCommand command3 = new ChangeMultipleAggregatesCommand();
        command3.setAggregateRootId(IdGenerator.id());
        command3.setAggregateRootId1(command1.getAggregateRootId());
        command3.setAggregateRootId2(command2.getAggregateRootId());
        CommandResult commandResult = Task.await(commandService.executeAsync(command3));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    @Test
    public void no_handler_command_test() {
        NoHandlerCommand command = new NoHandlerCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    // 运行时检测
    public void two_handlers_command_test() {
        TwoHandlersCommand command = new TwoHandlersCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    @Test
    public void handler_throw_exception_command_test() {
        ThrowExceptionCommand command = new ThrowExceptionCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    @Test
    public void aggregate_throw_exception_command_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        Task.await(commandService.executeAsync(command));
        AggregateThrowExceptionCommand command1 = new AggregateThrowExceptionCommand();
        command1.setAggregateRootId(aggregateId);
        command1.setPublishableException(false);
        CommandResult commandResult = Task.await(commandService.executeAsync(command1));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
        AggregateThrowExceptionCommand command2 = new AggregateThrowExceptionCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setPublishableException(true);
        commandResult = Task.await(commandService.executeAsync(command2));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    @Test
    public void command_inheritance_test() {
        BaseCommand command = new BaseCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
        Assertions.assertEquals("ResultFromBaseCommand", commandResult.getResult());
        command = new ChildCommand();
        command.setAggregateRootId(IdGenerator.id());
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);


        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
        Assertions.assertEquals("ResultFromChildCommand", commandResult.getResult());
    }

    // Command Tests
    @Test
    public void async_command_handler_test() {
        AsyncHandlerCommand command = new AsyncHandlerCommand();
        command.setShouldGenerateApplicationMessage(true);
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
    }

    @Test
    public void async_command_handler_throw_exception_test() {
        AsyncHandlerCommand command = new AsyncHandlerCommand();
        command.setAggregateRootId(IdGenerator.id());
        command.setShouldThrowException(true);
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
        AsyncHandlerCommand command1 = new AsyncHandlerCommand();
        command1.setAggregateRootId(IdGenerator.id());
        command1.setShouldThrowIOException(true);
        commandResult = Task.await(commandService.executeAsync(command1));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
    }

    @Test
    public void async_command_two_handlers_test() {
        TwoAsyncHandlersCommand command = new TwoAsyncHandlersCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
    }

    @Test
    public void duplicate_async_command_test() {
        AsyncHandlerCommand command = new AsyncHandlerCommand();
        command.setShouldGenerateApplicationMessage(true);
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
    }

    @Test
    public void duplicate_async_command_with_application_message_test() {
        AsyncHandlerCommand command = new AsyncHandlerCommand();
        command.setAggregateRootId(IdGenerator.id());
        command.setShouldGenerateApplicationMessage(true);
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);


        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
    }

    @Test
    public void duplicate_async_command_not_check_handler_exist_with_result_test() {
        NotCheckAsyncHandlerExistWithResultCommand command = new NotCheckAsyncHandlerExistWithResultCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);


        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
    }

    @Test
    public void async_command_inheritance_test() {
        AsyncHandlerBaseCommand command = new AsyncHandlerBaseCommand();
        command.setAggregateRootId(IdGenerator.id());
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
        command = new AsyncHandlerChildCommand();
        command.setAggregateRootId(IdGenerator.id());
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.NoChange, commandResult.getStatus());
    }

    /**
     * region Event
     * Service Tests
     */
    @Test
    public void create_concurrent_conflict_and_then_update_many_times_test() {
        String aggregateId = IdGenerator.id();
        String commandId = IdGenerator.id();
        TestAggregateCreated aggregateCreated = new TestAggregateCreated("Note Title");
        aggregateCreated.setVersion(1);
        aggregateCreated.setAggregateRootId(aggregateId);
        //往EventStore直接插入事件，用于模拟并发冲突的情况
        DomainEventStream eventStream = new DomainEventStream(commandId, aggregateId, TestAggregate.class.getName(), new Date(), Lists.newArrayList(aggregateCreated), Maps.newHashMap());
        EventAppendResult result = Task.await(eventStore.batchAppendAsync(Lists.newArrayList(eventStream)));
        Assertions.assertNotNull(result);
        assertAppendResult(result);
        LOGGER.info("----create_concurrent_conflict_and_then_update_many_times_test, _eventStore.appendAsync success");
        Task.await(publishedVersionStore.updatePublishedVersionAsync("DefaultEventProcessor", TestAggregate.class.getName(), aggregateId, 1));
        LOGGER.info("----create_concurrent_conflict_and_then_update_many_times_test, UpdatePublishedversion()Async success");
        //执行创建聚合根的命令
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setId(commandId);
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        LOGGER.info("----create_concurrent_conflict_and_then_update_many_times_test, _commandService.executeAsync create success");
        List<CommandMessage> commandList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ChangeTestAggregateTitleCommand command1 = new ChangeTestAggregateTitleCommand();
            command1.setAggregateRootId(aggregateId);
            command1.setTitle("Changed Note Title" + i);
            commandList.add(command1);
        }
        ManualResetEvent waitHandle = new ManualResetEvent(false);
        AtomicLong count = new AtomicLong(0);
        for (CommandMessage updateCommand : commandList) {
            commandService.executeAsync(updateCommand).thenAccept(t -> {
                Assertions.assertNotNull(t);
                Assertions.assertEquals(CommandStatus.Success, t.getStatus());
                long totalCount = count.incrementAndGet();
                LOGGER.info("----create_concurrent_conflict_and_then_update_many_times_test, updateCommand finished, count: {}", totalCount);
                if (totalCount == commandList.size()) {
                    waitHandle.set();
                }
            });
        }
        waitHandle.waitOne();
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals(commandList.size() + 1, note.getVersion());
    }

    @Test
    public void create_concurrent_conflict_and_then_update_many_times_test2() {
        String aggregateId = IdGenerator.id();
        String commandId = IdGenerator.id();
        TestAggregateCreated aggregateCreated = new TestAggregateCreated("Note Title");
        aggregateCreated.setAggregateRootId(aggregateId);
        aggregateCreated.setVersion(1);
        //往EventStore直接插入事件，用于模拟并发冲突的情况
        DomainEventStream eventStream = new DomainEventStream(commandId, aggregateId, TestAggregate.class.getName(), new Date(), Lists.newArrayList(aggregateCreated), Maps.newHashMap());
        EventAppendResult result = Task.await(eventStore.batchAppendAsync(Lists.newArrayList(eventStream)));
        Assertions.assertNotNull(result);
        assertAppendResult(result);
        Task.await(publishedVersionStore.updatePublishedVersionAsync("DefaultEventProcessor", TestAggregate.class.getName(), aggregateId, 1));
        List<CommandMessage> commandList = new ArrayList<>();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setId(commandId);
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        commandList.add(command);
        for (int i = 0; i < 50; i++) {
            ChangeTestAggregateTitleCommand command1 = new ChangeTestAggregateTitleCommand();
            command1.setAggregateRootId(aggregateId);
            command1.setTitle("Changed Note Title" + i);
            commandList.add(command1);
        }
        ManualResetEvent waitHandle = new ManualResetEvent(false);
        AtomicLong count = new AtomicLong(0);
        AtomicBoolean createCommandSuccess = new AtomicBoolean(false);
        for (CommandMessage updateCommand : commandList) {
            commandService.executeAsync(updateCommand).whenComplete((commandResult, y) -> {
                Assertions.assertNotNull(commandResult);
                Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
                long totalCount = count.incrementAndGet();
                if (totalCount == commandList.size() - 1) {
                    waitHandle.set();
                }
                if (Objects.equals(commandResult.getCommandId(), commandId)) {
                    createCommandSuccess.set(true);
                }
            });
        }
        waitHandle.waitOne();
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertTrue(createCommandSuccess.get());
        Assertions.assertEquals(commandList.size(), note.getVersion());
    }

    @Test
    public void create_concurrent_conflict_not_enable_batch_insert_test() {
        String aggregateId = IdGenerator.id();
        String commandId = IdGenerator.id();
        TestAggregateTitleChanged titleChanged = new TestAggregateTitleChanged("Note Title");
        titleChanged.setAggregateRootId(aggregateId);
        titleChanged.setVersion(1);
        //往EventStore直接插入事件，用于模拟并发冲突的情况
        DomainEventStream eventStream = new DomainEventStream(commandId, aggregateId, TestAggregate.class.getName(), new Date(), Lists.newArrayList(titleChanged), Maps.newHashMap());
        EventAppendResult result = Task.await(eventStore.batchAppendAsync(Lists.newArrayList(eventStream)));
        Assertions.assertNotNull(result);
        assertAppendResult(result);
        Task.await(publishedVersionStore.updatePublishedVersionAsync("DefaultEventProcessor", TestAggregate.class.getName(), aggregateId, 1));
        //执行创建聚合根的命令
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        CommandResult commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Failed, commandResult.getStatus());
        Assertions.assertEquals("Duplicate aggregate creation.", commandResult.getResult());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Note Title", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        //执行创建聚合根的命令
        command = new CreateTestAggregateCommand();
        command.setId(commandId);
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        commandResult = Task.await(commandService.executeAsync(command));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Note Title", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
    }

    @Test
    public void update_concurrent_conflict_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令，因为 publishedVersionStore.updatePublishedVersionAsync 是收到event消息后才执行的。
        //可以认为在下面模拟插入数据时 更新版本为2和插入操作是并行的，如果发生更新版本 先执行，版本的进度就会一直落后
        CommandResult commandResult = Task.await(commandService.executeAsync(command, CommandReturnType.EventHandled));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());
        TestAggregateTitleChanged aggregateTitleChanged = new TestAggregateTitleChanged("Changed Title");
        aggregateTitleChanged.setAggregateRootId(aggregateId);
        aggregateTitleChanged.setVersion(2);
        //往EventStore直接插入事件，用于模拟并发冲突的情况
        DomainEventStream eventStream = new DomainEventStream(IdGenerator.id(), aggregateId, TestAggregate.class.getName(), new Date(), Lists.newArrayList(aggregateTitleChanged), Maps.newHashMap());
        EventAppendResult result = Task.await(eventStore.batchAppendAsync(Lists.newArrayList(eventStream)));
        Assertions.assertNotNull(result);
        assertAppendResult(result);
        Task.await(publishedVersionStore.updatePublishedVersionAsync("DefaultEventProcessor", TestAggregate.class.getName(), aggregateId, 2));
        List<CommandMessage> commandList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ChangeTestAggregateTitleCommand command1 = new ChangeTestAggregateTitleCommand();
            command1.setAggregateRootId(aggregateId);
            command1.setTitle("Changed Note2-" + i);
            commandList.add(command1);
        }
        ManualResetEvent waitHandle = new ManualResetEvent(false);
        AtomicLong count = new AtomicLong(0);
        for (CommandMessage updateCommand : commandList) {
            commandService.executeAsync(updateCommand, CommandReturnType.EventHandled).thenAccept(t -> {
                Assertions.assertNotNull(t);
                Assertions.assertEquals(CommandStatus.Success, t.getStatus());
                long totalCount = count.incrementAndGet();
                if (totalCount == commandList.size()) {
                    waitHandle.set();
                }
            });
        }
        waitHandle.waitOne();
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals(2 + commandList.size(), note.getVersion());
        // 命令发送成功的顺序和i的顺序不一定一致，批量发送都是异步执行的
        Assertions.assertTrue(note.getTitle().contains("Changed Note2-"));
    }

    @Test
    public void event_handler_priority_test() {
        String noteId = IdGenerator.id();
        CreateTestAggregateCommand command1 = new CreateTestAggregateCommand();
        command1.setAggregateRootId(noteId);
        command1.setTitle("Sample Title1");
        TestEventPriorityCommand command2 = new TestEventPriorityCommand();
        command2.setAggregateRootId(noteId);
        CommandResult commandResult1 = Task.await(commandService.executeAsync(command1, CommandReturnType.EventHandled));
        CommandResult commandResult2 = Task.await(commandService.executeAsync(command2, CommandReturnType.EventHandled));
        Assertions.assertEquals(CommandStatus.Success, commandResult1.getStatus());
        Assertions.assertEquals(CommandStatus.Success, commandResult2.getStatus());
        Assertions.assertEquals(3, HandlerTypes.get(1).size());
        Assertions.assertEquals(Handler3.class.getName(), HandlerTypes.get(1).get(0));
        Assertions.assertEquals(Handler2.class.getName(), HandlerTypes.get(1).get(1));
        Assertions.assertEquals(Handler1.class.getName(), HandlerTypes.get(1).get(2));
        Assertions.assertEquals(3, HandlerTypes.get(2).size());
        Assertions.assertEquals(Handler122.class.getName(), HandlerTypes.get(2).get(0));
        Assertions.assertEquals(Handler121.class.getName(), HandlerTypes.get(2).get(1));
        Assertions.assertEquals(Handler123.class.getName(), HandlerTypes.get(2).get(2));
        Assertions.assertEquals(3, HandlerTypes.get(3).size());
        Assertions.assertEquals(Handler1232.class.getName(), HandlerTypes.get(3).get(0));
        Assertions.assertEquals(Handler1231.class.getName(), HandlerTypes.get(3).get(1));
        Assertions.assertEquals(Handler1233.class.getName(), HandlerTypes.get(3).get(2));
        HandlerTypes.clear();
    }

    @Test
    public void note_update_many_times_test() {
        String noteId = IdGenerator.id();
        // 使用无界队列模拟不会出现异常，如果有界队列
        Executor executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(300);
        for (int i = 0; i < 300; i++) {
            CompletableFuture.runAsync(() -> {
                String title = "Create Note";
                CreateTestAggregateCommand createNoteCommand = new CreateTestAggregateCommand();
                createNoteCommand.setTitle(title);
                createNoteCommand.setAggregateRootId(noteId);
                Task.await(commandService.executeAsync(createNoteCommand, CommandReturnType.CommandExecuted));
                ChangeTestAggregateTitleCommand titleCommand = new ChangeTestAggregateTitleCommand();
                titleCommand.setTitle(title + "Changed");
                titleCommand.setAggregateRootId(noteId);
                Task.await(commandService.executeAsync(titleCommand, CommandReturnType.CommandExecuted));
                latch.countDown();
            }, executor).whenComplete((x, y) -> {
                latch.countDown();
            });
        }
        Task.await(latch);
    }

    @Test
    public void sequence_domain_event_process_test() {
        TestAggregate note = new TestAggregate(IdGenerator.id(), "initial title");
        DomainEventStream message1 = createMessage(note);
        ((AggregateRoot) note).acceptChanges();
        note.changeTitle("title1");
        DomainEventStream message2 = createMessage(note);
        ((AggregateRoot) note).acceptChanges();
        note.changeTitle("title2");
        DomainEventStream message3 = createMessage(note);
        ManualResetEvent waitHandle = new ManualResetEvent(false);
        List<Integer> versionList = new ArrayList<>();
        processor.process(new ProcessingEvent(message1, new DomainEventStreamProcessContext(message1, waitHandle, versionList)));
        processor.process(new ProcessingEvent(message3, new DomainEventStreamProcessContext(message3, waitHandle, versionList)));
        processor.process(new ProcessingEvent(message2, new DomainEventStreamProcessContext(message2, waitHandle, versionList)));
        waitHandle.waitOne();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(i + 1, versionList.get(i).intValue());
        }
    }

    @Test
    public void sequence_domain_event_process_test2() {
        TestAggregate note = new TestAggregate(IdGenerator.id(), "initial title");
        AggregateRoot aggregate = note;
        DomainEventStream message1 = createMessage(aggregate);

        aggregate.acceptChanges();
        note.changeTitle("title1");
        DomainEventStream message2 = createMessage(aggregate);

        aggregate.acceptChanges();
        note.changeTitle("title2");
        DomainEventStream message3 = createMessage(aggregate);

        ManualResetEvent waitHandle = new ManualResetEvent(false);
        List<Integer> versionList = new ArrayList<>();

        //模拟从publishedVersionStore中取到的publishedVersion不是最新的场景
        processor.process(new ProcessingEvent(message1, new DomainEventStreamProcessContext(message1, waitHandle, versionList)));
        processor.process(new ProcessingEvent(message3, new DomainEventStreamProcessContext(message3, waitHandle, versionList)));

        //等待5秒后更新publishedVersion为2
        Task.sleep(3000);
        Task.await(publishedVersionStore.updatePublishedVersionAsync(processor.getName(), TestAggregate.class.getName(), aggregate.getUniqueId(), 2));

        //等待Enode内部自动检测到最新的publishedVersion，并继续处理mailbox waitingList中的version=3的事件
        waitHandle.waitOne();

        Assertions.assertEquals(1, versionList.get(0).intValue());
        Assertions.assertEquals(3, versionList.get(1).intValue());

        //再等待3秒，等待Enode内部异步打印Removed problem aggregate的日志
        Task.sleep(1000);
    }

    @Test
    public void create_and_update_dirty_aggregate_test() {
        String aggregateId = IdGenerator.id();
        CreateTestAggregateCommand command = new CreateTestAggregateCommand();
        command.setAggregateRootId(aggregateId);
        command.setTitle("Sample Note");
        //执行创建聚合根的命令
        CommandResult commandResult = Task.await(commandService.executeAsync(command, CommandReturnType.EventHandled));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        TestAggregate note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Sample Note", note.getTitle());
        Assertions.assertEquals(1, note.getVersion());

        String directUpdateEventStoreCommandId = IdGenerator.id();
        List<DomainEventStream> eventStreamList = new ArrayList<>();
        List<DomainEventMessage> evnts = new ArrayList<>();
        TestAggregateTitleChanged evnt = new TestAggregateTitleChanged("Note Title2");
        evnt.setAggregateRootId(aggregateId);
        evnt.setAggregateRootTypeName(TestAggregate.class.getName());
        evnt.setCommandId(directUpdateEventStoreCommandId);
        evnt.setVersion(2);
        evnts.add(evnt);
        DomainEventStream eventStream = new DomainEventStream(IdGenerator.id(), aggregateId, TestAggregate.class.getName(), new Date(), evnts, Maps.newHashMap());
        eventStreamList.add(eventStream);
        Task.await(eventStore.batchAppendAsync(eventStreamList));

        String eventProcessorName = "DefaultEventProcessor";
        Task.await(publishedVersionStore.updatePublishedVersionAsync(eventProcessorName, TestAggregate.class.getName(), aggregateId, 2));

        //执行修改聚合根的命令
        ChangeTestAggregateTitleWhenDirtyCommand command2 = new ChangeTestAggregateTitleWhenDirtyCommand();
        command2.setAggregateRootId(aggregateId);
        command2.setTitle("Changed Note");
        command2.setFirstExecute(true);
        commandResult = Task.await(commandService.executeAsync(command2));
        Assertions.assertNotNull(commandResult);
        Assertions.assertEquals(CommandStatus.Success, commandResult.getStatus());
        note = Task.await(memoryCache.getAsync(aggregateId, TestAggregate.class));
        Assertions.assertNotNull(note);
        Assertions.assertEquals("Changed Note", note.getTitle());
        Assertions.assertEquals(3, note.getVersion());
    }

    private DomainEventStream createMessage(AggregateRoot aggregateRoot) {
        return new DomainEventStream(IdGenerator.id(), aggregateRoot.getUniqueId(), aggregateRoot.getVersion() + 1, aggregateRoot.getClass().getName(), aggregateRoot.getChanges(), Maps.newHashMap());
    }

    private void assertAppendResult(EventAppendResult appendResult) {
        Assertions.assertEquals(1, appendResult.getSuccessAggregateRootIdList().size());
        Assertions.assertEquals(0, appendResult.getDuplicateCommandAggregateRootIdList().size());
        Assertions.assertEquals(0, appendResult.getDuplicateEventAggregateRootIdList().size());
    }
}