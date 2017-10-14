package com.company;

import com.rabbitmq.client.*;
import ruleBase.RequestBankRules;
import ruleBase.RequestBankRulesService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Get_Credit_Score_Queue";
    private static final String PUBLISH_QUEUE_NAME = "Get_Banks_Queue";
    private static List<String> banks;

    // TODO
    // 1. Hent data fra queuen "Get_Credit_Score_Queue".
    // 2. Hent banke med getBanks(). Med data fra queuen.
    // 3. Send det der var på queuen i forvejen videre på queuen "Get_Banks_Queue".
    // 4. Send getBanks() resultat på quenen "Get_Banks_Queue".

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String xmlMessage = receiveMessage(channel);

//        banks = getBanks(700, 100000, "1976-01-23 01:00:00.0 CET");
//        System.out.println(banks);
    }

    private static java.util.List<java.lang.String> getBanks(int minCreditScore, double loanAmount, String customerLoanDuration) {
        RequestBankRulesService service = new RequestBankRulesService();
        RequestBankRules port = service.getRequestBankRulesPort();
        return port.getBanks(minCreditScore, loanAmount, customerLoanDuration);
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

    private static void receiveAndSendMessage() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for messages...");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("[x] received '" + message + "'");
                try {
                    sendMessage(message, channel);
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

            }
        };
        channel.basicConsume(CONSUME_QUEUE_NAME, true, consumer);
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


}
