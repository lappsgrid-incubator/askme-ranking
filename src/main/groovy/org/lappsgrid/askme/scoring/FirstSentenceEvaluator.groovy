package org.lappsgrid.askme.scoring



import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.askme.core.model.Sentence
import org.lappsgrid.askme.core.model.Token
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
