package com.nocapai.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** A single issue or return event, pushed onto the Recent Actions stack. */
public class Transaction {

    public enum Type { ISSUE, RETURN, QUEUED }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private Type type;
    private String bookId;
    private String bookTitle;
    private String memberId;
    private String memberName;
    private LocalDate date;
    private LocalDate dueDate; // only meaningful for ISSUE
    private boolean wasOverdue; // only meaningful for RETURN

    public Transaction(Type type, String bookId, String bookTitle, String memberId,
                        String memberName, LocalDate date, LocalDate dueDate, boolean wasOverdue) {
        this.type = type;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.memberId = memberId;
        this.memberName = memberName;
        this.date = date;
        this.dueDate = dueDate;
        this.wasOverdue = wasOverdue;
    }

    public Type getType() { return type; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public LocalDate getDate() { return date; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isWasOverdue() { return wasOverdue; }

    @Override
    public String toString() {
        String base = "[" + date.format(FMT) + "] " + type + "  Book: " + bookTitle
                + " (" + bookId + ")  Member: " + memberName + " (" + memberId + ")";
        if (type == Type.ISSUE) {
            base += "  Due: " + dueDate.format(FMT);
        } else if (type == Type.RETURN && wasOverdue) {
            base += "  [Returned LATE]";
        }
        return base;
    }
}
