package codetree.core;

public interface CodeFragment {
    public abstract byte getVlabel();

    public abstract byte[] getelabel();

    public abstract boolean contains(CodeFragment other);

    public abstract boolean bigger(CodeFragment other);

    public abstract boolean contains_adj(CodeFragment other);

    public abstract boolean equals_nec(CodeFragment other, int nec, int v_nec);

    public abstract boolean equals_nec_sub(CodeFragment other, int nec, int v_nec);

    public abstract boolean equals(Object other);

    public abstract boolean contains_nec(CodeFragment other, int m_nec, int v_nec);

}
