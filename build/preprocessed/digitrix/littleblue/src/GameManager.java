/*
 * GameManager.java
 *
 * Created on 26 April 2006, 21:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

import com.sun.midp.dev.GraphicalInstaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.rms.*;
import java.util.*;

/**
 *
 * @author Josh
 */
public class GameManager extends GameCanvas implements CommandListener, Runnable, ICallBack {
    
private static final int BGCOL_SPLASH = 0xffffff;
private static final int BGCOL_MENU = 0x000000;

public static final int MENUITM_NEW_GAME = 0;
public static final int MENUITM_RESUME_GAME = 1;
public static final int MENUITM_RESUME_SAVED_GAME = 2;
public static final int MENUITM_SAVE_GAME = 3;
public static final int MENUITM_HELP = 4;
public static final int MENUITM_SPLASH = 5;
public static final int MENUITM_HIGH_SCORES = 6;
public static final int MENUITM_SUBMIT_SCORE = 7;
public static final int MENUITM_ABOUT = 8;
public static final int MENUITM_EXIT = 9;

private static final int SPLASH_DELAY = 2000; // time the splash screen is displayed
private static final int ADVERT_DELAY = 3000; // time the advert is displayed
private static final int MESSAGE_DELAY = 3000; 

private static final String IMGPATH_ALPHABET = "/digitrix/littleblue/res/alphabet.png";
private static final String IMGPATH_SPLASH = "/digitrix/littleblue/res/splash.png";
private static final String IMGPATH_TITLEICON = "/digitrix/littleblue/res/menuicon.png";
private static final String IMGPATH_TITLE = "/digitrix/littleblue/res/title.png";

private static final int MAX_FPS = 20;
private static GameManager _instance = null;

// game manager states
public static final int GAME_PLAYING = 0;
public static final int GAME_PAUSED = 1;
public static final int GAME_DONE = 2;
public static final int GAME_OVER = 3;
public static final int GAME_WAITING = 4;

public static final int MENU_SPLASH = 11;
public static final int MENU_ADVERT = 12;
public static final int MENU_MENU = 13;
public static final int MENU_HELP = 14;
public static final int MENU_ABOUT = 15;
public static final int MENU_HIGHSCORES = 16;
public static final int MENU_SUBMITSCORE = 17;
public static final int MENU_LOGIN = 18;
public static final int MENU_NETSTART = 19;
public static final int MENU_NETEND = 20;
public static final int MENU_CHOICE = 21;
public static final int MENU_MESSAGE = 22; 

private int _currentStateTime = 0;
private int _currentMenu = MENUITM_NEW_GAME;
private int[] _menuSeq = {0,1,2,3,4,5,6,7,8,9};
private String[] _menuStr = { "NEW*GAME", "CONTINUE*GAME", "RESUME*GAME", "SAVE*GAME", "HELP", "SPLASH", "HIGH*SCORES", "SUBMIT*SCORE", "ABOUT", "EXIT" };

// positions for images, text, etc. the reason we store the variables is
// so we can work them out on constructor of the object therefore not having to work them out
// every cycle.
private int posSplashY;
private int posSplashX;
private int posSplashTextY;
private int posLogoX;
private int posLogoY;
private int posArrowX;
private int posArrowUpY;
private int posArrowDownY;
private int posScrollingY; // holds the y position for scrolling text
private int posMenuY;
private int posHalfWidth;
private int posHalfHeight; 

// game objects
private World _world = null;
private Thread _gameThread = null;

// game status
private boolean _alive = false;
private boolean _running = false;
private int _gameState;
private int _score;
private int _levelsScore; 

// fps variables
private int fps=0;
private int cyclesThisSecond=0;
private long lastFPSTime=0;

private Font _defaultFont;
private int _bgColour = 0x314273;

// commands
private Command _cmdGameMenu = null;
private Command _cmdSubmit = null;
private Command _cmdLoginOK = null;
private Command _cmdLoginCancel = null;

// others
private Image _imgAlphabet = null;
private Image _imgSplash = null;
private Image _imgTitle = null;

// handlers
private GameHandler _gameHandler = null;
private MenuHandler _menuHandler = null;

private TextBox _tbUsername = null;
private TextBox _tbPassword = null;

private int _lastKeyCode = 666;

private ConnectionManager _cxnManager = null;

/** Creates a new instance of GameManager */
public GameManager() throws Exception {
    super( false ); // super( false ); so that the keyPressed, keyReleased events are not supressed
    
    _instance = this;
    
    initImages();
    
    // set up the commands
    _cmdSubmit = new Command( "Submit", Command.OK, 1 );
    _cmdGameMenu = new Command( "Menu", Command.BACK, 2 );
    setCommandListener(this);
    
    // set up login TextBox
    _cmdLoginOK = new Command( "OK", Command.OK, 1 );
    _cmdLoginCancel = new Command( "Cancel", Command.BACK, 2 );
    _tbUsername = new TextBox( "Enter Username", "", 20, TextField.ANY );
    _tbUsername.addCommand( _cmdLoginOK );
    _tbUsername.addCommand( _cmdLoginCancel );
    _tbUsername.setCommandListener( this );
    _tbPassword = new TextBox( "Enter Password", "", 20, TextField.ANY );
    _tbPassword.addCommand( _cmdLoginOK );
    _tbPassword.addCommand( _cmdLoginCancel );
    _tbPassword.setCommandListener( this );
    
    // set up handlers
    _menuHandler = new MenuHandler();
    _gameHandler = new GameHandler();
    
    // instantiate the world
    try{
        _world = new World( this.getWidth(), this.getHeight() );
    } catch( Exception e ){
        e.printStackTrace();       
    }
    
    _defaultFont = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL );
    
    setState( MENU_SPLASH );
    
    // start games thread
    if ( _gameThread == null || !_gameThread.isAlive() ){
        _gameThread = new Thread( this );
        _running = true;
        _gameThread.start();
    }
}

public void initNewGame( ){
    _score = 0; 
    _levelsScore = 0;
    
    try{       
        _alive = true;
        _world.resetGame();
        _world.loadLevel();
         _world.getHero().setLives( 3 ); 
        _bgColour = _world.getBGColour();
    } catch( Exception e ){
        e.printStackTrace();
    }
    
    // set state to starting up (i.e. display the level but do not process the
    // sprite/show text like 'level 0 \n Press any key to start'
    setState( GAME_WAITING );
}

/**
 * Main game loop; basic task of our game loop is to:
 * 1. check input
 * 2. update actors on stage
 * 3. update our world
 * 4. check for player collisions (with baddies and other interactive sprites)
 * 5. render world
 **/
public void run() {
    long currTime = System.currentTimeMillis();
    long endTime;
    
    while ( _running ) {
        
        long elapsedTime = System.currentTimeMillis() - currTime;
        currTime += elapsedTime;
        _currentStateTime += elapsedTime;
        
        if( _gameState == MENU_SPLASH ){
            if( _currentStateTime > SPLASH_DELAY )
                setState( MENU_ADVERT  );
        }
        else if( _gameState == MENU_ADVERT ){
            if( _currentStateTime > ADVERT_DELAY )
                setState( MENU_MENU  );
        }
        else if( _gameState == MENU_MESSAGE ){
            if( _currentStateTime > MESSAGE_DELAY )
                setState( MENU_MENU ); 
        }
        else{
            
            // update user sprite
            checkUserInput( elapsedTime );
            
            if( _gameState == GAME_PLAYING ){
                // cycle actors
                
                //if( _world.isHeroInView() ){
                    updateActors( elapsedTime );                  
                
                // update world
                _world.updateWorld( elapsedTime );
                
                // test to see if the game is over i.e. player has no more lives
                if( !_world.getHero().isAlive() )
                    setState( GAME_OVER );
            }
        }
        
        // render onto screen
        render();
        
        // slow thread down, if necessary, to keep game consistant/smooth
        syncGameLoop( currTime );
        
        // fps code
        if( System.currentTimeMillis() - lastFPSTime > 1000 ){
            lastFPSTime = System.currentTimeMillis();
            fps = cyclesThisSecond;
            cyclesThisSecond = 0;
        } else{
            cyclesThisSecond ++;
        }
        
    }
}

/**
 *
 **/
private void syncGameLoop( long currTime ){
    // sleep if necessary to make a smooth framerate
    long endTime = System.currentTimeMillis() - currTime;
    //System.out.println( "Start Time = " + startTime + " End Time = " + endTime );
    if (endTime < (1000 / MAX_FPS)) {
        try {
            // frames per second
            _gameThread.sleep((1000 / MAX_FPS) - endTime);
        } catch (Exception e) { }
    } else {
        try {
            _gameThread.yield();
        } catch (Exception e) { }
    }
}

/**
 *
 **/
private void checkUserInput( long elapsedTime ){
    int keyState = getKeyStates();
    
    if( _gameState < 10 )
        _gameHandler.handleInput( keyState );
    else
        _menuHandler.handleInput( keyState );
}

protected void keyPressed(int keyCode) {
    if( _gameState < 10 || _gameState == MENU_SPLASH || _gameState == MENU_ADVERT )
        return; // use this method only when the state is within the menu range
    
    // process only one key at a time, so if _lastKey is greater than 0 (i.e. a
    // key has already been captured but not processed) then return
    if( _lastKeyCode != 666 )
        return;
    
    _lastKeyCode = keyCode;
}

/**
 *
 **/
private int getLastKeyCode(){
    if( _lastKeyCode == 666 )
        return 666;
    
    int tmp = _lastKeyCode;
    _lastKeyCode = 666;
    
    return tmp;
}

/**
 * Clears the key states.
 */
void flushKeys() {
    getKeyStates();
}

/**
 *
 **/
private void updateActors( long elapsedTime ){
    // update hero
    try{
        _world.getHero().cycle( elapsedTime );
    } catch( Exception e ){ e.printStackTrace(); }
    
    // update baddies
    // iterate through all of the baddies, if performAI returns true then
    // check collision, if no collision then cycle them
    Enumeration worldSprites = _world.getWorldSprites();
    
    // replace while( worldSprites.hasMoreElements() ) with for( int i = 0; i++ ) as a optimization technique
    // desribed on http://developer.sonyericsson.com/site/global/techsupport/tipstrickscode/java/p_fastiteratingarrayorvectorjava.jsp
    try{
        for( int i = 0; ; i++ ){
            Sprite sprite = (Sprite)worldSprites.nextElement();
            if( sprite instanceof Baddie ){
                Baddie baddie = (Baddie)sprite;
                try{
                    if( baddie.performAI() ){
                        if( baddie.collidesWith( _world.getHero(), true ) ){
                            if( _world.getHero().canAttack() ){
                                _world.getHero().jump( true, -8 );
                                baddie.takeDamage( _world.getHero().getStrength() );
                            } else{
                                _world.getHero().takeDamage( baddie.getStrength() );
                            }
                        } else{
                            baddie.cycle( elapsedTime );
                        }
                    }
                } catch( Exception e ){ e.printStackTrace(); }
            } else if( sprite instanceof Prop ){
                Prop prop = (Prop)sprite;
                if( _world.getHero().collidesWith( prop, true ) )
                    prop.acquire( _world.getHero() );
                prop.cycle( elapsedTime );
            }
        }
    } catch( Exception e ){}
    
    // replace while( worldEffects.hasMoreElements() ) with for( int i = 0; i++ ) as a optimization technique
    // desribed on http://developer.sonyericsson.com/site/global/techsupport/tipstrickscode/java/p_fastiteratingarrayorvectorjava.jsp
    Enumeration worldEffects = _world.getWorldEffects();
    try{
        for( int i=0; ; i++ ){
            GameEffect effect = (GameEffect)worldEffects.nextElement();
            if( effect.isVisible() )
                effect.update( elapsedTime );
        }
    } catch( Exception e ){}
    
}

private void render(){           
    if( LittleBlueMIDLET.getActivatedDisplayable() != this )
        return; 
    
    Graphics g = getGraphics();        
    
    try{
    if( _gameState < 10 )
        _gameHandler.render( g );
    else
        _menuHandler.render( g );
    }
    catch( Exception e ){}        
    
    flushGraphics();
}

public static GameManager GetInstance(){
    return _instance;
}

// mutator methods
public int getScore(){
    return _score;
}

public int getLevelsScore()
{
    return _levelsScore; 
}

public void advanceScore( int value, boolean byCoin ){
    if( byCoin )
        World.GetInstance().coinCollected();
    
    _score += value;
    _levelsScore += value; 
}

/**
 * Called when the player tocuhes the checkpoint on the current level
 **/
public void advanceLevel(){
    
    // reset current levels score
    _levelsScore = 0; 
    
    // if true is returned then a another level exists otherwise the player has clocked the game :)
    if( _world.nextLevel() ){
        setState( GAME_WAITING );
        _bgColour = _world.getBGColour();
    } else
        setState( GAME_DONE );
}

/**
 * advanceLevel is called when the game is loaded from a previously saved game
 **/
public void advanceLevel( int level ){
    // reset current levels score
    _levelsScore = 0; 
    
    // if true is returned then a another level exists otherwise the player has clocked the game :)
    if( _world.loadLevel( level ) ){
        setState( GAME_WAITING );
        _bgColour = _world.getBGColour();
    } else
        setState( GAME_DONE );
}

public void setState( int value ){
    
    // if any game state is set then clear the keys
    if( _gameState < 10 ){
        _lastKeyCode = -1; // reset lastkey
        flushKeys();
    }
    
    // if state has been set to advert then test if an ad exist; if not then 
    // set the state to menu else leave ;)
    if( value == MENU_ADVERT ){
        if( DataAccess.getClientImage() == null )
            value = MENU_MENU; 
    }
    
    // if game state was in the login menu but moving to a different state then
    // clear the login message
    if( value == MENU_LOGIN )
        _menuHandler.resetMenu();
    
    if( value == GAME_OVER ){
        DataAccess.addHighScore( _score );
        _alive = false;
    } else if( value == GAME_DONE )
        DataAccess.addHighScore( _score );
    else if( value == GAME_WAITING || value == MENU_HIGHSCORES || value == MENU_HELP || value == MENU_ABOUT )
        addCommand( _cmdGameMenu );
    else if( value == MENU_LOGIN ){
        addCommand( _cmdGameMenu );
        addCommand( _cmdSubmit );
    } else if( value == MENU_NETEND ){
        removeCommand( _cmdGameMenu );
        removeCommand( _cmdSubmit );
    } else if( _gameState == MENU_NETSTART && value == MENU_MENU ){
        removeCommand( _cmdGameMenu );
        removeCommand( _cmdSubmit );
    } else if( _gameState == MENU_LOGIN && value == MENU_MENU ){
        removeCommand( _cmdGameMenu );
        removeCommand( _cmdSubmit );
    } else if( _gameState == MENU_LOGIN && value == MENU_NETSTART ){
        removeCommand( _cmdGameMenu );
        removeCommand( _cmdSubmit );
    } else if( _gameState == MENU_MENU && value == MENU_NETSTART )
        removeCommand( _cmdGameMenu );
    else if( value == MENU_MENU ){
        removeCommand( _cmdGameMenu );
        posScrollingY = getHeight() - 8; 
    }
        
    _currentStateTime = 0;
    _gameState = value;
}

public int getState(){
    return _gameState;
}

public void pause(){
    setState( GAME_PAUSED );
}

public boolean hasRunningGame(){
    return _alive;
}

public void resume(){
    if( _gameState < 10 ){
        setState( GAME_WAITING );
    }
}

public void stop(){
    _running = false;
}

/**
 *
 **/
public void commandAction( Command cmd, Displayable displayable ){   
    if( cmd == _cmdGameMenu )
        setState( MENU_MENU );
    else if( cmd == _cmdSubmit ){
        // validation
        if( _menuHandler.getTempUsername() != null && _menuHandler.getTempPassword() != null ){
            // submit score to server
            if( _cxnManager == null )
                _cxnManager = new ConnectionManager( GameManager.GetInstance() );
            setState( MENU_NETSTART );
            _cxnManager.submitHighScore( _menuHandler.getTempUsername(), _menuHandler.getTempPassword() );
        } else{
            _menuHandler.setTempNotification( "USERNAME*AND*PASSWORD*REQUIRED" );
        }
    } else if( cmd == _cmdLoginOK ){        
        if( displayable == _tbUsername ){
            if( _tbUsername.getString().length() > 0 )
                _menuHandler.setTempUsername( _tbUsername.getString() );
        }
        else if( displayable == _tbPassword ){
            if( _tbPassword.getString().length() > 0 )
                _menuHandler.setTempPassword( _tbPassword.getString() );
        }       
                         
        LittleBlueMIDLET.activateDisplayable( GameManager.GetInstance() );
    } else if( cmd == _cmdLoginCancel )
        LittleBlueMIDLET.getMidlet().activateDisplayable( this );    
}

/**
 *
 **/
public void NetworkResponse( int status ){
    if( status == ConnectionManager.RES_FAIL_USERREQ )
        setState( MENU_LOGIN );
    else
        setState( MENU_NETEND );
    
}

/**
 *
 **/
private void printToScreen( Graphics g, int midX, int midY, String text, boolean fromLeft ){
    int startX;
    int startY = midY - 4; // 4 = half of the fonts height
    char[] chars = text.toCharArray();
    String nextLine = null;
    
    // test to ensure that the string is not wider than the screen
    if( chars.length*8 > getWidth() ){
        //TODO: Split text up so we can fit it on the screen
    }
    
    // work out starting point
    if( fromLeft )
        startX = midX;
    else
        startX = midX - ((chars.length/2) * 8);
    
    for( int i = 0; i < chars.length; i++ ){
        char mystr = chars[i];
        if ((mystr!= '\n') && (mystr!=32)) {
            if (mystr>64) mystr-=33;
            else mystr-=32;
            g.setClip(startX,startY,8,8);
            g.drawImage(_imgAlphabet,startX-((mystr&15)<<3) ,startY-((mystr>>4)<<3), Graphics.LEFT | Graphics.TOP );
            startX += 8;
        }
    }
    
    if( nextLine != null )
        printToScreen( g, midX, midY, nextLine, fromLeft );
}

private void initImages(){
    try{
        _imgAlphabet = Image.createImage( IMGPATH_ALPHABET );
        _imgSplash = Image.createImage( IMGPATH_SPLASH );
        _imgTitle = Image.createImage( IMGPATH_TITLE );
    } catch( Exception e ){
        e.printStackTrace();
    }
    
    initImagePositions();
}

/**
 *
 **/
private void initImagePositions(){
    posSplashY = getHeight()/2 - 19;
    posSplashX = (getWidth()/2)-(_imgSplash.getWidth()/2)-10;
    posSplashTextY = posSplashY + _imgSplash.getHeight() + 20; // 10 being our buffer
    posLogoX = (getWidth()/2)-(_imgTitle.getWidth()/2);
    posLogoY = 30;
    posArrowX = getWidth()/2-8;
    posMenuY = posLogoY + _imgTitle.getHeight()+ 15; 
    posArrowUpY = posMenuY-12;
    posArrowDownY = posMenuY+12;
    posHalfWidth = getWidth()/2;
    posHalfHeight = getHeight()/2; 
}

private class GameHandler{       
    
    public void handleInput( int keyState ){
        if( _gameState == GAME_PLAYING )
            _world.getHero().handleInput( keyState );
        else if( _gameState == GAME_WAITING && (keyState & GameCanvas.FIRE_PRESSED) != 0 )
            setState( GAME_PLAYING );
        else if( (_gameState == GAME_DONE || _gameState == GAME_OVER) && (keyState & GameCanvas.FIRE_PRESSED) != 0 )
            setState( MENU_MENU );
    }
    
    public void render( Graphics g ){
        switch( _gameState ){
            case GAME_DONE:
            case GAME_OVER:
            case GAME_PAUSED:
            case GAME_PLAYING:
                renderWorld( g );
                break;
            case GAME_WAITING:
                renderWaiting( g );
                break;
        }
    }
    
    /**
     *
     **/
    private void renderWaiting( Graphics g ){
        // clear screen
        g.setColor( _bgColour );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        _world.paintBackGround( g, 0, 0, getWidth(), getHeight() );
        
        printToScreen( g, getWidth()/2, (getHeight()/2) - 10, "LEVEL*" + _world.getCurrentLevel(), false );
        printToScreen( g, getWidth()/2, (getHeight()/2) + 10, "PRESS*ANY*KEY", false );
    }
    
    /**
     *
     **/
    private void renderWorld( Graphics g ){
        
        // clear screen
        g.setColor( _bgColour );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        _world.paint( g, 0, 0, getWidth(), getHeight() );
        
        if( _gameState == GAME_PAUSED ){
            
        } else if( _gameState == GAME_OVER ){
            printToScreen( g, getWidth()/2, (getHeight()/2) - 10, "GAME*OVER", false );
            printToScreen( g, getWidth()/2, (getHeight()/2) + 10, "PRESS*ANY*KEY", false );
        } else if( _gameState == GAME_DONE ){
            printToScreen( g, getWidth()/2, (getHeight()/2) - 10, "WELL*DONE", false );
            printToScreen( g, getWidth()/2, (getHeight()/2) + 10, "FINAL*SCORE*" + _score, false );
        }
        
        //g.setColor( 0x00ffffff );
        //g.drawString( "fps = " + fps, 0, getHeight() - ( _defaultFont.getHeight() + 25 ), Graphics.LEFT | Graphics.TOP );
    }
    
}

private class MenuHandler{
    
    private String[] helpText = {"LITTLE*BLUE","", "LITTLE*BLUE*IS*ON*A", "MISSON*HELP*HIM",
        "COLLECT*ALL*OF", "THE*COINS*TO*PROGRESS", "TO*THE*NEXT*LEVEL", "", "CONTROLS", "4*TO*RUN*LEFT", "6*TO*RUN*RIGHT", "2*OR*5*TO*JUMP"}; 
    private String[] aboutText = {"DIGITRIX*PRESENTS", "", "LITTLE*BLUE", "VERSION*1.1", "", "WWW.DIGITRIX.CO.NZ", 
        "", "", "COPYRIGHT*2006", "ALL*RIGHTS*RESERVED"};         
    
    public static final byte LOGIN_USERNAME_SEL = 0;
    public static final byte LOGIN_PASSWORD_SEL = 1;
    
    public static final byte CHOICE_YES_SEL = 1;
    public static final byte CHOICE_NO_SEL = 0;  
    
    public static final byte ACTION_SAVE_GAME = 0;    
    
    private final String _pressFireToContinue = "PRESS*FIRE*TO*CONTINUE";
    
    private String _tmpUsername = null;
    private String _tmpPassword = null;
    private String _tmpNotification = null;
    
    private byte _selectedLoginItem = LOGIN_USERNAME_SEL;
    private byte _selectedChoiceItem = CHOICE_YES_SEL; 
    private byte _currentAction; 
    
    private String message = null; 
    
    
    public void handleInput( int keyState ){
        int lastKeyCode = getLastKeyCode();
        if( lastKeyCode == 666 )
            return;
        
        int gameAction = getGameAction( lastKeyCode );
        
        switch( _gameState ){
            case MENU_NETEND:
            case MENU_HIGHSCORES:
            case MENU_ABOUT:
            case MENU_HELP: 
                if( gameAction == FIRE )
                    setState( MENU_MENU );
                break;
            case MENU_MENU:
                if  ( gameAction == UP ) {
                    // scroll to the menu item above the currently selected item
                    _currentMenu = Math.max( --_currentMenu, 0 );
                    
                    if( _currentMenu == MENUITM_SAVE_GAME && !_alive )
                        _currentMenu = MENUITM_RESUME_SAVED_GAME; 
                    
                    if( _currentMenu == MENUITM_RESUME_SAVED_GAME && !DataAccess.hasSavedGame() )
                        _currentMenu = MENUITM_RESUME_GAME; 
                    
                    // if user has a running game (is _alive) then display 'Resume Game' otherwise
                    // skip it
                    if( _currentMenu == MENUITM_RESUME_GAME && !_alive )
                        _currentMenu = MENUITM_NEW_GAME;
                                        
                } else if ( gameAction == DOWN ){
                    // scroll to the menu item below the currently selected item
                    _currentMenu = Math.min( ++_currentMenu, _menuSeq.length-1 );
                    
                    // if user has a running game (is _alive) then display 'Resume Game' otherwise
                    // skip it
                    if( _currentMenu == MENUITM_RESUME_GAME && !_alive )
                        _currentMenu = MENUITM_RESUME_SAVED_GAME;
                    
                    if( _currentMenu == MENUITM_RESUME_SAVED_GAME && !DataAccess.hasSavedGame() )
                        _currentMenu = MENUITM_SAVE_GAME;
                    
                    if( _currentMenu == MENUITM_SAVE_GAME && !_alive )
                        _currentMenu = MENUITM_HELP;                                        
                    
                } else if( gameAction == FIRE ){
                    handleSelectedItem();
                }
                break;
                
            case MENU_LOGIN:
                // handle user entering in their username and password
                if( gameAction == UP ){
                    if( _selectedLoginItem == LOGIN_USERNAME_SEL )
                        _selectedLoginItem = LOGIN_PASSWORD_SEL;
                    else if( _selectedLoginItem == LOGIN_PASSWORD_SEL )
                        _selectedLoginItem = LOGIN_USERNAME_SEL;
                } else if( gameAction == DOWN ){
                    if( _selectedLoginItem == LOGIN_USERNAME_SEL )
                        _selectedLoginItem = LOGIN_PASSWORD_SEL;
                    else if( _selectedLoginItem == LOGIN_PASSWORD_SEL )
                        _selectedLoginItem = LOGIN_USERNAME_SEL;
                } else if( gameAction == FIRE ){
                    // open up a text box and save their input into the text field
                    if( _selectedLoginItem == LOGIN_USERNAME_SEL )
                        LittleBlueMIDLET.activateDisplayable( _tbUsername );
                    else if( _selectedLoginItem == LOGIN_PASSWORD_SEL )
                        LittleBlueMIDLET.activateDisplayable( _tbPassword );
                }
                
                break;
            case MENU_CHOICE:
                if( gameAction == UP ){
                    if( _selectedChoiceItem == CHOICE_NO_SEL )
                        _selectedChoiceItem = CHOICE_YES_SEL;
                    else if( _selectedChoiceItem == CHOICE_YES_SEL )
                        _selectedChoiceItem = CHOICE_NO_SEL;
                } else if( gameAction == DOWN ){
                    if( _selectedChoiceItem == CHOICE_NO_SEL )
                        _selectedChoiceItem = CHOICE_YES_SEL;
                    else if( _selectedChoiceItem == CHOICE_YES_SEL )
                        _selectedChoiceItem = CHOICE_NO_SEL;
                } else if( gameAction == FIRE ){
                    // open up a text box and save their input into the text field
                    if( _selectedChoiceItem == CHOICE_YES_SEL ){
                        DataAccess.saveGame( _world.getActualLevel(), _score - _levelsScore, _world.getHero().getLives() );
                        message = "LEVEL*SAVED";
                        setState( MENU_MESSAGE );
                    }                        
                    else if( _selectedChoiceItem == CHOICE_NO_SEL )
                        setState( MENU_MENU );
                }
                break; 
            case MENU_MESSAGE:
                if( gameAction == FIRE ){
                    setState( MENU_MENU ); 
                }
                break; 
        }
        
    }
    
    /**
     *
     **/
    private void handleSelectedItem(){
        
        if( _gameState == MENU_SPLASH ){
            
        } else if( _gameState == MENU_MENU ){
            switch( _currentMenu ){
                case MENUITM_NEW_GAME:
                    initNewGame();
                    break;
                case MENUITM_RESUME_GAME:
                    setState( GAME_WAITING );
                    break;
                
                case MENUITM_RESUME_SAVED_GAME:
                    if( DataAccess.hasSavedGame() ){
                        int[] details = DataAccess.getSavedGame(); 
                        if( details != null ){
                            int level = details[0];
                            int score = details[1];
                            int lives = details[2]; 
                            
                            _score = score;
                            advanceLevel( level );
                            _world.getHero().setLives( lives );
                        }
                    }
                    break;
                    
                case MENUITM_SAVE_GAME:
                    if( DataAccess.hasSavedGame() ){
                        _currentAction = ACTION_SAVE_GAME; 
                        message = "OVERWRITE";
                        setState( MENU_CHOICE );
                    }
                    else{
                        DataAccess.saveGame( _world.getActualLevel(), _score - _levelsScore, _world.getHero().getLives() );
                        message = "LEVEL*SAVED";
                        setState( MENU_MESSAGE ); 
                    }
                    break; 
                    
                case MENUITM_HIGH_SCORES:
                    setState( MENU_HIGHSCORES );
                    break;
                    
                case MENUITM_SUBMIT_SCORE:
                    //if( _cxnManager == null )
                        _cxnManager = new ConnectionManager( GameManager.GetInstance() );
                    setState( MENU_NETSTART );
                    _cxnManager.submitHighScore();
                    break;
                
                case MENUITM_ABOUT:
                    setState( MENU_ABOUT );
                    break; 
                    
                case MENUITM_HELP:
                    setState( MENU_HELP );
                    break;
                    
                case MENUITM_SPLASH:
                    setState( MENU_SPLASH );
                    break;
                    
                case MENUITM_EXIT:
                    stop();
                    LittleBlueMIDLET.getMidlet().close();
                    break;
            }
        } else if( _gameState == MENU_HELP ){
            // if the fire key was pressed then take the user back to the
            // main menu
            setState( MENU_MENU );
       }                
    }
    
    public void render( Graphics g ){
        switch( _gameState ){
            case MENU_SPLASH:
                renderSplash( g );
                break;
            case MENU_ADVERT:
                renderAdvert( g );
                break;
            case MENU_MENU:
                renderMenu( g );
                break;
            case MENU_ABOUT:
                renderScrollingText( g, aboutText );
                break; 
            case MENU_HELP:
                renderScrollingText( g, helpText );
                break;                
            case MENU_HIGHSCORES:
                renderHighScores( g );
                break;                
            case MENU_SUBMITSCORE:
                // nothing to render
                break;                
            case MENU_LOGIN:
                renderLogin( g );
                break;                
            case MENU_NETSTART:
                renderNetworkActivity( g );
                break;                
            case MENU_NETEND:
                renderNetworkResult( g );
                break;
            case MENU_MESSAGE:
                renderMessage( g ); 
                break;
            case MENU_CHOICE:
                renderChoice( g ); 
                break; 
        }
    }
    
    /**
     *
     **/
    private void renderLogin( Graphics g ){
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        int curPosY = 20;
        int curPosX = getWidth()/2;
        
        // draw title
        printToScreen( g, getWidth()/2, curPosY, "LOGIN*DETAILS", false );
        g.setClip( 0, 0, getWidth(), getHeight() );
        g.setColor( 0xffffff );
        g.drawLine( 25, curPosY+5, getWidth() -25, curPosY+5 );
        
        // draw name details
        curPosY += 40;
        printToScreen( g, curPosX, curPosY, "USERNAME", false );
        curPosY += 20;
        if( _tmpUsername != null )
            printToScreen( g, curPosX, curPosY, _tmpUsername.toUpperCase(), false );
        else
            printToScreen( g, curPosX, curPosY, "ENTER*USERNAME", false );
        
        // draw rectangle around username field if currently selected
        if( _selectedLoginItem == LOGIN_USERNAME_SEL ){
            g.setClip( 0, 0, getWidth(), getHeight() );
            g.setColor( 0xffffff );
            g.drawRoundRect( 25, curPosY-7, getWidth()-50, 12, 15, 5 );
        }
        
        // draw password details
        curPosY += 40;
        printToScreen( g, curPosX, curPosY, "PASSWORD", false );
        curPosY += 20;
        if( _tmpPassword != null )
            printToScreen( g, curPosX, curPosY, _tmpPassword.toUpperCase(), false );
        else
            printToScreen( g, curPosX, curPosY, "ENTER*PASSWORD", false );
        
        // draw rectangle around password field if currently selected
        if( _selectedLoginItem == LOGIN_PASSWORD_SEL ){
            g.setClip( 0, 0, getWidth(), getHeight() );
            g.setColor( 0xffffff );
            g.drawRoundRect( 25, curPosY-7, getWidth()-50, 12, 15, 5 );
            
        }
        
        // draw login button
        
        // print a notification (maybe a red background)
        if( _tmpNotification != null )
            printToScreen( g, getWidth()/2, getHeight()-35, _tmpNotification, false );
        
    }
    
    /**
     *
     **/
    private void renderNetworkResult( Graphics g ){
        
        int curPosY = getHeight()/2;
        int curPosX = getWidth()/2;
        int pos = 0; 
        
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // draw network result
        if( _cxnManager != null ){
            switch( _cxnManager.getResponse() ){                
                case ConnectionManager.RES_SUCCESS:
                    pos = _cxnManager.getPosition();
                    if( pos > 0 ){
                        if( pos <= 10 )
                            printToScreen( g, curPosX, curPosY, "LEDGEND*TOP*TEN", false );
                        else if( pos <= 15 )
                            printToScreen( g, curPosX, curPosY, "GOOD*ONE*ALMOST*TOP*TEN", false );
                        else if( pos > 100 )
                            printToScreen( g, curPosX, curPosY, "YOU*SUCK*YOUR*" + pos + "TH", false );
                        else
                            printToScreen( g, curPosX, curPosY, "YOUR*" + pos + "TH", false );
                    } else{
                        printToScreen( g, curPosX, curPosY, "SCORE*UPLOADED", false );
                    }                                        
                    break;
                    
                case ConnectionManager.RES_FAIL_CXN:
                    printToScreen( g, curPosX, curPosY, "CONNECTION*FAILED", false );
                    break;
                    
                case ConnectionManager.RES_FAIL_NOSCORE:
                    printToScreen( g, curPosX, curPosY, "NO*SCORE", false );
                    break;
                    
                case ConnectionManager.RES_FAIL_USERREG:
                    curPosY -= 20;
                    printToScreen( g, curPosX, curPosY, "NOT*REGISTERED", false );
                    curPosY += 10;
                    printToScreen( g, curPosX, curPosY, "REGISTER*AT", false );
                    curPosY += 10;
                    printToScreen( g, curPosX, curPosY, "WWW.DIGITRIX.CO.NZ", false );
                    curPosY += 10;
                    printToScreen( g, curPosX, curPosY, "AND*BE*IN*TO*WIN!", false );
                    break;
            }
        } else{
            // no connection manager found
            
        }
        
        // near the buttom indicate that the user can press any key to continue
        // length of 'press fire to continue' =
        printToScreen( g, getWidth()/2, (getHeight() - getHeight()/5), _pressFireToContinue, false );
        
    }
    
    /**
     *
     **/
    private void renderNetworkActivity( Graphics g ){
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // draw status
        if( _cxnManager == null ){
            setState( MENU_NETEND );
        } else{
            switch( _cxnManager.getResponse() ){
                case ConnectionManager.RES_CONNECTING:
                    printToScreen( g, getWidth()/2, getHeight()/2, "CONNECTING", false );
                    break;
                case ConnectionManager.RES_SUCCESS_CXN:
                    printToScreen( g, getWidth()/2, getHeight()/2, "UPLOADING*SCORE", false );
                    break;
                default:
                    printToScreen( g, getWidth()/2, getHeight()/2, "TRYING*TO*CONNECT", false );
            }
        }
        
    }
    
    /**
     *
     **/
    private void renderMenu( Graphics g ){
        // clear background
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // paint logo
        g.drawImage( _imgTitle, posLogoX, posLogoY , Graphics.TOP | Graphics.LEFT );
        
        // paint up arrow
        if( _currentMenu != 0 )
            printToScreen( g, posArrowX, posArrowUpY, "$", false );
        
        // paint down arrow
        if( _currentMenu != _menuSeq.length -1 )
            printToScreen( g, posArrowX, posArrowDownY, "%", false );
        
        // print selected menu item on screen
        printToScreen( g, getWidth()/2, posMenuY, _menuStr[_menuSeq[_currentMenu]], false );
    }
    
    /**
     *
     **/
    private void renderChoice( Graphics g ){
        // clear background
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // display message
        printToScreen( g, posHalfWidth,posArrowUpY-20, message, false );         
        
        // paint up arrow
        if( _currentMenu != 0 )
            printToScreen( g, posHalfWidth, posArrowUpY, "$", false );
        
        // paint down arrow
        if( _currentMenu != _menuSeq.length -1 )
            printToScreen( g, posArrowX, posArrowDownY, "%", false );
        
        // print selected menu item on screen
        if( _selectedChoiceItem == CHOICE_YES_SEL )
            printToScreen( g, posHalfWidth, posHalfHeight, "YES", false );
        else
            printToScreen( g, posHalfWidth, posHalfHeight, "NO", false );
    }
    
    /**
     *
     **/
    private void renderHighScores( Graphics g ){
        
        // clear background
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        int posScore = 35;
        int[] scores = null;
        
        // draw title
        printToScreen( g, getWidth()/2, 20, "YOUR*HIGH*SCORES", false );
        g.setClip( 0, 0, getWidth(), getHeight() );
        g.setColor( 0xffffff );
        g.drawLine( 25, 25, getWidth() -25, 25 );
        
        scores = DataAccess.getHighScores();
        
        if( scores == null ){
            printToScreen( g, getWidth()/2, posScore, "CURRENTLY*NO*SCORES", false );
        } else{
            for( int i=0; i<scores.length; i++ ){
                int score = scores[i];
                
                if( score > 0 ){
                    printToScreen( g, getWidth()/2, posScore, Integer.toString( score ), false );
                    posScore += 15;
                }
            }
        }
        
    }
    
    /**
     *
     **/
    private void renderAdvert( Graphics g ){
        Image ad = DataAccess.getClientImage();
        
        if( ad == null ){
            setState( MENU_MENU ); 
            return; 
        }

        // clear screen 
        g.setColor( BGCOL_SPLASH );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // get x coordinate
        int x = (getWidth()-ad.getWidth())/2;
        if( x < 0 )
            x = 0; 
        
        // get x coordinate
        int y = (getHeight()-ad.getHeight())/2;
        if( y < 0 )
            y = 0; 
        
        // paint ad 
        g.drawImage( ad, x, y, Graphics.TOP | Graphics.LEFT );
        
        // print whatever message         
//        y = (getHeight()-ad.getHeight())/2;
//        if( y >= 20 ){            
//            printToScreen( g, getWidth()/2, getHeight()-12, "PRESENT", false );
//        }
        
    }
    
    /**
     *
     **/
    private void renderScrollingText( Graphics g, String[] scrollingText ){
        // clear screen 
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // if the bottom line is above the top of the screen, then reset the variable/pointer
        // posScrollingY to the screensHeight - 10; 
        if( posScrollingY + (scrollingText.length * 10) < 10 )
            posScrollingY = getHeight() -10; 
        
        for( int i = 0; i < scrollingText.length; i ++ ){
            printToScreen( g, getWidth()/2, posScrollingY + (i*15), scrollingText[i], false );
        }
        
        posScrollingY -= 1; 
    }  
    
    /**
     *
     **/
    private void renderSplash( Graphics g ){
        // clear screen
        g.setColor( BGCOL_SPLASH );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        // paint splash
        g.drawImage( _imgSplash, (getWidth()-_imgSplash.getWidth())/2, posSplashY, Graphics.TOP | Graphics.LEFT );
        
        // print text at bottom of picture (if room available)
//        if( DataAccess.getClientImage() == null )
//            printToScreen( g, getWidth()/2, posSplashTextY, "PRSENTS", false );
//        else
//            printToScreen( g, getWidth()/2, posSplashTextY, "WITH", false );
    }     
    
    /**
     *
     **/
    private void renderMessage( Graphics g ){
        g.setColor( BGCOL_MENU );
        g.fillRect( 0, 0, getWidth(), getHeight() );
        
        // message
        printToScreen( g, getWidth()/2, getHeight()/2, message, false );        
    }
    
    public String getTempUsername(){
        return _tmpUsername;
    }
    
    public void setTempUsername( String value ){
        _tmpUsername = value;
    }
    
    public String getTempPassword(){
        return _tmpPassword;
    }
    
    public void setTempPassword( String value ){
        _tmpPassword = value;
    }
    
    public String getTempNotification(){
        return _tmpNotification;
    }
    
    public void setTempNotification( String value ){
        _tmpNotification = value;
    }
    
    public void resetMenu(){
        _currentMenu = MENUITM_NEW_GAME;
        _selectedLoginItem = LOGIN_USERNAME_SEL;
        _tmpNotification = null;
    }
    
    public int getSelectedLogin(){
        return _selectedLoginItem;
    }
}
}
