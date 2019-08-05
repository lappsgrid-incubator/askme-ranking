package org.lappsgrid.eager.mining.ranking

import groovy.util.logging.Slf4j
import org.apache.solr.common.SolrDocument

import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice

import org.lappsgrid.eager.mining.api.Query
import org.lappsgrid.eager.mining.ranking.model.Document
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
        String command = message.getCommand()
        if(message.getBody() == 'EXIT') {
            shutdown()
        }
        else if(command == "remove_ranking_processor"){
            ranking_processors.remove(id)
            logger.info('Removed ranking processor {}',id)
        }
        else {
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
    void shutdown(){
        logger.info('Received shutdown message, terminating askme-ranking')
        po.close()
        logger.info('askme-ranking terminated')
        System.exit(0)
    }


    static void main(String[] args) {
        new Main()
    }


}
