/*
 * LittleBlueMIDLET.java
 *
 * Created on 4 April 2006, 21:13
 */

package digitrix.littleblue.src;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 *
 * @author  Josh Newnham
 * @version
 */
public class LittleBlueMIDLET extends MIDlet {
    
    public static final int GAMEID = 1000;  // game identifier (for server requests etc) 
    
    private static LittleBlueMIDLET _midlet = null; 
    
    private Displayable _currentDisplay = null;
    private GameManager _gameManager = null; 
    private boolean     _hasRunningGame = false; 
    
    public LittleBlueMIDLET(){
        _midlet = this;
        
        // start game
        
    }
    
    public void startApp() {                
        activateGameManager(); 
    }
    
    public void pauseApp() {
        if( _gameManager != null )
            _gameManager.pause(); 
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
    public void close(){
        try{
            destroyApp( true );
            notifyDestroyed(); 
        }
        catch( Exception ex ){}
    }        
    
    // static methods
    public static LittleBlueMIDLET getMidlet(){
        return _midlet;
    }
    
    public static void activateDisplayable( Displayable s ){
        try{           
            Display.getDisplay( getMidlet() ).setCurrent( s ); 
        }
        catch( Exception e ){
            e.printStackTrace(); 
        }
    }
    
    public static Displayable getActivatedDisplayable(){
        return Display.getDisplay( getMidlet() ).getCurrent();  
    }
    
    /**
     * Either start the game or resume the game 
     **/
    public void activateGameManager(){
        // turn off GameMenu thread         
        if( _gameManager == null ){
            try{
            _gameManager = new GameManager();   
            
            }
            catch( Exception e ){
                e.printStackTrace(); 
            }
        }                
        
        if( _gameManager.hasRunningGame() )
            _gameManager.resume();                          
        
        _currentDisplay = _gameManager;
        activateDisplayable( _gameManager );
                                         
    }            
    
}
