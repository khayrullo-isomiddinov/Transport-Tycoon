package com.team404.tycoon.model;

public enum Difficulty {
    EASY(150_000L, "Easy"),
    NORMAL(100_000L, "Normal"),
    HARD(50_000L, "Hard");

    private final long startingCapital;
    private final String label;

    Difficulty(long startingCapital, String label) {
        this.startingCapital = startingCapital;
        this.label = label;
    }

    public long getStartingCapital() {
        return startingCapital;
    }

    public String getLabel() {
        return label;
    }
}
