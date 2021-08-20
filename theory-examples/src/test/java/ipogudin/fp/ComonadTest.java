package ipogudin.fp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComonadTest {

    @Test
    public void immutableListZipperAsComonad() {
        var list = new ImmutableList<>(1)
                .add(2).add(3).add(4).add(5).add(6).add(7).add(8).add(9);

        var zipper = new ImmutableListZipper<>(list)
                .backward()
                .backward();

        assertEquals(
                "<1, 2, 3, 4, 5, 6, *7*, 8, 9>",
                zipper.toString());
        assertEquals(
                "<2, 4, 6, 8, 10, 12, *14*, 16, 8>",
                zipper.coflatMap(z -> z.forward().focus(0) + z.backward().focus(0)).toString());
    }

}
