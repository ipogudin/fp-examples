package ipogudin.fp;

public class Pair<L, R> {
    private L left;
    private R right;

    public Pair(L left, R rigth) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
}
