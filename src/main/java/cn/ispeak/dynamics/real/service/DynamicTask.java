/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
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
import cn.ispeak.dynamics.real.entity.Dynamics_record;
import cn.ispeak.dynamics.real.entity.Square_dynamics;
import cn.ispeak.dynamics.real.entity.TaskInitReady;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 接待主播动态任务
 * @author Mr Yin
 *
 */
@Service
public class DynamicTask implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(DynamicTask.class);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	@Autowired
	private MysqlDao mysqlDao;
	
	private static int sleepMs = Integer.parseInt(Constants.getProperty("dynamic.sleep.ms"));
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		dealDynamicsListByQueue();
		
		//条件就绪时启动实时任务
		TaskInitReady.squareReady = 1;
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
			
			try{
				dealDynamicsListByQueue();
				
				//线程休眠时间
				TimeUnit.MILLISECONDS.sleep(sleepMs);
			}catch(Exception e){
				
				Random r = new Random();
				try {
					TimeUnit.MILLISECONDS.sleep(r.nextInt(1000));
				} catch (InterruptedException e1) {
					logger.error(e.getMessage(), e);
				}
				
				logger.error(e.getMessage(),e);
			}
		}
	}

	/**
	 * 从新表处理动态上广场，下广场
	 */
	private void dealDynamicsListByQueue() {
		
		Set<Integer> newDynamicsSet = new HashSet<>();
		Set<Integer> delDynamicsSet = new HashSet<>();
		List<Integer> removeData = new ArrayList<>();
		
		List<Dynamics_record> optList = mongodbDao.queryDynamicsChangeList();
		optList.forEach(k->{
			int status = k.getStatus();
			int dynamics_id = k.getDynamic_id();
			removeData.add(k.get_id());
			
			if(status == 1){
				//先下广场，后上广场，则为上广场
				if(delDynamicsSet.contains(dynamics_id)){
					logger.info("dynamic id:{} first down square than up to sqaure.",dynamics_id);
					delDynamicsSet.remove(dynamics_id);
				}
				newDynamicsSet.add(dynamics_id);
			}else if(status == 0){
				//先上广场，后下广场，则为下广场
				if(newDynamicsSet.contains(dynamics_id)){
					logger.info("dynamic id:{} first up to square than down sqaure.",dynamics_id);
					newDynamicsSet.remove(dynamics_id);
				}
				delDynamicsSet.add(dynamics_id);
			}
		});
		
		//新上广场动态
		if(!newDynamicsSet.isEmpty()){
			
			logger.info("dynamics id {} up to square.",newDynamicsSet);
			
			//动态
			addToSquare(newDynamicsSet);
			
			//评论
			dealNewDynamicsComments(newDynamicsSet,mongodbDao.findTs("comments"));
			
			//点赞
			dealNewDynamicsPoints(newDynamicsSet,mongodbDao.findTs("dynamic_point"));
			
			//首次不用添加
			if(!Constants.DYNAMICID.isEmpty()){
				Constants.DYNAMICID.addAll(newDynamicsSet);
			}
		}
		
		//下广场动态
		if(!delDynamicsSet.isEmpty()){
			
			logger.info("dynamics id {} down square.",delDynamicsSet);
			
			mongodbDao.downSquare(delDynamicsSet);
		}
		
		//删除表中数据
		if(!removeData.isEmpty()){
			mongodbDao.removeUpDownSquareData(removeData);
		}
	}

	private void addToSquare(Set<Integer> newDynamicsList) {
		
		List<Square_dynamics> list = mongodbDao.queryDynamicsList(newDynamicsList);
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(Square_dynamics s:list){
			BatchUpdateOptions o = new BatchUpdateOptions();
			o.setQuery(query(where("dynamic_id").is(s.getDynamic_id())));
			o.setUpdate(update("userid",s.getUserid()).set("dynamic_id", s.getDynamic_id()).set("status", 1)
					.set("create_time", s.getCreate_time()).set("contents", s.getContents())
					.set("type", s.getType()));
			o.setUpsert(true);
			options.add(o);
			
			i++;
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","dynamics");
				options = new ArrayList<>();
				i = 0;
				logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow);
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","dynamics");
			logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow);
		}
	}
	
	/**
	 * 新接待主播动态评论数
	 * @param newUidList
	 */
	private void dealNewDynamicsComments(Set<Integer> newDynamicsList,Long nowTs) {
		List<Document> list = mongodbDao.queryCommentsList(newDynamicsList, 0L, nowTs);
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(int k = 0,l = list.size();k<l;k++){
			Document s =list.get(k);
			BatchUpdateOptions o = new BatchUpdateOptions();
			o.setQuery(query(where("dynamic_id").is(s.getDouble("dynamic_id").intValue())));
			o.setUpdate(new Update().set("comments_in",s.getDouble("comments_in").intValue())
					.set("comments_out", s.getDouble("comments_out").intValue()));
			options.add(o);
			i++;
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_in,comments_out");
				options = new ArrayList<>();
				i = 0;
				logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow );
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_in,comments_out");
			logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow);
		}
	}
	
	/**
	 * 新接待主播动态点赞数
	 * @param newUidList
	 */
	private void dealNewDynamicsPoints(Set<Integer> newDynamicsList,Long nowTs) {
		List<DynamicPoint> list = mysqlDao.queryPointCountList(newDynamicsList,0L, nowTs);
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(DynamicPoint s:list){
			BatchUpdateOptions o = new BatchUpdateOptions();
			o.setQuery(query(where("dynamic_id").is(s.getDynamic_id())));
			o.setUpdate(new Update().set("point_in",s.getPoint_in())
					.set("point_out", s.getPoint_out()));
			options.add(o);
			i++;
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","points_int,points_out");
				options = new ArrayList<>();
				i = 0;
				logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow);
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","points_int,points_out");
			logger.debug("dynamic run : batch update or insert dynamics_data {} row.",batchRow);
		}
	}
}
