/**
 * 
 */
package cn.ispeak.dynamics.real.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Repository;

/**
 * 环境上下文
 * @author Mr Yin
 *
 */
@Repository
public class SpringContextUtil implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.ApplicationContextAware#
	 * setApplicationContext( org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringContextUtil.applicationContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public static <T> T getBean(Class<T> clazz){
		
		T bean = applicationContext.getBean(clazz);
		
		return bean;
	}
}
