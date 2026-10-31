package com.nocapai.ds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A hand-written singly linked list, used to store Book and Member records.
 *
 * Why a linked list instead of a plain array/ArrayList?
 *  - The number of books/members is not known ahead of time.
 *  - Insertions and deletions happen often (a new book arrives, a member
 *    leaves, etc.) and a linked list does these in O(1) once the node is
 *    located, without shifting other elements as an array would.
 */
public class MyLinkedList<T> implements Iterable<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public MyLinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    /** Adds a new item at the end of the list. O(1). */
    public void add(T item) {
        Node<T> newNode = new Node<>(item);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    /** Removes the first item that matches the given condition. O(n). */
    public boolean removeIf(Predicate<T> condition) {
        Node<T> current = head;
        Node<T> previous = null;

        while (current != null) {
            if (condition.test(current.data)) {
                if (previous == null) {
                    head = current.next;
                } else {
                    previous.next = current.next;
                }
                if (current == tail) {
                    tail = previous;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    /** Returns the first item that matches the given condition, or null. O(n). */
    public T find(Predicate<T> condition) {
        Node<T> current = head;
        while (current != null) {
            if (condition.test(current.data)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    /** Returns true if at least one item matches the given condition. */
    public boolean contains(Predicate<T> condition) {
        return find(condition) != null;
    }

    /** Converts the list into a plain ArrayList — handy for populating GUI tables. */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        Node<T> current = head;
        while (current != null) {
            result.add(current.data);
            current = current.next;
        }
        return result;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return toList().iterator();
    }
}
