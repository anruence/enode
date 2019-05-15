package com.enodeframework.eventing.impl;

import com.enodeframework.common.serializing.IJsonSerializer;
import com.enodeframework.eventing.IDomainEvent;
import com.enodeframework.eventing.IEventSerializer;
import com.enodeframework.infrastructure.IMessage;
import com.enodeframework.infrastructure.ITypeNameProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultEventSerializer implements IEventSerializer {

    @Autowired
    private ITypeNameProvider typeNameProvider;

    @Autowired
    private IJsonSerializer jsonSerializer;

    @Override
    public Map<String, String> serialize(List<IDomainEvent> evnts) {
        Map<String, String> dict = new HashMap<String, String>();

        evnts.forEach(evnt -> {
            String typeName = typeNameProvider.getTypeName(evnt.getClass());
            String eventData = jsonSerializer.serialize(evnt);
            dict.put(typeName, eventData);
        });

        return dict;
    }

    @Override
    public <TEvent extends IDomainEvent> List<TEvent> deserialize(Map<String, String> data, Class<TEvent> domainEventType) {
        List<TEvent> evnts = new ArrayList<>();
        data.forEach((key, value) -> {
            Class eventType = typeNameProvider.getType(key);
            TEvent evnt = (TEvent) jsonSerializer.deserialize(value, eventType);
            evnts.add(evnt);
        });
        evnts.sort(Comparator.comparingInt(IMessage::sequence));
        return evnts;
    }
}
