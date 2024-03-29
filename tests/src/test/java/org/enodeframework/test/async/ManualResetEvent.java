package org.enodeframework.test.async;

import org.enodeframework.common.exception.EnodeInterruptException;

/**
 * @author anruence@gmail.com
 */
public class ManualResetEvent {

    private final Object monitor = new Object();

    private volatile boolean open;

    public ManualResetEvent(boolean initialState) {
        open = initialState;
    }

    public boolean waitOne() {
        synchronized (monitor) {
            if (!open) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    throw new EnodeInterruptException(e);
                }
            }
            return open;
        }
    }

    public boolean waitOne(long timeout) {
        synchronized (monitor) {
            if (!open) {
                try {
                    monitor.wait(timeout);
                } catch (InterruptedException e) {
                    throw new EnodeInterruptException(e);
                }
            }
            return open;
        }
    }

    public void set() {
        synchronized (monitor) {
            open = true;
            monitor.notifyAll();
        }
    }

    public void reset() {
        open = false;
    }
}
