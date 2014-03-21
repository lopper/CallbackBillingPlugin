package com.phonegap.plugin.billing.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.NameValuePair;
import org.apache.http.util.ByteArrayBuffer;

public class WebHelper {

	public static String Get(String url, NameValuePair[] parameters) throws IOException
	{
    	      
		if(!url.contains("?"))
        	url += "?";
		else if(!url.endsWith("&"))
			url += "&";
		
		if(parameters != null)
		{
			for(int i = 0 ; i < parameters.length; i++)
				url += parameters[i].getName() + "=" + URLEncoder.encode(parameters[i].getValue()) + "&";
		}
		
    	//http://www.androidsnippets.com/non-blocking-web-request
        URL updateURL = new URL(url); 
        URLConnection conn = updateURL.openConnection();
        
        InputStream is = conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayBuffer baf = new ByteArrayBuffer(50);

        int current = 0;
        while((current = bis.read()) != -1){
            baf.append((byte)current);
        }

        /* Convert the Bytes read to a String. */
        return new String(baf.toByteArray());

	}
	
	public static String Post(String postUrl, NameValuePair[] parameters) throws IOException
	{
		 HttpURLConnection connection;
		 OutputStreamWriter request = null;

        URL url = null;   
        String response = null;         


        url = new URL(postUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestMethod("POST");    

        String queryParams = "";
        
        if(parameters != null)
        {
        	for(int i = 0 ; i < parameters.length; i++)
        		queryParams += parameters[i].getName() + "=" + URLEncoder.encode(parameters[i].getValue()) + "&";
        }
        request = new OutputStreamWriter(connection.getOutputStream());
        request.write(queryParams);
        request.flush();
        request.close();            
        String line = "";               
        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null)
        {
            sb.append(line + "\n");
        }
        // Response from server after login process will be stored in response variable.                
        response = sb.toString();
        // You can perform UI operations here     
        isr.close();
        reader.close();
        
        return response;

	}
}

