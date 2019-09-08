package org.lappsgrid.askme.ranking

import groovy.util.logging.Slf4j
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.RabbitMQ
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Serializer
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

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
//    Stanford nlp = new Stanford()
    MailBox box


    Main(){
        println config.EXCHANGE
        println config.HOST
    }

    void run(Object lock) {
        box = new MailBox(config.EXCHANGE, 'ranking.mailbox', config.HOST) {
            // stores the ranking processor for given ID and parameters
            // all documents with the same ID will use the same ranking processor
            Map ranking_processors = [:]

            @Override
            void recv(String s) {
                AskmeMessage message = Serializer.parse(s, AskmeMessage)
                String id = message.getId()
                String command = message.getCommand()

                if (command == 'EXIT' || command == 'QUIT') {
                    logger.info('Received shutdown message, terminating Ranking service')
                    synchronized(lock) { lock.notify() }
                }
                else if(command == 'PING') {
                    logger.info('Received PING message from and sending response back to {}', message.route[0])
                    Message response = new Message()
//                    response.setBody('ranking.mailbox')
                    response.setCommand('PONG')
                    response.setRoute(message.route)
                    logger.info('Response PONG sent to {}', response.route[0])
                    Main.this.po.send(response)
                }
                else if (command == "remove_ranking_processor") {
                    logger.info('Received command to remove ranking processor {}', id)
                    ranking_processors.remove(id)
                    logger.info('Removed ranking processor {}', id)
                }
                else {
                    logger.info("Received documents from query {}", id)
                    Object params = message.getParameters()
                    String destination = message.route[0] ?: 'the void'
//                    Map dq = message.body as Map
//
//                    Query query = new Query(dq.query)
//                    List<Map> documents = (List) dq.documents

                    Packet packet = message.body
                    Query query = packet.query
                    List<Document> documents = packet.documents

                    RankingProcessor ranker = new RankingProcessor(params)
//                    List<Document> documents = solrToDoc(solr)
                    //List<Document> sorted_documents = rank(ranker, documents, query)
                    rank(ranker, packet)

                    logger.info('Sending ranked documents from message {} back to web', command, id)
                    //message.setBody(sorted_documents)

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

//    List<Document> solrToDoc(SolrDocumentList solr){
//        List<Document> doc_list = []
//        solr.each{solr_doc ->
//            doc_list.add(createDocument(solr_doc))
//        }
//        return doc_list
//    }

    void rank(RankingProcessor ranker, Packet packet) {
        packet.documents.each { Document doc ->

        }
    }
    List<Document> rank(RankingProcessor ranker, List<Document> documents, Query query) {
        List<Document> scored_documents = []
        documents.each{Document doc ->
            scored_documents.add(ranker.score(query, doc))
        }
        return scored_documents.sort{a,b -> b.score <=> a.score}
    }

/*
    Document createDocument(Map solr){
        Document document = new Document()
        ['id', 'pmid', 'pmc', 'doi', 'year', 'path'].each { field ->
            document.setProperty(field, solr[field])
        }
        Section title = nlp.process(solr['title'].toString())
        document.setProperty('title', title)
        Section abs = nlp.process(solr['abstract'].toString())
        document.setProperty('articleAbstract', abs)
        return document
    }
    RankingProcessor findCreateRanker(String id, Map params, Map ranking_processors){
        if (!ranking_processors.containsKey(id)) {
            ranking_processors."${id}" = new RankingProcessor(params)
            //ranking_processors[id] - new RankingProcessor(params) --> error with null object
        }
        RankingProcessor ranker = ranking_processors."${id}"
        //RankingProcessor ranker = ranking_processors[id] --> Cannot assign object to ranker

        return ranker
    }

*/
    static void main(String[] args) {
        logger.info("Starting Ranking service")
        Object lock = new Object()
        Thread.start {
            new Main().run(lock)
        }
    }

    /**
     *
     * CODE BELOW NOT CURRENTLY USED
     *


    //Checks if:
    // 1) Message body is Map
    // 2) body.document is not null TODO: check to see if not SolrDocument
    // 3) body.query is not null TODO: check to see if not Query
    // 4) Message parameters is not null
    // 5) Message command is not null
    boolean checkMessage(Message message) {
        Map error_check = [:]
        error_check.origin = "Ranking"
        error_check.messageId = message.getId()
        boolean error_flag = false
        if (message.body.getClass() != LinkedHashMap) {
            logger.info('ERROR: Body of Message {} is {}, expected Map', message.getId(), message.body.getClass().toString())
            error_check.body = 'REQUIRES MAP (given ' + message.body.getClass().toString() + ')'
            error_flag = true
        } else {
            Object dq = message.body

            if (!dq.document) {
                logger.info('ERROR: Body.document of Message {} is {}, expected SolrDocument', message.getId(), message.body.document.getClass().toString())
                error_check.bodyDoc = 'REQUIRES SolrDocument (given ' + message.body.document.getClass().toString() + ')'
                error_flag = true
            }

            if (!dq.query) {
                logger.info('ERROR: Body.query of Message {} is {}, expected Query', message.getId(), message.body.query.getClass().toString())
                error_check.bodyQuery = 'REQUIRES Query (given ' + message.body.query.getClass().toString() + ')'
                error_flag = true
            }
        }
        if (!message.getParameters()) {
            logger.info('ERROR: Parameters of Message {} is empty', message.getId())
            error_check.params = 'MISSING'
            error_flag = true
        }
        if (!message.getCommand()) {
            logger.info('ERROR: Document number (command) of Message {} is empty', message.getId())
            error_check.command = 'MISSING'
            error_flag = true
        }
        if(error_flag){
            logger.info('Notifying Web service of error')
            Message error_message = new Message()
            error_message.setCommand('ERROR')
            error_message.setBody(error_check)
            error_message.route('web.mailbox')
            po.send(error_message)
        }
        return !error_flag
    }

    */
}
