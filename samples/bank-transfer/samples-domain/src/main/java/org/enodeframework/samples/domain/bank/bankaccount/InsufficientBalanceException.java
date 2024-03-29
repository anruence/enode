package org.enodeframework.samples.domain.bank.bankaccount;

import io.vertx.core.json.JsonObject;
import org.enodeframework.domain.AbstractDomainExceptionMessage;

import java.util.Map;

public class InsufficientBalanceException extends AbstractDomainExceptionMessage {
    /**
     * 账户ID
     */
    public String accountId;
    /**
     * 交易ID
     */
    public String transactionId;
    /**
     * 交易类型
     */
    public int transactionType;
    /**
     * 交易金额
     */
    public double amount;
    /**
     * 当前余额
     */
    public double currentBalance;
    /**
     * 当前可用余额
     */
    public double currentAvailableBalance;

    public InsufficientBalanceException() {

    }

    public InsufficientBalanceException(String accountId, String transactionId, int transactionType, double amount, double currentBalance, double currentAvailableBalance) {
        super();
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currentBalance = currentBalance;
        this.currentAvailableBalance = currentAvailableBalance;
    }

    @Override
    public void serializeTo(Map<String, Object> serializableInfo) {
        serializableInfo.put("AccountId", accountId);
        serializableInfo.put("TransactionId", transactionId);
        serializableInfo.put("TransactionType", transactionType);
        serializableInfo.put("Amount", amount);
        serializableInfo.put("CurrentBalance", currentBalance);
        serializableInfo.put("CurrentAvailableBalance", currentAvailableBalance);
    }

    @Override
    public void restoreFrom(Map<String, Object> serializableInfo) {
        JsonObject object = new JsonObject(serializableInfo);
        accountId = object.getString("AccountId");
        transactionId = object.getString("TransactionId");
        transactionType = object.getInteger("TransactionType");
        amount = object.getDouble("Amount");
        currentBalance = object.getDouble("CurrentBalance");
        currentAvailableBalance = object.getDouble("CurrentAvailableBalance");
    }
}
