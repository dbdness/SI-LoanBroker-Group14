package com.group14;

import com.rabbitmq.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String HOST_NAME = "207.154.228.245";
    private static final String PUBLISH_QUEUE_NAME = "Aggregator_Queue";
    private static final String CONSUME_QUEUE_NAME = "Normalizer_Queue";


    public static void main(String[] args) throws Exception {
        ConnectionFactory hostConnectionFactory = new ConnectionFactory();
        hostConnectionFactory.setHost(HOST_NAME);
        Connection hostConnection = hostConnectionFactory.newConnection();

        Channel hostChannel = hostConnection.createChannel();

        List<String> loanRequests = receiveMessages(hostChannel);
        String bestInterestRate = calculateBestLoan(loanRequests);

        sendMessage(bestInterestRate, hostChannel);
    }

    /**
     * Receives all the different Loan Responses from the Normalizer queue.
     *
     * @param channel channel to consume messages from.
     * @return list of different Loan Responses in String format.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static List<String> receiveMessages(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, false, consumer); //TODO change Boolean parameter to "true" after testing.

        List<String> loanResponses = new ArrayList<>();
        String response;
        try {
            do {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(1000); //One seconds in milliseconds.
                try {
                    response = new String(delivery.getBody());
                    loanResponses.add(response);
                } catch (NullPointerException ex) {
                    break; //Breaks if there are no more incoming messages.
                }
            }
            while (!response.equals(""));


        } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
            ex.printStackTrace();
        }

        return loanResponses;

    }

    /**
     * Calculates the best interest rate, based on all loan responses.
     *
     * @param loanResponses list of loan responses to check.
     * @return best provided interest rate.
     */
    private static String calculateBestLoan(List<String> loanResponses) {
        double interestRate = 0;
        String bank = "";

        for (String loanResponse : loanResponses) {
            try {
                //loanResponse = loanResponse.toLowerCase(); //Due to missing camelcase in WSDL attribute. TODO Delete this code after XML altering.
                double responseInterestRate = Double.parseDouble(getNodeValue(loanResponse, "interestRate"));
                if (responseInterestRate > interestRate) interestRate = responseInterestRate;
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }

        return String.format("%s banks handled the request. Best interest rate: %s", loanResponses.size(), interestRate);

    }

    /**
     * Gets the value of requested node in an XML string.
     *
     * @param xml  to analyze.
     * @param node to extract data from.
     * @return value of requested node in String format.
     */
    private static String getNodeValue(String xml, String node) {
        String nodeValue = "";
        try {
            Document document = loadXMLFromString(xml);
            Node wantedNote = document.getElementsByTagName(node).item(0);
            nodeValue = wantedNote.getTextContent();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


        return nodeValue;

    }

    /**
     * Loads an XML String into a {@link Document} object, for better XML data handling.
     *
     * @param xml string to load into a {@link Document}.
     * @return input XML String as {@link Document}.
     */
    private static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new ByteArrayInputStream(xml.getBytes()));

    }

    /**
     * Puts the desired message on a queue.
     *
     * @param message message to send.
     * @param channel channel to publish message.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static void sendMessage(String message, Channel channel) throws IOException, TimeoutException {
        try {
            channel.queueDeclare(PUBLISH_QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message.getBytes());
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        channel.close();
        channel.getConnection().close();

    }
}
