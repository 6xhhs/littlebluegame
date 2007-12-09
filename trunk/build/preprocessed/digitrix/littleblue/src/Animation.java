/*
 * Animation.java
 *
 * Created on 14 April 2006, 10:22
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package digitrix.littleblue.src;

/**
 *
 * @author Joshua Newnham
 *  http://www.massey.ac.nz/~jnewnham
 *  http://www.digitrix.co.nz
 *
 * To be used by the Actor class to manage State animation. 
 */
public class Animation {
    
    private int _totalAnimations; 
    private int[][] _frameSeq;      // holds the animation/frame sequence for a particular animation
    private int[] _animTime;        // holds the time a animation takes to complete 
    private int[] _repeats;         // holds how many times a animation will repeat (<= 0 continous, 1 = once, 1+ specific amount of times)
    private int[] _nextAnimation;   // holds the next frame sequence that should be set when finished repeating 
    private int[] _associatedState; // 
    private int _repeated;          // holds how many times a specific animation has been repeated. 
    
    /**
     * Creates a new instance of Animation
     */
    public Animation( int numAnim ) {
        _totalAnimations = -1; 
        _animTime = new int[numAnim];
        _repeats = new int[numAnim];
        _nextAnimation = new int[numAnim];
        _associatedState = new int[numAnim];
        _frameSeq = new int[numAnim][];        
        _repeated = 0; 
    }
    
    public final int addAnimation( int[] frameSeq, int animTime, int repeats, int nextAnimation, int associatedState ){
        int anim = ++_totalAnimations;
        
        if( anim >= _frameSeq.length ){
            // expand array 
            _frameSeq = Helper.expandArray( _frameSeq, 1 );
            _animTime = Helper.expandArray( _animTime, 1 );
            _repeats = Helper.expandArray( _repeats, 1 );
            _associatedState = Helper.expandArray( _associatedState, 1 );
            _nextAnimation = Helper.expandArray( _nextAnimation, 1 );
        }
                
        _frameSeq[anim] = frameSeq;
        _animTime[anim] = animTime;
        _repeats[anim] = repeats;
        _associatedState[anim] = associatedState; 
        _nextAnimation[anim] = nextAnimation;
             
        return anim; 
    }
    
    /**
     * Gets the number of frames for a particular state
     **/
    public final int getTotalFrames( int anim ){
        _repeated = 0; 
        return _frameSeq[anim].length; 
    }
    
    /**
     * Gets the total amount of time a frame sequence takes for a particular state
     **/
    public final int getAnimTime( int anim ){
        return _animTime[anim];
    }
    
    /**
     * Helper class to get the amount of time a specific frame should be displayed
     **/
    public final int getAnimTimePerFrame( int anim ){
        return _animTime[anim] / _frameSeq[anim].length;
    }
    
    /**
     * Retrieve a particular states frame sequence
     **/
    public final int[] getFrameSequence( int anim ){       
        return _frameSeq[anim];        
    }
    
    /**
     * Retrieve the numer of repeats for a particular state
     **/
    public final int getRepeats( int anim ){
        return _repeats[anim];
    }
    
    /**
     * Retrieve the next animation for the current animation
     **/
    public final int getNextAnim( int currentAnimation ){                
        if( _repeats[currentAnimation] <= 0 )
            return -1; // meaning do not goto the next animation 
        else{
            if( _repeated >= _repeats[currentAnimation] )
                return _nextAnimation[currentAnimation];
            else
                return -1;
        }        
    }          
    
    /**
     * Retrieve the associated state for the current animation 
     **/
    public final int getAssociatedState( int currentAnimation ){
        return _associatedState[currentAnimation]; 
    }
    
    /**
     *
     **/
    public void incrementRepeat(){
        _repeated++; 
    }
    
    /**
     *
     **/
    public int getRepeated(){
        return _repeated; 
    }
    
    /**
     *
     **/
    public boolean continueAnim( int currentAnimation ){
        if( _repeats[currentAnimation] <= 0 )
            return true;
        else 
            return _repeats[currentAnimation] <= _repeated; 
    }
        
}
