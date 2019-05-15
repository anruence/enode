package com.enodeframework.common.scheduling;

import com.enodeframework.common.function.Action;
import com.enodeframework.common.logging.ENodeLogger;
import org.slf4j.Logger;

public class Worker {
    private static final Logger logger = ENodeLogger.getLog();

    private Object lockObject = new Object();
    private String actionName;
    private Action action;
    private Status status;
    private Thread thread;

    public Worker(String actionName, Action action) {
        this.actionName = actionName;
        this.action = action;
        this.status = Status.Initial;
    }

    public String actionName() {
        return this.actionName;
    }

    public Worker start() {
        synchronized (lockObject) {
            if (status.equals(Status.Running)) {
                return this;
            }

            status = Status.Running;

            thread = new Thread(this::loop, String.format("%s.Worker", actionName));

            thread.setDaemon(true);

            thread.start();

            return this;
        }
    }

    public Worker stop() {
        synchronized (lockObject) {
            if (status.equals(Status.StopRequested)) {
                return this;
            }

            status = Status.StopRequested;

            thread.interrupt();

            logger.info("Worker thread shutdown,thread id:{}", thread.getName());

            return this;
        }
    }

    private void loop() {
        while (this.status == Status.Running) {
            try {
                action.apply();
            } catch (InterruptedException e) {
                if (status != Status.StopRequested) {
                    logger.info("Worker thread caught ThreadAbortException, try to resetting, actionName:{}", actionName);
                    logger.info("Worker thread ThreadAbortException resetted, actionName:{}", actionName);
                }
            } catch (Exception ex) {
                logger.error(String.format("Worker thread has exception, actionName:%s", actionName), ex);
            }
        }
    }

    enum Status {
        Initial,
        Running,
        StopRequested
    }
}
