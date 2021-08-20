package ipogudin.fp;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class OptionalApplicative<F, T> {

    private Optional<F> valueOpt;

    private OptionalApplicative(Optional<F> valueOpt) {
        this.valueOpt = valueOpt;
    }

    public static <F extends Function<T, ?>, T> OptionalApplicative<F, T> pure(F f) {
        return new OptionalApplicative(ofNullable(f));
    }

    public OptionalApplicative<Function<T, ?>, T> ap(Optional<T> valueOpt) {
        if (valueOpt.isPresent()) {
            return new OptionalApplicative(this.valueOpt.map(f -> ((Function) f).apply(valueOpt.get())));
        }

        return new OptionalApplicative<>(Optional.empty());
    }

    public Optional<F> toOptional() {
        if (valueOpt.filter(v -> v instanceof Function).isPresent()) {
            throw new IllegalStateException("Value is not calculated");
        }
        return valueOpt;
    }
}
