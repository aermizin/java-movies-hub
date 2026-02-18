package ru.practicum.moviehub.model;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Movie {

    protected String title;
    protected int year;
    protected int id;

    public Movie() {
        this.title = title;
        this.year = year;
        this.id = generateUniqueId();
    }

    private static final Set<Integer> USED_IDS = new HashSet<>();
    private static final Random RANDOM = new Random();

    private int generateUniqueId() {

        while (true) {
            int numberRandom = RANDOM.nextInt(1_000_000);
            if (USED_IDS.add(numberRandom)) {
                return numberRandom;
            }
        }
    }
}