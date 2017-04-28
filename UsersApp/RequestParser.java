import java.util.*;
import java.util.concurrent.TimeoutException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import java.io.IOException;
import org.json.*;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.*;

public class RequestParser implements Runnable {
	private static final String QUEUE_NAME = "UsersAppRequests";
    protected   ParseListener       _parseListener;
    protected   ClientHandle		_clientHandle;
    
    public RequestParser( ParseListener parseListener , ClientHandle clientHandle ){
        _parseListener  =	parseListener;
        _clientHandle	=	clientHandle;
    }
    public static void Receive() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        final JSONObject json;
        channel.queueDeclare(QUEUE_NAME, false,false,false,null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        Consumer consumer = new DefaultConsumer(channel) {
          @Override
          public void handleDelivery(String consumerTag, Envelope envelope,
                                     AMQP.BasicProperties properties, byte[] body) throws IOException {
            String message = new String(body, "UTF-8");
            
            
            System.out.println(" [x] Received '" + message + "'");
          }
        };
        
        channel.basicConsume(QUEUE_NAME, true, consumer);
        
      }
    
    public void run( ){
    	try{
    		HttpRequest request = _clientHandle.getRequest();
    		HttpPostRequestDecoder postDecoder;

		if (request.method().compareTo(HttpMethod.POST) == 0) {
			postDecoder = new HttpPostRequestDecoder(request);
			List<InterfaceHttpData> lst = postDecoder.getBodyHttpDatas();
			System.out.println(lst.toString());
			// clientHandle
			
			String str = lst.get(0).toString();
			
			String jsonStr = str.substring(str.indexOf('{'));
			JSONObject json1 = new JSONObject(jsonStr);
			JSONObject json = (JSONObject) json1.get("data");
			String action = json1.getString("action");
			Map<String,Object> mapUserData = new HashMap<>();
			String targetapp = json1.getString("targetapp");
			System.out.println(json);
			String sessionID;
			ClientRequest cr=null;
			if(action.equalsIgnoreCase(("adduser"))) {
				mapUserData.put("email", json.get("email"));
				mapUserData.put("password", json.get("password"));
				mapUserData.put("firstName", json.get("firstname"));
				mapUserData.put("lastName", json.get("lastname"));
				mapUserData.put("nationalID", json.get("nationalID"));
				mapUserData.put("balance", json.get("balance"));
				 cr = new ClientRequest(action, null, mapUserData);
				 _parseListener.parsingFinished(_clientHandle, cr);
			};
			if(action.equalsIgnoreCase(("attemptlogin"))) {
				mapUserData.put("email", json.get("email"));
				mapUserData.put("password", json.get("password"));
				cr = new ClientRequest(action, null, mapUserData);
				_parseListener.parsingFinished(_clientHandle, cr);
			};
			if(action.equalsIgnoreCase("attemptlogout")) {
				sessionID =json.get("sessionID").toString();
				cr = new ClientRequest(action,sessionID , mapUserData);
				System.out.println(sessionID);
				_parseListener.parsingFinished(_clientHandle, cr);
			}
			if(action.equalsIgnoreCase("getuserbalance")) {
				sessionID = json.get("sessionID").toString();
				cr = new ClientRequest(action, sessionID,mapUserData);
				_parseListener.parsingFinished(_clientHandle, cr);
			}
			
		} else {
			System.out.println("The request is not a POST request");
		}
    	}
        catch( Exception exp ){
            _parseListener.parsingFailed( _clientHandle, "Exception while parsing JSON object " + exp.toString( ) );
        }
    	
    }    	  
}

      
