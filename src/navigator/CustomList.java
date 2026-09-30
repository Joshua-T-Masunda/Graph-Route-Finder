package navigator;

/**
 * Custom dynamic array (replaces java.util.ArrayList).
 * Enhanced for KNNClassifier: added copy() and reverse() methods.
 */
@SuppressWarnings("unchecked")
public class CustomList<T> {

    private static final int DEFAULT_CAPACITY = 16;

    private Object[] data;
    private int size;
    private int capacity;

    public CustomList() {
        this(DEFAULT_CAPACITY);
    }

    public CustomList(int initialCapacity) {
        this.capacity = Math.max(1, initialCapacity);
        this.data = new Object[this.capacity];
        this.size = 0;
    }

    //  Mutation 

    public void add(T item) {
        if (size == capacity) grow();
        data[size++] = item;
    }

    public void add(int index, T item) {
        checkIndexForAdd(index);
        if (size == capacity) grow();
        for (int i = size; i > index; i--) data[i] = data[i - 1];
        data[index] = item;
        size++;
    }

    public void set(int index, T item) {
        checkIndex(index);
        data[index] = item;
    }

    public T remove(int index) {
        checkIndex(index);
        T removed = (T) data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        data[--size] = null;
        return removed;
    }

    public boolean remove(Object item) {
        int index = indexOf(item);
        if (index != -1) {
            remove(index);
            return true;
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    //Access 

    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object item) {
        for (int i = 0; i < size; i++) {
            if (item == null ? data[i] == null : item.equals(data[i])) return true;
        }
        return false;
    }

    public int indexOf(Object item) {
        for (int i = 0; i < size; i++) {
            if (item == null ? data[i] == null : item.equals(data[i])) return i;
        }
        return -1;
    }

    // METHODS FOR KNNClassifier 

    /**
     * Returns a shallow copy of this list.
     * Critical for getTopK() in KNNClassifier to avoid modifying original list.
     */
    public CustomList<T> copy() {
        CustomList<T> copy = new CustomList<>(this.capacity);
        for (int i = 0; i < size; i++) {
            copy.add(this.get(i));
        }
        return copy;
    }

    /**
     * Reverses the list in-place.
     * Used in getTopK() after extracting from CustomMinHeap.
     */
    public void reverse() {
        int left = 0;
        int right = size - 1;
        while (left < right) {
            T temp = get(left);
            set(left, get(right));
            set(right, temp);
            left++;
            right--;
        }
    }

    /**
     * Shuffle the list Fisher-Yates - useful for bagging later.
     */
    public void shuffle() {
        for (int i = size - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            T temp = get(i);
            set(i, get(j));
            set(j, temp);
        }
    }

    // Internal

    private void grow() {
        capacity *= 2;
        Object[] newData = new Object[capacity];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }
    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds (size=" + size + ")");
    }

    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for add (size=" + size + ")");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(data[i]);
            if (i < size - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}