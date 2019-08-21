package org.lappsgrid.askme.ranking

import groovy.util.logging.Slf4j
import org.apache.solr.common.SolrDocument
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.model.Section
import org.lappsgrid.askme.ranking.model.Document
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


@Slf4j('logger')
class Main {
    static final String BOX = 'ranking.mailbox'
    static final String WEB_MBOX = 'web.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    static final PostOffice po = new PostOffice(EXCHANGE, HOST)
    Stanford nlp = new Stanford()
    MailBox box

    Main(){
    }

    void run(Object lock) {
        box = new MailBox(EXCHANGE, BOX, HOST) {
            // stores the ranking processor for given ID and parameters
            // all documents with the same ID will use the same ranking processor
            Map ranking_processors = [:]
            @Override
            void recv(String s) {
                Message message = Serializer.parse(s, Message)
                String id = message.getId()
                String command = message.getCommand()

                if (command == 'EXIT' || command == 'QUIT') {
                    logger.info('Received shutdown message, terminating Ranking service')
                    synchronized(lock) { lock.notify() }
                }
                else if(command == 'PING') {
                    String origin = message.getBody()
                    logger.info('Received PING message from and sending response back to {}', origin)
                    Message response = new Message()
                    response.setBody(BOX)
                    response.setCommand('PONG')
                    response.setRoute([origin])
                    po.send(response)
                    logger.info('Response PONG sent to {}', origin)
                }
                else if (command == "remove_ranking_processor") {
                    logger.info('Received command to remove ranking processor {}', id)
                    ranking_processors.remove(id)
                    logger.info('Removed ranking processor {}', id)
                }
                else {
                    logger.info("Received document {} from query {}", command, id)
                    Object dq = message.body
                    Query q = dq.query
                    SolrDocument solr = dq.document
                    Document document = createDocument(solr)
                    Object params = message.getParameters()
                    RankingProcessor ranker = findCreateRanker(id, params, ranking_processors)
                    Document scored_document = ranker.score(q, document)
                    logger.info('Score: {}', scored_document.getScore())
                    logger.info('Sending ranked document {} from message {} back to web', command, id)
                    message.setBody(scored_document)
                    message.setRoute([WEB_MBOX])
                    po.send(message)
                    logger.info('Ranked document {} from message {} sent back to web',command,id)
                }
            }
        }
        synchronized(lock) { lock.wait() }
        box.close()
        po.close()
        logger.info("Ranking service terminated")
        System.exit(0)
    }

    Document createDocument(SolrDocument solr){
        Document document = new Document()
        ['id', 'pmid', 'pmc', 'doi', 'year', 'path'].each { field ->
            document.setProperty(field, solr.getFieldValue(field))
        }
        Section title = nlp.process(solr.getFieldValue('title').toString())
        document.setProperty('title', title)
        Section abs = nlp.process(solr.getFieldValue('abstract').toString())
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
