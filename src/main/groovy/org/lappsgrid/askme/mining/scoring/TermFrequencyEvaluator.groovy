package org.lappsgrid.askme.mining.scoring

//import org.lappsgrid.eager.mining.api.Query
//import org.lappsgrid.eager.mining.model.Section

import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.askme.core.api.Query

/**
 * How many words in the passage are terms in the question.
 */
class TermFrequencyEvaluator implements ScoringAlgorithm {
    @Override
    float score(Query query, Section section) {
        int count = 0
        section.tokens.each { token ->
            if (query.contains(token)) {
                ++count
            }
        }
        return ((float)count) / section.tokens.size()
    }

    @Override
    String name() {
        return 'TermFrequency'
    }

    @Override
    String abbrev() {
        return "freq"
    }
}
