package ipogudin.fp;

import java.util.function.Function;

public interface Functor<T> {

    <U> Functor<U> map(Function<T, U> f);

}
