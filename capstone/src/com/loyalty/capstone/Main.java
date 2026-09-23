package com.loyalty.capstone;

import com.loyalty.capstone.gateway.ApiGateway;

import java.time.Clock;

/** Starts the collapsed runtime behind API Gateway. */
public final class Main {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Platform platform = new Platform(Clock.systemUTC());
        ApiGateway gateway = new ApiGateway(platform);
        gateway.start(port);
        System.out.println("Loyalty Banking Platform capstone runtime listening on port " + port);
        System.out.println("POST /partner-earn   POST /redemptions   GET /reports/point-liability");
    }
}
