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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.dao.MysqlDao;
import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.entity.DynamicPoint;
import cn.ispeak.dynamics.real.entity.Dynamics_data;
import cn.ispeak.dynamics.real.entity.TaskInitReady;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 接待主播动态点赞数任务
 * @author Mr Yin
 *
 */
@Service
public class PointTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(PointTask.class);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	@Autowired
	private MysqlDao mysqlDao;
	
	private String tsDoc;
	private Long preTs;
	private Long nowTs;
	
	private static int sleepMs = Integer.parseInt(Constants.getProperty("point.sleep.ms"));
	private static int delaySecond = Integer.parseInt(Constants.getProperty("deal.delay.second"));
	
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

		//更新平台点赞数、用户点赞数
		dealPointCount(nowTs);

		//条件就绪时启动实时任务
		TaskInitReady.pointReady = 1;
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
				Long nextTs = new Date().getTime() / 1000 - delaySecond;
				
				if(nextTs - preTs > 0){
					dealPointCount(nextTs);
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
	 * 更新平台点赞数、用户点赞数
	 * @param nextTs
	 * @return
	 */
	private List<DynamicPoint> dealPointCount(Long nextTs) {
		List<DynamicPoint> list = mysqlDao.queryPointCountList(null,preTs, nextTs);
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(DynamicPoint s:list){
			BatchUpdateOptions o = new BatchUpdateOptions();
			o.setQuery(query(where("dynamic_id").is(s.getDynamic_id())));
			o.setUpdate(new Update().inc("point_in",s.getPoint_in())
					.inc("point_out", s.getPoint_out()));
			options.add(o);
			i++;
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","points_in,point_out");
				options = new ArrayList<>();
				i = 0;
				logger.debug("point run : batch update or insert dynamics_data {} row.",batchRow );
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","points_in,point_out");
			logger.debug("point run : batch update or insert dynamics_data {} row.",batchRow);
		}
		
		//更新ts
		mongodbDao.updateTs(tsDoc, nextTs);
		logger.debug("update {} ts to {} for {} data size.",tsDoc,nextTs,list.size());
		preTs = nextTs;
		
		return list;
	}
}
