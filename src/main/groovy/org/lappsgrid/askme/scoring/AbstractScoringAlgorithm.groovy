package org.lappsgrid.askme.scoring

import org.lappsgrid.askme.core.model.Token

/**
 *
 */
@Deprecated
abstract class AbstractScoringAlgorithm implements ScoringAlgorithm {
    boolean contains(List<String> strings, Token token) {
        return strings.contains(token.word) || strings.contains(token.lemma)
    }
}
