package com.company;

import com.company.creditScore.CreditScoreService;
import com.company.creditScore.CreditScoreService_Service;
import com.rabbitmq.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String xmlMessage = receiveMessage(channel);
        String requestSsn = getSsn(xmlMessage);
        int creditScore = creditScore(requestSsn);
        byte[] updatedRequest = appendCreditScore(xmlMessage, String.valueOf(creditScore));
        sendMessage(updatedRequest, channel);


    }

    /**
     * Calls the 'Get Credit Score' WSDL service and receives a credit score, based on the input ssn.
     * @param ssn Social security number to receive a credit score on.
     * @return Credit score based on the input SSN.
     */
    private static int creditScore(java.lang.String ssn) {
        CreditScoreService_Service service = new CreditScoreService_Service();
        CreditScoreService port = service.getCreditScoreServicePort();
        return port.creditScore(ssn);
    }

    /**
     * Receives the incoming message from the previous queue.
     *
     * @param channel channel to consume messages from.
     * @return list of different Loan Responses in String format.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static String receiveMessage(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, true, consumer);

        String response = "";
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            response = new String(delivery.getBody());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        return response;

    }

    /**
     * Puts the desired message on a queue.
     *
     * @param message message to send.
     * @param channel channel to publish message.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static void sendMessage(byte[] message, Channel channel) throws IOException, TimeoutException {
        try {
            channel.queueDeclare(PUBLISH_QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message);
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        channel.close();
        channel.getConnection().close();

    }

    /**
     * Extracts the social security number from the specified XML String.
     * @param xml XML String to extract ssn from.
     * @return ssn as a String object.
     */
    private static String getSsn(String xml) {
        String ssn = "";
        try {
            Document document = loadXMLFromString(xml);
            Node ssnNode = document.getElementsByTagName("ssn").item(0);
            ssn = ssnNode.getTextContent();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return ssn;

    }

    /**
     * Appends a credit score value to an existing XML String.
     * @param xmlToAppend XML String to append credit score on.
     * @param creditScore credit score to append on specified XML String.
     * @return XML String with appended credit score, converted to byte array.
     */
    private static byte[] appendCreditScore(String xmlToAppend, String creditScore) {
        byte[] xmlByteArray = {};
        try {
            Document document = loadXMLFromString(xmlToAppend);

            Node creditScoreNode = document.getElementsByTagName("creditScore").item(0);
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

    /**
     * Loads the specified XML String as a Document object.
     * @param xml XML String to load.
     * @return Document object based on the XML String.
     * @throws Exception
     */
    private static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new ByteArrayInputStream(xml.getBytes()));


    }


}
