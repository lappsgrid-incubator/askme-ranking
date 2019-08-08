package org.lappsgrid.askme.mining.ranking

import groovy.util.logging.Slf4j
import org.apache.solr.common.SolrDocument
import org.lappsgrid.askme.mining.ranking.model.Document
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice

import org.lappsgrid.eager.mining.api.Query
import org.lappsgrid.eager.mining.model.Section




@Slf4j('logger')
class Main extends MessageBox{

    static final String BOX = 'ranking.mailbox'
    static final String WEB_MBOX = 'web.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    PostOffice po = new PostOffice(EXCHANGE, HOST)
    Stanford nlp = new Stanford()
    Map ranking_processors = [:]


    Main(){
        super(EXCHANGE, BOX)

    }

    void recv(Message message){
        String id = message.getId()
        logger.info('Received Message {}', id)
        String command = message.getCommand()
        if(command == 'EXIT' || command == 'QUIT') {
            shutdown()
        }
        else if(command == "remove_ranking_processor"){
            ranking_processors.remove(id)
            logger.info('Removed ranking processor {}',id)
        }
        else if(checkMessage(message)){
            logger.info("Received document {} from query {}", command, id)
            Object params = message.getParameters()
            Object dq = message.body
            Query q = dq.query
            SolrDocument solr = dq.document

            Document document = createDocument(solr)
            RankingProcessor ranker = findCreateRanker(id, params)


            Document scored_document = ranker.score(q, document)

            logger.info('Score: {}', scored_document.getScore())
            logger.info('Sending ranked document {} from message {} back to web', command, id)
            message.setBody(scored_document)
            message.setRoute([WEB_MBOX])
            po.send(message)
        }
        else {
            logger.info("Message {} terminated", message.getId())
        }

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

    RankingProcessor findCreateRanker(String id, Map params){
        if (!ranking_processors.containsKey(id)) {
            ranking_processors."${id}" = new RankingProcessor(params)
        }
        RankingProcessor ranker = ranking_processors."${id}"
        return ranker
    }
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
            /*
            if (!dq.document) {
                logger.info('ERROR: Body.document of Message {} is {}, expected SolrDocument', message.getId(), message.body.document.getClass().toString())
                error_check.bodyDoc = 'REQUIRES SolrDocument (given ' + message.body.document.getClass().toString() + ')'
                error_flag = true
            }
             */
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

    void shutdown(){
        logger.info('Received shutdown message, terminating Ranking service')
        po.close()
        logger.info('Ranking service terminated')
        System.exit(0)
    }


    static void main(String[] args) {
        new Main()
    }


}
