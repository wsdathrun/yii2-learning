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

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.entity.Dynamics_data_ts;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 处理实时访客列表，包含其等级
 * @author Mr Yin
 *
 */
@Service
public class RealVisitorTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(RealVisitorTask.class);
	
	private static int threadNum = Integer.parseInt(Constants.getProperty("real.thread.num"));
	private static int sleepMs = Integer.parseInt(Constants.getProperty("real.visitor.sleep.ms"));
	private static int delaySecond = Integer.parseInt(Constants.getProperty("deal.delay.second"));
	private ExecutorService visitorPool = Executors.newFixedThreadPool(threadNum);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	private Long preTs = null;
	private List<Callable<Boolean>> tasks;
	
	private int minute = Integer.parseInt(Constants.getProperty("visit.remain.min"));
	private int batchSize = Integer.parseInt(Constants.getProperty("dynamics.batch.size"));

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("real task to deal with dynamic visitor.");
		
		Long nextTs = null;
		//Long delTs = null;
		
		while(true){
			try{
				tasks = new ArrayList<>();
				
				nextTs = new Date().getTime()/1000 - delaySecond;
				
				//首次运行(重启)
				if(preTs == null){
					preTs = mongodbDao.findTs("dynamics_visitor");
					//首次启动
					if(preTs == null){
						preTs = nextTs - 60*minute;
						
						Dynamics_data_ts visitor_ts = new Dynamics_data_ts();
						visitor_ts.set_id(new ObjectId());
						visitor_ts.setTs_doc("dynamics_visitor");
						visitor_ts.setTs(nextTs);
						mongodbDao.insertTs(visitor_ts);
					}else{
						//重新启动
						if(nextTs - preTs > 60*minute){
							preTs = nextTs - 60*minute;
						}
					}
				}
				
				if(nextTs - preTs > 0){
					executeTask(nextTs);
				}
				
				//线程休眠时间
				TimeUnit.MILLISECONDS.sleep(sleepMs);
			}catch(Exception e){
				
				Random r = new Random();
				try {
					TimeUnit.MILLISECONDS.sleep(r.nextInt(1000));
				} catch (InterruptedException e1) {
					logger.error(e1.getMessage(), e1);
				}
				
				logger.error("此次 dynamics_visitor 处理失败,"+e.getMessage(),e);
			}
		}
	}

	private void executeTask(Long nextTs) throws Exception{
		List<Integer> dynamicList = mongodbDao.queryValidDynamicList();
		int size = dynamicList.size();
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
				
				submitTask(subList,preTs,nextTs);
			}while(toIndex != size);
		}else{
			submitTask(dynamicList,preTs,nextTs);
		}
		
		//等待所有线程完成
		if(!tasks.isEmpty()){
			List<Future<Boolean>> results = visitorPool.invokeAll(tasks);
			for(Future<Boolean> f : results){
				f.get();
			}
		}
		
		//删除一小时前的数据 使用数据库自动过期
		//delTs = nextTs - 60*minute;
		//mongodbDao.removeHourPreData(delTs);
		
		//更新ts
		mongodbDao.updateTs("dynamics_visitor", nextTs);
		logger.debug("update dynamics_visitor ts to {} for {} dynamic's row.",nextTs,size);
		preTs = nextTs;
	}

	private void submitTask(List<Integer> dynamicList,Long preTs,Long nextTs) {
		
		VisitorTask visitorTask = SpringContextUtil.getBean(VisitorTask.class);
		visitorTask.initData(dynamicList,preTs,nextTs);
		tasks.add(visitorTask);
	}

}
