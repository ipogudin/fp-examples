package ipogudin.fp;

import java.util.function.Function;

import static ipogudin.fp.ImmutableList.EMPTY_IMMUTABLE_LIST;

public class ImmutableListZipper<T> implements Functor<T> {

    private final ImmutableList<T> a;
    private final T focus;
    private final ImmutableList<T> b;

    public ImmutableListZipper(ImmutableList<T> a, T focus, ImmutableList<T> b) {
        this.a = a;
        this.focus = focus;
        this.b = b;
    }

    public ImmutableListZipper(ImmutableList<T> a) {
        this(a.tail(), a.head(), EMPTY_IMMUTABLE_LIST);
    }

    @Override
    public <U> ImmutableListZipper<U> map(Function<T, U> f) {
        return new ImmutableListZipper<>(a.map(f), f.apply(focus), b.map(f));
    }

    public <P> ImmutableListZipper<P> coflatMap(Function<ImmutableListZipper<T>, P> f) {
        return coflat(this).map(f);
    }

    public ImmutableListZipper<T> forward() {
        return new ImmutableListZipper<>(a.add(focus), b.head(), b.tail());
    }

    public ImmutableListZipper<T> backward() {
        return new ImmutableListZipper<>(a.tail(), a.head(), b.add(focus));
    }

    public ImmutableListZipper<T> set(T newFocus) {
        return new ImmutableListZipper<>(a, newFocus, b);
    }

    public T focus(T defaultFocus) {
        if (focus == null) {
            return defaultFocus;
        }

        return focus;
    }

    public ImmutableListZipper<T> reverse() {
        return new ImmutableListZipper<>(b, focus, a);
    }

    public int size() {
        return a.size() + 1 + b.size ();
    }

    @Override
    public String toString() {
        return "<" + a.toString() + ", *" + focus + "*, " + b.reverse().toString() + ">";
    }

    private ImmutableListZipper<T> addToLeft(T value) {
        return new ImmutableListZipper<>(a.add(value), focus, b);
    }

    private ImmutableListZipper<T> addToRight(T value) {
        return new ImmutableListZipper<>(a, focus, b.add(value));
    }

    private static <T> ImmutableListZipper<T> shiftToMaxForward(ImmutableListZipper<T> l) {
        if (l.b == EMPTY_IMMUTABLE_LIST) {
            return l;
        }

        return shiftToMaxForward(l.forward());
    }

    public static <T> ImmutableListZipper<ImmutableListZipper<T>> coflat(ImmutableListZipper<T> l) {
        var z = shiftToMaxForward(l.reverse());
        var zz = new ImmutableListZipper<>(EMPTY_IMMUTABLE_LIST, null, EMPTY_IMMUTABLE_LIST);
        var r = generateAllFocuses(z, zz, l.a.size(), 0, z.size());
        return new ImmutableListZipper<>(r.a, r.focus, r.b.reverse());
    }

    private static <T> ImmutableListZipper<ImmutableListZipper<T>> generateAllFocuses(
            ImmutableListZipper<T> z,
            ImmutableListZipper<ImmutableListZipper<T>> zz,
            int focus,
            int position,
            int limit) {

        if (position == limit) {
            return zz;
        }

        if (focus > position) {
            return generateAllFocuses(z.backward(), zz.addToLeft(z), focus, position + 1, limit);
        } else if (focus == position) {
            return generateAllFocuses(z.backward(), zz.set(z), focus, position + 1, limit);
        } else {
            return generateAllFocuses(z.backward(), zz.addToRight(z), focus, position + 1, limit);
        }
    }
}
