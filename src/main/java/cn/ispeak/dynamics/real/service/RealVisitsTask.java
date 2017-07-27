/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import java.util.ArrayList;
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
 * 处理实时统计数据，访客总数、访客未处理数
 * @author Mr Yin
 *
 */
@Service
public class RealVisitsTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(RealVisitsTask.class);
	
	private static int threadNum = Integer.parseInt(Constants.getProperty("real.thread.num"));
	private static int batchSize = Integer.parseInt(Constants.getProperty("dynamics.batch.size"));
	private static int sleepMs = Integer.parseInt(Constants.getProperty("real.visits.sleep.ms"));
	
	private List<Callable<Boolean>> visitTasks;
	
	@Autowired
	private MongodbDao mongodbDao;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("real task to deal with visits,visits_aud.");
		
		ExecutorService visitPool = Executors.newFixedThreadPool(threadNum);
		
		while(true){
			
			visitTasks = new ArrayList<>();
			
			try{
				List<Integer> dynamicList = mongodbDao.queryValidDynamicList();
				int size = dynamicList.size();
				logger.debug("update dynamic_data visits,visits_aud for {} row.",size);
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
				List<Future<Boolean>> results2 = visitPool.invokeAll(visitTasks);
				for(Future<Boolean> f : results2){
					f.get();
				}
				
				TimeUnit.MILLISECONDS.sleep(sleepMs);
			}catch(Exception e){
				
				Random r = new Random();
				try {
					TimeUnit.MILLISECONDS.sleep(r.nextInt(1000));
				} catch (InterruptedException e1) {
					logger.error(e1.getMessage(), e1);
				}
				
				logger.error("此次 visits,visits_aud 处理失败,"+e.getMessage(),e);
			}
		}
	}

	private void submitTask(List<Integer> dynamicList) {
		OnlineTask onlineTask = SpringContextUtil.getBean(OnlineTask.class);
		onlineTask.initData(dynamicList);
		visitTasks.add(onlineTask);
	}

}
