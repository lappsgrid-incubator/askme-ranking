package org.lappsgrid.askme.scoring

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section

/**
 *
 */
@Slf4j('logger')
class WeightedAlgorithm implements ScoringAlgorithm {
    float weight
    ScoringAlgorithm algorithm

    private String name

    WeightedAlgorithm(ScoringAlgorithm algorithm, float weight=1.0f) {
        this.weight = weight
        this.algorithm = algorithm
        this.name = String.format("Weighted %s: %1.3f", algorithm.name(), weight)
    }

    float score(Query query, Section section) {
        float value = algorithm.score(query, section)
        //logger.debug("{} scored {}", this.name, value)
        return weight * value //algorithm.score(query, section)
    }

    String name() {
        return name
    }

    String abbrev() {
        return algorithm.abbrev()
    }
}
