package com.group14;

import com.rabbitmq.client.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Json_Bank_Translator_Queue";
    private static final String BANK_HOST = "datdb.cphbusiness.dk";
    private static final String PUBLISH_EXCHANGE_NAME = "cphbusiness.bankJSON";
    private static final String REPLY_QUEUE_NAME = "Group14_Bank_Response_Queue";

    public static void main(String[] args) throws TimeoutException, IOException, Exception {
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

        String jsonRequest = xmlToJson(xmlRequest);
        System.out.println("Message successfully converted to JSON format.");
        getBankJsonResponseAndForward(jsonRequest, bankPublishChannel);

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

        System.out.println("[*] Consumed JSON message from queue: " + CONSUME_QUEUE_NAME);
        return response;

    }

    /**
     * Converts the specified XML String to JSON format.
     * @param xmlToConvert XML String to convert.
     * @return JSON String.
     */
    private static String xmlToJson(String xmlToConvert) {
        String asJson = "";
        try {
            JSONObject jsonObject = XML.toJSONObject(xmlToConvert);

            String jsonString = jsonObject.toString();

            //Cleaning the JSON String
            int startIndex = jsonString.indexOf("{", 1);
            int endIndex = jsonString.lastIndexOf("}");
            jsonString = jsonString.substring(startIndex, endIndex);

            //Changing data
            jsonObject = new JSONObject(jsonString);
            String loanDurationString = jsonObject.get("loanDuration").toString();
            long loanDurationDays = getAmountOfDays(loanDurationString);

            jsonObject.put("loanDuration", loanDurationDays);

            asJson = jsonObject.toString();


        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return asJson;


    }

    /**
     * Calculates amount of days in Integer format from a proper Loan Request Date String.
     * @param loanDuration Loan Request Date String.
     * @return amount of days in integer format.
     */
    private static long getAmountOfDays(String loanDuration) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long diff = 0;
        try {
            Date fromDate = dateFormat.parse("1970-01-01");
            Date toDate = dateFormat.parse(loanDuration);

            diff = toDate.getTime() - fromDate.getTime();
            diff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }


        return diff;

    }

    /**
     * Sends the Loan request to the JSON bank, and tells it to put the response on a reply-queue.
     * @param jsonRequest Loan request in JSON format to send to the bank.
     * @param channel channel to wire the message through.
     * @throws Exception
     */
    private static void getBankJsonResponseAndForward(String jsonRequest, Channel channel) throws Exception {
        channel.queueDeclare(REPLY_QUEUE_NAME, false, false, false, null);

        String replyKey = "json";

        System.out.println("Waiting for response...");

        //Sending request and routing the request using a builder.
        AMQP.BasicProperties builder = new AMQP.BasicProperties
                .Builder()
                .contentType("application/json")
                .deliveryMode(1)
                .replyTo(REPLY_QUEUE_NAME)
                .build();

        //Publish and route message
        channel.basicPublish(PUBLISH_EXCHANGE_NAME, replyKey, builder, jsonRequest.getBytes());

        channel.close();
        channel.getConnection().close();
        System.out.println("[x] forwarded response successfully!");

    }
}
