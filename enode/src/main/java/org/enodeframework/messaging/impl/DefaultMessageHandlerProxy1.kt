package org.enodeframework.messaging.impl

import kotlinx.coroutines.future.await
import org.enodeframework.messaging.Message
import org.enodeframework.messaging.MessageHandlerProxy1
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.invoke.MethodHandle
import java.lang.reflect.Method
import java.util.concurrent.CompletionStage
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.jvm.kotlinFunction

/**
 * @author anruence@gmail.com
 */
class DefaultMessageHandlerProxy1 : MessageHandlerProxy1 {
    private lateinit var innerObject: Any
    private lateinit var methodHandle: MethodHandle
    private lateinit var method: Method

    override suspend fun handleAsync(message: Message) {
        if (method.kotlinFunction?.isSuspend == true) {
            invokeSuspend(getInnerObject(), message)
            return
        }
        if (CompletionStage::class.java.isAssignableFrom(method.returnType)) {
            val result = methodHandle.invoke(getInnerObject(), message) as CompletionStage<*>
            Mono.fromCompletionStage(result).toFuture().await()
        } else if (Mono::class.java.isAssignableFrom(method.returnType)) {
            val result = methodHandle.invoke(getInnerObject(), message) as Mono<*>
            result.toFuture().await()
        } else if (Flux::class.java.isAssignableFrom(method.returnType)) {
            val result = methodHandle.invoke(getInnerObject(), message) as Flux<*>
            result.all { true }.toFuture().await()
        } else {
            Mono.fromCallable {
                methodHandle.invoke(getInnerObject(), message)
            }.toFuture().await()
        }
    }

    private suspend fun invokeSuspend(obj: Any, message: Message): Any? =
        suspendCoroutineUninterceptedOrReturn { continuation ->
            methodHandle.invoke(obj, message, continuation)
        }

    override fun getInnerObject(): Any {
        return innerObject
    }

    override fun setInnerObject(innerObject: Any) {
        this.innerObject = innerObject
    }

    override fun setMethodHandle(methodHandle: MethodHandle) {
        this.methodHandle = methodHandle
    }

    override fun getMethod(): Method {
        return method
    }

    override fun setMethod(method: Method) {
        this.method = method
    }
}