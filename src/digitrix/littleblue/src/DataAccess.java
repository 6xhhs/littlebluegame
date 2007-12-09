/*
 * DataAccess.java
 *
 * Created on 7 June 2006, 21:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

import java.util.*;
import java.io.*;
import javax.microedition.rms.*;
import javax.microedition.lcdui.*;
/**
 *
 * @author Josh
 */
public class DataAccess {
    
    public static final String RS_HIGHSCORES    = "HiScores";
    private static final int NUMSCORES          = 5;
    
    // RecordStore Details for Game
    public static final String RS_GAME          ="Game"; // store level and store
    public static int REC_LEVEL                 = 1;
    public static int REC_SCORE                 = 2;
    public static int REC_LIVES                 = 3;
    
    // flags
    private static boolean _checkForSavedGame   = false;
    private static boolean _hasSavedGame        = false;
    
    // RecordStore Details for User
    public static final String RS_USER          = "User"; // {username, password}
    private static final int REC_USER_NAME      = 1;
    private static final int REC_USER_PASSWORD  = 2;
    
    // RecordStore Details for Client
    public static final String RS_CLIENT          = "Client";
    private static final int REC_CLIENT_NAME      = 1;
    private static final int REC_CLIENT_MSG_1     = 2;
    private static final int REC_CLIENT_MSG_2     = 3;
    private static final int REC_CLIENT_IMGWIDHT  = 4;
    private static final int REC_CLIENT_IMGHEIGHT = 5;
    private static final int REC_CLIENT_IMAGE     = 6;
    
    private static boolean _userLoaded = false;
    
    private static int[] _highScores = null;
    private static boolean _highScoresLoaded = false;
    
    private static String _username = null;
    private static String _password = null;
    
    private static String _clientName = null;   // should always be mandantory when a clients ad is added
    private static String _clientMsg1 = null;
    private static String _clientMsg2 = null;
    private static Image _clientAd = null;
    private static boolean _clientLoaded = false;
    
    /** Creates a new instance of DataAccess */
    public DataAccess() {
        
    }
    
    /***************************************************************************
     * GAME DATA ACCESS METHODS
     **************************************************************************/
    
    public static boolean hasSavedGame(){
        RecordStore rs = null;
        
        if( _checkForSavedGame )
            return _hasSavedGame;
        
        try{
            rs = RecordStore.openRecordStore( RS_GAME, false );
        } catch( Exception e ){}
        
        // close record store
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        if( rs != null )
            _hasSavedGame = true;
        else
            _hasSavedGame = false;
        
        _checkForSavedGame = true;
        
        return _hasSavedGame;
    }
    
    public static void saveGame( int level, int score, int lives ){
        RecordStore rs = null;
        
        // delete previous high scores record store
        try{
            RecordStore.deleteRecordStore( DataAccess.RS_GAME );
        } catch( Exception e ){}
        
        // create a new high scores record store to hold the new array
        try{
            rs = RecordStore.openRecordStore( DataAccess.RS_GAME, true );
        } catch( Exception e ){
            _hasSavedGame = false;            
            return;
        }
        
        // write integers stored in the array into the record store
        // format score for writing
        byte[] datLevel = Integer.toString( level ).toString().getBytes();
        byte[] datScore = Integer.toString( score ).toString().getBytes();
        byte[] datLives = Integer.toString( lives ).toString().getBytes();                
        
        try{
            rs.addRecord( datLevel, 0, datLevel.length );
            rs.addRecord( datScore, 0, datScore.length );
            rs.addRecord( datLives, 0, datLives.length );
        } catch( Exception e ){
            _hasSavedGame = false;
            return; 
        } 
        
        // close record store
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        _hasSavedGame = true;
    }
    
    public static int[] getSavedGame(){
        
        RecordStore rs = null;
        int level = -1;
        int score = -1; 
        int lives = -1; 
        int[] res = new int[3]; 
        
        try{
            rs = RecordStore.openRecordStore( DataAccess.RS_GAME, false );
        } catch( Exception e ){ return null; }
        
        // load up array with stored values
        if( rs == null )
            return null;
        
        try{
            int len;
            byte[] recordData = new byte[8];
            
            for( int i=1; i <= rs.getNumRecords(); i++ ){
                // re-allocate record holder if necessary
                if( rs.getRecordSize(i) > recordData.length )
                    recordData = new byte[ rs.getRecordSize(i)];
                
                // read record from record store
                len = rs.getRecord( i, recordData, 0 );
                
                // record the record and store it into the array                 
                if( i == REC_LEVEL ){
                    level = Integer.parseInt( new String( recordData, 0, len ) );
                } else if( i == REC_SCORE ){
                    score = Integer.parseInt( new String( recordData, 0, len ) );
                }  else if( i == REC_LIVES ){
                    lives = Integer.parseInt( new String( recordData, 0, len ) );
                }
            }
        } catch( Exception e ){
            return null; 
        }
        
     // try closing the connection
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        res[0] = level;
        res[1] = score;
        res[2] = lives; 
        
        return res;
    }
    
    
    /***************************************************************************
     * USER DATA ACCESS METHODS
     **************************************************************************/
    
    public static void loadHighScores(){
        RecordStore rs = null;
        
        // initilise highscores array
        _highScores = new int[5];
        for( int i=0; i<NUMSCORES; i++ )
            _highScores[i] = 0;
        
        //
        try{
            rs = RecordStore.openRecordStore( RS_HIGHSCORES, false );
        } catch( Exception e ){}
        
        // load up array with stored values
        if( rs != null ){
            try{
                int len;
                byte[] recordData = new byte[8];
                
                for( int i=1; i <= rs.getNumRecords(); i++ ){
                    // re-allocate record holder if necessary
                    if( rs.getRecordSize(i) > recordData.length )
                        recordData = new byte[ rs.getRecordSize(i)];
                    
                    // read record from record store
                    len = rs.getRecord( i, recordData, 0 );
                    
                    // record the record and store it into the array
                    _highScores[i-1] = (Integer.parseInt( new String( recordData, 0, len ) ) );
                }
            } catch( Exception e ){}
        }
        
        // try closing the connection
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
    }
    
    /**
     * persist score array to RMS
     **/
    public static boolean updateHighScore(){
        RecordStore rs = null;
        
        if( _highScores == null )
            return false;
        
        // delete previous high scores record store
        try{
            RecordStore.deleteRecordStore( DataAccess.RS_HIGHSCORES );
        } catch( Exception e ){}
        
        // create a new high scores record store to hold the new array
        try{
            rs = RecordStore.openRecordStore( DataAccess.RS_HIGHSCORES, true );
        } catch( Exception e ){ return false; }
        
        // write integers stored in the array into the record store
        for( int i=0; i<NUMSCORES; i++ ){
            // format score for writing
            byte[] recordData = Integer.toString( _highScores[i] ).toString().getBytes();
            
            try{
                rs.addRecord( recordData, 0, recordData.length );
            } catch( Exception e ){}
        }
        
        // close record store
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        return true;
        
    }
    
    /**
     * new score obtained; if score is greater than any of the existing scores then
     * add it to the array
     **/
    public static void addHighScore( int score ){
        if( _highScores == null )
            loadHighScores();
        
        int i;
        for( i=0; i<NUMSCORES; i++ ){
            if( score > _highScores[i] )
                break;
        }
        
        // insert the current score into the hi score list (moving the other ones down)
        if( i<NUMSCORES ){
            for( int j=NUMSCORES-1; j>i; j-- ){
                _highScores[j] = _highScores[j-1];
            }
            _highScores[i] = score;
        }
        
        updateHighScore();
    }
    
    public static void updateHighScore( int score ){
        addHighScore( score );
        updateHighScore();
    }
    
    public static int[] getHighScores(){
        if( _highScores == null )
            loadHighScores();
        
        return _highScores;
    }
    
    public static int getHighestScore(){
        if( _highScores == null )
            loadHighScores();
        
        if( _highScores == null )
            return 0;
        else
            return _highScores[0];
    }
    
    public static void loadUserDetails(){
        // if an attempt has already been made to load the details from the recordstore then
        // just return without doing any processing (done to reduce the unecassary processing)
        if( _userLoaded )
            return;
        
        RecordStore rs = null;
        
        try{
            rs = RecordStore.openRecordStore( RS_USER, false );
        } catch( Exception e ){}
        
        // load up array with stored values
        if( rs != null ){
            
            try{
                int len;
                byte[] recordData = new byte[8];
                
                for( int i=1; i <= rs.getNumRecords(); i++ ){
                    // re-allocate record holder if necessary
                    if( rs.getRecordSize(i) > recordData.length )
                        recordData = new byte[ rs.getRecordSize(i)];
                    
                    // read record from record store
                    len = rs.getRecord( i, recordData, 0 );
                    
                    // record the record and store it into the array
                    if( i == REC_USER_NAME ){
                        _username = new String( recordData, 0, len );
                    } else if( i == REC_USER_PASSWORD ){
                        _password = new String( recordData, 0, len );
                    }
                }
            } catch( Exception e ){}
        }
        
        // try closing the connection
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        if( _username != null && _password != null )
            _userLoaded = true;
        
    }
    
    public static String getUserName(){
        // if the username has not be loaded then try to load it from the
        // recordstore
        if( _username == null )
            loadUserDetails();
        
        return _username;
    }
    
    public static String getUserPassword(){
        // if the password has not be loaded then try to load it from the
        // recordstore
        if( _password == null )
            loadUserDetails();
        
        return _password;
    }
    
    /**
     * this method is called when a user successfully logs onto the
     * 'highscore' site; thus meaning they have successfully registered onto the
     * site, therefore allow them to upload their highscores to go into win the
     * amazing prizes ;)
     **/
    public static void setUser( String un, String pw ){
        RecordStore rs = null;
        
        _username = un;
        _password = pw;
        
        // delete previous high scores record store
        try{
            RecordStore.deleteRecordStore( DataAccess.RS_USER );
        } catch( Exception e ){}
        
        // create a new high scores record store to hold the new array
        try{
            rs = RecordStore.openRecordStore( DataAccess.RS_USER, true );
        } catch( Exception e ){ }
        
        // format score for writing
        byte[] datUser = _username.getBytes();
        byte[] datPassword = _password.getBytes();
        
        try{
            rs.addRecord( datUser, 0, datUser.length );
            rs.addRecord( datPassword, 0, datPassword.length );
        } catch( Exception e ){}
        
        // close record store
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        _userLoaded = true;
    }
    
    /***************************************************************************
     * CLIENT DATA ACCESS METHODS
     **************************************************************************/
    
    public static String getClientName(){
        // if clientname is null then attempt to load from the recordstore
        if( _clientName == null )
            loadClientDetails();
        
        return _clientName;
    }
    
    public static Image getClientImage(){
        // if clientimage is null then attempt to load from the recordstore
        if( _clientAd == null )
            loadClientDetails();
        
        return _clientAd;
    }
    
    /**
     * retrieve client name from recordstore (if exist)
     **/
    public static void loadClientDetails() {
        // if an attempt has already been made to load the details from the recordstore then
        // just return without doing any processing (done to reduce the unecassary processing)
        if( _clientLoaded )
            return;
        
        int adWidth = 0;
        int adHeight = 0;
        
        RecordStore rs = null;
        
        try{
            rs = RecordStore.openRecordStore( RS_CLIENT, false );
        } catch( Exception e ){}
        
        // load up array with stored values
        if( rs != null ){
            
            try{
                int len;
                byte[] recordData = new byte[8];
                
                for( int i=1; i <= rs.getNumRecords(); i++ ){
                    // re-allocate record holder if necessary
                    if( rs.getRecordSize(i) > recordData.length )
                        recordData = new byte[ rs.getRecordSize(i)];
                    
                    // read record from record store
                    len = rs.getRecord( i, recordData, 0 );
                    
                    // record the record and store it into the array
                    switch( i ){
                        case REC_CLIENT_NAME:
                            _clientName = new String( recordData, 0, len );
                            break;
                        case REC_CLIENT_MSG_1:
                            _clientMsg1 = new String( recordData, 0, len );
                            break;
                        case REC_CLIENT_MSG_2:
                            _clientMsg2 = new String( recordData, 0, len );
                            break;
                        case REC_CLIENT_IMGWIDHT:
                            adWidth = Integer.parseInt( new String( recordData, 0, len ) );
                            break;
                        case REC_CLIENT_IMGHEIGHT:
                            adHeight = Integer.parseInt( new String( recordData, 0, len ) );
                            break;
                        case REC_CLIENT_IMAGE:
                            ByteArrayInputStream  bin = new ByteArrayInputStream( recordData );
                            DataInputStream   din = new DataInputStream( bin );
                            
                            int[] rawdata = new int[len/4];
                            
                            for(int k =0 ;k<rawdata.length ;k++) {
                                rawdata[k] = din.readInt();
                            }
                            
                            _clientAd = Image.createRGBImage( rawdata, adWidth, adHeight, false );
                            
                            bin.reset();
                            din.close();
                            din =null;
                            break;
                    }
                    
                }
            } catch( Exception e ){
                // courrupt so delete the record
                try{
                    RecordStore.deleteRecordStore( RS_CLIENT );
                } catch( Exception e2 ){}
            }
        }
        
        // try closing the connection
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        _clientLoaded = true;
        
    }
    
    public static void deleteClientDetails(){
        try{
            RecordStore.deleteRecordStore( DataAccess.RS_CLIENT );
        } catch( Exception e ){}
    }
    
    public static void submitClientDetails( String name, String msg1, String msg2, Image img){
        RecordStore rs = null;
        
        byte[] datName = null;
        byte[] datMsg1 = null;
        byte[] datMsg2 = null;
        byte[] datAd = null;
        byte[] datAdWidth = null;
        byte[] datAdHeight = null;
        
        _clientName = name;
        _clientMsg1 = msg1;
        _clientMsg2 = msg2;
        _clientAd = img;
        
        // delete recordstore (if exist)
        try{
            RecordStore.deleteRecordStore( DataAccess.RS_CLIENT );
        } catch( Exception e ){}
        
        // open new recordstore
        try{
            rs = RecordStore.openRecordStore( DataAccess.RS_CLIENT, true );
        } catch( Exception e ){ }
        
        // client name
        datName = name.getBytes();
        datMsg1 = msg1.getBytes();
        datMsg2 = msg2.getBytes();
        
        // image details
        if( img != null ){
            try{
                int width = img.getWidth();
                int height = img.getHeight();
                
                datAdWidth = Integer.toString( width ).getBytes();
                datAdHeight = Integer.toString( height ).getBytes();
                
                // break image down to array of integers
                int[] imgRgbData = new int[width * height];
                
                img.getRGB( imgRgbData, 0, width, 0, 0, width, height );
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                
                for (int i = 0; i < imgRgbData.length; i++) {
                    dos.writeInt(imgRgbData[i]);
                }
                
                datAd = baos.toByteArray();
                
                baos.close();
                
            } catch( Exception e ){
                //TODO: Delete me
                System.out.println( "Error while trying to save client details; " + e.toString() );
            }
        }
        
        // save to recordstore
        try{
            rs.addRecord( datName, 0, datName.length );
            rs.addRecord( datMsg1, 0, datMsg1.length );
            rs.addRecord( datMsg2, 0, datMsg2.length );
            
            if( datAd != null ){
                System.out.println( "DataAccess: Adding image to RMS" );
                rs.addRecord( datAdWidth, 0, datAdWidth.length );
                rs.addRecord( datAdHeight, 0, datAdHeight.length );
                rs.addRecord( datAd, 0, datAd.length );
            }
        } catch( Exception e ){
            System.out.println( "DataAccess: Trying to add image to RMS; " + e.toString() );
        }
        
        // try to close recordstore
        try{
            rs.closeRecordStore();
        } catch( Exception e ){}
        
        _clientLoaded = true;
    }
    
}
