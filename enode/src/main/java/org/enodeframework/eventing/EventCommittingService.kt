package org.enodeframework.eventing

import org.enodeframework.commanding.ProcessingCommand
import java.util.concurrent.CompletableFuture

interface EventCommittingService {
    /**
     * Commit the given aggregate's domain events to the eventstore async and publish the domain events.
     */
    fun commitDomainEventAsync(eventCommittingContext: EventCommittingContext)

    /**
     * Publish the given domain event stream async.
     */
    fun publishDomainEventAsync(
        processingCommand: ProcessingCommand,
        eventStream: DomainEventStream
    ): CompletableFuture<Boolean>
}