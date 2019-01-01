package com.here.geocoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/**
 * Hello world!
 *
 */
public class App 
{
    private static String appCode=null;
    private static String appId=null;
    private static String baseUrl="https://geocoder.api.here.com/6.2/geocode.json";
    private static String QUEUE_NAME="com_here_geocoder";
    private static String OUTPUT_FILE_NAME=null;
    private static int ADDRESS_COLUMN=0;
    private static String SPLIT_BY=null;

    private static AmazonSQS sqs;
    private static int counter=0;

    private static PrintWriter outputPrintWriter=null;
    private static FileWriter outpuFileWriter=null;
    private static BufferedWriter outpuBufferedWriter=null;

    public static void main( String[] args )
    {   
        if(args.length == 0){
            System.out.println("Output file path required.");
            System.exit(0);
        }

        appId = System.getenv("HERE_APP_ID");
        appCode = System.getenv("HERE_APP_CODE");

        if(appId == null || appCode == null)
            System.exit(0);

        OUTPUT_FILE_NAME = args[0];
        SPLIT_BY = ";";
        ADDRESS_COLUMN = Integer.parseInt(args[1]);

        sqs = AmazonSQSClientBuilder.defaultClient();

        while(true){
            List<Message> messages = sqs.receiveMessage(QUEUE_NAME).getMessages();
            for(Message m:messages){
                String body= m.getBody();
                String[] itemArray = body.split(SPLIT_BY);
                String addressS = itemArray[ADDRESS_COLUMN];
                String[] ba = addressS.split(",");
                String address=null;
                if(ba.length > 1){
                    address = ba[1];
                } else{
                    address = addressS;
                }
                if(address == null)
                    continue;

                float[] coords = geoCode(address.replace(" ", "%20"));
                if(coords != null)
                    writeToFile(m.getBody(), coords[0], coords[1]);
                sqs.deleteMessage(QUEUE_NAME, m.getReceiptHandle());
                counter++;
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println(counter+" items geocoded");
            }
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //https://geocoder.api.here.com/6.2/geocode.json?searchtext=200%20S%20Mathilda%20Sunnyvale%20CA&app_id=devportal-demo-20180625&app_code=9v2BkviRwi9Ot26kp2IysQ&gen=9
        
    }

    private static float[] geoCode(String address){
        float[] coords = new float[2];
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String url = String.format("%s?searchtext=%s&app_id=%s&app_code=%s",baseUrl,address,appId,appCode);
        //System.out.println("URL:"+url);
        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse response=null;
        
        try {
            response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
            }
            //System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            
            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

            String output;
            StringBuffer buf = new StringBuffer();
		    //System.out.println("Output from Server .... \n");
		    while ((output = br.readLine()) != null) {
                buf.append(output);
			    //System.out.println(output);
            }
            
            JSONObject obj= new JSONObject(buf.toString());
            JSONArray viewArr = obj.getJSONObject("Response").getJSONArray("View");
            JSONObject view = viewArr.getJSONObject(0);
            JSONArray resultArray = view.getJSONArray("Result");
            JSONObject result = resultArray.getJSONObject(0);
            JSONObject location = result.getJSONObject("Location");
            JSONArray navigationPosition = location.getJSONArray("NavigationPosition");
            JSONObject coordinates = navigationPosition.getJSONObject(0);
            float lat = coordinates.getFloat("Latitude");
            float lng = coordinates.getFloat("Longitude");
            coords[0] = lat;
            coords[1] = lng;
            EntityUtils.consume(entity);
            //System.out.println("Lat:"+lat+",Long:"+lng);
            return coords;
        } catch(Exception ex){
            ex.printStackTrace();
            return null;
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    private static void writeToFile(String line,float lat,float lng){
        try {
            outpuFileWriter = new FileWriter(OUTPUT_FILE_NAME, true);
            outpuBufferedWriter = new BufferedWriter(outpuFileWriter);
            outputPrintWriter = new PrintWriter(outpuBufferedWriter);

        } catch (IOException e) {
            System.out.println("ERROR: Couldn't open output file");
            return;
        }

        System.out.println(line+";"+String.format("%.2f",lat)+";"+String.format("%.2f",lng));
        outputPrintWriter.println(line+";"+String.format("%.2f",lat)+";"+String.format("%.2f",lng));

        if(outputPrintWriter != null)
            outputPrintWriter.close();

        try {
            if(outpuBufferedWriter != null)
                outpuBufferedWriter.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

        try {
            if(outpuFileWriter != null)
                outpuFileWriter.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
