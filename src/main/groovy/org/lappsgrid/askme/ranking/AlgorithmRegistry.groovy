package org.lappsgrid.askme.ranking

import org.lappsgrid.askme.scoring.ConsecutiveTermEvaluator
import org.lappsgrid.askme.scoring.FirstSentenceEvaluator
import org.lappsgrid.askme.scoring.PercentageOfTermsEvaluator
import org.lappsgrid.askme.scoring.ScoringAlgorithm
import org.lappsgrid.askme.scoring.SentenceCountEvaluator
import org.lappsgrid.askme.scoring.TermFrequencyEvaluator
import org.lappsgrid.askme.scoring.TermOrderEvaluator
import org.lappsgrid.askme.scoring.TermPositionEvaluator

/**
 *
 */
class AlgorithmRegistry {

    static Map<String, ScoringAlgorithm> algorithms = [
            '1': { new ConsecutiveTermEvaluator() },
            '2': { new PercentageOfTermsEvaluator() },
            '3': { new TermPositionEvaluator() },
            '4': { new TermFrequencyEvaluator() },
            '5': { new TermOrderEvaluator() },
            '6': { new FirstSentenceEvaluator() },
            '7': { new SentenceCountEvaluator() }
    ]

    static ScoringAlgorithm get(String id) {
        Closure constructor = algorithms[id]
        if (!constructor) {
            return null
        }
        return constructor()
    }
}
