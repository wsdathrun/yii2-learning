/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.entity.Dynamics_data;
import cn.ispeak.dynamics.real.entity.TaskInitReady;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 接待主播评论数统计任务
 * @author Mr Yin
 *
 */
@Service
public class CommentsTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(CommentsTask.class);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	private String tsDoc;
	private Long preTs;
	private Long nowTs;
	
	private int sleepMs = Integer.parseInt(Constants.getProperty("comments.sleep.ms"));
	
	public void initData(String tsDoc, Long preTs,Long nowTs) {
		this.tsDoc = tsDoc;
		this.preTs = preTs;
		this.nowTs = nowTs;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		//更新平台评论数、用户评论数
		dealComments(nowTs);
		
		//条件就绪时启动实时任务
		TaskInitReady.commentsReady = 1;
		if(TaskInitReady.getRead()){
			
			//加载统计表数据到缓存
			List<Dynamics_data> list = mongodbDao.queryDynamicsCache();
			list.forEach(d->{
				Constants.COMMENTS_AUD.put(d.getDynamic_id(), 
						(d.getComments_aud() == null?0:d.getComments_aud()));
				Constants.VISITS.put(d.getDynamic_id(), 
						(d.getVisits() == null?0:d.getVisits())+"-"+
						(d.getVisits_aud() == null?0:d.getVisits_aud()));
				Constants.DYNAMICID.add(d.getDynamic_id());
			});
			
			RealCommentsTask realcommentsTask = SpringContextUtil.getBean(RealCommentsTask.class);
			new Thread(realcommentsTask,"real-comments").start();
			RealVisitsTask realVisitsTask = SpringContextUtil.getBean(RealVisitsTask.class);
			new Thread(realVisitsTask,"real-visits").start();
			RealVisitorTask vReal = SpringContextUtil.getBean(RealVisitorTask.class);
			new Thread(vReal,"visitor-real-time").start();
		}
		
		//转正常统计流程
		while(true){
			try {
				//延迟一秒统计
				Long nextTs = new Date().getTime() / 1000-1;
				
				//数据库时间精度为秒
				if(nextTs - preTs > 0){
					dealComments(nextTs);
				}
				
				//线程休眠时间
				TimeUnit.MILLISECONDS.sleep(sleepMs);
			} catch (Exception e) {
				
				Random r = new Random();
				try {
					TimeUnit.MILLISECONDS.sleep(r.nextInt(1000));
				} catch (InterruptedException e1) {
					logger.error(e.getMessage(), e);
				}
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 更新平台评论数、用户评论数
	 * @param nextTs
	 */
	private void dealComments(Long nextTs) {
		List<Document> list = mongodbDao.queryCommentsList(null,preTs,nextTs);
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		int l = list.size();
		for(int k = 0;k<l;k++){
			Document s = list.get(k);
			BatchUpdateOptions o = new BatchUpdateOptions();
			o.setQuery(query(where("dynamic_id").is(s.getDouble("dynamic_id").intValue())));
			o.setUpdate(new Update().inc("comments_in",s.getDouble("comments_in").intValue())
					.inc("comments_out", s.getDouble("comments_out").intValue()));
			options.add(o);
			i++;
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_in,comments_out");
				options = new ArrayList<>();
				i = 0;
				logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow );
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_in,comments_out");
			logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow);
		}
		
		//更新ts
		mongodbDao.updateTs(tsDoc, nextTs);
		logger.debug("update {} ts to {} for {} data size.",tsDoc,nextTs,l);
		preTs = nextTs;
	}
}
