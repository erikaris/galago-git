/*
 *  BSD License (http://www.galagosearch.org/license)
 */
package org.lemurproject.galago.core.retrieval.iterator;

import java.io.IOException;
import org.lemurproject.galago.core.retrieval.query.NodeParameters;
import org.lemurproject.galago.core.util.ExtentArray;
import org.lemurproject.galago.tupleflow.Utility;

/**
 *
 * @author sjh
 */
public class OrderedWindowIterator extends ExtentConjunctionIterator {

  private int width;

  public OrderedWindowIterator(NodeParameters parameters, MovableExtentIterator[] iterators) throws IOException {
    super(parameters, iterators);
    this.width = (int) parameters.get("default", -1);
    syncTo(0);
  }

  @Override
  public void loadExtents() {
    int document = currentCandidate();
    if (context != null) {
      document = context.document;
    }

    if (document == 12110526 && context != null) {
      System.err.printf("\t%s: loading extent (context=%d, this=%d)\n",
              Utility.shortName(this), context.document, document);
    }
    if (isDone() || this.extents.getDocument() == document) {
      if (document == 12110526) {
        System.err.printf("\t%s: already loaded. Returning.\n",
                Utility.shortName(this));
      }
      return;
    }
    extents.reset();
    if (context != null) {
      extents.setDocument(document);
    }

    ExtentArrayIterator[] arrayIterators;
    arrayIterators = new ExtentArrayIterator[iterators.length];
    for (int i = 0; i < iterators.length; i++) {
      if (iterators[i].isDone()
              || !iterators[i].hasMatch(document)) {
        // we can not load any extents if the iterator is done - or is at the wrong document.
        if (document == 12110526) {
          if (iterators[i].isDone()) {
            System.err.printf("Iterator %s is done. Returning.\n",
                    Utility.shortName(iterators[i]));
          }
          if (!iterators[i].hasMatch(document)) {
            System.err.printf("Iterator %s doesn't match. Returning.\n",
                    Utility.shortName(iterators[i]));
          }
        }
        return;
      }

      ExtentArray ea = ((MovableExtentIterator) iterators[i]).extents();
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int eai = 0; eai < ea.size(); ++eai) {
        sb.append(ea.begin(eai)).append(",");
      }
      sb.append("]");
      if (document == 12110526) {
        System.err.printf("EXTENTS (%d): %s -> %s\n", ea.getDocument(),
                Utility.shortName(iterators[i]), sb.toString());
      }
      arrayIterators[i] = new ExtentArrayIterator(((MovableExtentIterator) iterators[i]).extents());
      if (arrayIterators[i].isDone()) {
        // if this document does not have any extents we can not load any extents
        return;
      }

    }

    boolean notDone = true;
    while (notDone) {
      // find the start of the first word
      boolean invalid = false;
      int begin = arrayIterators[0].currentBegin();

      // loop over all the rest of the words
      for (int i = 1; i < arrayIterators.length; i++) {
        int end = arrayIterators[i - 1].currentEnd();

        // try to move this iterator so that it's past the end of the previous word
        assert (arrayIterators[i] != null);
        assert (!arrayIterators[i].isDone());
        while (end > arrayIterators[i].currentBegin()) {
          notDone = arrayIterators[i].next();

          // if there are no more occurrences of this word,
          // no more ordered windows are possible
          if (!notDone) {
            return;
          }
        }

        if (arrayIterators[i].currentBegin() - end >= width) {
          invalid = true;
          break;
        }
      }

      int end = arrayIterators[arrayIterators.length - 1].currentEnd();

      // if it's a match, record it
      if (!invalid) {
        extents.add(begin, end);
      }

      // move the first iterator forward - we are double dipping on all other iterators.
      notDone = arrayIterators[0].next();
    }
  }
}
