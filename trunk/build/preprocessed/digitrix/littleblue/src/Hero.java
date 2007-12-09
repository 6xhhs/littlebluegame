/*
 * Hero.java
 *
 * Created on 16 April 2006, 15:03
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
public class Hero extends Actor {
    
    // additional animation
    protected int ANIM_SPRINTING;
     
    // additional attributes
    private boolean _doubleJumped = false;
    private boolean _allowDoubleJump = false; 
    
    private int _velocityTick = 0;
    private final int _velocityTicksRequired = 6; 
    
    /** Creates a new instance of Hero */
    public Hero( Image image, int width, int height ) {
        super( image, width, height );
    }
    
    /**
     *
     **/
    public void initActor( int posX, int posY ){
        
        _maxHealth = 10;       
        _strength = 5;
        _speedX = 8;
        _speedY = -10;
        _velocityTick = 0;
                
        super.initActor( _maxHealth, _lives, _strength, _speedX, _speedY, posX, posY );        
    }
    
    /**
     * Setup states for a particular Actor
     **/
    public void initAnimations(){
        int fsSprinting[] = {4,5,6,5}; 
        int fsWalking[] = {1,2,3,2};
        int fsIdle[] = {0};
        int fsJumping[] = {7};
        int fsFalling[] = {8};
        
        _anim = new Animation( 5 );
                
        ANIM_MOVING = _anim.addAnimation( fsWalking, 220, -1, 0, STATE_NORMAL );
        ANIM_SPRINTING = _anim.addAnimation( fsSprinting, 80, -1, 0, STATE_NORMAL );
        ANIM_IDLE = _anim.addAnimation( fsIdle, 120, -1, 0, STATE_NORMAL );
        ANIM_JUMPING = _anim.addAnimation( fsJumping, 120, -1, 0, STATE_NORMAL );
        ANIM_FALLING = _anim.addAnimation( fsFalling, 120, -1, 0, STATE_NORMAL );
    }
    
    /**
     *
     **/
    public void setState( int nextState ){
        if ( getState() != nextState ) {
            _currentState = nextState;
            _currentStateBegin = 0;
            
            if ( getState() == STATE_DYING ){
                setVelocityX(0);
                setVelocityY(0);
                setVisible( false );
                // add an effect
                World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_HERO_BOOM );
            } else if ( getState() == STATE_DEAD ){                
                if ( _lives > 0 ){
                    _lives--;
                    reset();                    
                } else{
                    setVisible( false );
                    // signal game over to the game manager
                }
            }
            
        }
    }
    
    /**
     *
     **/
    public void handleInput( int keyState ){                     
        
        if ( getState() != STATE_DYING && getState() != STATE_DEAD ) { 
            
            // ************* HANDLE MOVEMENT ******************
            // Logic here; if user is continually running in one direction then increase 
            // the velocity every third cycle
            // If the user changes direction then set the velocity to 1 in the opposite
            // direction and display smoke if the maximum speed was reached
            // If the user stops running then slow the user down every cycle (by decreasing 
            // the velocity by 1) and show smoke if the user had reached the maximum speed
            if ( (keyState & GameCanvas.LEFT_PRESSED) != 0 ) {
                // if running left already then increase velocity up to a maximum of the 
                // speedX variable else increase speed to one (slow the movement of left down)
                if( getVelocityX() < 0 ){
                    if( ++_velocityTick >= _velocityTicksRequired ){
                        setVelocityX( getVelocityX()-1 );           
                        _velocityTick = 0;
                    }
                    else{
                        setVelocityX( getVelocityX() ); 
                    }
                }
                else{                    
                    //setVelocityX( -1 );           
                    setVelocityX( getVelocityX() -1 ); 
                    _velocityTick = 0;
                    
                    // if reached top speed then display the smoke effect 
                    if( getVelocityX() == (-1*_speedX) ){
                        // show smoke
                    }
                }
            }
            else if ( (keyState & GameCanvas.RIGHT_PRESSED) != 0 ) {
                // if running left already then increase velocity up to a maximum of the 
                // speedX variable else increase speed to one (slow the movement of left down) 
                if( getVelocityX() > 0 ){
                    if( ++_velocityTick >= _velocityTicksRequired ){
                        setVelocityX( getVelocityX()+1 );           
                        _velocityTick = 0;
                    }
                    else{
                        setVelocityX( getVelocityX() ); 
                    }
                }
                else{
                    //setVelocityX( 1 );
                     setVelocityX( getVelocityX() +1 ); 
                    _velocityTick = 0;
                    
                    // if reached top speed then display the smoke effect 
                    if( getVelocityX() == _speedX ){
                        // show smoke
                    }                    
                }
            }
            else{
                // slow user down to a velocity of 0
                int dx; 
                if( getVelocityX() < 0 ){
                    dx = getVelocityX() + 1;
                    if( dx > 0 )
                        dx = 0;
                    
                    setVelocityX( dx );
                }
                else if( getVelocityX() > 0 ){
                    dx = getVelocityX() - 1;
                    if( dx < 0 )
                        dx = 0;
                    
                    setVelocityX( dx );
                }
            }
            
            // ************* HANDLE JUMPING ******************
            if ( (keyState & GameCanvas.UP_PRESSED) != 0 || (keyState & GameCanvas.FIRE_PRESSED) != 0 )
                jump(false);
                        
        }          
    }
    
    /**
     * Called every game cycle; moves the actor relative to its current speed
     **/
    public void cycle( long elapsedTime ){
        super.cycle( elapsedTime );
        
        transform(); // transform the image depending on the actos velocity
        
        _currentStateBegin += elapsedTime;
        int newAnim;
        
        switch( getState() ){
            case STATE_NORMAL:
            case STATE_ATTACKED:
            case STATE_ATTACKING:
                if ( !_onGround ){
                    if( this.getVelocityY() > 0 ){
                        newAnim = ANIM_FALLING;
                    } else{
                        newAnim = ANIM_JUMPING;
                    }
                } else{
                    
                    if( _velocityX == 0 ){
                        newAnim = ANIM_IDLE;
                    } else{
                        if( getVelocityX() == getSpeedX() || getVelocityX() == (-1*getSpeedX()) )
                            newAnim = ANIM_SPRINTING; 
                        else
                            newAnim = ANIM_MOVING;
                    }
                }
                
                if( newAnim == _currentAnim ){
                    if ( _lastFrameChange >= _anim.getAnimTimePerFrame( _currentAnim ) ){
                        nextAnimFrame();
                        _lastFrameChange = 0;
                        
                        // if the sequence is at the end of its cycle then progress to the next animation
                        if( _anim.getNextAnim( _currentAnim ) > 0 ){
                            // change animation
                            setAnimation( _anim.getNextAnim( _currentAnim ) );
                            
                            // change state
                            setState( _anim.getAssociatedState( _currentAnim ) );
                        }
                        
                    } else{
                        _lastFrameChange += elapsedTime;
                    }
                } else{
                    setAnimation( newAnim );
                    
                    // if animation set to sprinting, then add smoke effect
                    if( newAnim == ANIM_SPRINTING ){
                        int y = (getY() + (getHeight()) - 16); // place the smoke by his feet
                        int x = getX(); 
                        World.GetInstance().addGameEffect( x, y, GameEffect.EFFECT_SPRINT_SMOKE ); 
                    }
                }
                
                break;
                
            case STATE_DYING:
                if ( _currentStateBegin >= TIME_DYING ){                                        
                    setState( STATE_DEAD );
                } else{
                    _lastFrameChange += elapsedTime;
                }
                break;
                
            case STATE_DEAD:
                // do nothing - should be dead (invisible)
                break;
        }
    }
    
    /**
     * Called before update() if the actor collided with a
     * tile horizontally.
     */
    public void collideHorizontal() {
        setVelocityX( 0 );
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
        
        if (getVelocityY() > 0){
            _onGround = true;
            _doubleJumped = false; 
        }
        
        setVelocityY(0);
    }
    
    /**
     *
     **/
    public boolean canAttack(){
        return !_onGround && _velocityY > 0;
    }        
    
    /**
     *
     **/
    public void jump( boolean forceJump ){
        if( _onGround || forceJump ){
            _onGround = false;
            setVelocityY( _speedY );
        }
        else if( !_doubleJumped && getVelocityY() > 0 && _allowDoubleJump ){
            _doubleJumped = true; 
            setVelocityY( getVelocityY() - Math.abs(_speedY) );            
        }
    }
    
    public boolean isAlive(){
        if( _lives <= 0 && (_currentState == STATE_DYING || _currentState == STATE_DEAD) )
            return false;
        else
            return true; 
    }
    
}
