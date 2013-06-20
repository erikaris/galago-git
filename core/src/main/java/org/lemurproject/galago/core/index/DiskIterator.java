/*
 *  BSD License (http://www.galagosearch.org/license)
 */
package org.lemurproject.galago.core.index;

import java.io.IOException;
import org.lemurproject.galago.core.retrieval.iterator.BaseIterator;
import org.lemurproject.galago.core.retrieval.processing.ScoringContext;

/**
 *
 * @author sjh
 */
public abstract class DiskIterator implements BaseIterator {
  protected ScoringContext context;
  
  /**
   * For debugging only: getKey as string (it is a byte[]), you don't know the encoding.
   * @return string of byte[] key
   * @throws IOException 
   * @see Utility.toString
   */
  abstract public String getKeyString() throws IOException;
  
  // could add a few extra functions to the Leaf Node iterator here.
  // This will pass up topdocs information if it's available
  @Override
  public void setContext(ScoringContext context) {
    this.context = context;
  }

  @Override
  public boolean hasMatch(int identifier) {
    return !isDone() && currentCandidate() == identifier;
  }

  @Override
  public ScoringContext getContext() {
    return context;
  }
}