package com.deadlocksim;

import com.deadlocksim.api.ApiServer;
import com.deadlocksim.engine.SimulationEngine;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            port = Integer.parseInt(args[0]);
        }
        String frontendFolder = "../frontend";
        if (args != null && args.length > 1 && !args[1].isBlank()) {
            frontendFolder = args[1];
        }

        SimulationEngine engine = new SimulationEngine();
        ApiServer server = new ApiServer(engine, port, Path.of(frontendFolder));
        int boundPort = server.start();
        System.out.println("Simulator running at http://localhost:" + boundPort);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
