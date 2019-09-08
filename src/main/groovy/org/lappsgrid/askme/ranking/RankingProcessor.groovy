package org.lappsgrid.askme.ranking

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Document

import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Slf4j('logger')
class RankingProcessor{

    ExecutorCompletionService<Document> executor
    CompositeRankingEngine engines


    RankingProcessor(Map params) {
        this(Runtime.getRuntime().availableProcessors(), params)
     }


    RankingProcessor(int nThreads, Map params) {
        executor = new ExecutorCompletionService<>(Executors.newFixedThreadPool(nThreads))
        engines = new CompositeRankingEngine(params)
    }

    List<Document> rank(Query query, List<Document> documents) {
        List<Document> result = new ArrayList<>()
        List<Future<Document>> futures = new ArrayList<>()

        for (int i = 0; i < documents.size(); ++i) {
            Document document = documents.get(i)
            Future<Document> future = executor.submit(new RankingWorker(document, engines, query))
            futures.add(future)
        }

        int count = 0
        while (count < documents.size()) {
            ++count
            Future<Document> future = executor.take()
            try {
                Document document = future.get()
                result.add(document)
            }
            catch (Throwable e) {
                logger.error("Unable to get future document.", e)
            }
        }
        logger.debug("Sorting {} documents.", documents.size())
        return result.sort { a,b -> b.score <=> a.score }
    }

    Document score(Query query, Document document){
        Document ranked_document = new Document()
        Future<Document> future = executor.submit(new RankingWorker(document, engines, query))
        Future<Document> f2 = executor.take()
        try {
            ranked_document = f2.get()
        }
        catch (Throwable e) {
            logger.error("Unable to get future document.", e)
        }
        return ranked_document
    }



}
