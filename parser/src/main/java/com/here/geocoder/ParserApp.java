package com.here.geocoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * Hello world!
 *
 */
public class ParserApp 
{
    static String filePath;
    private static String QUEUE_NAME="com_here_geocoder";
    private static AmazonSQS sqs;
    private static int counter=0;
    public static void main( String[] args )
    {
        if(args.length == 0){
            System.out.println("Input file required");
            System.exit(0);
        }
            
        filePath = args[0];
        if(args.length > 1)
            QUEUE_NAME = args[1];
        
        sqs = AmazonSQSClientBuilder.defaultClient();
        initSQSIfNeeded();
        process();
    }

    private static void initSQSIfNeeded(){ 
        CreateQueueRequest create_request = new CreateQueueRequest(QUEUE_NAME)
        .addAttributesEntry("DelaySeconds", "60")
        .addAttributesEntry("MessageRetentionPeriod", "86400");

        try {
            sqs.createQueue(create_request);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }
    }

    private static void process(){
        String line = "";
        String cvsSplitBy = ";";

        try{
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                SendMessageRequest send_msg_request = new SendMessageRequest()
                    .withQueueUrl(QUEUE_NAME)
                    .withMessageBody(line)
                    .withDelaySeconds(5);
                sqs.sendMessage(send_msg_request);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                counter++;
            }
            System.out.println(counter+" items queued for geocoding");
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
