/*
 * Prop.java
 *
 * Created on 7 May 2006, 14:52
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
public class Prop extends Sprite {
    
    // NON INTERACTIVE PROPS
    public static final int PROP_TYPE_FLOWER1 = 0;
    public static final int PROP_TYPE_FLOWER2 = 1;
    public static final int PROP_TYPE_GRASS1 = 2;
    public static final int PROP_TYPE_GRASS2 = 3;
    public static final int PROP_TYPE_MUSHROOM = 4;
    
    // INTERACTIVE PROPS
    public static final int PROP_TYPE_SPRING = 20;
    public static final int PROP_TYPE_COIN = 21;
    public static final int PROP_TYPE_CHECKPOINT = 22;
    public static final int PROP_TYPE_YELLOW_BLOCK = 23;
    public static final int PROP_TYPE_GREY_BLOCK = 24;
    public static final int PROP_TYPE_BLUE_BLOCK = 25;
    
    public static final int COIN_POINTS = 5;
    
    private static final int STATE_SHOWING = 0;
    private static final int STATE_CONSUMED = 1;
    
    private int ANIM_IDLE;
    private int ANIM_ACTION;
    
    private int _type;
    private Animation _anim = null;
    
    private int _lastFrameChange;
    private int _currentAnim;
    private int _currentState;
    private boolean _onGround = false;
    private boolean _floats = false;
    private Actor _playerOnBlock = null;
    private byte _genericFlag = 0;      // just a general purpose flag used for anything
    
    private int _startX;
    private int _startY; 
    
    /** Creates a new instance of Prop */
    public Prop( Image image ) {
        super( image );
    }
    
    public Prop( Image image, int width, int height ) {
        super( image, width, height );
    }
    
    /**
     *
     **/
    public void initProp( int type, int x, int y ){
        
        // remember where this prop was initally positioned in-case we have to re-initilize it
        // later on in the game
        _startX = x; 
        _startY = y; 
        
        _type = type;
        defineReferencePixel( this.getWidth()/2, this.getHeight()/2 );                
        setPosition( _startX, _startY );
        setState( STATE_SHOWING );
        initAnimations();
        _onGround = false;
        
        // if a coin or block then float...
        if( _type == PROP_TYPE_COIN || _type == PROP_TYPE_BLUE_BLOCK || _type == PROP_TYPE_YELLOW_BLOCK
                || _type == PROP_TYPE_GREY_BLOCK )
            setFloat( true );
        
        // if prop is a check point then set its visibility to false as
        // the players is required to collect all of the coins before he/she
        // can advance to the next level
        if( _type == PROP_TYPE_CHECKPOINT )
            setVisible( false );
    }
    
    /**
     *
     **/
    public void cycle( long elapsedTime ){
        if ( !_onGround && ( !floats() || _playerOnBlock != null ) ){
            int dy = 1;
            
            // faster pace depending on what block is used
            if( _type == PROP_TYPE_BLUE_BLOCK )
                dy = 3;
            else if( _type == PROP_TYPE_GREY_BLOCK && _playerOnBlock != null )
                dy = 4;
            
            // move player/actor in proportion of block
            // NB: we do this so the player moves smoothly with the block, otherwise the player only moves
            // as fast as gravity (1) and the effect is jerky... It's a hack but it seems to be working.
            if( (_type == PROP_TYPE_BLUE_BLOCK || _type == PROP_TYPE_YELLOW_BLOCK || _type == PROP_TYPE_GREY_BLOCK) && _playerOnBlock != null ){
                _playerOnBlock.move( 0, (dy-1) );
                _playerOnBlock = null;
            }   
            
            int[] tile = World.GetInstance().getTileCollision( this, getX(), getY() + dy );
            
            if( tile == null ){
                setPosition( getX(), getY() + dy );
            } else{
                // line up with tile boundary
                if ( dy > 0 ){
                    // moving down
                    setPosition( getX(), World.GetInstance().tilesYToPixels(tile[1]) - getHeight() );
                } else if ( dy < 0 ){
                    // moving up
                    setPosition( getX(), World.GetInstance().tilesYToPixels(tile[1] + 1 ) );
                }
                _onGround = true;
                
                // if of type block then disapear and show some sort of effect
                if( _type == PROP_TYPE_BLUE_BLOCK || _type == PROP_TYPE_YELLOW_BLOCK || _type == PROP_TYPE_GREY_BLOCK ){
                    //setVisible( false );
                    // game was a little hard when the block was removed completely (in such a way that the 
                    // user was forced to commit suicide and start from the beginning again) 
                    // therefore instead of removing the block completely we will re-initilize it
                    World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_BADDIE_BOOM );
                    initProp( _type, _startX, _startY );                     
                }
            }
        }
        
        // if user has collected all of the coins then set the visibility of the prop
        // checkpoint to true and start animating, otherwise return
        if( _type == PROP_TYPE_CHECKPOINT ){
            if( World.GetInstance().coinsLeft() == 0 )
                setVisible( true );
            else
                return;
        }
        
        // if the prop is just for decoration (e.g. plant, mushroom, i.e. has no animation)
        // then return as we don't do any processing
        if( _type < 20 || !this.isVisible() )
            return;
        
        
        if ( _lastFrameChange >= _anim.getAnimTimePerFrame( _currentAnim ) ){
            animate();
            _lastFrameChange = 0;
            
            // if the sequence is at the end of its cycle then progress to the next animation
            if( _anim.getNextAnim( _currentAnim ) >= 0 ){
                // change animation
                setAnimation( _anim.getNextAnim( _currentAnim ) );
                // change state
                setState( _anim.getAssociatedState( _currentAnim ) );
            }
            
        } else{
            _lastFrameChange += elapsedTime;
        }
    }
    
    /**
     *
     **/
    public void acquire( Actor actor ){
        if( _type < 20 || !isVisible() )
            return;
        
        switch( _type ){
            case PROP_TYPE_SPRING:
                // project actor up
                if( actor.isJumping() && actor.getVelocityY() > 0 ){
                    setAnimation( ANIM_ACTION );
                    actor.setVelocityY( Math.max( actor.getVelocityY() * -2, -20 ) );
                }
                break;
            case PROP_TYPE_COIN:
                // add the points effect, increment the number of coins collected
                // increment the players score
                acquireCoin();
                break;
                
            case PROP_TYPE_CHECKPOINT:
                // advance to the next level
                acquireCheckPoint();
                break;
                
            case PROP_TYPE_GREY_BLOCK:
            case PROP_TYPE_YELLOW_BLOCK:
            case PROP_TYPE_BLUE_BLOCK:
                // only activate/aquire prop if velocity is positive i.e. only when walked on top
                // of jumped on (moving down and not jumping through).
                
                // If the player is not on top of the block...
                if( actor.getVelocityY() >= 0 && actor.getY() < (this.getY()-(this.getHeight()/3) )  ){
                    // re-position the actor so that he/she is standing directly on top and 
                    // stop the player from falling through by calling collideVertical
                    actor.setPosition( actor.getX(), ((this.getY()-(this.getHeight()/2))-(actor.getHeight()/2)+4) ); 
                    actor.collideVertical();
                    _playerOnBlock = actor;
                    
                    // the difference between the grey blocks and the other two is that
                    // the block will only lower when the player is on the block - this can be done with
                    // a flag that indicates that the place is on the block (this is possible becuase the hero
                    // cycle is called first, therefore we can set this flag during this check - may not behave
                    // quite right with the baddies becuase the prop maybe processed before the baddie).
                    if( _type != PROP_TYPE_GREY_BLOCK )
                        setFloat( false );
                } else{
                    // if the player has ran into the block or jumped from below (i.e. knocked his/her 
                    // head on the block) then readjust the actors position 
//                    int actorX = actor.getX();
//                    int actorY = actor.getY();
//                    
//                    // if under the block then place head under block and set collideVertical()
//                    if( actor.getVelocityY() < 0 && actor.getY() > (this.getY()+(this.getHeight()/2) ) ){
//                        actor.collideVertical(); 
//                    }                                                            
//                    else if( actorX < getX() ){
//                        actor.collideHorizontal();
//                    }
//                    else if( actorX < getX() ){
//                        actor.collideHorizontal();
//                    }
                }
                break;
        }
        
    }
    
    /**
     *
     **/
    public void setState( int newState ){
        if( newState == STATE_SHOWING )
            setVisible( true );
        if( newState == STATE_CONSUMED )
            setVisible( false );
        
        _currentState = newState;
    }
    
    /**
     *
     **/
    public void initAnimations(){
        switch( _type ){
            case PROP_TYPE_CHECKPOINT:
                _anim = new Animation( 1 );
                int[] arr1 = {0};
                ANIM_IDLE = _anim.addAnimation( arr1, 90, 0, 0, STATE_SHOWING );
                break;
            case PROP_TYPE_COIN:
                _anim = new Animation( 2 );
                int[] arr2 = {0,1,3};
                ANIM_IDLE = _anim.addAnimation( arr2, 180, 0, 2, STATE_SHOWING );
                int[] arr3 = {0};
                ANIM_ACTION = _anim.addAnimation( arr3, 90, 3, -1, STATE_SHOWING );
                break;
                
            case PROP_TYPE_FLOWER1:
            case PROP_TYPE_FLOWER2:
            case PROP_TYPE_GRASS1:
            case PROP_TYPE_GRASS2:
            case PROP_TYPE_MUSHROOM:
                int flowerType;
                if( _type == PROP_TYPE_FLOWER1 )
                    flowerType = 0;
                else if( _type == PROP_TYPE_FLOWER2 )
                    flowerType = 2;
                else if( _type == PROP_TYPE_GRASS1 )
                    flowerType = 4;
                else if( _type == PROP_TYPE_GRASS2 )
                    flowerType = 1;
                else
                    flowerType = 3;
                
                _anim = new Animation( 1 );
                int[] arr4 = {flowerType};
                ANIM_IDLE = _anim.addAnimation( arr4, 180, 0, 1, STATE_SHOWING );
                break;
                
            case PROP_TYPE_BLUE_BLOCK:
            case PROP_TYPE_GREY_BLOCK:
            case PROP_TYPE_YELLOW_BLOCK:
                int blockType = 0;
                if( _type == PROP_TYPE_YELLOW_BLOCK )
                    blockType = 0;
                else if( _type == PROP_TYPE_BLUE_BLOCK )
                    blockType = 1;
                else if( _type == PROP_TYPE_GREY_BLOCK )
                    blockType = 2;
                
                _anim = new Animation( 1 );
                int[] arr5 = {blockType};
                ANIM_IDLE = _anim.addAnimation( arr5, 180, 0, 1, STATE_SHOWING );
                break;
                
            case PROP_TYPE_SPRING:
                _anim = new Animation( 2 );
                int[] arr6 = {0};
                ANIM_IDLE = _anim.addAnimation( arr6, 180, 0, 1, STATE_SHOWING );
                int[] arr7 = {0,0,1,1,2,2};
                ANIM_ACTION = _anim.addAnimation( arr7, 220, 33, ANIM_IDLE, STATE_SHOWING );
                break;
        }
        
        setAnimation( ANIM_IDLE );
    }
    
    /**
     *
     **/
    public int getType(){
        return _type;
    }
    
    /**
     *
     **/
    private void animate(){
        nextFrame();
        
        if( _anim != null && getFrame() == 0 )
            _anim.incrementRepeat();
        
        // movement
        switch( _type ){
            case PROP_TYPE_CHECKPOINT:
                // bounce
                if( _genericFlag <= 4 ){
                    move( 0, -1 );
                    _genericFlag++;
                } else if( _genericFlag <= 8 ){
                    move( 0, 1 );
                    _genericFlag++;
                } else
                    _genericFlag = 0; // reset
                break;
                
            case PROP_TYPE_COIN:
                // if ANIM_ACTION being displayed, aka points, then move upwards
                if( _currentAnim == ANIM_ACTION )
                    setPosition( getX(), (getY()-2) );
                break;
                
            case PROP_TYPE_SPRING:
                // no movement
                break;
        }
    }
    
    /**
     *
     **/
    public void setAnimation( int anim ){
        if( anim < 0 ){
            setState( STATE_CONSUMED );
            return;
        }
        
        _currentAnim = anim;
        setFrameSequence( _anim.getFrameSequence( _currentAnim ) );
        setFrame( 0 );
        _lastFrameChange = 0;
    }
    
    public boolean floats(){
        return getFloat();
    }
    
    private void setFloat( boolean value ){
        _floats = value;
    }
    
    private boolean getFloat(){
        return _floats;
    }
    
    /**
     * Method called on acquire when user collides with this prop and
     * the type of this prop is a Coin...
     * Here we will hide the coin, add an effect (points effect) and increment
     * the players score by the amount for a Coin.
     **/
    private void acquireCoin(){
        setState( STATE_CONSUMED );
        World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_5_POINTS );
        GameManager.GetInstance().advanceScore( COIN_POINTS, true );
    }
    
    private void acquireCheckPoint(){
        GameManager.GetInstance().advanceLevel();
    }
    
}
