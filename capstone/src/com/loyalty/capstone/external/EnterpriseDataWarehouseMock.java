package com.loyalty.capstone.external;

import java.util.ArrayList;
import java.util.List;

/** Stub for I-3 'Enterprise Data Warehouse'. Receives period figures only. */
public final class EnterpriseDataWarehouseMock {

    private final List<String> publishedFigures = new ArrayList<>();

    public void publishPeriodFigures(String summary) { publishedFigures.add(summary); }

    public List<String> publishedFigures() { return publishedFigures; }
}
