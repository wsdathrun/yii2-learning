/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.entity.Dynamics_data_ts;
import cn.ispeak.dynamics.real.entity.TsDocType;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.SpringContextUtil;

/**
 * 统计启动任务
 * @author Mr Yin
 *
 */
@Service
public class DynamicsBootstrap implements Runnable{
	
	private static final Logger logger = LoggerFactory.getLogger(DynamicsBootstrap.class);
	
	private static final Long switchControl = Long.valueOf(Constants.getProperty("switch.control"));
	
	@Autowired
	private MongodbDao mongodbDao;
	
	@Override
	public void run() {
		
		List<Dynamics_data_ts> allTs = mongodbDao.queryAllTs();
		
		Long nowTs = new Date().getTime()/1000;
		logger.info("do the bootstrap task to : {}",nowTs);
		
		boolean initFlag = false;
		if(CollectionUtils.isEmpty(allTs)){
			mongodbDao.insertTsRecord(nowTs);
			allTs = mongodbDao.queryAllTs();
			initFlag = true;
		}
		
		//客服线程
		if(switchControl.equals(1L)){
			DynamicQueueTask dynamicQueueTask = SpringContextUtil.getBean(DynamicQueueTask.class);
			try {
				dynamicQueueTask.call();
			} catch (Exception e) {
				logger.error("DynamicQueueTask error!",e);
			}
		}
		
		int size = allTs.size();
		for(int i = 0;i<size;i++){
			Dynamics_data_ts ts = allTs.get(i);
			String ts_doc = ts.getTs_doc();
			Long preTs = ts.getTs();
			
			switch(ts_doc){
			case TsDocType.SQUARE:
				//mongodb square_dynamics新动态，userid,dynamic_id,status
				DynamicTask squareTask = SpringContextUtil.getBean(DynamicTask.class);
				new Thread(squareTask,"square-task").start();
				break;
			case TsDocType.COMMENTS:
				//mongodb comments取评论数，comments_in，comments_out
				CommentsTask commentsTask = SpringContextUtil.getBean(CommentsTask.class);
				commentsTask.initData(ts_doc,initFlag?0L:preTs,nowTs);
				new Thread(commentsTask,"comments-task").start();
				break;
			case TsDocType.POINT:
				//mysql取点赞数，point_in，point_out
				PointTask pointTask = SpringContextUtil.getBean(PointTask.class);
				pointTask.initData(ts_doc, initFlag?0L:preTs,nowTs);
				new Thread(pointTask,"point-task").start();
				break;
			}
		}
	}
	
}
