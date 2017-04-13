
import java.util.*;
import java.util.concurrent.TimeoutException;

import org.boon.json.*;

import io.netty.handler.codec.http.multipart.MixedAttribute;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import com.rabbitmq.client.*;
import java.io.IOException;
import org.json.*;
public class RequestParser implements Runnable {
    
    protected   ParseListener       _parseListener;
    protected   ClientHandle		_clientHandle;
    
    public RequestParser( ParseListener parseListener , ClientHandle clientHandle ){
        _parseListener  =	parseListener;
        _clientHandle	=	clientHandle;
    }
    
    public void run( ){
        try{
			HttpRequest request = _clientHandle.getRequest();
			HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
			String data = decoder.getBodyHttpDatas().toString();
			JSONObject json1 = new JSONObject(data);
			JSONObject json ;
			String action = json.getString("TargetMethod");
			String sessionID = json.getString("SessionID");
			String fname = json.getString("fname");
			String lname = json.getString("lname");
			String email = json.getString("email");
			String nationalID = json.getString("nationalID");
			int balance = json.getInt("balance");
			Map<String,Object> map = new HashMap<String,Object>() ;
			map.put("firstName", fname);
			map.put("lastName", lname);
			map.put("email", email);
			map.put("nationalID", nationalID);
			map.put("Balance", balance);
			ClientRequest req = new ClientRequest(action,sessionID, map);
        }
        catch( Exception exp ){
            _parseListener.parsingFailed( _clientHandle, "Exception while parsing JSON object " + exp.toString( ) );
        }
    }
    public class ReceiveLogs {
    	  private static final String EXCHANGE_NAME = "Requests";

    	  public  JSONObject Recieve(String[] argv) throws Exception {
    	    ConnectionFactory factory = new ConnectionFactory();
    	    JSONObject json = null;
    	    factory.setHost("localhost");
    	    Connection connection = factory.newConnection();
    	    Channel channel = connection.createChannel();

    	    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
    	    String queueName = "usersAppRequests";
    	    channel.queueBind(queueName, EXCHANGE_NAME, "");

    	    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    	    Consumer consumer = new DefaultConsumer(channel) {
    	      @Override
    	      public void handleDelivery(String consumerTag, Envelope envelope,
    	                                 AMQP.BasicProperties properties, byte[] body) throws IOException {
    	        String message = new String(body, "UTF-8");
    	       // json= new JSONObject(message);
    	        System.out.println(" [x] Received '" + json.toString() + "'");
    	      }
    	    };
    	    channel.basicConsume(queueName, true, consumer);
    	    return json;
    	  }
    	  
    	}
      
}