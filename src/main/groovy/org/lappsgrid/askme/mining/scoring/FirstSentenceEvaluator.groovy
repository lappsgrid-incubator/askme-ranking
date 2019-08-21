package org.lappsgrid.askme.mining.scoring

//import org.lappsgrid.eager.mining.api.Query
//import org.lappsgrid.eager.mining.model.Section
//import org.lappsgrid.eager.mining.model.Sentence
//import org.lappsgrid.eager.mining.model.Token
import org.lappsgrid.askme.core.model.Token
import org.lappsgrid.askme.core.model.Sentence
import org.lappsgrid.askme.core.model.Section

import org.lappsgrid.askme.core.api.Query

/**
 * Returns the perctage of query terms found in the first sentence.
 */
class FirstSentenceEvaluator implements ScoringAlgorithm {
    @Override
    float score(Query query, Section section) {
        Set<String> found = new HashSet<>()
        if (section.sentences.size() > 0) {
            Sentence s = section.sentences[0]
            for (Token t : s.tokens) {
                if (query.contains(t)) {
                    found.add(t.lemma)
                }
            }
        }
        return ((float)found.size()) / query.terms.size()
    }

    @Override
    String name() {
        return "FirstSentenceEvaluator"
    }

    @Override
    String abbrev() {
        return "1stSent"
    }
}
