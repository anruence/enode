package com.microsoft.conference.common.management.commands;

import org.enodeframework.commanding.Command;

public class RemoveSeatType extends Command<String> {
    public String SeatTypeId;

    public RemoveSeatType() {
    }

    public RemoveSeatType(String conferenceId) {
        super(conferenceId);
    }
}
