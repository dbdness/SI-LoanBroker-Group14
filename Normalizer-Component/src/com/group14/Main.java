package com.group14;

import com.rabbitmq.client.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String HOST_NAME = "207.154.228.245";
    private static final String PUBLISH_QUEUE_NAME = "Normalizer_Queue";
    private static final String CONSUME_QUEUE_NAME = "Group14_Bank_Response_Queue";
    private static final String BANK_HOST = "datdb.cphbusiness.dk";

    public static void main(String[] args) throws Exception {

        ConnectionFactory bankConnectionFactory = new ConnectionFactory();
        bankConnectionFactory.setHost(BANK_HOST);
        Connection bankConnection = bankConnectionFactory.newConnection();

        Channel bankConsumeChannel = bankConnection.createChannel();

        ConnectionFactory hostConnectionFactory = new ConnectionFactory();
        hostConnectionFactory.setHost(HOST_NAME);
        Connection hostConnection = hostConnectionFactory.newConnection();

        Channel hostPublishChannel = hostConnection.createChannel();

        List<String> loanResponses = receiveMessages(bankConsumeChannel);
        bankConsumeChannel.close();
        bankConsumeChannel.getConnection().close();

        for (String loan : loanResponses) {
            String identifier = identifyMessage(loan);
            switch (identifier) {
                case "JSON":
                    loan = jsonToXml(loan);
                    break;
                case "XML":
                    break;
                case "unknown":
                    System.out.println("[ ] Error - The incoming message format was not recognised.");
                    continue;
            }

            sendMessage(loan, hostPublishChannel);
        }

        hostPublishChannel.close();
        hostPublishChannel.getConnection().close();


    }

    /**
     * Identifies the format of the specified message.
     *
     * @param message to identify.
     * @return identification string.
     */
    private static String identifyMessage(String message) {
        String identifier = "";
        if (message.startsWith("{")) identifier = "JSON";
        else if (message.startsWith("<")) identifier = "XML";
        else identifier = "unknown";
        return identifier;

        //String loanResponse = "<LoanResponse><interestRate>4.5600000000000005</interestRate><ssn>12345678</ssn></LoanResponse>";
        //String loanResponse = "{\"interestRate\":5.5,\"ssn\":1605789787}";

    }

    private static String jsonToXml(String jsonToConvert) {
        String asXML = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonToConvert);
            asXML = XML.toString(jsonObject);
            StringBuilder formatted = new StringBuilder(asXML);
            formatted.insert(0, "<LoanResponse>");
            formatted.append("</LoanResponse>");
            asXML = formatted.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }


        return asXML;
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
        String response = "";
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

    private static void sendMessage(String message, Channel channel) throws IOException, TimeoutException {
        try {
            channel.queueDeclare(PUBLISH_QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message.getBytes());
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }


    }
}
