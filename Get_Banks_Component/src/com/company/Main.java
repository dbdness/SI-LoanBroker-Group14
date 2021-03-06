package com.company;

import com.rabbitmq.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ruleBase.RequestBankRules;
import ruleBase.RequestBankRulesService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Get_Credit_Score_Queue";
    private static final String PUBLISH_QUEUE_NAME = "Get_Banks_Queue";
    private static List<String> banks;

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String xmlMessage = receiveMessage(channel);
        sendMessage(xmlMessage, channel);
        Object[] requestData = getData(xmlMessage);
        int creditScore = Integer.parseInt(String.valueOf(requestData[1]));
        double loanAmount = Double.parseDouble(String.valueOf(requestData[2]));
        banks = getBanks(creditScore, loanAmount, String.valueOf(requestData[3]));
        sendMessage(String.valueOf(banks), channel);
        channel.close();
        channel.getConnection().close();
    }

    /**
     * Calls the WSDL Rule Base service, and receives a list of applicable banks, based on loan details.
     * @param minCreditScore Credit score of the requester of the loan.
     * @param loanAmount Desired loan amount.
     * @param customerLoanDuration Desired loan duration.
     * @return A list of Banks willing to issue the loan.
     */
    private static java.util.List<java.lang.String> getBanks(int minCreditScore, double loanAmount, String customerLoanDuration) {
        RequestBankRulesService service = new RequestBankRulesService();
        RequestBankRules port = service.getRequestBankRulesPort();
        return port.getBanks(minCreditScore, loanAmount, customerLoanDuration);
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
     * Extracts the data values (SSN, credit score, loan amount & loan duration) from the specified XML String.
     * @param xml XML String to extract data from.
     * @return Array of data values as objects.
     */
    private static Object[] getData(String xml) {
        String ssn = "";
        String creditScore = "";
        String loanAmount = "";
        String loanDuration = "";
        Object[] data = {};
        try {
            Document document = loadXMLFromString(xml);
            Node ssnNode = document.getElementsByTagName("ssn").item(0);
            ssn = ssnNode.getTextContent();

            Node creditScoreNode = document.getElementsByTagName("creditScore").item(0);
            creditScore = creditScoreNode.getTextContent();

            Node loanAmountNode = document.getElementsByTagName("loanAmount").item(0);
            loanAmount = loanAmountNode.getTextContent();

            Node loanDurationNode = document.getElementsByTagName("loanDuration").item(0);
            loanDuration = loanDurationNode.getTextContent();

            data = new Object[] {ssn, creditScore, loanAmount, loanDuration};

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return data;

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
            channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message.getBytes("UTF-8"));
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


}
