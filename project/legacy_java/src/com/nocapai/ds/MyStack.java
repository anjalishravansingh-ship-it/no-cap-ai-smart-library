package com.nocapai.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written Stack (LIFO — Last In, First Out), used to log recent
 * issue/return transactions system-wide.
 *
 * Every new transaction is pushed on top; the librarian can instantly see
 * the most recent activity by looking at the top of the stack, which is
 * exactly the "undo-style" review this project's Recent Actions module needs.
 */
public class MyStack<T> {

    private Node<T> top;
    private int size;

    public MyStack() {
        top = null;
        size = 0;
    }

    /** Pushes a new item on top of the stack. O(1). */
    public void push(T item) {
        Node<T> newNode = new Node<>(item);
        newNode.next = top;
        top = newNode;
        size++;
    }

    /** Removes and returns the item on top of the stack. O(1). */
    public T pop() {
        if (top == null) {
            return null;
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    /** Looks at the top item without removing it. */
    public T peek() {
        return top == null ? null : top.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /** Returns the stack contents top-first (most recent action first), without modifying the stack. */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        Node<T> current = top;
        while (current != null) {
            result.add(current.data);
            current = current.next;
        }
        return result;
    }
}
