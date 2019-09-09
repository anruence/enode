package org.enodeframework.domain.impl;

import org.enodeframework.common.container.IObjectContainer;
import org.enodeframework.domain.IAggregateRepository;
import org.enodeframework.domain.IAggregateRepositoryProvider;
import org.enodeframework.domain.IAggregateRepositoryProxy;
import org.enodeframework.domain.IAggregateRoot;
import org.enodeframework.infrastructure.IAssemblyInitializer;
import org.enodeframework.infrastructure.TypeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author anruence@gmail.com
 */
public class DefaultAggregateRepositoryProvider implements IAggregateRepositoryProvider, IAssemblyInitializer {
    private final Map<Class, IAggregateRepositoryProxy> repositoryDict = new HashMap<>();
    @Autowired
    private IObjectContainer objectContainer;

    public DefaultAggregateRepositoryProvider setObjectContainer(IObjectContainer objectContainer) {
        this.objectContainer = objectContainer;
        return this;
    }

    @Override
    public IAggregateRepositoryProxy getRepository(Class<? extends IAggregateRoot> aggregateRootType) {
        return repositoryDict.get(aggregateRootType);
    }

    @Override
    public void initialize(Set<Class<?>> componentTypes) {
        componentTypes.stream().filter(this::isAggregateRepositoryType).forEach(this::registerAggregateRepository);
    }

    private void registerAggregateRepository(Class aggregateRepositoryType) {
        Type superGenericInterface = TypeUtils.getSuperGenericInterface(aggregateRepositoryType, IAggregateRepository.class);
        if (superGenericInterface instanceof Class) {
            return;
        }
        ParameterizedType superGenericInterfaceType = (ParameterizedType) superGenericInterface;
        IAggregateRepository resolve = (IAggregateRepository) objectContainer.resolve(aggregateRepositoryType);
        AggregateRepositoryProxy aggregateRepositoryProxy = new AggregateRepositoryProxy(resolve);
        repositoryDict.put((Class) superGenericInterfaceType.getActualTypeArguments()[0], aggregateRepositoryProxy);
    }

    private boolean isAggregateRepositoryType(Class type) {
        return type != null && !Modifier.isAbstract(type.getModifiers()) && IAggregateRepository.class.isAssignableFrom(type);
    }
}
