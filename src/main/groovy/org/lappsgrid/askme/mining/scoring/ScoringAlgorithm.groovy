package org.lappsgrid.askme.mining.scoring

//import org.lappsgrid.eager.mining.api.Query
//import org.lappsgrid.eager.mining.model.Section
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section


/**
 * Interface for any component that calculates a score for the input document.
 */
interface ScoringAlgorithm {

    float score(Query query, Section section)
    String name()
    String abbrev()
}
