/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 处理实时统计数据，评论未评数
 * @author Mr Yin
 *
 */
@Service
public class RealCommentsTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(RealCommentsTask.class);
	
	private static int threadNum = Integer.parseInt(Constants.getProperty("real.thread.num"));
	private static int batchSize = Integer.parseInt(Constants.getProperty("dynamics.batch.size"));
	private static int sleepMs = Integer.parseInt(Constants.getProperty("real.comments.sleep.ms"));
	private static int updateMs = Integer.parseInt(Constants.getProperty("comments.update.ms"));
	
	private List<Callable<Boolean>> commentTasks;
	
	private Long preDealTime;
	
	@Autowired
	private MongodbDao mongodbDao;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("real task to deal with comments_aud.");
		
		ExecutorService commentPool = Executors.newFixedThreadPool(threadNum);
		
		while(true){
			
			commentTasks = new ArrayList<>();
			
			try{
				List<Integer> dynamicList = mongodbDao.queryValidDynamicList();
				int size = dynamicList.size();
				
				if (size > 0 && (preDealTime == null || new Date().getTime() / 1000 - preDealTime > updateMs)) {
					mongodbDao.updateCommentsStatus(preDealTime, dynamicList);
				}
				
				logger.debug("update dynamic_data comments_aud for {} row.",size);
				if(size > batchSize){
					int fromIndex = 0;
					int toIndex = 0;
					do{
						if(toIndex + batchSize < size){
							toIndex += batchSize;
						}else{
							toIndex = size;
						}
						List<Integer> subList = dynamicList.subList(fromIndex, toIndex);
						fromIndex += batchSize;
						
						submitTask(subList);
					}while(toIndex != size);
				}else{
					submitTask(dynamicList);
				}
				
				//等待所有线程完成
				if(!commentTasks.isEmpty()){
					List<Future<Boolean>> results = commentPool.invokeAll(commentTasks);
					for(Future<Boolean> f : results){
						f.get();
					}
				}
				
				TimeUnit.MILLISECONDS.sleep(sleepMs);
			}catch(Exception e){
				
				Random r = new Random();
				try {
					TimeUnit.MILLISECONDS.sleep(r.nextInt(1000));
				} catch (InterruptedException e1) {
					logger.error(e1.getMessage(), e1);
				}
				
				logger.error("此次 comments_aud 处理失败,"+e.getMessage(),e);
			}
		}
	}

	private void submitTask(List<Integer> dynamicList) {
		CommentsAudTask commentsAudTask = SpringContextUtil.getBean(CommentsAudTask.class);
		commentsAudTask.initData(dynamicList);
		commentTasks.add(commentsAudTask);
	}

}
