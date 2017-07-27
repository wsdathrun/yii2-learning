/**
 * 
 */
package cn.ispeak.dynamics.real.entity;

/**
 * @author Mr Yin
 *
 */
public class TaskInitReady {

	public static int squareReady = 0;
	public static int commentsReady = 0;
	public static int pointReady = 0;
	
	private TaskInitReady(){
		throw new IllegalAccessError("Utility class");
	}
	
	public static synchronized boolean getRead(){
		
		return (squareReady+commentsReady+pointReady == 3)?true:false;
		
	}
}
