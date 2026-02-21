package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.LogManager;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;
    final private LogManager logManager;
    Gson gson = new Gson();

    public MoviesHandler(MoviesStore store, LogManager logManager) {
        this.store = store;
        this.logManager = logManager;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();

        switch (method) {
            case "GET":
                handleGet(ex);
                break;
            case "POST":
                handlePost(ex);
                break;
            case "DELETE":
                handleDelete(ex);
                break;
            default:
                sendJson(ex, 405, "Метод '" + method + "' не поддерживается.");
                break;
        }
    }


    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        try {
            if (path.equals("/movies") && query == null) {
                Optional<List<Movie>> optionalMovies = store.getAllMovies();
                if (optionalMovies.isPresent()) {
                    List<Movie> movies = optionalMovies.get();
                    String jsonResponse = gson.toJson(movies);
                    sendJson(ex, 200, jsonResponse);
                } else {
                    sendJson(ex, 200, "[]");
                }
            } else if (path.startsWith("/movies/")) {
                String idPart = path.substring("/movies/".length());

                try {
                    int id = Integer.parseInt(idPart);
                    Movie movie = store.getMovieById(id);
                    String jsonResponse = gson.toJson(movie);
                    sendJson(ex, 200, jsonResponse);
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "Неверный формат идентификатора фильма.");
                    logManager.logInfo(e.getMessage());
                }
            } else if (query != null && query.contains("year=")) {
                String yearStr = query.substring(query.indexOf('=') + 1).trim();

                try {
                    int year = Integer.parseInt(yearStr);
                    Optional<List<Movie>> optionalMoviesByReleaseYear = store.filterMoviesByReleaseYear(year);
                    if (optionalMoviesByReleaseYear.isPresent()) {
                        List<Movie> moviesByReleaseYear = optionalMoviesByReleaseYear.get();
                        String jsonResponse = gson.toJson(moviesByReleaseYear);
                        sendJson(ex, 200, jsonResponse);
                    } else {
                        sendJson(ex, 200, "[]");
                    }
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "Некорректный формат года. Ожидается число.");
                    logManager.logInfo(e.getMessage());
                }
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (ErrorResponse e) {
            sendJson(ex, e.getStatus(), e.getError());
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        Headers headers = ex.getRequestHeaders();
        String contentType = headers.getFirst("Content-Type");

        if (!contentType.startsWith("application/json")) {
            sendJson(ex, 415, "Неподдерживаемый Content-Type: " + contentType);
            return;
        }

        String path = ex.getRequestURI().getPath();

        InputStream bodyInputStream = ex.getRequestBody();
        String requestBody = new String(bodyInputStream.readAllBytes(), StandardCharsets.UTF_8);

        try {
            if (path.equals("/movies")) {
                try {
                    Movie movie = store.addMovie(requestBody);
                    String jsonResponse = gson.toJson(movie);
                    sendJson(ex, 201, jsonResponse);
                } catch (JsonSyntaxException e) {
                    sendJson(ex, 400, "Некорректный JSON: " + e.getMessage());
                    logManager.logInfo(e.getMessage());
                }
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (ErrorResponse e) {
            String jsonResponse = gson.toJson(e.getError() + e.getErrors());
            sendJson(ex, e.getStatus(), jsonResponse);
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith("/movies/")) {
                try {
                    String idPart = path.substring("/movies/".length());
                    int id = Integer.parseInt(idPart);
                    store.deleteMovieById(id);
                    sendNoContent(ex, 204);
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "Неверный формат идентификатора фильма.");
                    logManager.logInfo(e.getMessage());
                }
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (ErrorResponse e) {
            sendJson(ex, e.getStatus(), e.getError());
        }
    }
}
