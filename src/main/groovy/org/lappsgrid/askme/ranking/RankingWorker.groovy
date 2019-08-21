package org.lappsgrid.askme.ranking

import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.ranking.model.Document
import java.util.concurrent.Callable

class RankingWorker implements Callable<Document> {

    Document document
    CompositeRankingEngine engines
    Query query

    RankingWorker(Document document, CompositeRankingEngine engines, Query query) {
        this.document = document
        this.engines = engines
        this.query = query
    }

    Document call() {
        return score(document, engines, query)
    }

    public Document score(Document document, CompositeRankingEngine engines, Query query) {
        return engines.rank(query, document)
    }
}
