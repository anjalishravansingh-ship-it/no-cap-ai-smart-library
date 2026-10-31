package com.nocapai.service;

import com.nocapai.ds.MyHashMap;
import com.nocapai.ds.MyLinkedList;
import com.nocapai.ds.MyQueue;
import com.nocapai.ds.MyStack;
import com.nocapai.ds.SortUtil;
import com.nocapai.model.Book;
import com.nocapai.model.Member;
import com.nocapai.model.Transaction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * The heart of the backend. This single class wires together every data
 * structure described in the project documentation and exposes simple,
 * GUI-friendly methods (issueBook, returnBook, addBook, ...) that the
 * Swing screens call into.
 *
 *  - MyLinkedList<Book> / MyLinkedList<Member>  -> dynamic record storage
 *  - MyHashMap<String, Book> / <String, Member> -> O(1) lookup by ID
 *  - MyHashMap<String, MyQueue<Member>>         -> one waiting queue per book
 *  - MyStack<Transaction>                       -> recent actions log
 *  - SortUtil.mergeSort / binarySearch          -> Search & Sort module
 */
public class LibraryService {

    private static final int LOAN_PERIOD_DAYS = 14;

    private final MyLinkedList<Book> bookList = new MyLinkedList<>();
    private final MyLinkedList<Member> memberList = new MyLinkedList<>();

    private final MyHashMap<String, Book> bookIndex = new MyHashMap<>();
    private final MyHashMap<String, Member> memberIndex = new MyHashMap<>();

    /** One waiting Queue per book ID, created lazily the first time it's needed. */
    private final MyHashMap<String, MyQueue<Member>> waitingQueues = new MyHashMap<>();

    /** System-wide log of recent issue/return actions (most recent on top). */
    private final MyStack<Transaction> recentActions = new MyStack<>();

    /** How many times each book has been issued — used for the "Most Popular" report. */
    private final MyHashMap<String, Integer> issueCounts = new MyHashMap<>();

    public LibraryService() {
        seedDemoData();
    }

    /** Persist the mutable library state in a small, dependency-free text format. */
    public synchronized void saveState(Path path) throws IOException {
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>();
        for (Book b : bookList) {
            lines.add("BOOK|" + enc(b.getId()) + "|" + enc(b.getTitle()) + "|" + enc(b.getAuthor()) + "|" + enc(b.getGenre()) + "|" + b.getTotalCopies() + "|" + b.getAvailableCopies());
        }
        for (Member m : memberList) {
            lines.add("MEMBER|" + enc(m.getId()) + "|" + enc(m.getName()) + "|" + enc(m.getContact()));
            for (String bookId : m.getIssuedDueDates().keys()) {
                lines.add("ISSUED|" + enc(m.getId()) + "|" + enc(bookId) + "|" + m.getIssuedDueDates().get(bookId));
            }
        }
        for (String bookId : waitingQueues.keys()) {
            MyQueue<Member> q = waitingQueues.get(bookId);
            if (q != null) for (Member m : q.toList()) lines.add("QUEUE|" + enc(bookId) + "|" + enc(m.getId()));
        }
        for (String bookId : issueCounts.keys()) {
            Integer count = issueCounts.get(bookId);
            if (count != null) lines.add("COUNT|" + enc(bookId) + "|" + count);
        }
        Files.write(path, lines);
    }

    /** Restore state saved by saveState(). Demo data is kept when no state file exists. */
    public synchronized void loadState(Path path) throws IOException {
        if (!Files.exists(path)) return;
        List<String> lines = Files.readAllLines(path);
        bookList.toList().forEach(b -> {});
        // Rebuild indexes/lists by removing seeded records through public operations.
        for (Book b : new ArrayList<>(bookList.toList())) { bookIndex.remove(b.getId()); bookList.removeIf(x -> x.getId().equals(b.getId())); }
        for (Member m : new ArrayList<>(memberList.toList())) { memberIndex.remove(m.getId()); memberList.removeIf(x -> x.getId().equals(m.getId())); }
        for (String id : new ArrayList<>(waitingQueues.keys())) waitingQueues.remove(id);
        for (String id : new ArrayList<>(issueCounts.keys())) issueCounts.remove(id);
        for (String line : lines) {
            String[] a = line.split("\\|", -1);
            if (a.length == 0) continue;
            if (a[0].equals("BOOK") && a.length >= 7) {
                Book b = new Book(dec(a[1]), dec(a[2]), dec(a[3]), dec(a[4]), Integer.parseInt(a[5]));
                b.setAvailableCopies(Integer.parseInt(a[6])); bookList.add(b); bookIndex.put(b.getId(), b);
            } else if (a[0].equals("MEMBER") && a.length >= 4) {
                Member m = new Member(dec(a[1]), dec(a[2]), dec(a[3])); memberList.add(m); memberIndex.put(m.getId(), m);
            }
        }
        for (String line : lines) {
            String[] a = line.split("\\|", -1);
            if (a[0].equals("ISSUED") && a.length >= 4) {
                Member m = memberIndex.get(dec(a[1])); if (m != null) m.getIssuedDueDates().put(dec(a[2]), LocalDate.parse(a[3]));
            } else if (a[0].equals("QUEUE") && a.length >= 3) {
                String bookId=dec(a[1]); Member m=memberIndex.get(dec(a[2])); if(m!=null){MyQueue<Member> q=waitingQueues.get(bookId); if(q==null){q=new MyQueue<>(); waitingQueues.put(bookId,q);} q.enqueue(m);}
            } else if (a[0].equals("COUNT") && a.length >= 3) issueCounts.put(dec(a[1]), Integer.parseInt(a[2]));
        }
    }

    private static String enc(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s) { return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8); }

    // ----------------------------------------------------------------
    // BOOK MANAGEMENT
    // ----------------------------------------------------------------

    public String addBook(String id, String title, String author, String genre, int copies) {
        if (bookIndex.containsKey(id)) {
            return "A book with ID " + id + " already exists.";
        }
        Book book = new Book(id, title, author, genre, copies);
        bookList.add(book);
        bookIndex.put(id, book);
        return "Book added successfully.";
    }

    public String editBook(String id, String title, String author, String genre, int totalCopies) {
        Book book = bookIndex.get(id);
        if (book == null) {
            return "Book not found.";
        }
        int issuedCount = book.getTotalCopies() - book.getAvailableCopies();
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(Math.max(0, totalCopies - issuedCount));
        return "Book updated successfully.";
    }

    public String deleteBook(String id) {
        Book book = bookIndex.get(id);
        if (book == null) {
            return "Book not found.";
        }
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            return "Cannot delete: this book currently has copies issued.";
        }
        bookIndex.remove(id);
        bookList.removeIf(b -> b.getId().equals(id));
        return "Book deleted successfully.";
    }

    public Book findBookById(String id) {
        return bookIndex.get(id);
    }

    public List<Book> getAllBooks() {
        return bookList.toList();
    }

    /** Search & Sort module: sorts books by title using merge sort. */
    public List<Book> getBooksSortedByTitle() {
        return SortUtil.mergeSort(bookList.toList(), Comparator.comparing(b -> b.getTitle().toLowerCase()));
    }

    /** Simple linear search across title/author/id — fine for small catalogues. */
    public List<Book> searchBooks(String keyword) {
        String k = keyword.toLowerCase().trim();
        List<Book> result = new ArrayList<>();
        for (Book b : bookList) {
            if (b.getId().toLowerCase().contains(k)
                    || b.getTitle().toLowerCase().contains(k)
                    || b.getAuthor().toLowerCase().contains(k)) {
                result.add(b);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // MEMBER MANAGEMENT
    // ----------------------------------------------------------------

    public String addMember(String id, String name, String contact) {
        if (memberIndex.containsKey(id)) {
            return "A member with ID " + id + " already exists.";
        }
        Member member = new Member(id, name, contact);
        memberList.add(member);
        memberIndex.put(id, member);
        return "Member added successfully.";
    }

    public String editMember(String id, String name, String contact) {
        Member member = memberIndex.get(id);
        if (member == null) {
            return "Member not found.";
        }
        member.setName(name);
        member.setContact(contact);
        return "Member updated successfully.";
    }

    public String deleteMember(String id) {
        Member member = memberIndex.get(id);
        if (member == null) {
            return "Member not found.";
        }
        if (member.getIssuedDueDates().size() > 0) {
            return "Cannot delete: this member still has books issued.";
        }
        memberIndex.remove(id);
        memberList.removeIf(m -> m.getId().equals(id));
        return "Member deleted successfully.";
    }

    public Member findMemberById(String id) {
        return memberIndex.get(id);
    }

    public List<Member> getAllMembers() {
        return memberList.toList();
    }

    // ----------------------------------------------------------------
    // ISSUE / RETURN + WAITING QUEUE
    // ----------------------------------------------------------------

    /**
     * Attempts to issue a book to a member.
     * If no copy is available, the member is placed in that book's waiting
     * queue instead (FIFO) and will be auto-issued the book when it is
     * returned by someone else.
     */
    public String issueBook(String bookId, String memberId) {
        Book book = bookIndex.get(bookId);
        Member member = memberIndex.get(memberId);

        if (book == null) return "Book not found.";
        if (member == null) return "Member not found.";
        if (member.getIssuedDueDates().containsKey(bookId)) {
            return member.getName() + " already has this book issued.";
        }

        if (book.isAvailable()) {
            return performIssue(book, member);
        } else {
            MyQueue<Member> queue = waitingQueues.get(bookId);
            if (queue == null) {
                queue = new MyQueue<>();
                waitingQueues.put(bookId, queue);
            }
            for (Member m : queue.toList()) {
                if (m.getId().equals(memberId)) {
                    return member.getName() + " is already in the waiting queue for this book.";
                }
            }
            queue.enqueue(member);
            recentActions.push(new Transaction(Transaction.Type.QUEUED, book.getId(), book.getTitle(),
                    member.getId(), member.getName(), LocalDate.now(), null, false));
            return "\"" + book.getTitle() + "\" is currently unavailable. "
                    + member.getName() + " has been added to the waiting queue (position "
                    + queue.size() + ").";
        }
    }

    private String performIssue(Book book, Member member) {
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        LocalDate dueDate = LocalDate.now().plusDays(LOAN_PERIOD_DAYS);
        member.getIssuedDueDates().put(book.getId(), dueDate);

        Integer count = issueCounts.get(book.getId());
        issueCounts.put(book.getId(), (count == null ? 0 : count) + 1);

        recentActions.push(new Transaction(Transaction.Type.ISSUE, book.getId(), book.getTitle(),
                member.getId(), member.getName(), LocalDate.now(), dueDate, false));

        return "\"" + book.getTitle() + "\" issued to " + member.getName()
                + ". Due date: " + dueDate;
    }

    /**
     * Processes a return. If members are waiting in the queue for this
     * book, the one at the front is automatically issued the book next.
     */
    public String returnBook(String bookId, String memberId) {
        Book book = bookIndex.get(bookId);
        Member member = memberIndex.get(memberId);

        if (book == null) return "Book not found.";
        if (member == null) return "Member not found.";

        LocalDate dueDate = member.getIssuedDueDates().get(bookId);
        if (dueDate == null) {
            return "This book was not issued to " + member.getName() + ".";
        }

        boolean overdue = LocalDate.now().isAfter(dueDate);
        member.getIssuedDueDates().remove(bookId);
        book.setAvailableCopies(book.getAvailableCopies() + 1);

        recentActions.push(new Transaction(Transaction.Type.RETURN, book.getId(), book.getTitle(),
                member.getId(), member.getName(), LocalDate.now(), null, overdue));

        StringBuilder message = new StringBuilder();
        message.append("\"").append(book.getTitle()).append("\" returned by ").append(member.getName());
        message.append(overdue ? " (was OVERDUE)." : " (on time).");

        MyQueue<Member> queue = waitingQueues.get(bookId);
        if (queue != null && !queue.isEmpty()) {
            Member next = queue.dequeue();
            String issueMsg = performIssue(book, next);
            message.append("\nNext in queue: ").append(next.getName())
                    .append(" has been automatically issued this book. ").append(issueMsg);
        }
        return message.toString();
    }

    public MyQueue<Member> getQueueForBook(String bookId) {
        return waitingQueues.get(bookId);
    }

    public List<String> getBooksWithActiveQueues() {
        List<String> result = new ArrayList<>();
        for (String bookId : waitingQueues.keys()) {
            MyQueue<Member> q = waitingQueues.get(bookId);
            if (q != null && !q.isEmpty()) {
                result.add(bookId);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    // RECENT ACTIONS (STACK)
    // ----------------------------------------------------------------

    /** Returns recent transactions, most recent first, without modifying the stack. */
    public List<Transaction> getRecentActions() {
        return recentActions.toList();
    }

    // ----------------------------------------------------------------
    // REPORTS
    // ----------------------------------------------------------------

    public static class IssuedRecord {
        public final Book book;
        public final Member member;
        public final LocalDate dueDate;

        IssuedRecord(Book book, Member member, LocalDate dueDate) {
            this.book = book;
            this.member = member;
            this.dueDate = dueDate;
        }
    }

    public List<IssuedRecord> getCurrentlyIssued() {
        List<IssuedRecord> result = new ArrayList<>();
        for (Member member : memberList) {
            for (String bookId : member.getIssuedDueDates().keys()) {
                Book book = bookIndex.get(bookId);
                LocalDate due = member.getIssuedDueDates().get(bookId);
                if (book != null) {
                    result.add(new IssuedRecord(book, member, due));
                }
            }
        }
        return result;
    }

    public List<IssuedRecord> getOverdueBooks() {
        List<IssuedRecord> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (IssuedRecord record : getCurrentlyIssued()) {
            if (today.isAfter(record.dueDate)) {
                result.add(record);
            }
        }
        return result;
    }

    /** Books ranked by how many times they have ever been issued (descending). */
    public List<Map.Entry<Book, Integer>> getMostPopularBooks() {
        List<Map.Entry<Book, Integer>> entries = new ArrayList<>();
        for (String bookId : issueCounts.keys()) {
            Book book = bookIndex.get(bookId);
            if (book != null) {
                entries.add(new java.util.AbstractMap.SimpleEntry<>(book, issueCounts.get(bookId)));
            }
        }
        entries.sort((a, b) -> b.getValue() - a.getValue()); // small helper sort for a 2-field ranking
        return entries;
    }

    // ----------------------------------------------------------------
    // DEMO DATA (so the app is ready to explore immediately)
    // ----------------------------------------------------------------

    private void seedDemoData() {
        addBook("B001", "Introduction to Algorithms", "Cormen et al.", "Computer Science", 2);
        addBook("B002", "Clean Code", "Robert C. Martin", "Software Engineering", 1);
        addBook("B003", "The Pragmatic Programmer", "Hunt & Thomas", "Software Engineering", 1);
        addBook("B004", "Data Structures in Java", "Robert Lafore", "Computer Science", 3);
        addBook("B005", "Design Patterns", "Gang of Four", "Software Engineering", 1);

        addMember("M001", "Aarav Sharma", "aarav@example.com");
        addMember("M002", "Diya Patel", "diya@example.com");
        addMember("M003", "Kabir Mehta", "kabir@example.com");
        addMember("M004", "Ishita Rao", "ishita@example.com");
    }
}
