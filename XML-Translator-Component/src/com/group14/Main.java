package com.group14;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Xml_Bank_Translator_Queue";
    private static final String BANK_HOST = "10.18.144.10";
    private static final String PUBLISH_EXCHANGE_NAME = "cphbusiness.bankXML";
    private static final String REPLY_QUEUE_NAME = "Bank_Response_Queue";

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(BANK_HOST);
        Connection bankConnection = factory.newConnection();
        factory.setHost(HOST_NAME);
        Connection replyQueueConnection = factory.newConnection();
        Channel channel = replyQueueConnection.createChannel();

        Channel bankPublishChannel = bankConnection.createChannel(); //Channel that sends our request to the bank.
        Channel bankConsumeChannel = bankConnection.createChannel(); //Channel that gets the response from the bank.

        //String xmlRequest = receiveMessage(channel);
        String xmlRequest = "<LoanRequest>    <ssn>12345678</ssn>    <creditScore>685</creditScore>    <loanAmount>1000.0</loanAmount>    <loanDuration>1973-01-01 01:00:00.0 CET</loanDuration> </LoanRequest>";
        getBankXmlResponseAndForward(xmlRequest, bankPublishChannel, bankConsumeChannel);


    }

    private static String receiveMessage(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, false, consumer);

        String response = "";
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            response = new String(delivery.getBody());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.println("[*] Consumed message from queue");
        return response;

    }

    private static String getBankXmlResponseAndForward(String xmlRequest, Channel publishChannel, Channel consumeChannel) throws Exception{
        publishChannel.exchangeDeclare(PUBLISH_EXCHANGE_NAME, "fanout");

        String replyQueueName = REPLY_QUEUE_NAME;
        String replyKey = "xml";

        System.out.println("Waiting for response from bank...");

        //Sending request and routing the request using a builder.
        AMQP.BasicProperties builder = new AMQP.BasicProperties
                .Builder()
                .contentType("text/plain")
                .deliveryMode(1)
                .replyTo(replyQueueName)
                .build();

        //Publish and route message
        consumeChannel.basicPublish(PUBLISH_EXCHANGE_NAME, replyKey, builder, xmlRequest.getBytes());

        //QueueingConsumer consumer = new QueueingConsumer(consumeChannel);

        //consumeChannel.basicConsume(replyQueueName, false, consumer);

//        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
//        String response = new String(delivery.getBody());
//
//        System.out.println("[x] Forwarded following response from bank:");
//        return response;

        return "[x] Forwarded bank response!";
    }
}
