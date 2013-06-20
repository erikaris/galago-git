/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lemurproject.galago.core.retrieval.iterator;

import java.io.IOException;
import org.lemurproject.galago.core.index.DataSource;
import org.lemurproject.galago.core.index.DiskIterator;
import org.lemurproject.galago.core.retrieval.query.AnnotatedNode;

/**
 * This is the base abstract implementation of an Iterator that
 * wraps a DataSource.
 * 
 * @author jfoley
 */
public abstract class SourceIterator<T extends DataSource> extends DiskIterator {
  T source = null;
  
  public SourceIterator(T src) {
    source = src;
  }
  
  @Override
  public String getKeyString() {
    return source.key();
  }
  
  @Override
  public void reset() throws IOException {
    source.reset();
  }
  
  @Override
  public boolean isDone() {
    return source.isDone();
  }

  @Override
  public int currentCandidate() {
    return source.currentCandidate();
  }

  @Override
  public void movePast(int identifier) throws IOException {
    source.movePast(identifier);
  }

  @Override
  public void syncTo(int identifier) throws IOException {
    source.syncTo(identifier);
  }

  @Override
  public boolean hasMatch(int identifier) {
    return source.hasMatch(identifier);
  }

  @Override
  public boolean hasAllCandidates() {
    return source.hasAllCandidates();
  }

  @Override
  public long totalEntries() {
    return source.totalEntries();
  }

  @Override
  public int compareTo(BaseIterator other) {
    if (isDone() && !other.isDone()) {
      return 1;
    }
    if (other.isDone() && !isDone()) {
      return -1;
    }
    if (isDone() && other.isDone()) {
      return 0;
    }
    return currentCandidate() - other.currentCandidate();
  }

  // This is not implemented here, because it needs to be customized for each SourceIterator
  @Override
  public abstract String getValueString() throws IOException;

  // This is not implemented here, because it needs to be customized for each SourceIterator
  @Override
  public abstract AnnotatedNode getAnnotatedNode() throws IOException;
  
}