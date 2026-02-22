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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    static final String MOVIES_ENDPOINT = "/movies";

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int EARLIEST_FILM_YEAR = 1888;
    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    private final MoviesStore store;
    private final LogManager logManager;
    private final Gson gson = new Gson();
    ErrorResponse error;

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
            if (path.equals(MOVIES_ENDPOINT) && query == null) {
                List<Movie> movies = store.getAllMovies();
                String jsonResponse = gson.toJson(movies);
                sendJson(ex, 200, jsonResponse);
            } else if (path.startsWith(MOVIES_ENDPOINT + "/")) {
                String idPart = path.substring("/movies/".length());

                int id = Integer.parseInt(idPart);
                Movie movieById = store.getMovieById(id);

                if (movieById == null) {
                    error = new ErrorResponse(404, "Фильм не найден");
                    String jsonResponse = gson.toJson(error.getError());
                    sendJson(ex, error.getStatus(), jsonResponse);
                    return;
                }

                String jsonResponse = gson.toJson(movieById);
                sendJson(ex, 200, jsonResponse);
            } else if (query != null && query.contains("year=")) {
                String yearStr = query.substring(query.indexOf('=') + 1).trim();

                int year = Integer.parseInt(yearStr);
                List<Movie> moviesByReleaseYear = store.filterMoviesByReleaseYear(year);

                if (moviesByReleaseYear == null) {
                    error = new ErrorResponse(404, "Фильм не найден.");
                    String jsonResponse = gson.toJson(error.getError());
                    sendJson(ex, error.getStatus(), jsonResponse);
                    return;
                }

                String jsonResponse = gson.toJson(moviesByReleaseYear);
                sendJson(ex, 200, jsonResponse);
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "Некорректный формат числового параметра в запросе.");
            logManager.logInfo(e.getMessage());
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
            Movie parsedMovie = gson.fromJson(requestBody, Movie.class);
            List<String> errors = isValidMovie(parsedMovie);

            if (!errors.isEmpty()) {
                error = new ErrorResponse(422, "Ошибка валидации.", errors);
                String jsonResponse = gson.toJson(error.getError() + error.getErrors());
                sendJson(ex, error.getStatus(), jsonResponse);
                return;
            }

            if (path.equals(MOVIES_ENDPOINT)) {
                Movie movie = store.addMovie(parsedMovie);
                String jsonResponse = gson.toJson(movie);
                sendJson(ex, 201, jsonResponse);
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, "Некорректный JSON: " + e.getMessage());
            logManager.logInfo(e.getMessage());
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith(MOVIES_ENDPOINT + "/")) {
                String idPart = path.substring("/movies/".length());
                int id = Integer.parseInt(idPart);
                Movie movie = store.deleteMovieById(id);

                if (movie == null) {
                    error = new ErrorResponse(404, "Фильм не найден.");
                    String jsonResponse = gson.toJson(error.getError());
                    sendJson(ex, error.getStatus(), jsonResponse);
                    return;
                }

                sendNoContent(ex, 204);
            } else {
                sendJson(ex, 404, "Endpoint не найден.");
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "Некорректный формат числового параметра в запросе.");
            logManager.logInfo(e.getMessage());
        }
    }

    private List<String> isValidMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        String title = movie.getTitle();
        int year = movie.getYear();

        boolean errorTitle = (title.length() >= MAX_TITLE_LENGTH) || title.isBlank();
        boolean errorYear = (year < EARLIEST_FILM_YEAR) || (year > CURRENT_YEAR);

        if (errorTitle) {
            errors.add("Название фильма не должно быть пустым или иметь больше 100 символов.");
        }

        if (errorYear) {
            errors.add("Год выпуска не может быть больше текущего года. Год самого раннего из сохранившихся " +
                    "фильмов — 1888.");
        }

        return errors;
    }
}
