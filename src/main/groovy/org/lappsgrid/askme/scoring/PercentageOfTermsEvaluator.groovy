package org.lappsgrid.askme.scoring

import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.askme.core.model.Token

/**
 *  How many terms appear in the passage.
 */
class PercentageOfTermsEvaluator implements ScoringAlgorithm {
    @Override
    float score(Query query, Section section) {
        int count = 0
        query.terms.each { term ->
            Token token = section.tokens.find { it.word == term }
            if (token) {
                ++count
            }
        }
        return ((float) count) / query.terms.size()
    }

    @Override
    String name() {
        return 'PercentageOfTerms'
    }

    @Override
    String abbrev() {
        return "pterms"
    }
}
