package com.loyalty.capstone.service;

/** UC-LB-04 result. 'stale' carries the CON.4 outcome rather than hiding it. */
public final class PointLiabilityReport {
    public final long outstandingPoints;
    public final double liabilityUsd;
    public final boolean stale;
    public final long dataAgeSeconds;

    public PointLiabilityReport(long outstandingPoints, double liabilityUsd, boolean stale, long dataAgeSeconds) {
        this.outstandingPoints = outstandingPoints;
        this.liabilityUsd = liabilityUsd;
        this.stale = stale;
        this.dataAgeSeconds = dataAgeSeconds;
    }
}
