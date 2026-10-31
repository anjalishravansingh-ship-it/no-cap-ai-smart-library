package com.nocapai.model;

import com.nocapai.ds.MyHashMap;

import java.time.LocalDate;

/** Represents a library member (student). */
public class Member {
    private String id;
    private String name;
    private String contact;

    /** Maps bookId -> due date, for every book currently issued to this member. */
    private MyHashMap<String, LocalDate> issuedDueDates;

    public Member(String id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.issuedDueDates = new MyHashMap<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public MyHashMap<String, LocalDate> getIssuedDueDates() { return issuedDueDates; }

    @Override
    public String toString() {
        return id + " | " + name + " | " + contact
                + " | Books held: " + issuedDueDates.size();
    }
}
