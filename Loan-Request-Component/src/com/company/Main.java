package com.company;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {
    private static final String HOST_NAME = "207.154.228.245";
    private final static String PUBLISH_QUEUE_NAME = "Loan_Request_Queue";
    private static final String CONSUME_QUEUE_NAME = "Aggregator_Queue";

    public static void main(String[] args) throws IOException, TimeoutException, SAXException, ParseException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST_NAME);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        Loan loan = new Loan();

        Scanner reader = new Scanner(System.in);
        System.out.println("Enter your social security number in the format ******-****:");
        loan.setSSN(reader.next());

        System.out.println("Enter how much you want to loan:");
        loan.setLoanAmount(reader.nextDouble());

        System.out.println("Enter the loan's duration in days:");
        loan.setLoanDuration(reader.nextInt());

        reader.close();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = dateFormat.parse("1970-01-01");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, loan.getLoanDuration());
        String output = dateFormat.format(calendar.getTime());

        String finalLoanDuration = output + " 01:00:00.0 CET";

        // Runs the writeXML method and puts the loan request on a queue.
        byte[] xmlAsBytes = writeXML(loan.getSSN(), loan.getLoanAmount(), finalLoanDuration);
        sendMessage(xmlAsBytes, channel);

        //Receives Loan Response
        receiveMessage(channel);

        //Closes the open connection
        channel.close();
        channel.getConnection().close();


    }

    // The method that makes the XML file
    private static byte[] writeXML(String ssn, double loanAmount, String loanDuration) {
        byte[] xmlByteArray = null;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("LoanRequest");
            doc.appendChild(rootElement);

            Element ssnNode = doc.createElement("ssn");
            ssnNode.appendChild(doc.createTextNode(ssn));
            rootElement.appendChild(ssnNode);

            Element credScoreNode = doc.createElement("creditScore");
            rootElement.appendChild(credScoreNode);

            Element loanAmountNode = doc.createElement("loanAmount");
            loanAmountNode.appendChild(doc.createTextNode(String.valueOf(loanAmount)));
            rootElement.appendChild(loanAmountNode);

            Element loanDurationNode = doc.createElement("loanDuration");
            loanDurationNode.appendChild(doc.createTextNode(loanDuration));
            rootElement.appendChild(loanDurationNode);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);

            transformer.transform(source, result);

            xmlByteArray = bos.toByteArray();

            System.out.println("=== File converted to byte array ===");


        } catch (ParserConfigurationException | TransformerException ex) {
            ex.printStackTrace();
        }

        return xmlByteArray;

    }

    private static void receiveMessage(Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(CONSUME_QUEUE_NAME, false, false, false, null);
        System.out.println("[*] Waiting for loan response...");

        QueueingConsumer consumer = new QueueingConsumer(channel);

        channel.basicConsume(CONSUME_QUEUE_NAME, true, consumer);

        String response = "";
        try {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            response = new String(delivery.getBody());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.println("[*] Received loan response:");
        System.out.println(response);
    }

    // The messaging method, which in the moment sends a test string
    private static void sendMessage(byte[] message, Channel channel) throws IOException, TimeoutException {
        channel.queueDeclare(PUBLISH_QUEUE_NAME, false, false, false, null);
        channel.basicPublish("", PUBLISH_QUEUE_NAME, null, message);
        System.out.println(" [X] Sent '" + message + "'");

    }

}

