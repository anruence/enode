package org.enodeframework.test.eventhandler;

import org.enodeframework.annotation.Event;
import org.enodeframework.annotation.Priority;
import org.enodeframework.annotation.Subscribe;
import org.enodeframework.test.EnodeCoreTest;
import org.enodeframework.test.domain.Event1;
import org.enodeframework.test.domain.Event2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

@Priority(1)
@Event
public class Handler123 {
    private final static Logger logger = LoggerFactory.getLogger(Handler123.class);

    @Priority(4)
    @Subscribe
    public void handleAsync(Event1 evnt, Event2 evnt2) {
        logger.info("event1,event2 handled by handler3.");
        EnodeCoreTest.HandlerTypes.computeIfAbsent(2, k -> new ArrayList<>()).add(getClass().getName());
    }
}