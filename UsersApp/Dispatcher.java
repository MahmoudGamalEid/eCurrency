import java.io.*;
import java.sql.*;
import java.util.*;
import java.math.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import com.zaxxer.hikari.HikariDataSource;


public class Dispatcher{

    protected Hashtable         _htblCommands;
    protected ExecutorService   _threadPoolCmds;
    protected HikariDataSource  _hikariDataSource;
	
	public Dispatcher( ){
	}

	abstract class Command {
    
        protected HikariDataSource      _hikariDataSource;
        protected ClientHandle			_clientHandle;
		protected ClientRequest 		_clientRequest;
			
		protected ArrayList<String>	_arrColsToKeep;
			
		public void init( HikariDataSource	hikariDataSource,   	
						  ClientHandle 		clientHandle, 
						  ClientRequest 	clientRequest ){
			_hikariDataSource   =   hikariDataSource;
			_clientRequest		=	clientRequest;
			_clientHandle		=	clientHandle;
		}
		
        public void run( ){
			Connection	connection = null;
            try{
			    Map<String, Object>     map;
				StringBuffer            strbufResponse;
                connection	    = _hikariDataSource.getConnection( );
				map			    = _clientRequest.getData( );
				MongoCollection<Document> collection = connectMongo("mahmoud","rezingard1731");
				strbufResponse  = execute( connection, map ,collection );
                if( strbufResponse != null )
                    _clientHandle.passResponsetoClient( strbufResponse );
                else
                    _clientHandle.terminateClientRequest( );
            }
            catch( Exception exp ){
                System.err.println( exp.toString( ) );
                _clientHandle.terminateClientRequest( );
            }
			finally{
				closeConnectionQuietly( connection );
			}
        }
		
		protected void closeConnectionQuietly( Connection	connection ){
			try{
				if( connection != null )
					connection.close( );
			}
			catch( Exception exp ){
				// log this...
				exp.printStackTrace( );
			}
		}
		
		 protected StringBuffer makeJSONResponseEnvelope( int nResponse, StringBuffer strbufRequestData, StringBuffer strbufResponseData  ){
            StringBuffer    		strbufJSON;
			String					strStatusMsg;
			String					strData = "";
			Map<String, Object>		mapInputData;
			String 					strKey;
             
            strbufJSON    	= new StringBuffer( );
			strbufJSON.append( "{" );
			strbufJSON.append( "\"responseTo\":\"" +  _clientRequest.getAction( ) + "\","  );
            if( _clientRequest.getSessionID( ) != null )
                strbufJSON.append( "\"sessionID\":\""  +  _clientRequest.getSessionID( ) + "\","  );
            
			strbufJSON.append( "\"StatusID\":\""     +  nResponse + "\","  );
		   	strStatusMsg  = (String)ResponseCodes.getMessage( Integer.toString( nResponse ) );
			strbufJSON.append( "\"StatusMsg\":\""     +  strStatusMsg + "\","  );
		   
            if( strbufRequestData != null )
                strbufJSON.append( "\"requestData\":{" +  strbufRequestData + "},"   );		
                
			if( strbufResponseData != null ){
                if( strbufResponseData.charAt( 0 ) == '[' )
                    strbufJSON.append( "\"responseData\":" +  strbufResponseData  );	// if it is a list, no curley
                else
                    strbufJSON.append( "\"responseData\":{" +  strbufResponseData  + "}"  );	
            }
			if( strbufJSON.charAt( strbufJSON.length( ) - 1 ) == ',' )
				strbufJSON.deleteCharAt( strbufJSON.length( ) - 1 );
				
            strbufJSON.append( "}" );
            return strbufJSON;
        }
        
		protected StringBuffer serializeRequestDatatoJSON( ArrayList arrFieldstoKeep  )  throws Exception {
		  
			return serializeMaptoJSON( _clientRequest.getData( ) , arrFieldstoKeep ) ;
		}
        
		protected StringBuffer serializeResultSettoJSON( ResultSet resultSet, ArrayList arrColstoKeep, int nMaxSize  )  throws Exception {
            
            StringBuffer    strbufJSON;
            int             nCount;
            boolean         bKeepColumn;
             
            strbufJSON    	= new StringBuffer( );
            ResultSetMetaData rsmd 	= resultSet.getMetaData( );
            strbufJSON.append( "[ " );
            nCount = 0;

            while( resultSet.next( ) && ((nCount < nMaxSize) || (nMaxSize == 0) ) ) {
                int nColumns = rsmd.getColumnCount( );
                strbufJSON.append( "{" );
                nCount++;
                for (int nIndex=1; nIndex<nColumns+1; nIndex++) {
                    String strColumnName = rsmd.getColumnName( nIndex );
                    bKeepColumn = false;
                    if( arrColstoKeep == null )
                        bKeepColumn = true;
                    else
                        if(  arrColstoKeep.contains( strColumnName ) ) 
                            bKeepColumn = true;
                    
                    if( bKeepColumn ){
                        strbufJSON.append( "\"" +  strColumnName + "\": " );
                    
                        if(rsmd.getColumnType(nIndex)==java.sql.Types.BIGINT)
                            strbufJSON.append( "\"" +  resultSet.getInt(nIndex) + "\""  ); 
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.BOOLEAN)
                            strbufJSON.append( "\"" +  resultSet.getBoolean(nIndex) + "\""  ); 
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.BLOB)
                            strbufJSON.append( "\"" +  resultSet.getBlob(nIndex) + "\""  ); 
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.DOUBLE)
                            strbufJSON.append( "\"" +  resultSet.getDouble(nIndex) + "\""  );
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.FLOAT)
                           strbufJSON.append( "\"" +  resultSet.getFloat(nIndex) + "\""  );
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.INTEGER)
                           strbufJSON.append( "\"" +  resultSet.getInt(nIndex) + "\""  ); 
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.NVARCHAR)
                            strbufJSON.append( "\"" +  resultSet.getNString(nIndex) + "\""  ); 
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.VARCHAR)
                            strbufJSON.append( "\"" +  resultSet.getString(nIndex) + "\""  );
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.TINYINT)
                            strbufJSON.append( "\"" +  resultSet.getInt(nIndex) + "\""  );               
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.SMALLINT)
                            strbufJSON.append( "\"" +  resultSet.getInt(nIndex) + "\""  );
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.DATE)
                            strbufJSON.append( "\"" +  resultSet.getDate(nIndex) + "\""  );
                        else if(rsmd.getColumnType(nIndex)==java.sql.Types.TIMESTAMP)
                            strbufJSON.append( "\"" +  resultSet.getTimestamp(nIndex) + "\""  );
                        else
                         strbufJSON.append( "\"" +  resultSet.getObject(nIndex) + "\""  );
                        
                        strbufJSON.append( "," );
                    }
                }
                if( strbufJSON.charAt( strbufJSON.length( ) - 1  ) == ',' )
                        strbufJSON.setLength( strbufJSON.length( ) - 1 ); 
                strbufJSON.append( "}," ); 
            }
           
			if( strbufJSON.charAt( strbufJSON.length( ) - 1  ) == ',' )
				strbufJSON.setLength( strbufJSON.length( ) - 1 );
				
            strbufJSON.append( "]" );
            
            return strbufJSON;
        }
		
		protected StringBuffer	serializeMaptoJSON( Map<String, Object>  map, ArrayList arrFieldstoKeep ){
			
			StringBuffer	strbufData;
			
			strbufData		=	new StringBuffer( );
			if( arrFieldstoKeep == null ){
				for( Map.Entry<String, Object> entry : map.entrySet())
					strbufData.append(  "\"" + entry.getKey() + "\":\"" + entry.getValue().toString( )  + "\"," );
			}
			else{
				for( Map.Entry<String, Object> entry : map.entrySet()){
					if( arrFieldstoKeep.contains( entry.getKey( ) ) )
						strbufData.append(  "\"" + entry.getKey() + "\":\"" + entry.getValue().toString( )  + "\"," );
				}
			}
			
			if( strbufData.charAt( strbufData.length( ) - 1  ) == ',' )
					strbufData.setLength( strbufData.length( ) - 1 );
					
			return strbufData;
		}
        

        public abstract StringBuffer execute(   Connection connection,  
                                                Map<String, Object> mapUserData ,MongoCollection<Document> collection ) throws Exception;
    }
	  
    class AddUserSimpleCmd extends Command implements Runnable {
        
        public StringBuffer execute(Connection connection,  Map<String, Object> mapUserData ,MongoCollection<Document> collection ) throws Exception {
            
			StringBuffer        strbufResult;
            CallableStatement   sqlProc;
			String              strEmail,
                                strPassword,
                                strFirstName,
                                strLastName;
			BigDecimal 			balance;
			long 				nationalID;
            strEmail    =   (String)mapUserData.get( "email" );
            strPassword =   (String)mapUserData.get( "password" );
            strFirstName=   (String)mapUserData.get( "firstName" ); 
            strLastName =   (String)mapUserData.get( "lastName" );
            nationalID = 	(long)mapUserData.get("nationalID");
            balance =       new BigDecimal((int)mapUserData.get("balance"));
            
            if( strEmail == null || strEmail.trim( ).length( ) == 0 || 
                strPassword == null || strPassword.trim( ).length( ) == 0  || 
                strFirstName == null || strFirstName.trim( ).length( ) == 0  ||
                strLastName == null || strLastName.trim( ).length( ) == 0)
               return null;

            if( !EmailVerifier.verify( strEmail ) )
                return null;

            sqlProc = connection.prepareCall("{?=call addusersimple(?,?,?)}");
            sqlProc.registerOutParameter(1, Types.INTEGER );
            sqlProc.setBigDecimal(2, balance);
            sqlProc.setString(3, strPassword );	
            sqlProc.setString(4, strEmail);
            sqlProc.execute( );
            int ID = sqlProc.getInt(1);
			strbufResult = makeJSONResponseEnvelope( ID , null, null );
            sqlProc.close( );
            Document doc = new Document("fname",strFirstName).append("lname",strLastName).append("email", strEmail)
    				.append("nationalID", nationalID).append("ID",ID);
            collection.insertOne(doc);
            return strbufResult;
        }
    }
	class AttemptLoginCmd extends Command implements Runnable {
        
        public StringBuffer execute(Connection connection,  Map<String, Object> mapUserData,MongoCollection<Document> collection) throws Exception {
            CallableStatement   sqlProc;
			StringBuffer		strbufResult = null,
								strbufResponseJSON;
            String	            strSessionID,
                                strEmail,
								strPassword,
                                strFirstName,
								strClientIP;
			int		            nSQLResult;
			
			strEmail    =	((String)mapUserData.get( "email" ));
			strPassword =	((String)mapUserData.get( "password" ));
			System.out.println(strEmail + strPassword);
            if( strEmail == null || strEmail.trim( ).length( ) == 0 ||
                strPassword == null || strPassword.trim( ).length( ) == 0 )
                return null;
            
            if( !EmailVerifier.verify( strEmail ) ) {
            	System.err.println("Not an actual email");
                return null;}
            strClientIP		=	_clientHandle.getClientIP( );
			strSessionID 	=	UUID.randomUUID( ).toString( );
            System.out.println("About to call SQL now");
            sqlProc = connection.prepareCall("{?=call attemptuserLogin(?,?,?,?)}");
            sqlProc.registerOutParameter( 1, Types.INTEGER );
            sqlProc.setString(2, strEmail  );		
            sqlProc.setString(3, strPassword  );	
            sqlProc.setString(4, strSessionID  );	
			sqlProc.setString(5, strClientIP );
            sqlProc.execute( );
			nSQLResult = sqlProc.getInt( 1 );
            sqlProc.close( );
			if( nSQLResult >= 0 ){
				//Cache.addSession( strSessionID, strEmail );
				//System.err.println(" adding following session to Cache " + strSessionID );
				Map<String, Object> mapResult = new HashMap<String, Object>( );
				mapResult.put( "userID", Integer.toString( nSQLResult)  );
				mapResult.put( "sessionID", strSessionID );
               /* sqlProc = connection.prepareCall( "{?=call getUserFirstName(?)}" );
                sqlProc.registerOutParameter( 1, Types.VARCHAR );
                sqlProc.setInt( 2, nSQLResult  );	
                sqlProc.execute( );
                strFirstName = sqlProc.getString( 1 );
                sqlProc.close( );
                mapResult.put( "firstName", strFirstName );*/
				strbufResponseJSON	=	serializeMaptoJSON( mapResult, null );
				strbufResult = makeJSONResponseEnvelope( 0, null, strbufResponseJSON  );
			}
			else
				strbufResult = makeJSONResponseEnvelope( nSQLResult , null, null );
            
			
            return strbufResult;
        }
    }
	class LogoutCmd extends Command implements Runnable {
        
        public StringBuffer execute(Connection connection,  Map<String, Object> mapUserData,MongoCollection<Document> collection) throws Exception {
            
            CallableStatement   sqlProc;
			StringBuffer		strbufResult = null;
            String	            sessionID;
            sessionID = 		_clientRequest.getSessionID();
			int		            nSQLResult;

            if( sessionID == null || sessionID.trim( ).length( ) == 0  )
                return null;

            sqlProc = connection.prepareCall("{?=call userlogout(?)}");
            sqlProc.registerOutParameter( 1, Types.INTEGER );
            sqlProc.setString(2, sessionID  );		
            sqlProc.execute( );
			nSQLResult = sqlProc.getInt( 1 );
            sqlProc.close( );
            strbufResult = makeJSONResponseEnvelope( nSQLResult , null, null );
            return strbufResult;
        }
    }
	class getBalanceCmd extends Command implements Runnable {
        
        public StringBuffer execute(Connection connection,  Map<String, Object> mapUserData,MongoCollection<Document> collection) throws Exception {
            
            CallableStatement   sqlProc;
			StringBuffer		strbufResult = null;
            String	            sessionID;
			int		            nSQLResult;
			
			sessionID    =	_clientRequest.getSessionID();
            if( sessionID == null || sessionID.trim( ).length( ) == 0  )
                return null;

            sqlProc = connection.prepareCall("{?=call getUserBalance(?)}");
            sqlProc.registerOutParameter( 1, Types.INTEGER );
            sqlProc.setString(2, sessionID  );		
            sqlProc.execute( );
			nSQLResult = sqlProc.getInt(1);
            sqlProc.close( );
            strbufResult = makeJSONResponseEnvelope( nSQLResult , null, null );
            return strbufResult;
        }
    } 
	protected void dispatchRequest( ClientHandle  clientHandle, 
									ClientRequest clientRequest ) throws Exception{
		Command				cmd;
		String				strAction;
		strAction				 = clientRequest.getAction( );
		Class<?> innerClass		 = (Class<?>)_htblCommands.get( strAction );
		Class<?> enclosingClass  = Class.forName( "Dispatcher" );
        Object enclosingInstance = enclosingClass.newInstance( );
		Constructor<?> ctor 	 = innerClass.getDeclaredConstructor( enclosingClass );
        cmd = (Command) ctor.newInstance( enclosingInstance );
		cmd.init( _hikariDataSource, clientHandle, clientRequest );
		_threadPoolCmds.execute( (Runnable) cmd );
        
    }
      
	protected void loadHikari( String strAddress, int nPort, 
                                String strDBName, 
                                String strUserName, String strPassword   ){
		_hikariDataSource 	= new HikariDataSource(  );
		_hikariDataSource.setJdbcUrl( "jdbc:postgresql://" + strAddress + ":" + nPort + "/" + strDBName);
		_hikariDataSource.setUsername( strUserName );
		_hikariDataSource.setPassword( strPassword );
	}
       
	protected void loadCommands( ) throws Exception{
        _htblCommands   = new Hashtable( );
        Properties prop = new Properties();
        InputStream in  = getClass().getResourceAsStream("config/commands.properties");
        prop.load( in );
        in.close( );
        Enumeration enumKeys = prop.propertyNames( );
        String  strActionName,
                strClassName;
        
        while( enumKeys.hasMoreElements( ) ){
            strActionName = (String)enumKeys.nextElement(  );
            strClassName  = (String)prop.get( strActionName );            
            Class<?> innerClass = Class.forName("Dispatcher$" + strClassName);
            _htblCommands.put( strActionName, innerClass );
        }
	}
    
    protected void loadThreadPool( ){
        _threadPoolCmds = Executors.newFixedThreadPool( 20 );
    }
    public static MongoCollection<Document> connectMongo(String myUsername , String myPassword) {
		MongoCollection<Document> collection = null ;
		try {
	         // To connect to mongodb server
			 MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
	         // Now connect to your databases
	         MongoDatabase db = mongoClient.getDatabase( "eCurrency" );
	         collection = db.getCollection("users");
	         System.out.println("Connect to database successfully");
	      }catch(Exception e){
	         System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      }
		 return collection;
	}
    public void init( ) throws Exception{
        loadHikari( "localhost", 5432, "ecurrency", "mahmoud", "rezingard1731");
        loadThreadPool( );
        loadCommands( );
        
	}  
    
}