package com.loyalty.capstone.domain;

/** I-7 object owned by Earning DB. */
public final class PointBalance {
    public final String memberId;
    private long confirmedPoints;

    public PointBalance(String memberId) { this.memberId = memberId; }

    public long confirmedPoints() { return confirmedPoints; }

    public void credit(long points) { confirmedPoints += points; }

    public void debit(long points) { confirmedPoints -= points; }
}
