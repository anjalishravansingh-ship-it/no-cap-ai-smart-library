package com.nocapai.model;

/** Represents a single book title in the library catalogue. */
public class Book {
    private String id;
    private String title;
    private String author;
    private String genre;
    private int totalCopies;
    private int availableCopies;

    public Book(String id, String title, String author, String genre, int totalCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public String toString() {
        return id + " | " + title + " | " + author + " | " + genre
                + " | " + availableCopies + "/" + totalCopies + " available";
    }
}
