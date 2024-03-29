package org.enodeframework.domain.impl

import org.enodeframework.common.exception.AggregateRootCreateException
import org.enodeframework.domain.AggregateRoot
import org.enodeframework.domain.AggregateRootFactory

/**
 * @author anruence@gmail.com
 */
class DefaultAggregateRootFactory : AggregateRootFactory {
    /**
     * Aggregate root factory that uses a convention to create instances of aggregate root.
     * The type must declare a no-arg constructor accepting.
     * <p>
     * If the constructor is not accessible (not public), and the JVM's security setting allow it, the
     * factory will try to make it accessible use declared constructor. If that doesn't succeed, an AggregateRootCreateException is thrown.
     */
    override fun <T : AggregateRoot?> createAggregateRoot(aggregateRootType: Class<T>): T {
        return try {
            aggregateRootType.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw AggregateRootCreateException(e)
        }
    }
}