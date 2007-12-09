/*
 * Actor.java
 *
 * Created on 14 April 2006, 10:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.*;

/**
 *
 * @author Joshua Newnham
 *  http://www.massey.ac.nz/~jnewnham
 *  http://www.digitrix.co.nz
 */
public abstract class Actor extends Sprite {
    
    public static final int STATE_ATTACKED = 1;
    public static final int STATE_ATTACKING = 2;
    public static final int STATE_DYING = 3;
    public static final int STATE_DEAD = 4;
    public static final int STATE_NORMAL = 5;
    
    protected final int TIME_DYING = 2000; 
    
    protected int ANIM_MOVING;
    protected int ANIM_IDLE;
    protected int ANIM_JUMPING;
    protected int ANIM_FALLING;    
    
    // state information for the actor
    protected boolean _onGround;
    protected int _currentState;
    protected int _currentAnim; 
    protected long _currentStateBegin;
    protected long _lastFrameChange;
    protected int _totalCycles;
    protected Animation _anim = null;
    
    // actor attributes (energy, max speed, starting x, starting y etc
    protected int _maxHealth;
    protected int _health;
    protected int _lives;
    protected int _strength;
    protected int _velocityX;
    protected int _velocityY;
    protected int _speedX;
    protected int _speedY;
    protected int _startingX;
    protected int _startingY;
    
    // some other variables
    protected long fluff = 0;
    
    
    /** Creates a new instance of Actor */
    public Actor( Image image, int width, int height ) {
        super( image, width, height );
        defineReferencePixel( this.getWidth()/2, this.getHeight()/2 );
        setVisible( false );
        initAnimations();
    }
    
    /**
     * Initilise the actor; usually called when creating a new actor or reusing an
     * existing actor
     * @param maxHealth assigns the maximum hit points this actor has
     * @param lives defines how many chances this actor has in life
     * @param strength assigns the amount of hit points this actor will take off when attacking an opponent
     * @param hSpeed assigns the horizontal speed of the actor (jumping capability of the actor)
     * @param vSpeed assigns the vertical speed of the actor (the initial vertical velocity of a jump)
     * @param posX assigns the starting x position
     * @param posY assigns the starting y position
     **/
    public void initActor( int maxHealth, int lives, int strength, int xSpeed, int vSpeed, int posX, int posY ){
        _maxHealth = maxHealth;
        _lives = lives;
        _strength = strength;
        _speedX = xSpeed;
        _speedY = vSpeed;
        _startingX = posX;
        _startingY = posY;
        _onGround = true;       
        
        reset();
    }
    
    /**
     * Reset the Actor back to its initial state
     **/
    public void reset(){
        _health = _maxHealth;
        _velocityX = 0;
        _velocityY = 0;
        
        _currentState = STATE_NORMAL;
        _currentAnim = -1; // no animation being used. 
        
        setPosition( _startingX, _startingY );
        setVisible( true ); 
    }
    
    
    /**
     * Setup states for a particular Actor
     **/
    public abstract void initAnimations();
    
    /**
     *
     **/
    public abstract void setState( int nextState );
    
    /**
     * Called every game cycle; moves the actor relative to its current speed
     **/
    public void cycle( long elapsedTime ){
        // apply gravity if creature if not flying
        if ( !isFlying() ){
            setVelocityY( getVelocityY() + World.GRAVITY);
        }
        
        // change x
        int dx = getVelocityX();
        int oldX = getX();
        int newX = oldX + dx;
        
        int tile[] = World.GetInstance().getTileCollision( this, newX, getY() );
        
        if (tile == null ){
            setPosition( newX, getY() );
        }
        else{
            // line up with tile boundary                        
            if( dx > 0 ){                
                setPosition( World.GetInstance().tilesXToPixels( tile[0] ) - (getWidth() + 1), getY() );
            }
            else if ( dx < 0 ){
                setPosition( World.GetInstance().tilesXToPixels( tile[0] + 1 ) + 1, getY() );                
            }            
            
            collideHorizontal();
            
        }                         
        
        // change y
        int dy = getVelocityY();
        int oldY = getY(); 
        int newY = oldY + dy; 
        tile = World.GetInstance().getTileCollision( this, getX(), newY );
        
        if ( tile == null ){
            setPosition( getX(), newY );
        }
        else if( World.GetInstance().getTile( tile[0], tile[1] ) == World.TILE_SPIKE ){            
            takeDamage( World.SPIKE_DAMAGE ); 
        }       
        else{
            // line up with tile boundary
            if ( dy > 0 ){
                // moving down
                setPosition( getX(), World.GetInstance().tilesYToPixels(tile[1]) - getHeight() );
            }
            else if ( dy < 0 ){
                // moving up
                setPosition( getX(), World.GetInstance().tilesYToPixels(tile[1] + 1 ) );
            }
            collideVertical();            
        }           
    }
    
    /**
     *
     **/
    public boolean isJumping(){
        return !_onGround;
    }
    
    /**
     *
     **/
    public void jump( boolean forceJump ){
        if( _onGround || forceJump ){
            _onGround = false;
            setVelocityY( _speedY );
        }
    }
    
    /**
     *
     **/
    public void jump( boolean forceJump, int forcedVelocityY ){
        if ( _onGround || forceJump ){
            _onGround = false;
            setVelocityY( forcedVelocityY );
        }
    }
    
    /**
     * Only right direction based sprites are stored in the image, therefore to make
     * our actor face left we must transform the image
     **/
    public void transform(){
        if ( _velocityX > 0 ){
            setTransform( this.TRANS_NONE );
        } else if ( _velocityX < 0 ){
            setTransform( this.TRANS_MIRROR );
        }
    }
    
    /**
     * Called when there is a conflict between two sprites or dangerous tile
     * @param damage is the value of the attackers strength; this value will be deducted from the actors health
     **/
    public void takeDamage( int damage ){
        
        // only affect if not in attacked state - poor fella, got to give him a little chance
        if( getState() != STATE_ATTACKED ){
            
            _health -= damage;
            if( _health <= 0 ){
                setState( STATE_DYING );
            }
        }
    }
    
    /**
     * If Actor has the capability of flying then gravity will not have any effect
     **/
    public boolean isFlying(){
        return false;
    }
    
    /**
     * Called before update() if the actor collided with a
     * tile horizontally.
     */
    public void collideHorizontal() {
        setVelocityX(-getVelocityX());
    }
    
    /**
     *  Called before update() if the creature collided with a
     *  tile vertically.
     *  If the velocity is high for the actor then we assume that he/she has
     *  fallen from a very high point therefore would be injuried.
     */
    public void collideVertical() {
        
        if( getVelocityY() > 20 ){
            takeDamage( 2 );
        }
        
        if (getVelocityY() > 0)
            _onGround = true;
        
        setVelocityY(0);
    }
    
// mutator methods for actors properties
    public int getState(){
        return _currentState;
    }
    public int getMaxHealth(){
        return _maxHealth;
    }
    public void setMaxHealth( int value ){
        _maxHealth = value;
    }
    public int getLives(){
        return _lives;
    }
    public void setLives( int value ){
        _lives = value;
    }
    public int getHealth(){
        return _health;
    }
    public void setHealth( int value ){
        _health = value;
    }
    public int getStrength(){
        return _strength;
    }
    public void setStrength( int value ){
        _strength = value;
    }
    public int getVelocityX(){
        return _velocityX;
    }
    public void setVelocityX( int value ){
        if( value < 0 )
            _velocityX = Math.max( -1*_speedX, value );
        else
            _velocityX = Math.min( _speedX, value );
    }
    public int getVelocityY(){
        return _velocityY;
    }
    public void setVelocityY( int value ){
        _velocityY = value;
    }
    public int getSpeedX(){
        return _speedX;
    }
    public void setSpeedX( int value ){
        _speedX = value;
    }
    public int getSpeedY(){
        return _speedY;
    }
    public void setSpeedY( int value ){
        _speedY = value;
    }
    
    // some helper methods
    
    /**
     *
     **/
    protected void nextAnimFrame(){
        nextFrame();
        
        if( _anim != null && getFrame() == 0 )
            _anim.incrementRepeat();                
    }
    
    /**
     *
     **/
    protected void setAnimation( int anim ){
        _currentAnim = anim; 
        setFrameSequence( _anim.getFrameSequence( _currentAnim ) );
        setFrame( 0 );
        _lastFrameChange = 0;
        transform();
    }
    
}
