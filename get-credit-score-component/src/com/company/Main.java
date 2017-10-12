package com.company;

import com.company.creditScore.CreditScoreService;
import com.company.creditScore.CreditScoreService_Service;
import com.rabbitmq.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Loan_Request_Queue";
    private static final String PUBLISH_QUEUE_NAME = "Get_Credit_Score_Queue";

    public static void main(String[] args) {

        



    }

    private static int creditScore(java.lang.String ssn) {
        CreditScoreService_Service service = new CreditScoreService_Service();
        CreditScoreService port = service.getCreditScoreServicePort();
        return port.creditScore(ssn);
    }

    private static String receiveMessage() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, false, consumer);

        String response = "";
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            response = new String(delivery.getBody());

        }
        catch (InterruptedException ex){
            ex.printStackTrace();
        }

        return response;

    }

    private static void sendMessage(String message, Channel channel) throws IOException, TimeoutException {
        try {
            channel.queueDeclare(PUBLISH_QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message.getBytes("UTF-8"));
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        channel.close();
        channel.getConnection().close();

    }

    private static String getSsn(String xml) {
        String ssn = "";
        try {
            Document document = loadXMLFromString(xml);
            Element ssnNode = document.getElementById("ssn");
            ssn = ssnNode.getTextContent();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ssn;

    }

    private static byte[] appendCreditScore(String xmlToAppend, String creditScore) {
        byte[] xmlByteArray = {};
        try {
            Document document = loadXMLFromString(xmlToAppend);
//            Element loanAmountNode = document.getElementById("loanAmount");
//            Element creditScoreNode = document.createElement("creditScore");
//            creditScoreNode.appendChild(document.createTextNode(creditScore));
//            document.insertBefore(creditScoreNode, loanAmountNode);
            Element creditScoreNode = document.getElementById("creditScore");
            creditScoreNode.appendChild(document.createTextNode(creditScore));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);

            transformer.transform(source, result);

            xmlByteArray = bos.toByteArray();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return xmlByteArray;

    }

    private static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    }


}
