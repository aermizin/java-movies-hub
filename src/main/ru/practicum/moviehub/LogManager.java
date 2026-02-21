package ru.practicum.moviehub;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LogManager implements AutoCloseable {
    private final PrintWriter writer;

    public LogManager(String outputFileName) throws IOException {
        this.writer = new PrintWriter(new FileWriter(outputFileName, true));
    }

    public void logInfo(String message) {
        writer.println("[INFO] " + message);
    }

    public void logError(String message) {
        writer.println("[ERROR] " + message);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}

