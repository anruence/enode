package com.microsoft.conference.registration.domain.SeatAssigning.Events;

import org.enodeframework.eventing.DomainEvent;

public class SeatUnassigned extends DomainEvent<String> {
    public int Position;

    public SeatUnassigned() {
    }

    public SeatUnassigned(int position) {
        Position = position;
    }
}
