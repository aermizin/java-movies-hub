package ru.practicum.moviehub.store;

import com.google.gson.Gson;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int EARLIEST_FILM_YEAR = 1888;
    public static final int CURRENT_YEAR = LocalDate.now().getYear();

    private HashMap<Integer, Movie> movieMap = new HashMap<>();
    private Set<Integer> usedIds = new HashSet<>();
    private Random random = new Random();

    public Optional<List<Movie>> getAllMovies() {
        if (!movieMap.isEmpty()) {
            return Optional.of(new ArrayList<>(movieMap.values()));
        } else {
            return Optional.empty();
        }
    }

    public Movie addMovie(String requestBody) throws IOException {
        Gson gson = new Gson();
        Movie movieClass = gson.fromJson(requestBody, Movie.class);

        Optional<List<String>> validationResult = isValidMovie(movieClass.getTitle(), movieClass.getYear());
        if (validationResult.isPresent()) {
            List<String> listErrors = validationResult.get();
            throw new ErrorResponse(422, "Ошибка валидации.", listErrors);
        }

        int id = generateUniqueId();
        movieClass.setId(id);
        movieMap.put(id, movieClass);

        return movieClass;
    }

    public Movie getMovieById(Integer id) {
        if (!movieMap.containsKey(id)) {
            throw new ErrorResponse(404, "Фильм не найден");
        }

        return movieMap.get(id);
    }

    public Optional<List<Movie>> filterMoviesByReleaseYear(int year) {
        List<Movie> filteredMovies = movieMap.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
        return filteredMovies.isEmpty() ? Optional.empty() : Optional.of(filteredMovies);
    }


    public void deleteMovieById(Integer id) {
        if (!movieMap.containsKey(id)) {
            throw new ErrorResponse(404, "Фильм не найден.");
        }

        movieMap.remove(id);
    }

    private Optional<List<String>> isValidMovie(String title, int year) {
        List<String> errors = new ArrayList<>();

        boolean errorTitle = (title.length() >= MAX_TITLE_LENGTH) || title.isBlank();
        boolean errorYear = (year < EARLIEST_FILM_YEAR) || (year > CURRENT_YEAR);

        if (errorTitle) {
            errors.add("Название фильма не должно быть пустым или иметь больше 100 символов.");
        }

        if (errorYear) {
            errors.add("Год выпуска не может быть больше текущего года. Год самого раннего из сохранившихся " +
                    "фильмов — 1888.");
        }

        return errors.isEmpty() ? Optional.empty() : Optional.of(errors);
    }

    private int generateUniqueId() {
        while (true) {
            int numberRandom = random.nextInt(1_000_000);
            if (usedIds.add(numberRandom)) {
                return numberRandom;
            }
        }
    }

}