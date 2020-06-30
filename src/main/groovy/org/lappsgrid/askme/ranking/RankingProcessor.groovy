package org.lappsgrid.askme.ranking

import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.metrics.Tags
import org.lappsgrid.askme.core.model.Document

import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Slf4j('logger')
class RankingProcessor{

    ExecutorCompletionService<Document> executor
//    CompositeRankingEngine engines


    RankingProcessor(MeterRegistry registry) {
        this(Runtime.getRuntime().availableProcessors(), registry)
     }


    RankingProcessor(int nThreads, MeterRegistry registry) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads)
        executor = new ExecutorCompletionService<>(executorService)
//        new ExecutorServiceMetrics(executorService, "ranking.executor", Tags.RANK).bindTo(registry)
//        engines = new CompositeRankingEngine(params)
    }

    List<Document> rank(Query query, Map params, List<Document> documents) {
        CompositeRankingEngine engines = new CompositeRankingEngine(params)
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

    Document score(Query query, Map params, Document document){
        CompositeRankingEngine engines = new CompositeRankingEngine(params)
        Document ranked_document = new Document()
        Future<Document> future = executor.submit(new RankingWorker(document, engines, query))
        try {
            ranked_document = future.get()
        }
        catch (InterruptedException e) {
            logger.error("Ranking was interrupted.")
            // We are not re-throwing the InterruptedException so we should set the interrupt flag.
            Thread.currentThread().interrupt()
        }
        catch (ExecutionException e) {
            logger.error("Unable to get future document.", e)
        }
        return ranked_document
    }



}
