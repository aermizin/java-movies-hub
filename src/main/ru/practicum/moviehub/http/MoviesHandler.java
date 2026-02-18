package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesService service;

    public MoviesHandler(MoviesService service) {
        this.service = service;
    }


    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();

        switch (method) {
            case "GET":
                handleGet(ex);
            case "POST":
                break;
            case "DELETE":
                break;
            default:

        }
    }

    private void handleGet(HttpExchange ex) {
        String path = ex.getRequestURI().getPath();

        if (path.endsWith("/movies")) {

        }
    }
}
