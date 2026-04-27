package vn.dzokha.soap.engine.qc.core;

import vn.dzokha.soap.domain.sequence.Sequence;

public interface QCModule {

    void processSequence(Sequence sequence);
    String name ();
    String description ();
    void reset ();
    boolean raisesError();
    boolean raisesWarning();

    default boolean isCalculated() { return true;  }
    default boolean ignoreFilteredSequences() {return true;}
    default boolean ignoreInReport() { return false;}
    default Object getResultsPanel() {return null; }
}

