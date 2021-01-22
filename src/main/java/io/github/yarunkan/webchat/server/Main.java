package io.github.yarunkan.webchat.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private final List<String> messageList = new ArrayList<>();
    private final List<String> clientsIdList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new Main().start();
    }

    public void start() throws IOException {
        final int backlog = 0;
        final InetSocketAddress serverSocketAddress = new InetSocketAddress(80);
        final HttpServer server = HttpServer.create(serverSocketAddress, backlog);
        server.createContext("/", exchange -> {
            final String requestMethod = getRequestMethod(exchange);

            getRequestHeaders(exchange);

            String message = getRequestBody(exchange);

            getResponseHeaders(exchange);

            if (requestMethod.equals("GET")) {
                message = processGETRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                processPOSTRequest(message);
            }

            sendResponseHeaders(exchange, message);

            getResponseBody(exchange, message);
        });
        server.setExecutor(null);
        server.start();

        System.out.println("The server is started on port 80");
    }

    private String getRequestMethod(HttpExchange exchange) {
        return exchange.getRequestMethod();
    }

    private void getRequestHeaders(HttpExchange exchange) {
        final Headers requestHeaders = exchange.getRequestHeaders();
    }

    @NotNull
    private String getRequestBody(HttpExchange exchange) throws IOException {
        final String message;
        try (InputStream requestBodyInput = exchange.getRequestBody()) {
            message = new String(requestBodyInput.readAllBytes());
        }
        return message;
    }

    private void getResponseHeaders(HttpExchange exchange) {
        final Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.add("Access-Control-Allow-Origin", "*");
    }

    private void sendResponseHeaders(HttpExchange exchange, String message) throws IOException {
        exchange.sendResponseHeaders(200, message.length());
    }

    private void getResponseBody(HttpExchange exchange, String message) throws IOException {
        try (OutputStream responseBodyOutput = exchange.getResponseBody()) {
            responseBodyOutput.write(message.getBytes());
        }
    }

    private void processPOSTRequest(String message) {
        messageList.add(message);
        System.out.println(message);
    }

    private String processGETRequest(HttpExchange exchange) {
        final URI requestURI = exchange.getRequestURI();
        final String query = requestURI.getQuery();

        StringBuilder response = new StringBuilder();

        if (query != null) {
            final Map<String, String> parsedQuery = parseQuery(query);
            final String id = parsedQuery.get("id");
            final int parsedId = parseId(id);

            if (id.equals("unknown")) {
                final String currentUserId = Integer.toString(clientsIdList.size());
                clientsIdList.add(currentUserId);

                response = new StringBuilder(currentUserId + "\n");

                for (String message : messageList) {
                    response.append(message).append("\n");
                }
            } else if (parsedId != -1) {
                final String lastMessage;

                if (messageList.isEmpty()) {
                    lastMessage = "";
                } else {
                    lastMessage = messageList.get(messageList.size() - 1);
                }

                response = new StringBuilder(lastMessage);
            }
        }

        return response.toString();
    }

    @NotNull
    private Map<String, String> parseQuery(String query) {
        final Map<String, String> parsedQueryMap = new HashMap<>();
        final String[] splitQueryStrings = query.split("=");
        parsedQueryMap.put(splitQueryStrings[0], splitQueryStrings[1]);

        return parsedQueryMap;
    }

    private int parseId(String id) {
        final Pattern pattern = Pattern.compile("[0-9]");
        final Matcher matcher = pattern.matcher(id);
        final boolean isNumber = matcher.matches();

        if (isNumber) {
            return Integer.parseInt(id);
        }

        return -1;
    }
}