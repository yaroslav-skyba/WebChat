package io.github.yarunkan.webchat.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        new Main().start();
    }

    public void start() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", exchange -> {
            final String requestMethod = exchange.getRequestMethod();
            final Headers requestHeaders = exchange.getRequestHeaders();
            final String message;

            try (InputStream requestBodyInput = exchange.getRequestBody()) {
                message = new String(requestBodyInput.readAllBytes(), StandardCharsets.UTF_8);
            }

            final Headers responseHeaders = exchange.getResponseHeaders();
            exchange.sendResponseHeaders(exchange.getResponseCode(), 0);

            try (OutputStream responseBodyOutput = exchange.getResponseBody()) {
                responseBodyOutput.write("Hello".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.setExecutor(null);
        server.start();
    }
}