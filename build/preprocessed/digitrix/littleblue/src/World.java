/*
 * World.java
 *
 * Created on 4 April 2006, 21:27
 *
 */

package digitrix.littleblue.src;

import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author Joshua Newnham
 *  http://www.massey.ac.nz/~jnewnham
 *  http://www.digitrix.co.nz
 */
public class World extends LayerManager {
    
    // becuase we include our sprites and interactive tiles in the
    // map file itself we must manually identify these in order to add them
    // appropreitely to the world. Be a good idea to place these into the level data
    // file, therefore multiple tile maps could be used with the same World class
    // TILE TYPES
    public static int TILE_SPIKE = 31;
    public static int TILE_GREY_BLOCK = 49;
    public static int TILE_GOLD_BLOCK = 50;
    public static int TILE_BLUE_BLOCK = 52;
    public static int TILE_GRASS1 = 57;
    public static int TILE_GRASS2 = 58;
    public static int TILE_FLOWER1 = 59;
    public static int TILE_MUSHROOM = 60;
    public static int TILE_FLOWER2 = 65;
    public static int TILE_SPRING = 66;
    public static int TILE_COIN = 67;
    public static int TILE_CHECK_POINT = 68;
    public static int TILE_GHOST = 69;
    public static int TILE_BAT = 70;
    public static int TILE_FLY = 71;
    public static int TILE_LITTLEBLUE = 72;
    
    // image paths
    private final String IMGPATH_BG_DEFAULT = "/digitrix/littleblue/res/bg0.png";
    private final int BG_COL_DEFAULT = 0x314273;
    private final String IMGPATH_BG_0 = "/digitrix/littleblue/res/bg0.png";
    private final int BG_COL_0 = 0x314273;
    private final String IMGPATH_BG_1 = "/digitrix/littleblue/res/bg1.png";
    private final int BG_COL_1 = 0x927430;
    private final String IMGPATH_BG_2 = "/digitrix/littleblue/res/bg2.png";
    private final int BG_COL_2 = 0xF69A2F;
    private final int BG_COUNT = 3;
    
    private final String IMGPATH_BLOCKS = "/digitrix/littleblue/res/blocks.png";
    private final String IMGPATH_GAME_EFFECTS = "/digitrix/littleblue/res/gameeffects.png";
    private final String IMGPATH_NONINTERACTIVE_PROPS = "/digitrix/littleblue/res/flowers.png";
    private final String IMGPATH_CHECKPOINT = "/digitrix/littleblue/res/checkpoint.png";
    private final String IMGPATH_COIN = "/digitrix/littleblue/res/coin.png";
    private final String IMGPATH_SINGLECOIN = "/digitrix/littleblue/res/singlecoin.png";
    private final String IMGPATH_SPRING = "/digitrix/littleblue/res/spring.png";
    private final String IMGPATH_BADGUYS = "/digitrix/littleblue/res/badguys.png";
    private final String IMGPATH_LITTLEBLUE = "/digitrix/littleblue/res/littleblue.png";
    private final String IMGPATH_TILES = "/digitrix/littleblue/res/tiles.png";
    private final String IMGPATH_LIFE = "/digitrix/littleblue/res/icon.png";
    private final String IMGPATH_NUMS = "/digitrix/littleblue/res/nums/";
    
    private final String DATPATH_LEVELS = "/digitrix/littleblue/res/levels.dat";
    
    private final int PADDINGX = 5;
    private final int PADDINGY = 5;
        
    public static int TILE_WIDTH = 32;
    public static int TILE_HEIGHT = 32;
    
    // world attributes
    public static final int GRAVITY = 1;
    public static final int SPIKE_DAMAGE = 3;
    
    private static World _instance = null;
    
    private TiledLayer _tiledWorld = null;
    
    private int _screenWidth = 0;
    private int _screenHeight = 0;
    
    private int _currentViewX = 0;
    private int _currentViewY = 0; 
    private float _pixelPerMS = 0.18f; 
    private float _panPixelsToMove; 
    private int _panPaddingX = 0;
    private int _panPaddingY = 0; 
    private int _viewX = 0;
    private int _viewY = 0;
    private int _backgroundX = 0;
    
    private int _currentLevel = 0;
    private int _requiredCoins = 0;  // number of coins the player must collect (relative to level)
    private int _coinsCollected = 0; // number of coins the player has collected
    
    // remember the current score and current number of lives left, this is so that when update is called (for either
    // score or lives) and the value hasn't changed we don't have to re-generate the image thus saving some
    // processing.
    private int _totalCoinsCollected = 0;
    private int _currentLives = -1;
    
    private int _worldBGColour = BG_COL_DEFAULT; // colour used to fill the screen with (dependant on what background image is loaded)
    private int _currentBackground = -1; // used so if the same background is selected (when randomly selecting one) we don't reload it'
    private Random _rand = null;
    
    // game images
    private Image _imgBackGround = null;
    private Image _imgTiles = null;
    private Image _imgLittleBlue = null;
    private Image _imgBadGuys = null;
    private Image _imgSpring = null;
    private Image _imgCoin = null;
    private Image _imgSingleCoin = null;
    private Image _imgCheckPoint = null;
    private Image _imgFlowers = null;
    private Image _imgGameEffects = null;
    private Image _imgBlocks = null;
    
    // game stat images
    private Image _imgLife = null;
    private Image[] _imgNums = null;
    //private Image _imgScore = null;
    private Image _imgLives = null;
    
    private Vector _sprites = null;
    private Vector _effects = null;
    
    private Sprite _background = null;
    private Hero _littleBlue = null;
    
    private boolean go = false;
    
    /** Creates a new instance of World */
    public World( int screenWidth, int screenHeight ) throws Exception {
        
        _instance = this;
        
        _sprites = new Vector();
        _effects = new Vector();
        
        _screenWidth = screenWidth;
        _screenHeight = screenHeight;
        
        _currentLevel = 0;
        
        // set up panning padding
        _panPaddingX = 10; 
        _panPaddingY = 10; 
        
        if( !initImages() )
            throw new Exception( "World.World: Error while trying to load files" );
        
    }
    
    public static World GetInstance(){
        return _instance;
    }
    
    public boolean nextLevel(){
        boolean hasLevel = false; 
        
        try{
            hasLevel = loadLevel();
        }
        catch( Exception e ){} 
        
        return hasLevel; 
    }
    
    public boolean loadLevel( int level ){
        // set level 
        _currentLevel = level;
        
        boolean hasLevel = false; 
        
        try{
            hasLevel = loadLevel();
        }
        catch( Exception e ){} 
        
        return hasLevel; 
    }
    
    public boolean loadLevel( ) throws Exception {
        int tilesHigh = 0;
        int tilesWide = 0;                
        
        // load level data
        try{
            InputStream is = null;
            is = this.getClass().getResourceAsStream( DATPATH_LEVELS );
            boolean foundLevel = false;
            
            // loop through until we get the correct level
            int b = is.read();
            while ( b != -1 && !foundLevel ){
                // level names starts with a ! and terminates with a ~ character
                // The readString method wraps p reading the string from the stream
                if ( b == '!' ){
                    // got a start of a level name char, read the name string
                    String level = readString( is, (byte)'~').toLowerCase();
                    
                    if( Integer.parseInt(level) == _currentLevel )
                        foundLevel = true;
                }
                
                // if the level hasn't been found yet then continue reading
                if ( !foundLevel ){
                    b = is.read();
                }                
            }
            
            if( !foundLevel )
                return false; 
            
            try{
                clearLevel();   
            }
            catch( Exception e ){}
            
            // load the level
            byte[] buffer = new byte[2];
            is.read(buffer);
            String ths = new String(buffer, 0, 2);
            is.read(buffer);
            String tws = new String(buffer, 0, 2);
            tilesHigh = Integer.parseInt(tws);
            tilesWide = Integer.parseInt(ths);
            
            // tiles must be of size 32, 32
            _tiledWorld = new TiledLayer( tilesWide,tilesHigh, _imgTiles, TILE_WIDTH, TILE_HEIGHT );
            
            // Next you read all the tiles into the tilemap.
            int bytesRead=0;
            
            // set a place holder for the baddie and powerups
            Baddie baddie;
            Prop prop;
            
            for (int ty=0; ty < tilesHigh; ty++) {
                for (int tx = 0; tx < tilesWide; tx++) {
                    bytesRead = is.read(buffer);
                    if (bytesRead > 0) {
                        tws = new String(buffer, 0, 2).trim();
                        
                        byte c = Byte.parseByte(tws);
                        
                        // *** LITTLE BLUE ***
                        if ( c == TILE_LITTLEBLUE ) {
                            if( _littleBlue == null )
                                _littleBlue = new Hero( _imgLittleBlue, 26, 26 );
                            
                            _littleBlue.initActor( (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _tiledWorld.setCell( tx, ty,  0 ); // add a blank tile to our tiled world
                        }
                        
                        // *** BAD GUYS ***
                        else if ( c == TILE_GHOST ) {
                            baddie = new Baddie( _imgBadGuys, 24, 26, Baddie.BADDIE_TYPE_GHOST );
                            baddie.initActor( (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( baddie );
                            _tiledWorld.setCell( tx, ty,  0 ); // add a blank tile to our tiled world
                        } else if ( c == TILE_BAT ) {
                            baddie = new Baddie( _imgBadGuys, 24, 26, Baddie.BADDIE_TYPE_BAT );
                            baddie.initActor( (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( baddie );
                            _tiledWorld.setCell( tx, ty,  0 ); // add a blank tile to our tiled world
                        } else if ( c == TILE_FLY ) {
                            baddie = new Baddie( _imgBadGuys, 24, 26, Baddie.BADDIE_TYPE_FLY );
                            baddie.initActor( (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( baddie );
                            _tiledWorld.setCell( tx, ty,  0 ); // add a blank tile to our tiled world
                        }
                        
                        // *** EFFECTS AND OTHER THINGS ***
                        else if ( c == TILE_SPRING ) {
                            prop = new Prop( _imgSpring, 28, 19 );
                            prop.initProp( Prop.PROP_TYPE_SPRING, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_COIN ) {
                            _requiredCoins++; // incremement how many coins the player needs to collect
                            prop = new Prop( _imgCoin, 8, 8 );
                            // work out 'y' ((ty*TILE_HEIGHT)+(TILE_HEIGHT-10)) to place the coin near the ground instead of in the middle of where the tile is meant to be
                            prop.initProp( Prop.PROP_TYPE_COIN,(tx*TILE_WIDTH), ((ty*TILE_HEIGHT)+(TILE_HEIGHT/2)) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_CHECK_POINT ) {
                            prop = new Prop( _imgCheckPoint );
                            prop.initProp( Prop.PROP_TYPE_CHECKPOINT,(tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_FLOWER1 ) {
                            prop = new Prop( _imgFlowers, 32, 28 );
                            prop.initProp( Prop.PROP_TYPE_FLOWER1, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_FLOWER2 ) {
                            prop = new Prop( _imgFlowers, 32, 28 );
                            prop.initProp( Prop.PROP_TYPE_FLOWER2, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_GRASS1 ) {
                            prop = new Prop( _imgFlowers, 32, 28 );
                            prop.initProp( Prop.PROP_TYPE_GRASS1, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_GRASS2 ) {
                            prop = new Prop( _imgFlowers, 32, 28 );
                            prop.initProp( Prop.PROP_TYPE_GRASS2, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if ( c == TILE_MUSHROOM ) {
                            prop = new Prop( _imgFlowers, 32, 28 );
                            prop.initProp( Prop.PROP_TYPE_MUSHROOM, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if( c == TILE_GREY_BLOCK ){
                            prop = new Prop( _imgBlocks, 32, 32 );
                            prop.initProp( Prop.PROP_TYPE_GREY_BLOCK, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if( c == TILE_GOLD_BLOCK ){
                            prop = new Prop( _imgBlocks, 32, 32 );
                            prop.initProp( Prop.PROP_TYPE_YELLOW_BLOCK, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else if( c == TILE_BLUE_BLOCK ){
                            prop = new Prop( _imgBlocks, 32, 32 );
                            prop.initProp( Prop.PROP_TYPE_BLUE_BLOCK, (tx*TILE_WIDTH), (ty*TILE_HEIGHT) );
                            _sprites.addElement( prop );
                            _tiledWorld.setCell( tx, ty,  0 );
                        } else{
                            // all else fails, try and add it as a standard tile
                            _tiledWorld.setCell( tx, ty,  c );
                        }
                    }
                }
            }
            
        }
        
        catch (Exception e) {
            e.printStackTrace();
        }
        
        // add our player
        // insert the player (just to make sure he's on top of all the baddies)
        if( _littleBlue != null ){
            insert( _littleBlue, 0 );
        }
        
        // add our sprites/baddies
        Enumeration sprites = _sprites.elements();
        try{
            for( int i=0; ; i++ ){
                Sprite nextSprite = (Sprite)sprites.nextElement();
                if( nextSprite instanceof Prop ){
                    if( ((Prop)nextSprite).getType() < 20 )
                        insert( nextSprite, 0 );
                    else
                        append( nextSprite );
                } else
                    append( nextSprite );
            }
        } catch( Exception e ){}
        
        // add our tiles to the world
        append( _tiledWorld );
        
        // increment to the next level so next time loadLevel is called we load the next
        // level
        _currentLevel++;
        
        // add some empty game effects, done last so they're on top of all of the other sprites
        initGameEffects();
        
        // now randomly select a background image for this level (to keep things interesting)
        initBackground();
        
        // return true to indicate that a level has been loaded and everything has loaded 
        // successfully 
        return true; 
        
    }
    
    public void paintBackGround( Graphics g, int x, int y, int screenWidth, int screenHeight ){
        g.drawImage( _imgBackGround, _backgroundX, screenHeight - _imgBackGround.getHeight(), Graphics.LEFT | Graphics.TOP );        
        
        // 2. draw stat bar
        // 2a. draw number of lifes left
        g.drawImage( _imgLife, PADDINGX, PADDINGY, Graphics.LEFT | Graphics.TOP );
        if( _imgNums != null )
            g.drawImage( _imgNums[_littleBlue.getLives()], PADDINGX + _imgLife.getWidth() + 5, PADDINGY + (_imgLife.getHeight()/3), Graphics.LEFT | Graphics.TOP );                 
        
        // 2c. take 2
        if( _imgNums != null ){
            // draw score
            drawScore( g, screenWidth, screenHeight );
            // draw number of coins left to collect
            drawCoinsLeft( g, screenWidth, screenHeight );
        }
    }
    
    public void paint( Graphics g, int x, int y, int screenWidth, int screenHeight ){
        g.drawImage( _imgBackGround, _backgroundX, screenHeight - _imgBackGround.getHeight(), Graphics.LEFT | Graphics.TOP );
        
        // paint the other layers
        super.paint( g, x, y );
        
        // 2. draw stat bar
        // 2a. draw number of lifes left
        g.drawImage( _imgLife, PADDINGX, PADDINGY, Graphics.LEFT | Graphics.TOP );
        if( _imgNums != null )
            g.drawImage( _imgNums[_littleBlue.getLives()], PADDINGX + _imgLife.getWidth() + 5, PADDINGY + (_imgLife.getHeight()/3), Graphics.LEFT | Graphics.TOP );
        
        // 2c. draw score
//        if( _imgScore != null ){
//            g.drawImage( _imgScore, (screenWidth - (PADDINGX + _imgScore.getWidth())), PADDINGY + (_imgLife.getHeight()/2) - (_imgScore.getHeight()/2), Graphics.LEFT | Graphics.TOP );
//        }
        // 2c. take 2
        if( _imgNums != null ){
            // draw score
            drawScore( g, screenWidth, screenHeight );
            // draw number of coins left to collect
            drawCoinsLeft( g, screenWidth, screenHeight );
        }
        
    }
    
    private void drawScore( Graphics g, int screenWidth, int screenHeight ){
        String score = Integer.toString( GameManager.GetInstance().getScore() );
        score = reverseString( score );
        
        int posY = PADDINGY + (_imgLife.getHeight()/2) - 3;
        int posX = screenWidth - (PADDINGX + 10);
        
        int x = 0;
        int num = 0;
        
        for( int i = 0; i <= 4; i++ ){
            
            if( i <= score.length()-1 ){
                // get number
                num = Integer.parseInt(score.substring( i, i+1 ));
            } else{
                // add a zero
                num = 0;
            }
            // add to our score image
            x = (4-i) * 6;
            
            try{
                g.drawImage( _imgNums[num], posX-(i*6), posY, Graphics.TOP | Graphics.LEFT );
            } catch( Exception e ){ e.printStackTrace(); }
        }
    }
    
    private void drawCoinsLeft( Graphics g, int screenWidth, int screenHeight ){
        String coinsLeft = Integer.toString( _requiredCoins - _coinsCollected );        
        
        int posY = PADDINGY + (_imgLife.getHeight()/2) - 3;
        int posX = screenWidth/2 - (PADDINGX + 10);
        
        int x = 0;
        int num = 0;                
        
        g.drawImage( _imgSingleCoin, posX, posY, Graphics.TOP | Graphics.LEFT  );
        posX += 10; 
        
        for( int i = 0; i < coinsLeft.length(); i++ ){
            
            if( i <= coinsLeft.length()-1 ){
                // get number
                num = Integer.parseInt(coinsLeft.substring( i, i+1 ));
            } else{
                // add a zero
                num = 0;
            }
            // add to our score image
            x = (4-i) * 6;
            
            try{
                g.drawImage( _imgNums[num], posX+(i*6), posY, Graphics.TOP | Graphics.LEFT );
            } catch( Exception e ){ e.printStackTrace(); }
        }
    }
    
    public int getBGColour(){
        return _worldBGColour;
    }
    
    /**
     * Called by the Game Manager at every cycle to update the viewport in that the user is always in the center of the
     * screen (unless the end of the level has been hit) as well as implement our parallax scrolling for the
     * background image :)
     **/
    public void updateWorld( long elapsedTime ){
        
        // update the viewport window
        if ( _littleBlue == null ){
            _viewX = 0;
            _viewY = 0;
        } else{
            // if the player is invisible then don't update the viewpoint as we dont want to follow a blank screen
            if( _littleBlue.getState() == Actor.STATE_DYING || _littleBlue.getState() == Actor.STATE_DEAD )
                return;
            
            _panPixelsToMove += _pixelPerMS * elapsedTime; 
            int wholePixels = (int)_panPixelsToMove; 
            
            // find x viewpoint
            // TODO; Add panning here... 
            _viewX = _littleBlue.getX() + _littleBlue.getWidth()/2 - _screenWidth/2; // optimal view
            // now make sure the view does not run off the map
            if ( _viewX < 0 )
                _viewX = 0;
            else if ( (_viewX + _screenWidth) > _tiledWorld.getWidth() )
                _viewX = _tiledWorld.getWidth() - _screenWidth;
            
            // adjust the current move slightly towards the ideal view point+            
            if( _currentViewX < _viewX ){
                if( (_viewX - _currentViewX) < _panPaddingX )
                    _currentViewX = _viewX; 
                else
                    _currentViewX += wholePixels;
            }
            else if( _currentViewX > _viewX ){
                if( (_currentViewX - _viewX) < _panPaddingY )
                    _currentViewX = _viewX; 
                else
                    _currentViewX -= wholePixels;                 
            }
            
            // and finally lets update viewY
            // TODO: Add panning here
            _viewY = _littleBlue.getY() + _littleBlue.getHeight()/2 - (_screenHeight/3)*2;
            // now lets make sure the view does not run off the map
            if ( _viewY < 0 )
                _viewY = 0;
            else if ( ( _viewY + _screenHeight ) > _tiledWorld.getHeight() )
                _viewY = _tiledWorld.getHeight() - _screenHeight;
            
            if( _currentViewY < _viewY ){
                 if( (_viewY - _currentViewY) < _screenHeight/8 )
                    _currentViewY = _viewY; 
                else
                    _currentViewY += wholePixels;                                 
            }
            else if( _currentViewY > _viewY ){
                if( (_currentViewY - _viewY) < _screenHeight/8 )
                    _currentViewY = _viewY; 
                else
                    _currentViewY -= wholePixels;
            }
            
            // take away the pixels that were moved
            _panPixelsToMove = _panPixelsToMove - wholePixels; 
        }
        
        //setViewWindow( _viewX, _viewY, _screenWidth, _screenHeight );
        setViewWindow( _currentViewX, _currentViewY, _screenWidth, _screenHeight );                
        
        // now work out the position for our background image to demonstrate parallax scrolling
        int _backgroundX = 0;
        if( _viewX == 0 )
            _backgroundX = 0;
        else if( _viewX == ( _screenWidth - _tiledWorld.getWidth() ) )
            _backgroundX = _screenWidth - _imgBackGround.getWidth();
        else
            _backgroundX = _viewX * ( _screenWidth - _imgBackGround.getWidth() ) / ( _screenWidth - _tiledWorld.getWidth() );
        
    }
    
    /**
     * Used when updading the logic of the game; if the game is panning to the hero when he/she is 
     * not in view then this method is used to determine in the hero is display on screen. If not then 
     * give littleblue a chance and don't update the baddies
     **/
    public boolean isHeroInView(){
        
        int difX = getHero().getX() - _currentViewX; 
        int difY = getHero().getY() - _currentViewY; 
        
        System.out.println( "dif x = " + difX + " dif y = " + difY ); 
        
        // check x
        if( difX < 0 )
            return false;
        else if( difX > _screenWidth )
            return false;
        else if( difY < 0 )
            return false;
        else if( difY > _screenHeight )
            return false; 
        
        return true; 
    }
    
    public Hero getHero(){
        return _littleBlue;
    }
    
    /**
     *
     **/
    public boolean nextHorizontalTileEmpty( Actor actor ){
        int nextX = actor.getX() + (int)actor.getVelocityX();
        int nextY = actor.getY() + (int)actor.getVelocityY();
        
        // ensure that the next position is in bounds
        if( nextX <= 0 || nextX >= getWidth() || nextY <= 0 || nextY >= getHeight() )
            return true;
        
        return getTile( pixelsXToTiles(nextX), pixelsXToTiles(nextY)+1 ) == 0; // pixelsXToTiles(nextY)+1 (+1 to look beneath the actor)
    }
    
    /**
     * Gets the tile that a Sprites collides with. Only the
     * Sprite's X or Y should be changed, not both. Returns null
     * if no collision is detected.
     */
    public int[] getTileCollision(Sprite sprite,
            int newX, int newY) {
        
        int[] point = null;
        
        int fromX = Math.min(sprite.getX(), newX);
        int fromY = Math.min(sprite.getY(), newY);
        int toX = Math.max(sprite.getX(), newX);
        int toY = Math.max(sprite.getY(), newY);
        
        // get the tile locations
        int fromTileX = pixelsXToTiles(fromX);
        int fromTileY = pixelsYToTiles(fromY);
        int toTileX = pixelsXToTiles(
                toX + sprite.getWidth() - 1 );
        int toTileY = pixelsYToTiles(
                toY + sprite.getHeight() - 1 );
        
        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= pixelsXToTiles(getWidth()) || y < 0 || y >= pixelsYToTiles(getHeight()) ||
                        (getTile(x, y) != 0) ) {
                    // collision found, return the tile
                    point = new int[2];
                    point[0] = x;
                    point[1] = y;
                    
                    if( newX > sprite.getX() ){ // moving right
                        //return point;
                    }
                }
            }
        }
        
        return point;
    }
    
    public void resetGame(){
        try{
            // place in a try-catch becuase if this is the first time then 
            // it may throw an exception if one of the collections is not initilized
            clearLevel(); 
        }
        catch( Exception e ){}
        _requiredCoins = 0; 
        _coinsCollected = 0;
        _totalCoinsCollected = 0; 
        _currentLevel = 0;       
    }
    
    public int getCurrentLevel(){
        return _currentLevel; 
    }
    
    public int getActualLevel(){
        return _currentLevel - 1; 
    }
    
    public int getViewX(){
        return _viewX;
    }
    
    public int getViewY(){
        return _viewY;
    }
    
    public int getScreenWidth(){
        return this._screenWidth;
    }
    public int getScreenHeight(){
        return this._screenHeight;
    }
    
    public int pixelsXToTiles( int pixels ){
        return pixels/_tiledWorld.getCellWidth();
    }
    
    public int pixelsYToTiles( int pixels ){
        return pixels/_tiledWorld.getCellHeight();
    }
    
    public int tilesXToPixels( int tiles ){
        return tiles * _tiledWorld.getCellWidth();
    }
    
    public int tilesYToPixels( int tiles ){
        return tiles * _tiledWorld.getCellHeight();
    }
    
    public int getWidth(){
        return _tiledWorld.getWidth();
    }
    
    public int getHeight(){
        return _tiledWorld.getHeight();
    }
    
    public int getTile( int x, int y ){
        try{
            return _tiledWorld.getCell(x, y);
        } catch( Exception e ){
            // out of bounds therefore return something greater than 0 to indicate that
            // a collision was detected
            return 1;
        }
    }
    
    /**
     * Returns the number of coins the user has to collect before the user can advance to the next
     * level
     **/
    public int coinsLeft(){
        return ( _requiredCoins - _coinsCollected );
    }
    
    /**
     * Called every time the user collects a coin; if coins left is equal to zero then scroll through the
     * props (looking for the checkpoint) and sets its visibility to true
     **/
    public void coinCollected(){
        _coinsCollected++;
        _totalCoinsCollected++;
        
        //updateScoreImage( 1 );
    }
    
    public void coinCollected( int value ){
        _coinsCollected += value;
        _totalCoinsCollected += value;
        
        //updateScoreImage( value );
    }
    
    public Enumeration getWorldSprites(){
        return _sprites.elements();
    }
    
    public Enumeration getWorldEffects(){
        return _effects.elements();
    }
    
    /**
     * Queue
     **/
    public void addGameEffect( int x, int y, int type ){
        // check if we have any elements in the queue
        GameEffect effect = null;
        
        if( _effects.size() <= 0 ){
            effect = new GameEffect( _imgGameEffects );
            insert( effect, 0 ); // add to our world
        } else{
            // test if the last item in the vector is invisible, if so then use this one
            effect = (GameEffect)_effects.lastElement();
            if( !effect.isVisible() ){
                // leave the effect where it is and generate a new one (just change the reference
                // to a new object)
                effect = new GameEffect( _imgGameEffects );
                insert( effect, 0 ); // add to our world
            }
            // otherwise (if we're going to use this game effect) then
            // remove it from the queue so we can place it at the top/start of
            // the queue
            else{
                _effects.removeElement( effect );
            }
        }
        
        // add it at the front of the queue
        _effects.insertElementAt( effect, 0 );
        
        // initilise the effect
        effect.init( x, y, type );
        
    }
    
    private boolean initImages(){
        try{
            _imgTiles = Image.createImage( IMGPATH_TILES );
            _imgLittleBlue = Image.createImage( IMGPATH_LITTLEBLUE );
            _imgBadGuys = Image.createImage( IMGPATH_BADGUYS );
            _imgSpring = Image.createImage( IMGPATH_SPRING );
            _imgCoin = Image.createImage( IMGPATH_COIN );
            _imgSingleCoin = Image.createImage( IMGPATH_SINGLECOIN );
            _imgCheckPoint = Image.createImage( IMGPATH_CHECKPOINT );
            _imgFlowers = Image.createImage( IMGPATH_NONINTERACTIVE_PROPS );
            _imgGameEffects = Image.createImage( IMGPATH_GAME_EFFECTS );
            _imgBlocks = Image.createImage( IMGPATH_BLOCKS );
            _imgLife = Image.createImage( IMGPATH_LIFE );
            initNumberImages();
        } catch( Exception e ){
            //e.printStackTrace();
            return false;
        }
        
        // advance the player score to 0 (starting from -1)
        
        return true;
    }
    
    /**
     * Called when this class is first instantiated to load the number images
     * into the number array
     **/
    private void initNumberImages(){
        if( _imgNums != null )
            return;
        
        _imgNums = new Image[11];
        
        try{
            for( int i = 0; i < 10; i++ ){
                // load i
                _imgNums[i] = Image.createImage( IMGPATH_NUMS + i + ".png" );
            }
        } catch( Exception e ){
            _imgNums = null;
        }
    }
    
    /**
     * Called by the loadLevel method to add some empty effects into our effects
     * vector; the reason for this is that object creation is an expensive process and
     * hopefully we get away with just doing it once
     **/
    private void initGameEffects(){
        GameEffect effect = null;
        
        effect = new GameEffect( _imgGameEffects );
        _effects.addElement( effect );
        insert( effect, 0 );
        
        effect = new GameEffect( _imgGameEffects );
        _effects.addElement( effect );
        insert( effect, 0 );
        
        effect = new GameEffect( _imgGameEffects );
        _effects.addElement( effect );
        insert( effect, 0 );
        
        effect = new GameEffect( _imgGameEffects );
        _effects.addElement( effect );
        insert( effect, 0 );
    }
    
    /**
     * Called each time loadLevel is called; to keep things interesting I decided to
     * randomly selected a background image for every new level; keeps the player
     * interested?
     **/
    private void initBackground(){
        if( _rand == null )
            _rand = new Random();
        
        int i = _rand.nextInt( BG_COUNT ); // select a random number between 0 and 2
        
        // if random background is the same as the one currently displaying then return
        // and use the current settings
        if( i == _currentBackground )
            return;
        
        _currentBackground = i;
        
        String bgPath = IMGPATH_BG_DEFAULT;
        
        switch( _currentBackground ){
            case 0:
                bgPath = IMGPATH_BG_0;
                _worldBGColour = 0x314273;
                break;
            case 1:
                bgPath = IMGPATH_BG_1;
                _worldBGColour = 0x927430;
                break;
            case 2:
                bgPath = IMGPATH_BG_2;
                _worldBGColour = 0xF69A2F;
                break;
        }
        try{
            _imgBackGround = Image.createImage( bgPath );
        } catch( Exception e ){
            e.printStackTrace();
        }
    }
    
    /**
     * Called everytime the player collects coins, this method incrememnts the variable
     * _totalCoinsCollected and re-generates the score image...
     * score = totalCoinsCollected x 25 (coins are worth 25 points each)
     * NB: Replaced method of displaying score becuase this method
     *     significantly reduced fps (from 19 to 14-15) when called i.e. when the player
     *     was collecting coins.
     **/
//    private void updateScoreImage( int value ){
//        // create a place holder for our score
//        if( _imgScore == null ){
//            _imgScore = Image.createImage( 30, 7 );
//        }
//
//
//        String score = Integer.toString( _totalCoinsCollected * 25 );
//        //score = reverseString( score );
//
//        int x = 0;
//
//        try{
//            Graphics g = _imgScore.getGraphics();
//            g.setColor( 0 );
//            g.drawRect( 0, 0, _imgScore.getWidth(), _imgScore.getHeight() );
//
//            for( int i = 0; i <= 4; i++ ){
//                Image result = Image.createImage( 6, 7 );
//
//                if( i <= score.length()-1 ){
//                    // get number
//                    int num = Integer.parseInt(score.substring( i, i+1 ));
//                    result.getGraphics().drawImage( _imgNums, -(num*6), 0, Graphics.TOP | Graphics.LEFT );
//                } else{
//                    // add a zero
//                    result.getGraphics().drawImage( _imgNums, 0, 0, Graphics.TOP | Graphics.LEFT );
//                }
//                // add to our score image
//                x = (4-i) * 6;
//
//                g.drawImage( result, x, 0, Graphics.TOP | Graphics.LEFT );
//            }
//        } catch(Exception ex){
//            System.out.println( ex.toString() );
//            ex.printStackTrace();
//        }
//
//        // make the color (xxx) transparent
//        // (we have to do this becuase mutable images do not carry their transparent property)
//        try{
//            int imgsize=30*7;   // size of required RGB data buffer
//            int[] rgbdata=new int[imgsize];   // initialize a buffer
//            _imgScore.getRGB(rgbdata,0, 30,0,0,30,7); // read image data into the buffer
//            for(int i=0;i<imgsize;i++)
//                if(rgbdata[i]==0x0000FF)    // all white pixels are made transparent by making the highest order byte zero.
//                    rgbdata[i]=0x00FFFFFF;
//
//            _imgScore = Image.createRGBImage(rgbdata,30, 7,true);  // creates a mutable image b from the RGB data
//        } catch( Exception e2 ){
//            e2.printStackTrace();
//        }
//    }
    
    private void clearLevel(){
        
        // reset all viewing variables
        //_viewX = 0;
        //_viewY = 0;
        _currentViewX = 0;
        _currentViewY = 0;        
        _panPixelsToMove = 0;
        
        // remove all sprites of the layer
        Enumeration gameSprites = _sprites.elements();
        try{
            for( int i=0; ; i++ ){
                remove( (Sprite)gameSprites.nextElement() );
            }
        } catch( Exception e ){}
        
        // remove all effects and projectiles
        Enumeration effectsSprites = _effects.elements();
        try{
            for( int i=0; ; i++ ){
                remove( (GameEffect)effectsSprites.nextElement() );
            }
        }catch( Exception e ){}
        
        // clear sprite collections/holders
        _sprites.removeAllElements();
        _effects.removeAllElements();
        
        // remove the hero from the layer manager
        remove( _littleBlue );
        
        // remove the tiled world from this (the layer manager)
        remove( _tiledWorld );
        
        // reset number of coins required to complete the level
        _requiredCoins = 0; 
        
    }
    
    private String readString(InputStream is, byte terminator) {
        try {
            StringBuffer sb = new StringBuffer();
            int b = is.read();
            while (b != -1) {
                if (b == terminator) {
                    return sb.toString();
                } else
                    sb.append((char)b);
                
                b = is.read();
            }
            
            return null;
        }
        
        catch(IOException e) {
            e.printStackTrace();
            return null;
        }
        
    }
    
    private String reverseString( String value ){
        char[] split = value.toCharArray();
        String result = "";
        
        for( int i = split.length-1; i >= 0; i-- )
            result += split[i];
        
        return result;
    }
    
}
