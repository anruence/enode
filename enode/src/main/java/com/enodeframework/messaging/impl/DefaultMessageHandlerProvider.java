package com.enodeframework.messaging.impl;

import com.enodeframework.common.container.IObjectContainer;
import com.enodeframework.infrastructure.impl.AbstractHandlerProvider;
import com.enodeframework.messaging.IMessage;
import com.enodeframework.messaging.IMessageHandlerProvider;
import com.enodeframework.messaging.IMessageHandlerProxy1;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * @author anruence@gmail.com
 */
public class DefaultMessageHandlerProvider extends AbstractHandlerProvider<Class, IMessageHandlerProxy1, Class> implements IMessageHandlerProvider {
    @Autowired
    private IObjectContainer objectContainer;

    @Override
    protected Class getKey(Method method) {
        return method.getParameterTypes()[0];
    }

    @Override
    protected Class<? extends IMessageHandlerProxy1> getHandlerProxyImplementationType() {
        return MessageHandlerProxy1.class;
    }

    @Override
    protected boolean isHandlerSourceMatchKey(Class handlerSource, Class key) {
        return key.equals(handlerSource);
    }

    @Override
    protected boolean isHandleMethodMatch(Method method) {
        if (method.getParameterTypes().length != 1) {
            return false;
        }
        if (IMessage.class.equals(method.getParameterTypes()[0])) {
            return false;
        }
        if (!IMessage.class.isAssignableFrom(method.getParameterTypes()[0])) {
            return false;
        }
        return isMethodAnnotationSubscribe(method);
    }

    @Override
    protected IObjectContainer getObjectContainer() {
        return objectContainer;
    }

    public DefaultMessageHandlerProvider setObjectContainer(IObjectContainer objectContainer) {
        this.objectContainer = objectContainer;
        return this;
    }
}
