package ipogudin.fp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogAndValueTest {

    @Test
    public void kleisli() {
        var r = LogAndValue.unit(10)
            .flatMap(v -> LogAndValue.unit(v + 5, "Added five"))
            .flatMap(v -> LogAndValue.unit(v - 3, "Subtracted three"))
            .flatMap(v -> LogAndValue.unit(v.toString(), "Converted to string"));

        assertEquals("12", r.getValue());
        assertEquals(List.of("Added five", "Subtracted three", "Converted to string"), r.getLogs());
    }

}
