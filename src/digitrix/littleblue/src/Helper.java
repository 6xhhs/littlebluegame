/*
 * Helper.java
 *
 * Created on 14 April 2006, 16:19
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
 */
public class Helper {
    
    /** Creates a new instance of Helper */
    public Helper() {
    }
    
    public final static int[] expandArray( int[] oldArray, int expandBy ){
        int[] newArray = new int[ oldArray.length + expandBy]; 
        System.arraycopy( oldArray, 0, newArray, 0, oldArray.length );
        return newArray; 
    }
    
    public final static int[][] expandArray( int[][] oldArray, int expandBy ){
        int[][] newArray = new int[ oldArray.length + expandBy ][];
        System.arraycopy( oldArray, 0, newArray, 0, oldArray.length );
        return newArray; 
    }
    
}
