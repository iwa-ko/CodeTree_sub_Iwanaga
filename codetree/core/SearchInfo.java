package codetree.core;

import java.util.BitSet;

public interface SearchInfo {
    abstract BitSet getOpen();

    abstract BitSet getClose();
}
