package com.group14;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import sun.reflect.annotation.ExceptionProxy;

import java.io.IOException;
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

        String loanResponse = receiveMessage(bankConsumeChannel);
        for(int i=0; i<10; i++){
            System.out.println(loanResponse);
        }

        bankConsumeChannel.close();
        bankConsumeChannel.getConnection().close();


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

        return response;

    }

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
}
