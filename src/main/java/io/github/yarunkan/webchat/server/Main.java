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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    private final List<Map<String, String>> messageList = new ArrayList<>();
    private final Collection<String> clientIdCollection = new ArrayList<>();
    private Map<String, Boolean> isMessageSentMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        final Main main = new Main();
        main.start();
    }

    public void start() throws IOException {
        final int backlog = 0;
        final InetSocketAddress serverSocketAddress = new InetSocketAddress(8080);
        final HttpServer server = HttpServer.create(serverSocketAddress, backlog);

        server.createContext("/", exchange -> {
            final String requestMethod = getRequestMethod(exchange);
            getRequestHeaders(exchange);
            String message = getRequestBody(exchange);
            getResponseHeaders(exchange);

            message = processRequest(exchange, requestMethod, message);

            sendResponseHeaders(exchange, message);
            getResponseBody(exchange, message);
        });

        server.setExecutor(null);
        server.start();

        System.out.println("The server is started on port 8080");
    }

    private String processRequest(HttpExchange exchange, String requestMethod, String message) {
        final URI requestURI = exchange.getRequestURI();
        final String query = requestURI.getQuery();
        final Map<String, String> parsedQueryMap = parseQuery(query);

        if (requestMethod.equals("GET")) {
            message = processGETRequest(parsedQueryMap);
        } else if (requestMethod.equals("POST")) {
            processPOSTRequest(message, parsedQueryMap);
        }

        return message;
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
        responseHeaders.add("Access-Control-Allow-Origin", "http://www.webchat.tk");
    }

    private void sendResponseHeaders(HttpExchange exchange, String message) throws IOException {
        exchange.sendResponseHeaders(200, message.length());
    }

    private void getResponseBody(HttpExchange exchange, String message) throws IOException {
        try (OutputStream responseBodyOutput = exchange.getResponseBody()) {
            responseBodyOutput.write(message.getBytes(StandardCharsets.UTF_16LE));
        }
    }

    private void processPOSTRequest(String message, Map<String, String> parsedQueryMap) {
        final String id = parsedQueryMap.get("id");
        final Map<String, String> messageMap = new HashMap<>();
        messageMap.put(id, message);
        messageList.add(messageMap);

        System.out.println(message);

        isMessageSentMap = new HashMap<>();
        for (String clientId : clientIdCollection) {
            isMessageSentMap.put(clientId, false);
        }
        isMessageSentMap.put(id, true);
    }

    private String processGETRequest(Map<String, String> parsedQueryMap) {
        StringBuilder response = new StringBuilder();

        final String id = parsedQueryMap.get("id");

        if (id.equals("unknown")) {
            final int clientIdListSize = clientIdCollection.size();
            final String currentUserId = Integer.toString(clientIdListSize);

            clientIdCollection.add(currentUserId);

            response = new StringBuilder(currentUserId + "\n");

            for (Map<String, String> messageMap : messageList) {
                final Map.Entry<String, String> messageEntry = getMessageEntry(messageMap);
                final String message = messageEntry.getValue();

                response.append(message).append("\n");
            }
        } else if (!id.equals("-1") && !messageList.isEmpty() && !isMessageSentMap.get(id)) {
            isMessageSentMap.put(id, true);

            final int messageListLastElementIndex = messageList.size() - 1;
            final Map<String, String> messageMap = messageList.get(messageListLastElementIndex);
            final Map.Entry<String, String> messageEntry = getMessageEntry(messageMap);
            final String lastMessage = messageEntry.getValue();

            response = new StringBuilder(lastMessage);
        }

        return response.toString();
    }

    private Map.Entry<String, String> getMessageEntry(Map<String, String> stringStringMap) {
        final Set<Map.Entry<String, String>> messageEntrySet = stringStringMap.entrySet();
        final Iterator<Map.Entry<String, String>> messageEntryIterator = messageEntrySet.iterator();

        return messageEntryIterator.next();
    }

    @NotNull
    private Map<String, String> parseQuery(String query) {
        final Map<String, String> parsedQueryMap = new HashMap<>();

        if (query != null) {
            final String[] splitQueryTokens = query.split("&");

            for (String splitQueryToken : splitQueryTokens) {
                final String[] splitQuerySubTokens = splitQueryToken.split("=");
                parsedQueryMap.put(splitQuerySubTokens[0], splitQuerySubTokens[1]);
            }
        }

        return parsedQueryMap;
    }
}