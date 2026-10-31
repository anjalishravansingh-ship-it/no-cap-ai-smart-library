package com.nocapai.ds;

/**
 * Generic singly-linked node used by MyLinkedList, MyQueue and MyStack.
 * Keeping one Node class for all three structures shows clearly, during a
 * viva, that a Linked List, a Queue and a Stack are really just different
 * *disciplines* of insertion/removal applied to the same underlying node
 * structure.
 */
public class Node<T> {
    public T data;
    public Node<T> next;

    public Node(T data) {
        this.data = data;
        this.next = null;
    }
}
