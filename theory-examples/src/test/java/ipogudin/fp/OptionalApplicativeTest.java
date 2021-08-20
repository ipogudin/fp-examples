package ipogudin.fp;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalApplicativeTest {

    Function<Integer, Function<Integer, Function<Integer, Function<Integer, Integer>>>> f =
            a -> b -> c -> d -> a * b + c - d;

    @Test
    public void allExistingValuesAreUsedForCalculation() {
        var r = OptionalApplicative.pure(f)
                .ap(ofNullable(1))
                .ap(ofNullable(2))
                .ap(ofNullable(3))
                .ap(ofNullable(4));

        assertEquals(ofNullable(1), r.toOptional());
    }

    @Test
    public void failFastShouldHappenIfAnyOfValuesDoesNotExist() {
        var r = OptionalApplicative.pure(f)
                .ap(ofNullable(1))
                .ap(empty())
                .ap(ofNullable(3))
                .ap(ofNullable(4));

        assertEquals(empty(), r.toOptional());
    }

}
