package dev.mgmeral.ticket.exception;

public class DuplicatePaymentRefException extends RuntimeException {

    private final Long existingPurchaseId;

    public DuplicatePaymentRefException(Long existingPurchaseId) {
        super("paymentRef already used by another purchase. existingPurchaseId=" + existingPurchaseId);
        this.existingPurchaseId = existingPurchaseId;
    }

    public Long getExistingPurchaseId() {
        return existingPurchaseId;
    }
}
