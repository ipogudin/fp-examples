package ipogudin.fp;

public interface Lens<W, P> {

    P get(W w);

    W set(W w, P p);

    default <P1> Lens<W, P1> compose(Lens<P, P1> l2) {
        var l1 = this;
        return new Lens<W, P1>() {
            @Override
            public P1 get(W w) {
                return l2.get(l1.get(w));
            }

            @Override
            public W set(W w, P1 p1) {
                return l1.set(w, l2.set(l1.get(w), p1));
            }
        };
    }

}
