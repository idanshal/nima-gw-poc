package io.examples.helidon.nima;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.helidon.common.http.Http.Header.AUTHORIZATION;
import static io.helidon.common.http.Http.HeaderValues.CONTENT_LENGTH_ZERO;

@RequiredArgsConstructor
@Slf4j
class GatewayService implements HttpService {
    private static final String PATH_DELIMITER = "/";
    private static final String WILDCARD_PATH = "**";

    private final Http1Client client;
    private final Map<String, String> routeMap;
    private final String jwkSourceUrl;

    @Override
    public void routing(HttpRules httpRules) {
        this.routeMap.keySet().forEach(path ->
                httpRules.any(path + WILDCARD_PATH, this::authMiddleware, this::handle));
    }

    private String dispatchUri(String path) {
        return this.routeMap.get(path);
    }

    private void authMiddleware(ServerRequest req, ServerResponse res) {
        String token = req.headers().get(AUTHORIZATION).value().split(" ")[1];

        if (!JwtService.verify(token, this.jwkSourceUrl)) {
            res.status(Http.Status.UNAUTHORIZED_401);
            res.send();
            return;
        }

        res.next();
    }

    private void handle(ServerRequest req, ServerResponse res) throws IOException {
        var clientRequest = buildClientRequest(this.client, req);
        try (var clientResponse = clientRequest.outputStream(os -> handleOutputStream(os, req))) {
            // set all headers from response
            clientResponse.headers().forEach(res::header);
            // transfer entity from response
            try (var outputStream = res.outputStream(); var inputStream = clientResponse.inputStream();) {
                inputStream.transferTo(outputStream);
            }
        }
    }

    private Http1ClientRequest buildClientRequest(Http1Client client, ServerRequest req) {
        var uri = assembleUri(req);
        var clientRequest = client.method(req.prologue().method()).uri(uri);

        // set all headers from request
        req.headers().forEach(clientRequest::header);

        if (req.headers().contentLength().isEmpty()) {
            clientRequest.header(CONTENT_LENGTH_ZERO);
        }

        return clientRequest;
    }

    private String assembleUri(ServerRequest req) {
        var path = req.path().path();
        var basePath = PATH_DELIMITER + path.split("/")[1];
        var baseUri = this.dispatchUri(basePath);
        return baseUri + path;
    }

    private void handleOutputStream(OutputStream os, ServerRequest req) throws IOException {
        // transfer entity from request
        try (os; var inputStream = req.content().inputStream()) {
            inputStream.transferTo(os);
        }
    }
}

