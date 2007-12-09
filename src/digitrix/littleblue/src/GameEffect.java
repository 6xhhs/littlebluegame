/*
 * GameEffect.java
 *
 * Created on 15 May 2006, 22:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

/**
 *
 * @author Josh
 */
public class GameEffect extends Sprite {
    
    /** Creates a new instance of GameEffect */
    public static final int EFFECT_BADDIE_BOOM = 0;
    public static final int EFFECT_SMOKE = 1;
    public static final int EFFECT_HERO_BOOM = 2;
    public static final int EFFECT_SPRINT_SMOKE = 3;
    public static final int EFFECT_HEART = 4;
    public static final int EFFECT_DIZZY_SPELL = 5;
    public static final int EFFECT_10_POINTS = 6;
    public static final int EFFECT_5_POINTS = 7;
    
    public static final int GENERAL_TIME = 110;  // used for effects with multiple frames
    public static final int VISIBLE_TIME = 850; // used for eccts with one frame
    
    private static final int EFFECT_WIDTH = 16;
    private static final int EFFECT_HEIGHT = 16; 
    
    private static int[][] _frameSequences;
    private int _frameTime;
    private int _visibleTime;
    private int _type;
    
    /** 
     * Creates a new instance of a Game Effect
     */
    public GameEffect(Image image ) {
        super( image, EFFECT_WIDTH, EFFECT_HEIGHT );
        defineReferencePixel( this.getWidth()/2, this.getHeight()/2 );
        setVisible(false);
        
        _type = -1;
        
        initFrameSequences();
        
    }
    
    public void init( int startingX, int startingY, int type ){
        
        // make sure a valid type has been passed through
        if( type < 0 || type > 8 )
            return;
        
        setPosition( startingX, startingY );
        _frameTime = 0;
        _visibleTime = 0;
        _type = type;
        setVisible(true);
        
        // assign the right frame sequence to the type of effect requested      
        setFrameSequence( _frameSequences[_type] );
        setFrame( 0 );
        
    }
    
    public void update( long elapsedTime ){
        
        if( getFrameSequenceLength() > 1 ){
            if( getFrame() >= getFrameSequenceLength() - 1 ){
                setVisible(false);
            } else{
                if( _frameTime >= GENERAL_TIME ){
                    nextFrame();
                    _frameTime = 0;
                } else{
                    _frameTime += elapsedTime;
                }
            }
        } else{
            if( _visibleTime >= VISIBLE_TIME ){
                setVisible( false );
            } else{
                if( _frameTime >= GENERAL_TIME ){
                    //move( getX(), getY()-1 );
                    setPosition( getX(), (getY()-2) );
                    _frameTime = 0;
                } else{
                    _frameTime += elapsedTime;
                }
                _visibleTime += elapsedTime;
            }
        }
    }
        
        public void setType( int value ){
            _type = value;
        }
        
        public int getType( ){
            return _type;
        }
        
        /**
         * Called when this method is called every time this object is
         * intantiated but only executes if the class variables has not already done
         * so
         **/
        public static void initFrameSequences(){
            // check if already initilized
            if( _frameSequences != null )
                return;
            
            _frameSequences = new int[8][];
            
            int baddieBoom[] = {0,1,2};
            int smoke[] = {3,4,5};
            int heroBoom[] = {6,7,8};
            int sprintSmoke[] = {9,10,11,12};
            int heart[] = {13,14,15};
            int dizzy[] = {16,17,18,19};
            int points100[] = {20};
            int points25[] = {21};
            
            _frameSequences[EFFECT_BADDIE_BOOM] = baddieBoom;
            _frameSequences[EFFECT_SMOKE] = smoke;
            _frameSequences[EFFECT_HERO_BOOM] = heroBoom;
            _frameSequences[EFFECT_SPRINT_SMOKE] = sprintSmoke;
            _frameSequences[EFFECT_HEART] = heart;
            _frameSequences[EFFECT_DIZZY_SPELL] = dizzy;
            _frameSequences[EFFECT_10_POINTS] = points100;
            _frameSequences[EFFECT_5_POINTS] = points25;
        }              
        
    }
