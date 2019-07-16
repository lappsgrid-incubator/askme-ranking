package org.lappsgrid.eager.mining.ranking

import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.lappsgrid.eager.mining.api.Query
import org.lappsgrid.eager.mining.core.json.Serializer
import org.lappsgrid.eager.mining.ranking.model.Document
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice

class Main extends MessageBox{

    static final String BOX = 'ranking.mailbox'
    static final String WEB_MBOX = 'web.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    PostOffice po = new PostOffice(EXCHANGE, HOST)

    Main(){
        super(EXCHANGE,BOX)
    }

    void recv(Message message){


        /**
         * TO-Do
         *
         * 1) Need to get params (from web? initial query?)
         * 2) Need to maintain query throughout to send to ranker
         * 3) solr documents --> ? --> json string via message --> document list to rank --> json string via message --> web results
         * 4)





        Map params = [:]
        RankingProcessor ranker = new RankingProcessor(params)
        List<Document> unranked_documents = []
        Query query =  Serializer.parse(Serializer.toJson(message.body), Query)
        List<Document> ranked_documents = ranker.rank(query, unranked_documents)
        message.setRoute([WEB_MBOX])
        message.setCommand('ranked')
        message.setBody('')
        po.send(message)

         */

        result = message.getBody()
        SolrDocumentList documents = result.documents


        Query query = result.query

        //WHERE DO THE PARAMS COME FROM
        Map params = [:]
        RankingProcessor ranker = new RankingProcessor(params)

        List docs = []
        for (int i = 0; i < n; ++i) {
            SolrDocument doc = documents.get(i)
             docs << new Document(doc)
        }

        List<Document> ranked_documents = ranker.rank(query, docs)

        result.documents = ranked_documents


    }





    static void main(String[] args) {
        new Main()
    }


}
