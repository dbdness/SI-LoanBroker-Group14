package com.group14;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Get_Banks_Queue";
    private static final String XML_BANK_TRANSLATOR_QUEUE = "Xml_Bank_Translator_Queue";
    private static final String JSON_BANK_TRANSLATOR_QUEUE = "Json_Bank_Translator_Queue";
    private static final String WSDL_BANK_TRANSLATOR_QUEUE = "Wsdl_Bank_Translator_Queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        List<String> messages = receiveMessage(channel);
        String loanRequest = messages.get(0); //First message receives LoanRequest
        String applicableBanksString = messages.get(1); //Second message receives appropriate banks.
        List<String> applicableBanks = splitString(applicableBanksString);

        for (String bank : applicableBanks) {
            String properBankQueue = "";
            Channel properChannel = channel;

            switch (bank) {
                case "CPHBusinessBankXML":
                    properBankQueue = XML_BANK_TRANSLATOR_QUEUE;
                    properChannel = connection.createChannel();
                    break;
                case "CPHBusinessBankJson":
                    properBankQueue = JSON_BANK_TRANSLATOR_QUEUE;
                    properChannel = connection.createChannel();
                    break;
                case "CPHBusinessBankWSDL":
                    properBankQueue = WSDL_BANK_TRANSLATOR_QUEUE;
                    properChannel = connection.createChannel();
                    break;
            }

            sendMessage(loanRequest.getBytes(), properChannel, properBankQueue);
        }
        channel.getConnection().close();


    }


    /**
     * Splits the specified raw Bank String into String objects.
     *
     * @param bankString Raw String of applicable banks.
     * @return List of Bank String objects.
     */
    private static List<String> splitString(String bankString) {
        bankString = bankString.replace("[", ""); //Trims first bracket.
        bankString = bankString.replace("]", ""); //Trims second bracket.
        return Arrays.asList(bankString.split(", ")); //Splits the entries at ',' and converts to List.
    }


    /**
     * Receives the incoming message from the previous queue.
     *
     * @param channel channel to consume messages from.
     * @return list of different Loan Responses in String format.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static List<String> receiveMessage(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, true, consumer);
        List<String> messages = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) { //The consumer should grab two messages.
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String response = new String(delivery.getBody());
                messages.add(response);
            }

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        return messages;

    }

    /**
     * Puts the desired message on a queue.
     *
     * @param message message to send.
     * @param channel channel to publish message.
     * @throws IOException
     * @throws TimeoutException if the channel takes too long to respond.
     */
    private static void sendMessage(byte[] message, Channel channel, String properQueue) throws IOException, TimeoutException {
        try {
            channel.queueDeclare(properQueue, false, false, false, null);
            channel.basicPublish("", properQueue, null, message);
            System.out.println("[x] sent '" + message + "'");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        channel.close();

    }


}
