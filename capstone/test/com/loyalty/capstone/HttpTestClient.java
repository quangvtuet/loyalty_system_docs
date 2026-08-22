package com.loyalty.capstone;

import com.loyalty.capstone.gateway.ApiGateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

/** Shared harness for tests that exercise the API Gateway over real HTTP. */
final class HttpTestClient {

    static final Instant START = Instant.parse("2026-08-22T09:00:00Z");
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private HttpTestClient() { }

    interface RuntimeCase {
        void run(String baseUrl, MutableClock clock, Platform platform) throws Exception;
    }

    static void withRuntime(RuntimeCase body) {
        MutableClock clock = new MutableClock(START);
        Platform platform = new Platform(clock);
        ApiGateway gateway = new ApiGateway(platform);
        try {
            gateway.start(0);
            body.run("http://localhost:" + gateway.port(), clock, platform);
        } catch (RuntimeException runtimeFailure) {
            throw runtimeFailure;
        } catch (Exception checkedFailure) {
            throw new RuntimeException(checkedFailure);
        } finally {
            gateway.stop();
        }
    }

    static HttpResponse<String> post(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
