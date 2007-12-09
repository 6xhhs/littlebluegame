/*
 * Baddie.java
 *
 * Created on 17 April 2006, 21:19
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
public class Baddie  extends Actor  {
    
    public static final int BADDIE_TYPE_FLY = 1;
    public static final int BADDIE_TYPE_BAT = 2;
    public static final int BADDIE_TYPE_GHOST = 3;  
    
    public static final int BADDIE_POINTS = 10; 
    
    private int _baddieType;
    private long _protectileDelay;       // the frequency of that a baddie can fire a protectile at our hero
    private long _lastProjectileFired;  // the last time the baddie fired a protecile at our hero
    private long _travellingDelay;       // the time the baddie will walk straight e.g. used by flying baddies so they dont fly off the screen
    private long _travelledTime = 0;         // how long the baddie has been travelling for
    private int _sight;        
    
    /** Creates a new instance of Baddie */
    public Baddie( Image image, int width, int height, int baddieType ) {
        super( image, width, height );
        
        _baddieType = baddieType;
        initAnimations(); 
    }
    
    /**
     *
     **/
    public void initActor( int posX, int posY ){
        if( _baddieType == BADDIE_TYPE_BAT ){
            // Setup the attributes for our BAT
            _maxHealth = 10;
            _lives = 1;
            _strength = 5;
            _speedX = 2;
            _speedY = -2;
            _protectileDelay = 10000;
            _lastProjectileFired = 0;
            _travellingDelay = 20000;
            _travelledTime = 0;
            
        } else if( _baddieType == BADDIE_TYPE_FLY ){
            // Setup the attributes for our FLY
            _maxHealth = 10;
            _lives = 1;
            _strength = 5;
            _speedX = 2;
            _speedY = -2;
            _travellingDelay = 15000;
            _travelledTime = 0;
            
        } else if( _baddieType == BADDIE_TYPE_GHOST ){
            // Setup the attributes for our GHOST
            _maxHealth = 10;
            _lives = 1;
            _strength = 5;
            _speedX = 1;
            _speedY = -2;
            _travellingDelay = 10000;
            _travelledTime = 0;
        }
                
        super.initActor( _maxHealth, _lives, _strength, _speedX, _speedY, posX, posY );
        
        setState( STATE_NORMAL ); 
    }
    
    /**
     * Setup Animations for a particular Actor
     **/
    public void initAnimations(){
        int fsMoving[];
        int fsIdle[]; 
        int animTime = 0;        
        
        _anim = new Animation( 2 );
        
        if( _baddieType == BADDIE_TYPE_BAT ){
            fsMoving = new int[2]; 
            fsIdle = new int[1];
            
            fsIdle[0] = 3;
            fsMoving[0] = 3;
            fsMoving[1] = 4;
            
            animTime = 80;
        }
        else if( _baddieType == BADDIE_TYPE_FLY ){
            fsMoving = new int[2]; 
            fsIdle = new int[1];
            
            fsIdle[0] = 0;
            fsMoving[0] = 0;
            fsMoving[1] = 1;           
            
            animTime = 80;
        }
        else{
            fsMoving = new int[2]; 
            fsIdle = new int[1];
            
            fsIdle[0] = 5;
            fsMoving[0] = 6;
            fsMoving[1] = 7;
            
            animTime = 450;
        }
        
        ANIM_MOVING = _anim.addAnimation( fsMoving, animTime, -1, 0, STATE_NORMAL );
        ANIM_IDLE = _anim.addAnimation( fsIdle, animTime, -1, 0, STATE_NORMAL );
    }
    
    /**
     *
     **/
    public void setState( int nextState ){
        if ( getState() != nextState && !( (getState() == STATE_DYING && nextState != STATE_DEAD) || getState() == STATE_DEAD) ) {
            _currentState = nextState;
            _currentStateBegin = 0;
            
            if ( getState() == STATE_DYING || getState() == STATE_DEAD ){
                setVelocityX(0);
                setVelocityY(0);
                setVisible( false );
                // add an effect
                World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_SMOKE );
                World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_10_POINTS );
                
                // advance score
                GameManager.GetInstance().advanceScore( BADDIE_POINTS, false ); 
            }
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
            if( _health <= 0 )
                setState( STATE_DYING );                
            else{             
                // add dizzy effect to our canvas 
                World.GetInstance().addGameEffect( getX(), getY(), GameEffect.EFFECT_DIZZY_SPELL );
            }
        }
    }
    
    /**
     * Called every game cycle; moves the actor relative to its current speed
     **/
    public void cycle( long elapsedTime ){            
         super.cycle( elapsedTime );        
         transform(); // transform the image depending on the actos velocity
                
        _currentStateBegin += elapsedTime;
        _travelledTime += elapsedTime;                
        
        int newAnim;
        
        switch( getState() ){            
            case STATE_ATTACKED:
            case STATE_NORMAL:
            case STATE_ATTACKING:               
                
                if( _velocityX == 0 ){
                    newAnim = ANIM_IDLE;
                } else{
                    newAnim = ANIM_MOVING;
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
                    
                }
                
                break;
                
            case STATE_DYING:
            case STATE_DEAD:
                // do nothing - should be dead (invisible)
                break;
        }
    }
    
    /**
     * The master mind behind our enemies, basically this method is called by the game manager 
     * each game cycle, if false is returned, becuase the enemy is dead or not in sight, then no further 
     * processing needs to take place e.g. update() 
     * otherwise perform some logic 
     **/
    public boolean performAI(){
        // first test if we are alive
        if( getState() == STATE_DYING || getState() == STATE_DEAD || !isVisible() )
            return false;                               
        
        // next test to see if we are in sight of the player
        int playerX = World.GetInstance().getHero().getX(); 
        int playerY = World.GetInstance().getHero().getY(); 
        int disX = Math.max( playerX, this.getX() ) - Math.min( playerX, this.getX() );
        int disY = Math.max( playerY, this.getY() ) - Math.min( playerY, this.getY() );
        
        
        
        if( !(disX <= World.GetInstance().getScreenWidth() && disY <= World.GetInstance().getScreenHeight()) )
            return false;                
        
        if( _currentAnim == ANIM_IDLE )
            setVelocityX( -1*getSpeedX() ); // send the baddie walking to the left at max speed 
            
                 
        if( _travelledTime >= _travellingDelay ){
            // for now we'll keep it simple, 
            // if we have reached the end of our travelling time then turn around            
            collideHorizontal(); 
        }
        else if( World.GetInstance().nextHorizontalTileEmpty( this ) && !isFlying() ){
            // else if we're not flying peek to see if the 
            // next horizontal tile is empty, if so then turn around else continue on walking
            collideHorizontal();             
        }                
        
        return true;
    }
    
    /**
     *
     **/
    public boolean isFlying(){
        if( _baddieType == BADDIE_TYPE_BAT || _baddieType == BADDIE_TYPE_FLY )
            return true;
        else
            return false;
    }
    
    /**
     * Called before update() if the actor collided with a
     * tile horizontally.
     */
    public void collideHorizontal() {
        _travelledTime = 0;
        
        super.collideHorizontal();
    }
    
    /**
     *
     **/
    private boolean inSight(){
        Hero player = World.GetInstance().getHero(); 
        
        double distance;
        int x1 = getX();
        int y1 = getY();
        int x2 = player.getX();
        int y2 = player.getY();
        
        boolean inSight = false;
        
        int disX = (x2 - x1) * (x2 - x1);
        int disY = (y2 - y1) * (y2 - y1);
        
        if (disX == 0 || disY == 0) return false;
        try {
            distance = Math.sqrt(disX + disY );
            //System.out.println( "DISTANCE: " + distance );
            if( distance <= _sight ){
                inSight = true;
            }
        } catch(ArithmeticException ae) {
            return false;
        }
        return inSight;
    }
    
}
