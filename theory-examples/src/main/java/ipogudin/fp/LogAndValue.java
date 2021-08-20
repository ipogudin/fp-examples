package ipogudin.fp;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class LogAndValue<T> implements Functor<T> {

    private final T value;
    private final List<String> logs;

    public LogAndValue(T value, List<String> logs) {
        this.value = value;
        this.logs = logs;
    }

    public T getValue() {
        return value;
    }

    public List<String> getLogs() {
        return logs;
    }

    public <U> LogAndValue<U> map(Function<T, U> f) {
        return new LogAndValue<>(f.apply(value), logs);
    }

    public <P> LogAndValue<P> flatMap(Function<T, LogAndValue<P>> f) {
        return flat(map(f));
    }

    public static <T> LogAndValue<T> flat(LogAndValue<LogAndValue<T>> ll) {
        var resultLogs = new LinkedList<>(ll.getLogs());
        resultLogs.addAll(ll.getValue().getLogs());
        return new LogAndValue<>(ll.getValue().getValue(), resultLogs);
    }

    public static <T> LogAndValue<T> unit(T value) {
        return new LogAndValue<>(value, emptyList());
    }

    public static <T> LogAndValue<T> unit(T value, String log) {
        return new LogAndValue<>(value, singletonList(log));
    }
}
