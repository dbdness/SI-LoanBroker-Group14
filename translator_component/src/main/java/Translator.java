import com.rabbitmq.client.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Translator {

    private static final String JSON_EXCHANGE_NAME = "cphbusiness.bankJSON";
    private static final String XML_EXCHANGE_NAME = "cphbusiness.bankXML";
    private static final String BANK_HOST = "10.18.144.10";
    private static final String HOST_NAME = "207.154.228.245";
    private static final String CONSUME_QUEUE_NAME = "Recipient_List_Queue";
    private static final String PUBLISH_QUEUE_NAME = "Translator_Queue";

    public static void main(String[] args) throws  IOException, TimeoutException, Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(BANK_HOST);
        Connection bankConnection = factory.newConnection();
        factory.setHost(HOST_NAME);
        Connection queueConnection = factory.newConnection();
        Channel channel = queueConnection.createChannel();

        Channel bankPublishChannel = bankConnection.createChannel(); //Channel that sends our request to the bank.
        Channel bankConsumeChannel = bankConnection.createChannel(); //Channel that gets the response from the bank.

        String jsonRequest = "{\"ssn\":1605789787,\"creditScore\":598,\"loanAmount\":10.0,\"loanDuration\":360}";

        System.out.println(getBankJsonResponseAndForward(jsonRequest, bankPublishChannel, bankConsumeChannel));

        //String jsonString = "{\"ssn\":1605789787,\"creditScore\":598,\"loanAmount\":10.0,\"loanDuration\":360}";
        //System.out.println(jsonToXml(jsonString));

        //String xmlString = "<LoanResponse>    <interestRate>4.5600000000000005</interestRate>    <ssn>12345678</ssn> </LoanResponse>";
        //System.out.println(xmlToJson(xmlString));

    }

    private static String jsonToXml(String jsonToConvert) {
        String asXML = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonToConvert);
            asXML = XML.toString(jsonObject);
            StringBuilder formatted = new StringBuilder(asXML);
            formatted.insert(0, "<LoanRequest>");
            formatted.append("</LoanRequest>");
            asXML = formatted.toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
        }


        return asXML;
    }

    private static String xmlToJson(String xmlToConvert) {
        String asJson = "";
        try {
            JSONObject jsonObject = XML.toJSONObject(xmlToConvert);
            String jsonString = jsonObject.toString();

            int startIndex = jsonString.indexOf("{", 1);
            int endIndex = jsonString.lastIndexOf("}");

            asJson = jsonString.substring(startIndex, endIndex);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return asJson;


    }

    private static void receiveAndSendMessage() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, HOST_NAME, "");

        Consumer consumer = new DefaultConsumer(channel){
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
    }

    private static void sendMessage(String message, Channel channel) throws IOException, TimeoutException{
        try {
            channel.basicPublish(HOST_NAME, "", null, message.getBytes());
            System.out.println("[x] sent '" + message + "'");
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        channel.close();
        channel.getConnection().close();

    }

    private static String getBankXmlResponseAndForward(String xmlRequest, Channel publishChannel, Channel consumeChannel) throws Exception{
        publishChannel.exchangeDeclare(XML_EXCHANGE_NAME, "fanout");

        String consumeQueueName = consumeChannel.queueDeclare().getQueue();
        String replyKey = "xml";

        System.out.println("Waiting for response...");

        //Sending request and routing the request using a builder.
        AMQP.BasicProperties builder = new AMQP.BasicProperties
                .Builder()
                .contentType("text/plain")
                .deliveryMode(1)
                .replyTo(consumeQueueName)
                .build();

        //Publish and route message
        consumeChannel.basicPublish(XML_EXCHANGE_NAME, replyKey, builder, xmlRequest.getBytes());

        QueueingConsumer consumer = new QueueingConsumer(consumeChannel);

        consumeChannel.basicConsume(consumeQueueName, false, consumer);

        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        String response = new String(delivery.getBody());

        return response;

    }

    private static String getBankJsonResponseAndForward(String jsonRequest, Channel publishChannel, Channel consumeChannel) throws Exception{
        publishChannel.exchangeDeclare(JSON_EXCHANGE_NAME, "fanout");

        String consumeQueueName = consumeChannel.queueDeclare().getQueue();
        String replyKey = "json";

        System.out.println("Waiting for response...");

        //Sending request and routing the request via. a builder.
        AMQP.BasicProperties builder = new AMQP.BasicProperties
                .Builder()
                .contentType("text/plain")
                .deliveryMode(1)
                .replyTo(consumeQueueName)
                .build();

        //Publish and route message
        consumeChannel.basicPublish(JSON_EXCHANGE_NAME, replyKey, builder, jsonRequest.getBytes());

        QueueingConsumer consumer = new QueueingConsumer(consumeChannel);

        consumeChannel.basicConsume(consumeQueueName, false, consumer);

        QueueingConsumer.Delivery delivery = consumer.nextDelivery();
        String response = new String(delivery.getBody());

        return response;

    }
}
