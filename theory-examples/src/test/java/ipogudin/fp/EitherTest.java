package ipogudin.fp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EitherTest {

    @Test
    public void eitherLeft() {
        Integer value = 1;
        Either<Integer, Throwable> e = Either.left(value);

        assertTrue(e.isLeft());
        assertFalse(e.isRight());
        assertEquals(value, e.getLeft());
        assertThrows(IllegalStateException.class, e::getRight);
    }

    @Test
    public void eitherRight() {
        Throwable exception = new RuntimeException();
        Either<Integer, Throwable> e = Either.right(exception);

        assertFalse(e.isLeft());
        assertTrue(e.isRight());
        assertEquals(exception, e.getRight());
        assertThrows(IllegalStateException.class, e::getLeft);
    }

    @Test
    public void eitherAsFunctorForLeft() {
        Integer value = 1;
        Either<Integer, Throwable> e = Either.left(value);

        Either<String, Throwable> e2 = e.map(String::valueOf);
        assertEquals("1", e2.getLeft());
    }

    @Test
    public void eitherAsFunctorForRight() {
        Throwable exception = new RuntimeException();
        Either<Integer, Throwable> e = Either.right(exception);

        Either<String, Throwable> e2 = e.map(String::valueOf);
        assertEquals(exception, e2.getRight());
    }

}
