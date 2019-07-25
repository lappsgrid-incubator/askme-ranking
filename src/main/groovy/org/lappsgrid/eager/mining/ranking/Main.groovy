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
import org.lappsgrid.eager.mining.model.Section



@Slf4j('logger')
class Main extends MessageBox{

    static final String BOX = 'ranking.mailbox'
    static final String WEB_MBOX = 'web.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    PostOffice po = new PostOffice(EXCHANGE, HOST)
    Stanford nlp = new Stanford()


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
        SolrDocument solr = dq.document
        Document doc = new Document()
        ['id', 'pmid','pmc','doi','year','path'].each { field ->
            doc.setProperty(field, solr.getFieldValue(field))
        }

        Section title = nlp.process(solr.getFieldValue('title').toString())
        doc.setProperty('title', title)

        Section abs = nlp.process(solr.getFieldValue('abstract').toString())
        doc.setProperty('articleAbstract', abs)

        List document = [doc]

        RankingProcessor ranker = new RankingProcessor(params)

        List<Document> sorted = ranker.rank(q, document)
        logger.info('Score:{}', sorted[0].getScore())
        logger.info('Sending ranked document {} from message {} back to web',num,id)
        message.setBody(sorted[0])
        message.setRoute([WEB_MBOX])
        po.send(message)

    }


    static void main(String[] args) {
        new Main()
    }


}
