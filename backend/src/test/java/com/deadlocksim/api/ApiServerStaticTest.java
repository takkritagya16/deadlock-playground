package com.deadlocksim.api;

import com.deadlocksim.engine.SimulationEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerStaticTest {
    @TempDir
    Path tempDir;

    private SimulationEngine engine;
    private ApiServer server;
    private int boundPort;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        engine = new SimulationEngine();

        Files.writeString(tempDir.resolve("index.html"), "<!DOCTYPE html><html><body><h1>Hello Test</h1></body></html>", StandardCharsets.UTF_8);
        Path cssDir = tempDir.resolve("css");
        Files.createDirectories(cssDir);
        Files.writeString(cssDir.resolve("style.css"), "body { color: #222; }", StandardCharsets.UTF_8);

        server = new ApiServer(engine, 0, tempDir);
        boundPort = server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getRootReturnsIndexHtmlWithTextHtml() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/html", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("<h1>Hello Test</h1>"));
    }

    @Test
    void getCssReturnsTextCss() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/css/style.css"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/css", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("color: #222"));
    }

    @Test
    void pathContainingDotDotReturns404() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", boundPort), 3000);
            OutputStream os = socket.getOutputStream();
            os.write("GET /../secret.txt HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            assertTrue(statusLine != null && statusLine.contains("404"), "Expected 404 response status, got: " + statusLine);
        }
    }

    @Test
    void missingFileReturns404() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/missing.html"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void getApiHealthStillReturnsHealthBody() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + boundPort + "/api/health"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }
}
