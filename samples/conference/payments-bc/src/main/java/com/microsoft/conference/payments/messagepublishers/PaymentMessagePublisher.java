package com.microsoft.conference.payments.messagepublishers;

import com.enodeframework.applicationmessage.IApplicationMessage;
import com.enodeframework.common.io.AsyncTaskResult;
import com.enodeframework.messaging.IMessagePublisher;
import com.microsoft.conference.common.payment.message.PaymentCompletedMessage;
import com.microsoft.conference.common.payment.message.PaymentRejectedMessage;
import com.microsoft.conference.payments.domain.Events.PaymentCompleted;
import com.microsoft.conference.payments.domain.Events.PaymentRejected;

import static com.enodeframework.common.io.Task.await;

public class PaymentMessagePublisher {
    private IMessagePublisher<IApplicationMessage> _messagePublisher;

    public PaymentMessagePublisher(IMessagePublisher<IApplicationMessage> messagePublisher) {
        _messagePublisher = messagePublisher;
    }

    public AsyncTaskResult HandleAsync(PaymentCompleted evnt) {
        PaymentCompletedMessage message = new PaymentCompletedMessage();
        message.PaymentId = evnt.getAggregateRootId();
        message.ConferenceId = evnt.ConferenceId;
        message.OrderId = evnt.OrderId;
        return await(_messagePublisher.publishAsync(message));
    }

    public AsyncTaskResult HandleAsync(PaymentRejected evnt) {
        PaymentRejectedMessage message = new PaymentRejectedMessage();
        message.PaymentId = evnt.getAggregateRootId();
        message.ConferenceId = evnt.ConferenceId;
        message.OrderId = evnt.OrderId;
        return await(_messagePublisher.publishAsync(message));
    }
}
