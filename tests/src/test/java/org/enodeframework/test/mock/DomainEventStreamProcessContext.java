package org.enodeframework.test.mock;

import org.enodeframework.common.extensions.ManualResetEvent;
import org.enodeframework.common.io.Task;
import org.enodeframework.eventing.DomainEventStreamMessage;
import org.enodeframework.eventing.IEventProcessContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DomainEventStreamProcessContext implements IEventProcessContext {
    private final DomainEventStreamMessage domainEventStreamMessage;
    private final ManualResetEvent manualResetEvent;
    private final List<Integer> versionList;

    public DomainEventStreamProcessContext(DomainEventStreamMessage domainEventStreamMessage, ManualResetEvent waitHandle, List<Integer> versionList) {
        this.domainEventStreamMessage = domainEventStreamMessage;
        manualResetEvent = waitHandle;
        this.versionList = versionList;
    }

    @Override
    public CompletableFuture<Boolean> notifyEventProcessed() {
        versionList.add(domainEventStreamMessage.getVersion());
        if (domainEventStreamMessage.getVersion() == 3) {
            manualResetEvent.set();
        }
        return Task.completedTask;
    }
}
