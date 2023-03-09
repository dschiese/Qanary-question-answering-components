package eu.wdaqua.qanary.component.textrazor.ner;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class TextRazor extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(TextRazor.class);

    private final String applicationName;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    @Value("${textrazor.key}")
    private String key;

    public TextRazor(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     *
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
        logger.info("process: {}", myQanaryMessage);
        // TODO: implement processing of question
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        ArrayList<Selection> selections = new ArrayList<Selection>();

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://api.textrazor.com");
        httppost.setHeader("x-textrazor-key", key);
        httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("text", myQuestion));
        params.add(new BasicNameValuePair("extractors", "entities"));
        httppost.setEntity(new UrlEncodedFormEntity(params));
        try {
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                // String result = getStringFromInputStream(instream);
                String text = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
                //JSONArray jsonArray = new JSONArray(text);
                JSONObject response2 = new JSONObject(text);
                logger.info("String: {}", response2);
                JSONObject ents = (JSONObject) response2.get("response");
                JSONArray jsonArray = (JSONArray) ents.get("entities");
                if (jsonArray.length() != 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject explrObject = jsonArray.getJSONObject(i);
                        int begin = (int) explrObject.get("startingPos");
                        int end = (int) explrObject.get("endingPos");
                        logger.info("Question: {}", explrObject);
                        logger.info("Question: {}", begin);
                        logger.info("Question: {}", end);
                        Selection s = new Selection();
                        s.begin = begin;
                        s.end = end;
                        selections.add(s);
                    }
                }
            }

        } catch (ClientProtocolException e) {
            logger.info("Exception: {}", e);
            // TODO Auto-generated catch block
        } catch (IOException e1) {
            logger.info("Except: {}", e1);
            // TODO Auto-generated catch block
        }
        logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
        // TODO: insert data in QanaryMessage.outgraph

        logger.info("apply vocabulary alignment on outgraph");
        // TODO: implement this (custom for every component)
        for (Selection s : selections) {

            QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
            bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
            bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
            bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(s.begin), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(s.end), XSDDatatype.XSDnonNegativeInteger));
            bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

            // get the template of the INSERT query
            String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
            logger.info("SPARQL query: {}", sparql);
            myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        }
        return myQanaryMessage;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    class Selection {
        public int begin;
        public int end;
    }
}
