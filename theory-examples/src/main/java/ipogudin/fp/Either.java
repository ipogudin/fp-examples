package ipogudin.fp;

import java.util.function.Function;

public interface Either<L, R> extends Functor<L> {

    boolean isLeft();
    boolean isRight();
    L getLeft();
    R getRight();

    static <L, R> Either<L, R> left(L value) {
        return new Left(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right(value);
    }

    @Override
    default <U> Either<U, R> map(Function<L, U> f) {
        if (this instanceof Left) {
            return new Left<>(f.apply(this.getLeft()));
        }
        return (Either) this;
    }
}

class Left<L, R> implements Either<L, R> {

    private L value;

    public Left(L value) {
        this.value = value;
    }

    public boolean isLeft() { return true; }
    public boolean isRight() { return false; }
    public L getLeft() { return value; }
    public R getRight() { throw new IllegalStateException(); }
}

class Right<L, R> implements Either<L, R> {

    private R value;

    public Right(R value) {
        this.value = value;
    }

    public boolean isLeft() { return false; }
    public boolean isRight() { return true; }
    public L getLeft() { throw new IllegalStateException(); }
    public R getRight() { return value; }
}
