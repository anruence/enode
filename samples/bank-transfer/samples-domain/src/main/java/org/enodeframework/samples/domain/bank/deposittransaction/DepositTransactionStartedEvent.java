package org.enodeframework.samples.domain.bank.deposittransaction;

import org.enodeframework.eventing.DomainEvent;

public class DepositTransactionStartedEvent extends DomainEvent<String> {
    public String AccountId;
    public double Amount;

    public DepositTransactionStartedEvent() {
    }

    public DepositTransactionStartedEvent(String accountId, double amount) {
        AccountId = accountId;
        Amount = amount;
    }
}
