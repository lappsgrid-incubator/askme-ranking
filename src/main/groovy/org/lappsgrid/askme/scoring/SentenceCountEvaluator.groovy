package org.lappsgrid.askme.scoring


import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.askme.core.model.Sentence
import org.lappsgrid.askme.core.model.Token

/**
 *
 */
class SentenceCountEvaluator implements ScoringAlgorithm {
    @Override
    float score(Query query, Section section) {
        int count = 0
        for (Sentence s : section.sentences) {
            for (Token t : s.tokens) {
                if (query.contains(t)) {
                    ++count
                    break
                }
            }
        }
        if (section.sentences == null || section.sentences.size() == 0) {
            return 0.0f
        }

        return ((float) count) / section.sentences.size()
    }

    @Override
    String name() {
        return "SentenceCountEvaluator"
    }

    @Override
    String abbrev() {
        return "sents"
    }
}
