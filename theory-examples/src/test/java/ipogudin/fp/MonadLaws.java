package ipogudin.fp;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonadLaws {

    @Test
    public void monadLaws() {
        Function<Integer, Optional<Integer>> f = i -> Optional.of(i + 1);
        Integer a = 1;
        assertEquals(Optional.of(a).flatMap(f), f.apply(a));

        Optional<Integer> m = Optional.of(1);
        assertEquals(m.flatMap(Optional::of), m);

        Function<Integer, Optional<Integer>> g = i -> Optional.of(i + 2);

        assertEquals(m.flatMap(f).flatMap(g), m.flatMap(x -> f.apply(x).flatMap(g)));


    }

}
