/**
 * 
 */
package cn.ispeak.dynamics.real;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import cn.ispeak.dynamics.real.service.DynamicsBootstrap;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * @author Mr Yin
 *
 */
public class BootstrapMain {
	
	static String path;
	private static ApplicationContext ctx = null;
    static{
        try {
            path = new File(".").getCanonicalPath()+File.separator;
            
            System.out.println("loading spring config from " + Constants.SPRING_FILE);
        	ctx = new FileSystemXmlApplicationContext(Constants.SPRING_FILE);
        	System.out.println("loading spring config end...");
        	
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure(path+"conf"+File.separator+"log4j.properties");
        
        Logger logger = Logger.getLogger(BootstrapMain.class);
        
    	logger.info("dynamic deal start...");
    	
    	new Thread(ctx.getBean(DynamicsBootstrap.class),"bootstrap-task").start();
	}

}
