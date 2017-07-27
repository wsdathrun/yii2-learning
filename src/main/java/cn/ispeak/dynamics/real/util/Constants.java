/**
 * 
 */
package cn.ispeak.dynamics.real.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import cn.ispeak.dynamics.real.entity.VisitorCacheVo;

/**
 * @author Mr Yin
 *
 */
public class Constants {
	
	static String path;
	static final Properties confPro = new Properties();
	static{
        try {
            path =File.separator + new File(".").getCanonicalPath()+File.separator;
            confPro.load(new FileInputStream(path+"conf"+File.separator+"conf.properties"));
        } catch (IOException e) {
           System.out.println(e.getMessage());
        }
    }
	
	private Constants(){
		throw new IllegalAccessError("Utility class");
	}
	
	//配置文件所在目录
    private static final String CONF_DIR = "conf"+File.separator;

	public static final String SPRING_FILE = path + CONF_DIR + "spring-dao.xml";
	
	public static final Map<Integer,Integer> COMMENTS_AUD = new ConcurrentHashMap<>(3000);
	
	public static final Map<Integer,String> VISITS = new ConcurrentHashMap<>(3000);
	
	public static final List<Integer> DYNAMICID = new ArrayList<>(3000);
	
	//在线接待客服缓存<客服所在组id-客服id,接待主播数>
	public static final Map<String,Integer> ONLINE_KF = new ConcurrentHashMap<>(100);
	
	//主播所在组缓存<主播id,主播所在组>
	public static final Map<Long,Integer> ANCHOR_GROUP = new HashMap<>();
	
	//分配主播接待<主播id,接待客服>
	public static final Map<Long,Long> ALLOC_ANCHOR = new ConcurrentHashMap<>(3000);
	
	public static final LoadingCache<String, VisitorCacheVo> VISITOR_CACHE = CacheBuilder.newBuilder()
			.initialCapacity(20000)
	        .maximumSize(Long.parseLong(getProperty("visitor.cache.size")))
	        .expireAfterWrite(Long.parseLong(getProperty("visitor.cache.min")), TimeUnit.MINUTES)
	        .build(
	            new CacheLoader<String, VisitorCacheVo>() {
					@Override
					public VisitorCacheVo load(String key) throws Exception {
						return this.load(key);
					}
	            });
	
	public static String getProperty(String str) {
		return confPro.getProperty(str);
	}
}
