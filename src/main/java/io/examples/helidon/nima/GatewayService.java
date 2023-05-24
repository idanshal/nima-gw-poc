package io.examples.helidon.nima;

import io.helidon.common.http.Http;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientRequest;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.nima.webserver.http.ServerRequest;
import io.helidon.nima.webserver.http.ServerResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static io.helidon.common.http.Http.HeaderValues.CONNECTION_CLOSE;
import static io.helidon.common.http.Http.HeaderValues.CONTENT_LENGTH_ZERO;

class GatewayService implements HttpService {
    private static final String PATH_DELIMITER = "/";
    private static final String WILDCARD_PATH = "**";

    private final Http1Client client;
    private final Map<String, String> routeMap;

    GatewayService(Http1Client client, Map<String, String> routeMap) {
        this.client = client;
        this.routeMap = routeMap;
    }

    @Override
    public void routing(HttpRules httpRules) {
        this.routeMap.keySet().forEach(path ->
                httpRules.any(path + WILDCARD_PATH, this::handle));
    }

    private String dispatchUri(String path) {
        return this.routeMap.get(path);
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
