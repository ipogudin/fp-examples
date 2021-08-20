package ipogudin.fp;

import java.util.function.Function;

public class ImmutableList<T> implements Functor<T> {

    public static final ImmutableList EMPTY_IMMUTABLE_LIST = new EmptyImmutableList();

    private T head;
    private ImmutableList<T> tail;

    public ImmutableList(T head, ImmutableList<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    public ImmutableList(T head) {
        this(head, EMPTY_IMMUTABLE_LIST);
    }

    public T head() {
        return head;
    }

    public ImmutableList<T> tail() {
        return tail;
    }

    @Override
    public <U> ImmutableList<U> map(Function<T, U> f) {
        return new ImmutableList<U>(f.apply(head), tail.map(f));
    }

    public ImmutableList<T> add(T newHead) {
        if (newHead == null) {
            return this;
        }

        return new ImmutableList<>(newHead, this);
    }

    public ImmutableList<T> reverse() {
        return EMPTY_IMMUTABLE_LIST.addAll(this);
    }

    public ImmutableList<T> addAll(ImmutableList<T> l) {
        if (l == null) {
            return this;
        }

        return add(l.head).addAll(l.tail);
    }

    public int size() {
        return tail.size() + 1;
    }

    @Override
    public String toString() {
        return ((tail == EMPTY_IMMUTABLE_LIST) ? "" : tail.toString() + ", ") + head ;
    }

    public static class EmptyImmutableList extends ImmutableList {

        public EmptyImmutableList() {
            super(null);
        }

        @Override
        public Object head() {
            return null;
        }

        @Override
        public ImmutableList map(Function f) {
            return this;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String toString() {
            return "";
        }
    }

}
