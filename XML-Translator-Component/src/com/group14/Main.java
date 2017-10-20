package com.group14;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Xml_Bank_Translator_Queue";
    private static final String BANK_HOST = "datdb.cphbusiness.dk";
    private static final String PUBLISH_EXCHANGE_NAME = "cphbusiness.bankXML";
    private static final String REPLY_QUEUE_NAME = "Group14_Bank_Response_Queue";

    public static void main(String[] args) throws Exception {

        ConnectionFactory bankConnectionFactory = new ConnectionFactory();
        bankConnectionFactory.setHost(BANK_HOST);
        Connection bankConnection = bankConnectionFactory.newConnection();

        Channel bankPublishChannel = bankConnection.createChannel();

        ConnectionFactory hostConnectionFactory = new ConnectionFactory();
        hostConnectionFactory.setHost(HOST_NAME);
        Connection hostConnection = hostConnectionFactory.newConnection();

        Channel hostConsumeChannel = hostConnection.createChannel();

        String xmlRequest = receiveMessage(hostConsumeChannel);
        hostConsumeChannel.close();
        hostConsumeChannel.getConnection().close();
        //String xmlRequest = "<LoanRequest>    <ssn>12345678</ssn>    <creditScore>685</creditScore>    <loanAmount>1000.0</loanAmount>    <loanDuration>1973-01-01 01:00:00.0 CET</loanDuration> </LoanRequest>";
        getBankXmlResponseAndForward(xmlRequest, bankPublishChannel);
    }

    private static String receiveMessage(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, false, consumer); //TODO change to true.

        String response = "";
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            response = new String(delivery.getBody());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.println("[*] Consumed message from queue: " + CONSUME_QUEUE_NAME);
        return response;

    }


    private static void getBankXmlResponseAndForward(String xmlRequest, Channel channel) throws Exception{
        channel.queueDeclare(REPLY_QUEUE_NAME, false, false, false, null);

        String replyKey = "xml";

        System.out.println("Waiting for response...");

        //Sending request and routing the request using a builder.
        AMQP.BasicProperties builder = new AMQP.BasicProperties
                .Builder()
                .contentType("application/xml")
                .deliveryMode(1)
                .replyTo(REPLY_QUEUE_NAME)
                .build();

        //Publish and route message
        channel.basicPublish(PUBLISH_EXCHANGE_NAME, replyKey, builder, xmlRequest.getBytes());

        channel.close();
        channel.getConnection().close();
        System.out.println("[x] forwarded response successfully!");

    }
}
