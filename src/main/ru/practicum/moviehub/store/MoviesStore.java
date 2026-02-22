package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;


import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {

    private final Map<Integer, Movie> movieMap = new HashMap<>();
    private final Set<Integer> usedIds = new HashSet<>();
    private final Random random = new Random();

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movieMap.values());
    }

    public Movie addMovie(Movie movie) {

        int id = generateUniqueId();
        movie.setId(id);
        movieMap.put(id, movie);

        return movie;
    }

    public Movie getMovieById(Integer id) {
        return movieMap.get(id);
    }

    public List<Movie> filterMoviesByReleaseYear(int year) {
        return movieMap.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }


    public Movie deleteMovieById(Integer id) {
        return movieMap.remove(id);
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