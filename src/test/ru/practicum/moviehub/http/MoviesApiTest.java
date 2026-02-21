package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        gson = new Gson();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode(), "GET /movies должен вернуть 200 OK");

        String contentTypeHeaderValue =
                getResponse.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = getResponse.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }


    @Test
    void getMovies_whenFileSent_returnsSuccess() throws Exception {
        Movie testMovie = new Movie("Переводчик", 2022, 1);
        String jsonRequest = gson.toJson(testMovie);


        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, getResponse.statusCode(),
                "GET /movies должен возвращать 200 OK");

        String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("\"title\":\"Переводчик\""),
                "Список фильмов должен содержать добавленный фильм Переводчик");
    }

    @Test
    void createNewMovieTest() throws Exception {
        Movie testMovie = new Movie("Гнев человеческий", 2021, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResponse.statusCode(),
                "POST /movies должен возвращать 201 Created");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("\"title\":\"Гнев человеческий\""),
                "Список фильмов должен содержать добавленный фильм Переводчик");
    }

    @Test
    void postMovie_whenTitleEmpty_returnsBadRequest() throws Exception {
        Movie testMovie = new Movie("", 2021, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, postResponse.statusCode(),
                "POST /movies 422 Unprocessable Entity");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("Название фильма не должно быть пустым"),
                "Название фильма должно быть заполнено");
    }

    @Test
    void createMovie_whenTitleExceeds100Chars_returnsBadRequest() throws Exception {
        Movie testMovie = new Movie("A".repeat(200), 2021, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, postResponse.statusCode(),
                "POST /movies 422 Unprocessable Entity");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("иметь больше 100 символов."),
                "Название фильма должно иметь меньше 100 символов.");
    }

    @Test
    void createMovie_whenYearLessThan1888_returnsBadRequest() throws Exception {
        Movie testMovie = new Movie("Чебурашка", 1000, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, postResponse.statusCode(),
                "POST /movies 422 Unprocessable Entity");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("Год самого раннего из сохранившихся фильмов — 1888."));
    }

    @Test
    void createMovie_whenYearGreaterThanCurrent_returnsBadRequest() throws Exception {
        Movie testMovie = new Movie("Достать ножи", 2027, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, postResponse.statusCode(),
                "POST /movies 422 Unprocessable Entity");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("Год выпуска не может быть больше текущего года."));
    }

    @Test
    void postMovie_withInvalidContentType_returnsUnsupportedMediaType() throws Exception {
        Movie testMovie = new Movie("Зеленая книга", 2018, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, postResponse.statusCode(),
                "POST /movies 415 Unsupported Media Type");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("Неподдерживаемый Content-Type:"));
    }

    @Test
    void postMovie_withInvalidJson_returnsBadRequest()  throws Exception {
        String jsonRequest = "{\"title\": \"Достать ножи\", \"year\": 2019,";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, postResponse.statusCode(),
                "POST /movies 400 Bad Request");
        String postResponseBody = postResponse.body().trim();
        assertTrue(postResponseBody.contains("Некорректный JSON:"),
                "Тело ответа должно содержать описание ошибки JSON");
    }

    @Test
    void getMovie_byExistingId_returnsMovie() throws Exception {
        Movie testMovie = new Movie("Джентльмены", 2019, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String postResponseBody = postResponse.body().trim();
        Movie createdMovie = gson.fromJson(postResponseBody, Movie.class);
        int movieId = createdMovie.getId();

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode(),
                "GET /movies/{id} должен возвращать 200 OK");

        String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("\"id\":" + movieId),
                "Должен вернуться фильм по заданному id");
    }

    @Test
    void getMovieById_whenMovieNotFound_returnsBadRequest() throws Exception {
        int movieId = 777;

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResponse.statusCode(),
                "GET /movies/{id} должен возвращать 404 Not Found");

        String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenIdIsNotANumber_returnsBadRequest() throws Exception {
        String movieId = "String";

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, getResponse.statusCode(),
                "GET /movies/{id} должен возвращать 400 Bad Request");

        String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("Неверный формат идентификатора фильма."));
    }

    @Test
    void deleteMovieById_whenMovieExists_returns204NoContent() throws Exception  {
        Movie testMovie = new Movie("Один дома", 1990, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String postResponseBody = postResponse.body().trim();
        Movie createdMovie = gson.fromJson(postResponseBody, Movie.class);
        int movieId = createdMovie.getId();

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> delResponse =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, delResponse.statusCode(),
                "DELETE /movies/{id} должен возвращать 204 No Content");

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResponse.statusCode(),
                "GET /movies/{id} должен возвращать 404 Not Found, после того как мы его удалили.");

    }

    @Test
    void deleteMovieById_whenMovieNotFound_returnsBadRequest() throws Exception  {
        int movieId = 101;

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> delResponse =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, delResponse.statusCode(),
                "DELETE /movies/{id} должен возвращать 404 Not Found");
        String getResponseBody = delResponse.body().trim();
        assertTrue(getResponseBody.contains("Фильм не найден."));
    }

    @Test
    void deleteMovieById_whenIdIsNotANumber_returnsBadRequest() throws Exception {
        String movieId = "String";

        HttpRequest delReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> delResponse =
                client.send(delReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, delResponse.statusCode(),
                "DELETE /movies/{id} должен возвращать 400 Bad Request");
        String getResponseBody = delResponse.body().trim();
        assertTrue(getResponseBody.contains("Неверный формат идентификатора фильма."));
    }

    @Test
    void getMoviesByYear_whenYearProvided_returnsFilteredList() throws Exception {
        Movie testMovie = new Movie("Дюна", 2021, 1);
        String jsonRequest = gson.toJson(testMovie);

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> postResponse =
                client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie testMovie1 = new Movie("Бойцовский клуб", 1999, 1);
        String jsonRequest1 = gson.toJson(testMovie1);

        HttpRequest postReq1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest1))
                .build();

        HttpResponse<String> postResponse1 =
                client.send(postReq1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie testMovie2 = new Movie("Остров проклятых", 2009, 1);
        String jsonRequest2 = gson.toJson(testMovie2);

        HttpRequest postReq2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest2))
                .build();

        HttpResponse<String> postResponse2 =
                client.send(postReq2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Movie testMovie3 = new Movie("Гнев человеческий", 2021, 1);
        String jsonRequest3 = gson.toJson(testMovie3);

        HttpRequest postReq3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest3))
                .build();

        HttpResponse<String> postResponse3 =
                client.send(postReq3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int yearFilter = 2021;
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=" + yearFilter))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode(), "GET /movies?year=2021 должен вернуть 200 OK");

      String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("\"title\":\"Гнев человеческий\""));
        assertTrue(getResponseBody.contains("\"title\":\"Дюна\""),
                "Список должен возвращать фильмы только с указанным годом " + yearFilter);
        assertFalse(getResponseBody.contains("\"title\":\"Остров проклятых\""),
                "Возвращаемом списке не должны присутствовать фильмы других годов выпуска.");
    }

    @Test
    void getMoviesByYear_whenIdIsNotANumber_returnsBadRequest() throws Exception {
        int yearFilter = 2020;
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=" + yearFilter))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResponse.statusCode(), "GET /movies?year=2020 должен вернуть 200 OK");

        String body = getResponse.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Должен вернутся пустой массив при отсутствии фильмов указанного года " + yearFilter);
    }

    @Test
    void getMoviesByYear_whenNoMoviesExistForYear_returnsEmptyList() throws Exception {
        String movieId = "String";
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=" + movieId))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, getResponse.statusCode(), "GET /movies?year=YYYY должен вернуть 400 " +
                "Bad Request");

        String getResponseBody = getResponse.body().trim();
        assertTrue(getResponseBody.contains("Некорректный формат года. Ожидается число."));
    }
}