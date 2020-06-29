package org.lappsgrid.askme.ranking

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.metrics.Tags
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Serializer

/**
 * TODO:
 * 1) Update imports to phase out eager (waiting on askme-core pom)
 * 2) Add exceptions / case statements to recv method?
 * 3) Errors regarding FindCreateRanker
 * 4) Close ranking processors BEFORE removing them from ranking_processors to have cleaner shutdown?
 */

@CompileStatic
@Slf4j('logger')
class Main {

    static final Configuration config = new Configuration()

    final PostOffice po = new PostOffice(config.EXCHANGE, config.HOST)

    final PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    MailBox box
    RankingProcessor ranker
    Counter documentsRanked
    Counter messagesReceived
    Timer timer
    Timer documentTimer

    void init() {
        new ClassLoaderMetrics().bindTo(registry)
        new JvmMemoryMetrics().bindTo(registry)
        new JvmGcMetrics().bindTo(registry)
        new ProcessorMetrics().bindTo(registry)
        new JvmThreadMetrics().bindTo(registry)
        ranker = new RankingProcessor(registry)
        documentsRanked = registry.counter("documents_ranked", "service", Tags.RANK)
        messagesReceived = registry.counter("messages", "service", Tags.RANK)
        timer = registry.timer("ranking_times")
        documentTimer = registry.timer("document_ranking_times")
    }

    void run(Object lock) {
        init()
        logger.info("Rabbit   : {}", config.HOST)
        logger.info("Exchange : {}", config.EXCHANGE)
        logger.info("Address  : {}", config.RANKING_MBOX)
        box = new MailBox(config.EXCHANGE, config.RANKING_MBOX, config.HOST) {
            // stores the ranking processor for given ID and parameters
            // all documents with the same ID will use the same ranking processor
            Map ranking_processors = [:]

            @Override
            void recv(String s) {
//                new File("/tmp/ranking.json").text = groovy.json.JsonOutput.prettyPrint(s)
                messagesReceived.increment()
                AskmeMessage message = Serializer.parse(s, AskmeMessage)
                String id = message.getId()
                String command = message.getCommand()

                if (command == 'EXIT' || command == 'QUIT') {
                    logger.info('Received shutdown message, terminating Ranking service')
                    synchronized(lock) { lock.notify() }
                }
                else if(command == 'PING') {
                    logger.info('Received PING message from and sending response back to {}', message.route[0])
                    message.setCommand('PONG')
                    logger.info('Response PONG sent to {}',  message.route[0])
                    Main.this.po.send(message)
                }
                else if (command == "METRICS") {
                    Message response = new Message()
                    response.id = message.id
                    response.setCommand('ok')
                    response.body(registry.scrape())
                    response.route = message.route
                    logger.trace('Metrics sent to {}', response.route[0])
                    Main.this.po.send(response)
                }
                else if (command == "remove_ranking_processor") {
                    logger.info('Received command to remove ranking processor {}', id)
                    ranking_processors.remove(id)
                    logger.info('Removed ranking processor {}', id)
                }
                else {
                    logger.info("Received documents from query {}", id)
                    Map params = message.getParameters()
                    String destination = message.route[0] ?: 'the void'
                    Packet packet = message.body
//                    RankingProcessor ranker = new RankingProcessor(params)
                    timer.record {
                        rank(ranker, params, packet)
                    }

                    logger.info('Sending ranked documents from message {} back to web', id)
                    logger.info('Command: {}', message.getCommand())
                    Main.this.po.send(message)
                    logger.info('Ranked documents from message {} sent back to {}',message.id, destination)
                }
            }


        }
        synchronized(lock) { lock.wait() }
        box.close()
        po.close()
        logger.info("Ranking service terminated")
        System.exit(0)
    }

    void rank(RankingProcessor ranker, Map params, Packet packet) {
        List<Document> scored = []
        packet.documents.each { Document doc ->
            logger.trace("Before Doc {}: {}", doc.id, doc.score)
            Document scoredDoc = documentTimer.recordCallable { ranker.score(packet.query, params, doc) }
            logger.trace("Scored Doc {}: {}", scoredDoc.id, scoredDoc.score)
            scored.add(scoredDoc)
//            ranker.score(packet.query, doc)
        }
        documentsRanked.increment(scored.size())
        logger.debug("Sorting {} documents.", packet.documents.size())
        packet.documents = scored.sort({a,b -> b.score <=> a.score})
        logger.trace("Done sort.")
    }

    static void main(String[] args) {
        logger.info("Starting Ranking service")
        Object lock = new Object()
        Thread.start {
            new Main().run(lock)
        }
    }
}
