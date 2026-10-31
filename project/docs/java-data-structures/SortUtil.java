package com.nocapai.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sorting and searching algorithms used by the Search & Sort module.
 *
 *  - mergeSort: O(n log n), stable — used to produce sorted views of books
 *    (by title/author) or transactions (by date) without relying on
 *    java.util.Collections.sort, so the algorithm itself is visible.
 *  - binarySearch: O(log n) — usable once a list is sorted by the same key.
 */
public class SortUtil {

    /** Returns a new, sorted list; does not modify the original list. */
    public static <T> List<T> mergeSort(List<T> list, Comparator<T> comparator) {
        List<T> copy = new ArrayList<>(list);
        if (copy.size() <= 1) {
            return copy;
        }
        int mid = copy.size() / 2;
        List<T> left = mergeSort(copy.subList(0, mid), comparator);
        List<T> right = mergeSort(copy.subList(mid, copy.size()), comparator);
        return merge(left, right, comparator);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> comparator) {
        List<T> result = new ArrayList<>();
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                result.add(left.get(i));
                i++;
            } else {
                result.add(right.get(j));
                j++;
            }
        }
        while (i < left.size()) {
            result.add(left.get(i));
            i++;
        }
        while (j < right.size()) {
            result.add(right.get(j));
            j++;
        }
        return result;
    }

    /**
     * Binary search on a list that is ALREADY sorted using the same comparator.
     * Returns the index of a matching element, or -1 if not found.
     */
    public static <T> int binarySearch(List<T> sortedList, T target, Comparator<T> comparator) {
        int low = 0;
        int high = sortedList.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            int cmp = comparator.compare(sortedList.get(mid), target);
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }
}
