package dev.mgmeral.ticket.model;

public record PurchaseCreateResult(PurchaseResponse response, boolean created) {
    public static PurchaseCreateResult created(PurchaseResponse r) {
        return new PurchaseCreateResult(r, true);
    }

    public static PurchaseCreateResult existing(PurchaseResponse r) {
        return new PurchaseCreateResult(r, false);
    }
}