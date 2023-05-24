package io.examples.helidon.nima;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import java.util.*;
import java.util.stream.Collectors;

public class NimaMain {
    private static final Http.HeaderValue SERVER = Http.Header.create(Http.Header.SERVER, "Nima");
    private static final String WEB_SERVER_PORT_CONFIG_ENTRY = "server.port";
    private static final String GATEWAY_ROUTES_CONFIG_ENTRY = "gw.routes";

    public static void main(String[] args) {
        Config config = Config.create();
        var webServerPort = config.get(WEB_SERVER_PORT_CONFIG_ENTRY).asInt().get();
        var client = Http1Client.builder().build();
        var routeMap = initRouteMapFromConfig(config);
        var gatewayService = new GatewayService(client,routeMap);

        WebServer.builder()
                .routing(rules -> routing(rules,gatewayService))
                .port(webServerPort)
                .start();
    }

    private static Map<String, String> initRouteMapFromConfig(Config config) {
        return config.get(GATEWAY_ROUTES_CONFIG_ENTRY)
                .asNodeList()
                .orElseThrow(() -> new RuntimeException("missing route config"))
                .stream()
                .collect(Collectors.toMap(
                        route -> route.get("path").asString().orElseThrow(() -> new RuntimeException("route missing path")),
                        route -> route.get("uri").asString().orElseThrow(() -> new RuntimeException("route missing uri"))
                ));
    }

    static void routing(HttpRouting.Builder rules, GatewayService gatewayService) {
        rules.addFilter((chain, req, res) -> {
                    res.header(SERVER);
                    chain.proceed();
                })
                .register("/", gatewayService);
    }
}
