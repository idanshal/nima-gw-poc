package io.examples.helidon.nima;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.log4j.BasicConfigurator.configure;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Bootstrap {
    private static final Http.HeaderValue SERVER = Http.Header.create(Http.Header.SERVER, "Nima");
    private static final String WEB_SERVER_PORT_CONFIG_ENTRY = "server.port";
    private static final String GATEWAY_ROUTES_CONFIG_ENTRY = "gw.routes";
    private static final String HTTP_CLIENT_CONNECTION_QUEUE_SIZE_CONFIG_ENTRY = "gw.http-client.connection-queue-size";
    private static final String AUTH_JWK_SOURCE_URL_CONFIG_ENTRY = "gw.auth.jwk-source-url";
    private static final Integer DEFAULT_HTTP_CLIENT_CONNECTION_QUEUE_SIZE = 256;
    private static final String LOG_PATTERN = "[%t] %p %c %x - %m%n";

    public static void run(Config config) {
        configure(new ConsoleAppender(new PatternLayout(LOG_PATTERN)));
        int httpClientConnectionQueueSize = config.get(HTTP_CLIENT_CONNECTION_QUEUE_SIZE_CONFIG_ENTRY).asInt().orElse(DEFAULT_HTTP_CLIENT_CONNECTION_QUEUE_SIZE);

        var client = Http1Client.builder().connectionQueueSize(httpClientConnectionQueueSize).build();
        var routeMap = initRouteMapFromConfig(config);
        var jwkSourceUrl = config.get(AUTH_JWK_SOURCE_URL_CONFIG_ENTRY).asString().orElse(null);
        var gatewayService = new GatewayService(client, routeMap, jwkSourceUrl);

        int webServerPort = config.get(WEB_SERVER_PORT_CONFIG_ENTRY).asInt().get();
        log.info("config -> web server port: {}; route map: {}; http client connection queue size: {}", webServerPort, routeMap, httpClientConnectionQueueSize);
        WebServer.builder()
                .routing(rules -> routing(rules, gatewayService))
                .port(webServerPort)
                .start();
    }

    public static Map<String, String> initRouteMapFromConfig(Config config) {
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
