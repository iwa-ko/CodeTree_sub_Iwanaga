package codetree.core;

public interface CodeFragment {
    public abstract byte getVlabel();

    public abstract byte[] getelabel();

    public abstract boolean contains(CodeFragment other);

    public abstract boolean contains1(CodeFragment other);
}
