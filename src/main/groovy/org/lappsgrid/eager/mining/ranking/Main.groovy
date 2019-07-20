package org.lappsgrid.eager.mining.ranking

import groovy.util.logging.Slf4j
import org.apache.calcite.adapter.java.Map
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.lappsgrid.eager.mining.api.Query
import org.lappsgrid.eager.mining.core.json.Serializer
import org.lappsgrid.eager.mining.ranking.model.Document
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import groovy.json.JsonSlurper

@Slf4j('logger')
class Main extends MessageBox{

    static final String BOX = 'ranking.mailbox'
    static final String WEB_MBOX = 'web.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    PostOffice po = new PostOffice(EXCHANGE, HOST)

    Main(){
        super(EXCHANGE, BOX)

    }

    void recv(Message message){
        String id = message.getId()
        String num = message.getCommand()
        logger.info("Received document {} from query {}", num, id)
        Object params = message.getParameters()

        Object dq = message.body
        Query q = dq.query

        //Cause of error - can't cast to document
        Document d = dq.document
        //Problem comes form web / solr ?

        RankingProcessor ranker = new RankingProcessor(params)
        List<Document> docList = [d]
        List<Document> sorted = ranker.rank(q, docList)
        logger.info('Score:{}', sorted[0].getScore())

    }


    static void main(String[] args) {
        new Main()
    }


}
