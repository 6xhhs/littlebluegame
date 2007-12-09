/*
 * ConnectionManager.java
 *
 * Created on 15 June 2006, 21:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.Image;
import java.util.*;

/**
 * Connection Manager is the class responsible for uploaded the players highscore etc
 * @author Josh
 */
public class ConnectionManager implements Runnable {
    
    // response identifiers (i.e. used to identify the next packet/content ready for downloading)
    private final int SERVER_ID_POSITION = 0;
    
    private final int SERVER_ID_NO_AD = 0;
    private final int SERVER_ID_HAS_CLIENT_NAME = 1;
    private final int SERVER_ID_HAS_CLIENT_NAME_IMAGE = 2;
    
    // messages that are used to send to the CallBack object (notification messages for the user)
    public static final int RES_CONNECTING       = 0;
    public static final int RES_SUCCESS          = 1;
    public static final int RES_SUCCESS_CXN      = 2;      // connecting to the server
    
    public static final int RES_FAIL_USERREQ = 11;     // user details (username and password) required
    public static final int RES_FAIL_USERREG = 12;     // user is not registered with the site
    public static final int RES_FAIL_CXN = 13;         // connection failure
    public static final int RES_FAIL_NOSCORE = 14;     // no score can be found
    
    private static final String URL = "http://localhost:8084/bemobile/HighScores";
    
    private String _responseMsg = null;
    private int _response = 0;
    private int _position = 0;
    
    private String _username = null;
    private String _password = null;
    private boolean _newUser = false;
    
    private ICallBack _listener = null;
    private Thread _thread = null;
    
    private HttpConnection _conn = null;
    private ByteArrayOutputStream _baos = null;
    private DataInputStream _dis = null;
    private OutputStream _os = null;
    
    private Byte[] response = null;        
    
    /** Creates a new instance of ConnectionManager */
    public ConnectionManager( ICallBack listener ) {
        _listener = listener;
    }
    
    public void submitHighScore( String un, String pw ){
        _username = un;
        _password = pw;
        _newUser = true;
        
        submitHighScore();
    }
    
    public void submitHighScore(){
        
        // ensure that a thread/connectoin is not already running/open
        if( _thread != null )
            return;
        
        // if either username or password is null then try to fetch them from
        // the RMS
        if( _username == null || _password == null ){
            _username = DataAccess.getUserName();
            _password = DataAccess.getUserPassword();
            
            if( _username == null || _password == null ){
                _response = RES_FAIL_USERREQ;
                _listener.NetworkResponse( _response );
                return;
            }
            
        }
        
        // if no high score exist, then return out of method
        if( DataAccess.getHighestScore() <= 0 ){
            _response = RES_FAIL_NOSCORE;
            _listener.NetworkResponse( _response );
            return;
        }
        
        _thread = new Thread( this );
        _thread.start();
    }
    
    /**
     *
     **/
    public void run(){
        int result = connect();
        
        _response = result;
        
        // if success and is the first time the user has submitted their highscore, then
        // submit the username and password to the RMS
        if( _response == RES_SUCCESS && _newUser )
            DataAccess.setUser( _username, _password );
        
        _listener.NetworkResponse( _response );
    }
    
    private int connect(){
        _response = RES_CONNECTING;
        
        int status = RES_FAIL_CXN;
        
        String clientName = null;
        String clientMsg1 = null;
        String clientMsg2 = null;
        Image clientAd = null;
        
        try{
            // connect to the server
            _conn = (HttpConnection)Connector.open( URL );
            
            // set up connection
            _conn.setRequestMethod( HttpConnection.POST );
            _conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
            _conn.setRequestProperty( "User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.1" );
            _conn.setRequestProperty( "Content-Language", "en-US" );
            _conn.setRequestProperty( "Accept", "application/octet-stream" );
            //_conn.setRequestProperty( "Connection", "close" ); // optional
            
            // send gameid, user credentials, and highscore to server
            String formData = "gameid=" + LittleBlueMIDLET.GAMEID + "&username=" + _username + "&password=" + _password + "&score=" + DataAccess.getHighestScore();
            byte[] data = formData.getBytes();
            _conn.setRequestProperty(" Content-Length", Integer.toString( data.length ) );
            _os = _conn.openOutputStream();
            _os.write(data);
            _os.close();
            _os = null;
            
            // notify the user that the connection was successful
            _response = RES_SUCCESS_CXN;
            //_listener.NetworkResponse( RES_SUCCESS_CXN );
            
            // now try to download the servers response
            switch( _conn.getResponseCode() ){
                case HttpConnection.HTTP_OK:                                       
                    _dis = _conn.openDataInputStream();
                    
                    // packet/stream structure goes something like this (where '()' denotes that this section is
                    // optional
                    // [int status][int position][int ad status{0,1,2}]([string client name][client msg1][client msg2]([byte client image]))
                    
                    // step 1 - check status, if status anything else other than success then
                    //          set the response, notify the user and return otherwise continue processing
                    status = _dis.readInt();
                    
                    if( status == RES_SUCCESS ){
                        _position = _dis.readInt();                                              
                        
                        // read ad stats
                        int clientStatus = _dis.readInt();
                        
                        // if stream has client name then read the clients name
                        // NB: if a client name exist then it is assumed that two messages also exist 
                        if( clientStatus >= SERVER_ID_HAS_CLIENT_NAME ){
                            clientName = _dis.readUTF();
                            clientMsg1 = _dis.readUTF();
                            clientMsg2 = _dis.readUTF(); 
                        }
                        
                        // if the stream has a client ad then download image
                        // NB: The rest of the stream will be the image
                        if( clientStatus >= SERVER_ID_HAS_CLIENT_NAME_IMAGE ){
                            ByteArrayOutputStream bout = new ByteArrayOutputStream();
                            DataOutputStream dout = new DataOutputStream( bout );
                            int nextByte;
                            while( (nextByte = _dis.read()) != -1 ){
                                dout.writeByte( nextByte );
                            }
                            dout.close();
                            byte[] imageData = bout.toByteArray();
                            
                            try{
                                clientAd = Image.createImage( imageData, 0, imageData.length );                               
                            } catch( Exception e ){}
                        }
                        
                        // save to RMS
                        if( clientName != null )
                            DataAccess.submitClientDetails( clientName, clientMsg1, clientMsg2, clientAd);
                        
                    }
                    
                    break;
                    
                case HttpConnection.HTTP_INTERNAL_ERROR:
                case HttpConnection.HTTP_UNAVAILABLE:
                case -1:
                    
                    break;
                    
                default:
                    status = RES_FAIL_CXN;
                    break;
            }
            
        } catch( Exception e ){
            e.printStackTrace();
            status = RES_FAIL_CXN;
        } finally{
            closeConnections();
        }
        
        return status;
    }
    
    public void Stop(){
        
    }
    
    private void closeConnections(){
        try{
            if( _os != null )
                _os.close();
            
            if( _baos != null )
                _baos.close();
            
            if( _dis != null )
                _dis.close();
            
            if( _conn != null )
                _conn.close();
            
        } catch( Exception e ){
            System.out.println( "Error while trying to close connections; " + e.toString() );
        }
    }        
    
    public String getResponseMessage(){
        return _responseMsg;
    }
    
    public int getResponse(){
        return _response;
    }
    
    public int getPosition(){
        return _position;
    }
    
}
