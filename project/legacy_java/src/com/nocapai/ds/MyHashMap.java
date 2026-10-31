package com.nocapai.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written Hash Table (separate chaining) used to index Books and
 * Members by their unique ID, giving average O(1) lookup/insert/remove —
 * far faster than scanning the whole linked list every time we search.
 *
 * Design:
 *  - An array of "buckets"; each bucket is a small chain of Entry objects.
 *  - index = (key.hashCode() bounded to be non-negative) % capacity.
 *  - Collisions (two keys mapping to the same bucket) are handled by
 *    chaining entries together, rather than overwriting data.
 *  - The table automatically doubles in size and rehashes once it gets
 *    more than 75% full, keeping lookups fast as data grows.
 */
public class MyHashMap<K, V> {

    /** One key-value pair stored inside a bucket's chain. */
    private static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<K, V>[] buckets;
    private int capacity;
    private int size;
    private static final double LOAD_FACTOR_LIMIT = 0.75;

    @SuppressWarnings("unchecked")
    public MyHashMap() {
        capacity = 16;
        buckets = new Entry[capacity];
        size = 0;
    }

    private int indexFor(K key, int cap) {
        int hash = key.hashCode();
        hash = hash ^ (hash >>> 16); // spread bits to reduce clustering
        return Math.abs(hash) % cap;
    }

    /** Inserts a key-value pair, or updates the value if the key already exists. */
    public void put(K key, V value) {
        if ((double) (size + 1) / capacity > LOAD_FACTOR_LIMIT) {
            resize();
        }
        int index = indexFor(key, capacity);
        Entry<K, V> current = buckets[index];

        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value; // key exists -> update
                return;
            }
            current = current.next;
        }

        Entry<K, V> newEntry = new Entry<>(key, value);
        newEntry.next = buckets[index];
        buckets[index] = newEntry;
        size++;
    }

    /** Returns the value for the given key, or null if not found. Average O(1). */
    public V get(K key) {
        int index = indexFor(key, capacity);
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    /** Removes the entry for the given key. Returns true if something was removed. */
    public boolean remove(K key) {
        int index = indexFor(key, capacity);
        Entry<K, V> current = buckets[index];
        Entry<K, V> previous = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Entry<K, V>[] newBuckets = new Entry[newCapacity];

        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = buckets[i];
            while (current != null) {
                Entry<K, V> nextEntry = current.next;
                int newIndex = indexFor(current.key, newCapacity);
                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;
                current = nextEntry;
            }
        }
        buckets = newBuckets;
        capacity = newCapacity;
    }

    public List<K> keys() {
        List<K> result = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = buckets[i];
            while (current != null) {
                result.add(current.key);
                current = current.next;
            }
        }
        return result;
    }

    public List<V> values() {
        List<V> result = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = buckets[i];
            while (current != null) {
                result.add(current.value);
                current = current.next;
            }
        }
        return result;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
