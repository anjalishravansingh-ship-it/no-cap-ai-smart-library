package com.nocapai.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written Queue (FIFO — First In, First Out), used to hold the list
 * of members waiting for a book that is currently unavailable.
 *
 * New requests are enqueued at the back; when a copy becomes free, the
 * member at the front (the one who has been waiting longest) is dequeued
 * first. This guarantees fairness in the order requests were made.
 */
public class MyQueue<T> {

    private Node<T> front;
    private Node<T> rear;
    private int size;

    public MyQueue() {
        front = null;
        rear = null;
        size = 0;
    }

    /** Adds an item to the back of the queue. O(1). */
    public void enqueue(T item) {
        Node<T> newNode = new Node<>(item);
        if (rear == null) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = newNode;
        }
        size++;
    }

    /** Removes and returns the item at the front of the queue. O(1). */
    public T dequeue() {
        if (front == null) {
            return null;
        }
        T data = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    /** Looks at the front item without removing it. */
    public T peek() {
        return front == null ? null : front.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /** Returns the queue contents from front to back, without modifying the queue. */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        Node<T> current = front;
        while (current != null) {
            result.add(current.data);
            current = current.next;
        }
        return result;
    }
}
