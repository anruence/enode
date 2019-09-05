package com.enodeframework.commanding;

import com.enodeframework.common.io.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.enodeframework.common.io.Task.await;

/**
 * @author anruence@gmail.com
 */
public class ProcessingCommandMailbox {

    public static final Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

    private final Object lockObj = new Object();
    private final Object asyncLock = new Object();
    private ConcurrentHashMap<Long, ProcessingCommand> messageDict;
    private IProcessingCommandHandler messageHandler;
    private int batchSize;

    private String aggregateRootId;
    private Date lastActiveTime;
    private boolean isRunning;
    private boolean isPauseRequested;
    private boolean isPaused;
    private long nextSequence;
    private long consumingSequence;

    public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler, int batchSize) {
        this.messageDict = new ConcurrentHashMap<>();
        this.messageHandler = messageHandler;
        this.batchSize = batchSize;
        this.aggregateRootId = aggregateRootId;
        lastActiveTime = new Date();
    }

    public Date getLastActiveTime() {
        return this.lastActiveTime;
    }

    public void setLastActiveTime(Date lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPauseRequested() {
        return isPauseRequested;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public long getConsumingSequence() {
        return consumingSequence;
    }

    public long getMaxMessageSequence() {
        return nextSequence - 1;
    }

    public long getTotalUnHandledMessageCount() {
        return nextSequence - consumingSequence;
    }


    public String getAggregateRootId() {
        return aggregateRootId;
    }

    public void setAggregateRootId(String aggregateRootId) {
        this.aggregateRootId = aggregateRootId;
    }

    /**
     * 放入一个消息到MailBox，并自动尝试运行MailBox
     */
    public void enqueueMessage(ProcessingCommand message) {
        synchronized (lockObj) {
            message.setSequence(nextSequence);
            message.setMailBox(this);
            // If the specified key is not already associated with a value (or is mapped to null) associates it with the given value and returns null, else returns the current value.
            if (messageDict.putIfAbsent(message.getSequence(), message) == null) {
                nextSequence++;
                if (logger.isDebugEnabled()) {
                    logger.debug("{} enqueued new message, aggregateRootId: {}, messageSequence: {}", getClass().getName(), aggregateRootId, message.getSequence());
                }
                lastActiveTime = new Date();
                tryRun();
            }
        }
    }

    public void tryRun() {
        synchronized (lockObj) {
            if (isRunning || isPauseRequested || isPaused) {
                return;
            }
            setAsRunning();
            if (logger.isDebugEnabled()) {
                logger.debug("{} start run, aggregateRootId: {}, consumingSequence: {}", getClass().getName(), aggregateRootId, consumingSequence);
            }
            CompletableFuture.runAsync(this::processMessages);
        }
    }

    /**
     * 请求完成MailBox的单次运行，如果MailBox中还有剩余消息，则继续尝试运行下一次
     */
    public void completeRun() {
        lastActiveTime = new Date();
        if (logger.isDebugEnabled()) {
            logger.debug("{} complete run, aggregateRootId: {}", getClass().getName(), aggregateRootId);
        }
        setAsNotRunning();
        if (getTotalUnHandledMessageCount() > 0) {
            tryRun();
        }
    }

    /**
     * 暂停当前MailBox的运行，暂停成功可以确保当前MailBox不会处于运行状态，也就是不会在处理任何消息
     */
    public void pause() {
        isPauseRequested = true;
        if (logger.isDebugEnabled()) {
            logger.debug("{} pause requested, aggregateRootId: {}", getClass().getName(), aggregateRootId);
        }
        long count = 0L;
        while (isRunning) {
            Task.sleep(10);
            count++;
            if (count % 100 == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} pause requested, but wait for too long to stop the current mailbox, aggregateRootId: {}, waitCount: {}", getClass().getName(), aggregateRootId, count);
                }
            }
        }
        lastActiveTime = new Date();
        isPaused = true;
    }

    /**
     * 恢复当前MailBox的运行，恢复后，当前MailBox又可以进行运行，需要手动调用TryRun方法来运行
     */
    public void resume() {
        isPauseRequested = false;
        isPaused = false;
        lastActiveTime = new Date();
        if (logger.isDebugEnabled()) {
            logger.debug("{} resume requested, aggregateRootId: {}, consumingSequence: {}", getClass().getName(), aggregateRootId, consumingSequence);
        }
    }

    public void resetConsumingSequence(long consumingSequence) {
        this.consumingSequence = consumingSequence;
        lastActiveTime = new Date();
        if (logger.isDebugEnabled()) {
            logger.debug("{} reset consumingSequence, aggregateRootId: {}, consumingSequence: {}", getClass().getName(), aggregateRootId, consumingSequence);
        }
    }

    public void clear() {
        messageDict.clear();
        nextSequence = 0;
        consumingSequence = 0;
        lastActiveTime = new Date();
    }

    public CompletableFuture<Void> completeMessage(ProcessingCommand message, CommandResult result) {
        try {
            ProcessingCommand removed = messageDict.remove(message.getSequence());
            if (removed != null) {
                lastActiveTime = new Date();
                return message.completeAsync(result);
            }
        } catch (Exception ex) {
            logger.error("{} complete message with result failed, aggregateRootId: {}, messageId: {}, messageSequence: {}, result: {}", getClass().getName(), aggregateRootId, message.getMessage().getId(), message.getSequence(), result, ex);
        }
        return Task.completedTask;
    }

    public boolean isInactive(int timeoutSeconds) {
        return (System.currentTimeMillis() - lastActiveTime.getTime()) >= timeoutSeconds;
    }


    private void processMessages() {
        synchronized (asyncLock) {
            lastActiveTime = new Date();
            try {
                long scannedCount = 0;
                while (getTotalUnHandledMessageCount() > 0 && scannedCount < batchSize && !isPauseRequested) {
                    ProcessingCommand message = getMessage(consumingSequence);
                    if (message != null) {
                        await(messageHandler.handleAsync(message));
                    }
                    scannedCount++;
                    consumingSequence++;
                }
            } catch (Exception ex) {
                logger.error("{} run has unknown exception, aggregateRootId: {}", getClass().getName(), aggregateRootId, ex);
                Task.sleep(1);
            } finally {
                completeRun();
            }
        }
    }

    private ProcessingCommand getMessage(long sequence) {
        return messageDict.getOrDefault(sequence, null);
    }

    private void setAsRunning() {
        isRunning = true;
    }

    private void setAsNotRunning() {
        isRunning = false;
    }

}
