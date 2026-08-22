package com.loyalty.capstone;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Test clock so CON.4 staleness can be reached without waiting ten minutes. */
public final class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant start) { this(start, ZoneOffset.UTC); }

    private MutableClock(Instant start, ZoneId zone) {
        this.instant = start;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() { return zone; }

    @Override
    public Clock withZone(ZoneId newZone) { return new MutableClock(instant, newZone); }

    @Override
    public Instant instant() { return instant; }

    public void advanceSeconds(long seconds) { instant = instant.plusSeconds(seconds); }
}
