package com.loyalty.capstone.gateway;

import com.loyalty.capstone.Platform;
import com.loyalty.capstone.domain.RedemptionOrder;
import com.loyalty.capstone.service.EarnResult;
import com.loyalty.capstone.service.PointLiabilityReport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * I-4 container 'API Gateway'. The only entry point from outside the platform.
 * It routes; it never writes a service-owned store, so the I-9 forbidden path has no route.
 */
public final class ApiGateway {

    public static final String CONTAINER = "API Gateway";

    private final Platform platform;
    private HttpServer server;

    public ApiGateway(Platform platform) { this.platform = platform; }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/partner-earn", this::handlePartnerEarn);
        server.createContext("/redemptions", this::handleRedemptions);
        server.createContext("/reports/point-liability", this::handlePointLiability);
        server.setExecutor(null);
        server.start();
    }

    /** Actual bound port. Pass 0 to start() to let the operating system choose one. */
    public int port() { return server.getAddress().getPort(); }

    /** The paths this gateway serves. The OpenAPI drift check compares against this list. */
    public static java.util.List<String> routes() {
        return java.util.Arrays.asList("/partner-earn", "/redemptions", "/reports/point-liability");
    }

    public void stop() { if (server != null) server.stop(0); }

    /**
     * CT-03/CT-04: Partner Systems → API Gateway → Earning Engine Service.
     * I-5 anti-tamper defense: the gateway reads only the three contract fields
     * (sourceTransactionId, memberId, amount). Any extra field — including a forged
     * {@code tier} — is stripped before the request reaches Earning Engine Service,
     * which computes points using its own authoritative projected tier from CT-08.
     * Test NEG-I5-02 sends a forged tier and asserts the runtime ignores it.
     */
    private void handlePartnerEarn(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, problem("method not allowed")); return; }
        Map<String, String> body = Json.parseFlat(read(exchange.getRequestBody()));
        String sourceTransactionId = body.get("sourceTransactionId");
        String memberId = body.get("memberId");
        String amount = body.get("amount");
        if (sourceTransactionId == null || memberId == null || amount == null) {
            send(exchange, 400, problem("sourceTransactionId, memberId and amount are required"));
            return;
        }
        // Only sourceTransactionId, memberId, and amount are forwarded.
        // body.get("tier") is intentionally NOT read — the earn multiplier comes from
        // EarningEngineService.projectedTier(), which is the authoritative local projection
        // maintained via the asynchronous tiering.tier_changed event (CT-08).
        EarnResult result = platform.earningEngineService.recordEarn(
                sourceTransactionId, memberId, Platform.PROGRAM_ID, Double.parseDouble(amount));
        int status = result.duplicate ? 409 : 201;
        send(exchange, status, Json.object(
                "pointTransactionId", result.pointTransactionId,
                "pointsAwarded", result.pointsAwarded,
                "duplicate", result.duplicate,
                "constraint", result.duplicate ? "CON.1" : null));
    }

    private void handleRedemptions(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, problem("method not allowed")); return; }
        Map<String, String> body = Json.parseFlat(read(exchange.getRequestBody()));
        String memberId = body.get("memberId");
        String rewardItemId = body.get("rewardItemId");
        if (memberId == null || rewardItemId == null) {
            send(exchange, 400, problem("memberId and rewardItemId are required"));
            return;
        }
        RedemptionOrder order;
        try {
            order = platform.redemptionEngineService.submitRedemption(memberId, rewardItemId);
        } catch (IllegalArgumentException unknownItem) {
            send(exchange, 404, problem(unknownItem.getMessage()));
            return;
        }
        int status = order.state().name().equals("CANCELLED") ? 422 : 201;
        send(exchange, status, Json.object(
                "orderId", order.orderId,
                "memberId", order.memberId,
                "state", order.state().name(),
                "reason", order.reason(),
                "constraint", order.state().name().equals("REVERSED") ? "CON.3" : null));
    }

    private void handlePointLiability(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { send(exchange, 405, problem("method not allowed")); return; }
        PointLiabilityReport report = platform.analyticsReportingService.pointLiabilityReport();
        send(exchange, 200, Json.object(
                "outstandingPoints", report.outstandingPoints,
                "liabilityUsd", report.liabilityUsd,
                "stale", report.stale,
                "dataAgeSeconds", report.dataAgeSeconds,
                "constraint", report.stale ? "CON.4" : null));
    }

    private String problem(String detail) { return Json.object("detail", detail); }

    private String read(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = input.read(chunk)) > 0) buffer.write(chunk, 0, read);
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
