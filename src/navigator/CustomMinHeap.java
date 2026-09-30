package navigator;

/**
 * Array-based binary heap used as a priority queue.
 *
 * <p>This follows the textbook's heap idea: keys are stored in an array and
 * parent/child positions are computed arithmetically. The default mode is a
 * min-heap for graph search.</p>
 */
@SuppressWarnings("unchecked")
public class CustomMinHeap<T> {

    private double[] priorities;
    private Object[] values;
    private int size;
    private int capacity;
    private boolean minHeap; 

    /**
     * Creates a min-heap with the requested initial capacity.
     */
    public CustomMinHeap(int initialCapacity) {
        this(initialCapacity, true); 
    }

    /**
     * Creates either a min-heap or max-heap.
     */
    public CustomMinHeap(int initialCapacity, boolean minHeap) {
        this.capacity = Math.max(1, initialCapacity);
        this.priorities = new double[this.capacity];
        this.values = new Object[this.capacity];
        this.size = 0;
        this.minHeap = minHeap;
    }

    /**
     * Maintains the "Best K" (smallest values) for KNN.
     * Uses Max-Heap logic: the LARGEST of the smallest distances is at the root.
     */
    public void offerForTopK(double priority, T value, int k) {
        this.minHeap = false; // KNN logic requires Max-Heap comparison

        if (size < k) {
            insert(priority, value);
        } else if (priority < priorities[0]) {
            // Replace the "worst" of our top-k if the new one is better
            priorities[0] = priority;
            values[0] = value;
            siftDown(0);
        }
    }

    /**
     * Inserts a value with the given priority key.
     */
    public void insert(double priority, T value) {
        if (size == capacity) grow();
        priorities[size] = priority;
        values[size] = value;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the root item.
     */
    public T extractMin() {
        if (size == 0) return null;
        T root = (T) values[0];
        size--;
        if (size > 0) {
            priorities[0] = priorities[size];
            values[0] = values[size];
            siftDown(0);
        }
        values[size] = null;
        return root;
    }
    
    /**
     * Lowers the priority of an existing item in a min-heap.
     */
    public void decreaseKey(T value, double newPriority) {
        for (int i = 0; i < size; i++) {
            // Find the node in our array
            if (values[i] != null && values[i].equals(value)) {
                // Only update if the new path is actually better
                if (minHeap ? (newPriority < priorities[i]) : (newPriority > priorities[i])) {
                    priorities[i] = newPriority;
                    siftUp(i); // Move it toward the root
                }
                return;
            }
        }
    }

    /**
     * Removes all items and returns them in priority order.
     */
    public CustomList<T> extractAllToList() {
        CustomList<T> list = new CustomList<>(size);
        int currentSize = size;
        for (int i = 0; i < currentSize; i++) {
            list.add(this.extractMin());
        }
        return list;
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (compare(priorities[i], priorities[parent])) {
                swap(i, parent);
                i = parent;
            } else break;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int best = i;

            if (left < size && compare(priorities[left], priorities[best])) best = left;
            if (right < size && compare(priorities[right], priorities[best])) best = right;

            if (best == i) break;
            swap(i, best);
            i = best;
        }
    }

    private boolean compare(double a, double b) {
        return minHeap ? (a < b) : (a > b);
    }

    private void swap(int a, int b) {
        double tempP = priorities[a];
        priorities[a] = priorities[b];
        priorities[b] = tempP;
        Object tempV = values[a];
        values[a] = values[b];
        values[b] = tempV;
    }

    private void grow() {
        capacity *= 2;
        double[] newP = new double[capacity];
        Object[] newV = new Object[capacity];
        System.arraycopy(priorities, 0, newP, 0, size);
        System.arraycopy(values, 0, newV, 0, size);
        priorities = newP;
        values = newV;
    }

    /**
     * Returns the number of entries.
     */
    public int size() { return size; }

    /**
     * Returns true if the heap has no entries.
     */
    public boolean isEmpty() { return size == 0; }
}
