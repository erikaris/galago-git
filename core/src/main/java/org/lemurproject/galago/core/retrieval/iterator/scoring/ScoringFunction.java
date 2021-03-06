// BSD License (http://lemurproject.org/galago-license)

package org.lemurproject.galago.core.retrieval.iterator.scoring;

/**
 * Interface of a class that transforms a count into a score.
 * Implementations of this are primarily used by a ScoringFunctionIterator
 *
 * @author irmarc
 * @deprecated 
 */
public interface ScoringFunction {
    public double score(int count, int length);
}
