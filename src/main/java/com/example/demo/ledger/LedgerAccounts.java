package com.example.demo.ledger;

/** Account codes seeded by V7 (numbering follows the Vietnamese chart of accounts, Circular 200). */
public final class LedgerAccounts {

    /** Asset: money customers paid through MoMo that MoMo owes the shop. */
    public static final String MOMO_RECEIVABLE = "1130";
    /** Liability: money received that must go back to the customer (paid twice / after cancellation). */
    public static final String REFUND_PAYABLE = "3388";
    /** Revenue from sales. */
    public static final String SALES_REVENUE = "5110";

    private LedgerAccounts() {
    }
}
