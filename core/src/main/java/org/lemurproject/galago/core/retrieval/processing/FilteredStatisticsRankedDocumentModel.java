// BSD License (http://lemurproject.org/galago-license)
package org.lemurproject.galago.core.retrieval.processing;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import org.lemurproject.galago.core.index.Index;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.iterator.MovableCountIterator;
import org.lemurproject.galago.core.retrieval.iterator.MovableIterator;
import org.lemurproject.galago.core.retrieval.iterator.MovableScoreIterator;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.core.retrieval.traversal.AdjustAnnotationsTraversal;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author irmarc
 */
public class FilteredStatisticsRankedDocumentModel extends ProcessingModel {

  LocalRetrieval retrieval;
  Index index;

  public FilteredStatisticsRankedDocumentModel(LocalRetrieval lr) {
    retrieval = lr;
    this.index = retrieval.getIndex();
  }

  @Override
  public ScoredDocument[] execute(Node queryTree, Parameters queryParams) throws Exception {
    // FIRST PASS -- used to only gather statistics for the second pass
    FilteredStatisticsScoringContext fssContext = new FilteredStatisticsScoringContext();

    // construct the iterators -- we use tree processing
    MovableScoreIterator iterator =
            (MovableScoreIterator) retrieval.createIterator(queryParams, queryTree, fssContext);
    ProcessingModel.initializeLengths(retrieval, fssContext);

    while (!iterator.isDone()) {
      int document = iterator.currentCandidate();

      // This context is shared among all scorers
      fssContext.document = document;
      fssContext.moveLengths(document);

      if (iterator.hasMatch(document)) {

        // update global statistics
        ++fssContext.documentCount;
        fssContext.collectionLength += fssContext.getLength();

        // update per-term statistics
        for (MovableIterator si : fssContext.iteratorsToNodes.keySet()) {
          if (MovableCountIterator.class.isAssignableFrom(si.getClass())) {
            MovableCountIterator ci = (MovableCountIterator) si;
            Node n = fssContext.iteratorsToNodes.get(si);
            if (ci.hasMatch(document)) {
              try {
                int count = ci.count();
                if (count > 0) {
                  // System.out.printf("node=%s,tf+=%d,df+=1\n", n.toString(), count);
                  fssContext.tfs.adjustOrPutValue(n, count, count);
                  fssContext.dfs.adjustOrPutValue(n, 1, 1);
                }
              } catch (NullPointerException npe) {
                // do nothing for this
              }
            }
          }
        }
      }
      iterator.movePast(iterator.currentCandidate());
    }

    // SECOND PASS -- should look like a normal run, except we run one more traversal
    // over the query tree to ''correct'' statistics, then instantiate the iterators.
    // We use a copy to make sure we don't perturb the original tree, in case there are
    // references outside this method.
    AdjustAnnotationsTraversal traversal = new AdjustAnnotationsTraversal(fssContext);
    queryTree = StructuredQuery.copy(traversal, queryTree);

    // Nothing special needed here
    ScoringContext context = new ScoringContext();
    // Number of documents requested.
    int requested = (int) queryParams.get("requested", 1000);
    boolean annotate = queryParams.get("annotate", false);

    // Maintain a queue of candidates
    PriorityQueue<ScoredDocument> queue = new PriorityQueue<ScoredDocument>(requested);

    // construct the iterators -- we use tree processing
    iterator = (MovableScoreIterator) retrieval.createIterator(queryParams, queryTree, context);
    ProcessingModel.initializeLengths(retrieval, context);
    while (!iterator.isDone()) {
      int document = iterator.currentCandidate();
      context.document = document;
      context.moveLengths(document);
      if (iterator.hasMatch(document)) {
        double score = iterator.score();
        if (requested < 0 || queue.size() <= requested || queue.peek().score < score) {
          ScoredDocument scoredDocument = new ScoredDocument(document, score);
          if (annotate) {
            scoredDocument.annotation = iterator.getAnnotatedNode();
          }
          queue.add(scoredDocument);
          if (requested > 0 && queue.size() > requested) {
            queue.poll();
          }
        }
      }
      iterator.movePast(iterator.currentCandidate());
    }
    return toReversedArray(queue);
  }
}