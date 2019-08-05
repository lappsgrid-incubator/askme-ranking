package org.lappsgrid.askme.mining.ranking

import org.lappsgrid.askme.mining.scoring.ConsecutiveTermEvaluator
import org.lappsgrid.askme.mining.scoring.FirstSentenceEvaluator
import org.lappsgrid.askme.mining.scoring.PercentageOfTermsEvaluator
import org.lappsgrid.askme.mining.scoring.ScoringAlgorithm
import org.lappsgrid.askme.mining.scoring.SentenceCountEvaluator
import org.lappsgrid.askme.mining.scoring.TermFrequencyEvaluator
import org.lappsgrid.askme.mining.scoring.TermOrderEvaluator
import org.lappsgrid.askme.mining.scoring.TermPositionEvaluator

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
