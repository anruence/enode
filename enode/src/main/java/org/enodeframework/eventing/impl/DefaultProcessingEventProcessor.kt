package org.enodeframework.eventing.impl

import com.google.common.base.Strings
import com.google.common.collect.Lists
import kotlinx.coroutines.CoroutineDispatcher
import org.enodeframework.common.extensions.SystemClock
import org.enodeframework.common.io.IOHelper.tryAsyncActionRecursively
import org.enodeframework.common.io.IOHelper.tryAsyncActionRecursivelyWithoutResult
import org.enodeframework.common.io.Task.sleep
import org.enodeframework.common.scheduling.ScheduleService
import org.enodeframework.common.serializing.SerializeService
import org.enodeframework.eventing.EnqueueMessageResult
import org.enodeframework.eventing.ProcessingEvent
import org.enodeframework.eventing.ProcessingEventMailBox
import org.enodeframework.eventing.ProcessingEventProcessor
import org.enodeframework.eventing.PublishedVersionStore
import org.enodeframework.messaging.MessageDispatcher
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author anruence@gmail.com
 */
class DefaultProcessingEventProcessor(
    private val scheduleService: ScheduleService,
    private val serializeService: SerializeService,
    private val messageDispatcher: MessageDispatcher,
    private val publishedVersionStore: PublishedVersionStore,
    private val coroutineDispatcher: CoroutineDispatcher,
    /**
     * 处理领域事件的处理器的名字，默认为DefaultEventHandler
     */
    private val domainEventStreamMessageHandlerName: String = "DefaultEventProcessor"
) : ProcessingEventProcessor {
    constructor(
        scheduleService: ScheduleService,
        serializeService: SerializeService,
        messageDispatcher: MessageDispatcher,
        publishedVersionStore: PublishedVersionStore,
        coroutineDispatcher: CoroutineDispatcher
    ) : this(
        scheduleService,
        serializeService,
        messageDispatcher,
        publishedVersionStore,
        coroutineDispatcher,
        "DefaultEventProcessor"
    )

    private val logger = LoggerFactory.getLogger(DefaultProcessingEventProcessor::class.java)
    private val scanInactiveMailBoxTaskName: String =
        "CleanInactiveProcessingEventMailBoxes_" + SystemClock.now() + Random().nextInt(10000)
    private val processTryToRefreshAggregateTaskName: String =
        "ProcessTryToRefreshAggregate_" + SystemClock.now() + Random().nextInt(10000)

    /**
     * The name of the processor
     */
    override val name = domainEventStreamMessageHandlerName

    private val toRefreshAggregateRootMailBoxDict: ConcurrentHashMap<String, ProcessingEventMailBox> =
        ConcurrentHashMap()
    private val mailboxDict: ConcurrentHashMap<String, ProcessingEventMailBox> = ConcurrentHashMap()
    private val refreshingAggregateRootDict: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap()
    private var timeoutSeconds = 3600 * 24 * 3
    private var scanExpiredAggregateIntervalMilliseconds = 5000
    private var processTryToRefreshAggregateIntervalMilliseconds = 1000

    override fun process(processingEvent: ProcessingEvent) {
        val aggregateRootId = processingEvent.message.aggregateRootId
        require(!Strings.isNullOrEmpty(aggregateRootId)) { "aggregateRootId of domain event stream cannot be null or empty, domainEventStreamId:" + processingEvent.message.id }
        var mailbox = mailboxDict.computeIfAbsent(aggregateRootId) { buildProcessingEventMailBox(processingEvent) }
        var mailboxTryUsingCount = 0L
        while (!mailbox.tryUsing()) {
            sleep(1)
            mailboxTryUsingCount++
            if (mailboxTryUsingCount % 10000 == 0L) {
                logger.warn(
                    "Event mailbox try using count: {}, aggregateRootId: {}, aggregateRootTypeName: {}",
                    mailboxTryUsingCount,
                    mailbox.aggregateRootId,
                    mailbox.aggregateRootTypeName
                )
            }
        }
        if (mailbox.isRemoved()) {
            mailbox = mailboxDict.computeIfAbsent(aggregateRootId) {
                buildProcessingEventMailBox(processingEvent)
            }
        }
        val enqueueResult = mailbox.enqueueMessage(processingEvent)
        if (enqueueResult == EnqueueMessageResult.Ignored) {
            processingEvent.processContext.notifyEventProcessed()
        } else if (enqueueResult == EnqueueMessageResult.AddToWaitingList) {
            addToRefreshAggregateMailBoxToDict(mailbox)
        }
        mailbox.exitUsing()
    }

    private fun addToRefreshAggregateMailBoxToDict(mailbox: ProcessingEventMailBox) {
        if (toRefreshAggregateRootMailBoxDict.putIfAbsent(mailbox.aggregateRootId, mailbox) == null) {
            logger.info(
                "Added toRefreshPublishedVersion aggregate mailbox, aggregateRootTypeName: {}, aggregateRootId: {}",
                mailbox.aggregateRootTypeName,
                mailbox.aggregateRootId
            )
            tryToRefreshAggregateMailBoxNextExpectingEventVersion(mailbox)
        }
    }

    private fun buildProcessingEventMailBox(processingMessage: ProcessingEvent): ProcessingEventMailBox {
        return ProcessingEventMailBox(
            processingMessage.message.aggregateRootTypeName,
            processingMessage.message.aggregateRootId,
            coroutineDispatcher
        ) { y: ProcessingEvent -> dispatchProcessingMessageAsync(y, 0) }
    }

    private fun tryToRefreshAggregateMailBoxNextExpectingEventVersion(processingEventMailBox: ProcessingEventMailBox) {
        if (refreshingAggregateRootDict.putIfAbsent(processingEventMailBox.aggregateRootId, true) == null) {
            getAggregateRootLatestPublishedEventVersion(processingEventMailBox, 0)
        }
    }

    private fun getAggregateRootLatestPublishedEventVersion(
        processingEventMailBox: ProcessingEventMailBox, retryTimes: Int
    ) {
        tryAsyncActionRecursively("GetAggregateRootLatestPublishedEventVersion", {
            publishedVersionStore.getPublishedVersionAsync(
                name, processingEventMailBox.aggregateRootTypeName, processingEventMailBox.aggregateRootId
            )
        }, { result: Int ->
            processingEventMailBox.setNextExpectingEventVersion(result + 1)
            refreshingAggregateRootDict.remove(processingEventMailBox.aggregateRootId)
        }, {
            "publishedVersionStore.GetPublishedVersionAsync has unknown exception, aggregateRootTypeName: ${processingEventMailBox.aggregateRootTypeName}, aggregateRootId: ${processingEventMailBox.aggregateRootId}"
        }, null, retryTimes, true
        )
    }

    override fun start() {
        scheduleService.startTask(
            scanInactiveMailBoxTaskName,
            { cleanInactiveMailbox() },
            scanExpiredAggregateIntervalMilliseconds,
            scanExpiredAggregateIntervalMilliseconds
        )
        scheduleService.startTask(
            processTryToRefreshAggregateTaskName,
            { processToRefreshAggregateRootMailBoxs() },
            processTryToRefreshAggregateIntervalMilliseconds,
            processTryToRefreshAggregateIntervalMilliseconds
        )
    }

    override fun stop() {
        scheduleService.stopTask(scanInactiveMailBoxTaskName)
        scheduleService.stopTask(processTryToRefreshAggregateTaskName)
    }

    private fun dispatchProcessingMessageAsync(processingEvent: ProcessingEvent, retryTimes: Int) {
        tryAsyncActionRecursivelyWithoutResult(
            "DispatchProcessingMessageAsync",
            { messageDispatcher.dispatchMessagesAsync(processingEvent.message.events) },
            {
                if (logger.isDebugEnabled) {
                    logger.debug(
                        "dispatch messages success, msg: {}", serializeService.serialize(processingEvent.message)
                    )
                }
                updatePublishedVersionAsync(processingEvent, 0)
            },
            {
                "sequence message [messageId:${processingEvent.message.id}, messageType:${processingEvent.message.javaClass.name}, aggregateRootId:${processingEvent.message.aggregateRootId}, aggregateRootVersion:${processingEvent.message.version}]"
            },
            null,
            retryTimes,
            true
        )
    }

    private fun updatePublishedVersionAsync(processingEvent: ProcessingEvent, retryTimes: Int) {
        val message = processingEvent.message
        tryAsyncActionRecursivelyWithoutResult("UpdatePublishedVersionAsync", {
            publishedVersionStore.updatePublishedVersionAsync(
                name, message.aggregateRootTypeName, message.aggregateRootId, message.version
            )
        }, {
            if (logger.isDebugEnabled) {
                logger.debug(
                    "update published version success, message ack: {}", serializeService.serialize(message)
                )
            }
            processingEvent.complete()
        }, {
            "DomainEventStreamMessage [messageId:${message.id}, messageType:${message.javaClass.name}, aggregateRootId:${message.aggregateRootId}, aggregateRootVersion:${message.version}]"
        }, null, retryTimes, true
        )
    }

    private fun processToRefreshAggregateRootMailBoxs() {
        val remainingMailboxList: MutableList<ProcessingEventMailBox> = Lists.newArrayList()
        val recoveredMailboxList: MutableList<ProcessingEventMailBox> = Lists.newArrayList()
        toRefreshAggregateRootMailBoxDict.values.forEach { aggregateRootMailBox: ProcessingEventMailBox ->
            if (aggregateRootMailBox.getWaitingMessageCount() > 0) {
                remainingMailboxList.add(aggregateRootMailBox)
            } else {
                recoveredMailboxList.add(aggregateRootMailBox)
            }
        }
        for (mailBox in remainingMailboxList) {
            tryToRefreshAggregateMailBoxNextExpectingEventVersion(mailBox)
        }
        for (mailBox in recoveredMailboxList) {
            val removed = toRefreshAggregateRootMailBoxDict.remove(mailBox.aggregateRootId)
            if (removed != null) {
                logger.info(
                    "Removed healthy aggregate mailbox, aggregateRootTypeName: {}, aggregateRootId: {}",
                    removed.aggregateRootTypeName,
                    removed.aggregateRootId
                )
            }
        }
    }

    private fun cleanInactiveMailbox() {
        val inactiveList = mailboxDict.entries.filter { entry -> isMailBoxAllowRemove(entry.value) }
        inactiveList.forEach { (key, value): Map.Entry<String, ProcessingEventMailBox> ->
            if (value.tryUsing()) {
                if (isMailBoxAllowRemove(value)) {
                    val removed = mailboxDict.remove(key)
                    if (removed != null) {
                        removed.markAsRemoved()
                        logger.info(
                            "Removed inactive domain event stream mailbox, aggregateRootTypeName: {}, aggregateRootId: {}",
                            removed.aggregateRootTypeName,
                            removed.aggregateRootId
                        )
                    }
                }
            }
        }
    }

    private fun isMailBoxAllowRemove(mailbox: ProcessingEventMailBox): Boolean {
        return (mailbox.isInactive(timeoutSeconds) && !mailbox.isRunning() && mailbox.getTotalUnHandledMessageCount() == 0 && mailbox.getWaitingMessageCount() == 0)
    }
}