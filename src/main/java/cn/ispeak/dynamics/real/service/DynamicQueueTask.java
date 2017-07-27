package cn.ispeak.dynamics.real.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.dao.MysqlDao;
import cn.ispeak.dynamics.real.entity.Community_anchor_group;
import cn.ispeak.dynamics.real.entity.Community_kf_login_out;
import cn.ispeak.dynamics.real.entity.Kf_anchor_queue;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * 客服上下班打卡、主播添加/移出组
 * @author Mr Yin
 *
 */
@Service
public class DynamicQueueTask implements Callable<Boolean> {
	
	private static final Logger logger = LoggerFactory.getLogger(DynamicQueueTask.class);
	
	private static int sleepMs = Integer.parseInt(Constants.getProperty("dynamic.queue.sleep.ms"));
	
	@Autowired
	private MongodbDao mongodbDao;
	
	@Autowired
	private MysqlDao mysqlDao;

	@Override
	public Boolean call() throws Exception {
		
		logger.info("begin to init ONLINE_KF,ANCHOR_GROUP");
		
		//启动时初始化主播所在组
		List<Community_anchor_group> groupList = mysqlDao.queryAnchorGroup();
		groupList.forEach(c->Constants.ANCHOR_GROUP.put(c.getAuid(), c.getGid()));
		logger.info("ANCHOR_GROUP : {}",Constants.ANCHOR_GROUP);
		
		//客服已接待主播数量
		Map<Long,Integer> allocNum = mysqlDao.queryKfAllocNum();
		//启动时初始化客服上班情况
		List<Community_kf_login_out> kfList = mysqlDao.queryKfLoginOut();
		kfList.forEach(k->{
			int status = k.getStatus();
			long kfuid = k.getUid();
			int cmgroup = k.getCmgroup();
			if(status == 0){
				Constants.ONLINE_KF.remove(cmgroup+"-"+kfuid);
			}else if(status == 1){
				Constants.ONLINE_KF.put(cmgroup+"-"+kfuid, allocNum.get(kfuid)==null?0:allocNum.get(kfuid));
			}
		});
		logger.info("ONLINE_KF : {}",Constants.ONLINE_KF);
		
		//已分配接待
		Map<Long,Long> map = mysqlDao.queryAllocMap();
		Constants.ALLOC_ANCHOR.putAll(map);
		logger.info("ALLOC_ANCHOR : {}",Constants.ALLOC_ANCHOR);
		
		//转正常统计流程
		new Thread(new Runnable(){
			@Override
			public void run() {
				logger.info("normal deal ONLINE_KF,ANCHOR_GROUP data change ... ");
				while(true){
					
					try{
						List<Kf_anchor_queue> list = mongodbDao.queryDynamicQueue();
						List<ObjectId> removeData = new ArrayList<ObjectId>();
						for(int i = 0,l = list.size();i<l;i++){
							Kf_anchor_queue q = list.get(i);
							
							int type = q.getType();
							Document doc = q.getDoc();
							Integer status = doc.getInteger("status");
							Long userid = doc.getLong("userid");
							if(type == 2){
								Integer cmgroup= doc.getInteger("cmgroup");
								//上下班打卡
								if(status == 1){
									Constants.ONLINE_KF.put(cmgroup+"-"+userid,0);
									logger.info("kf uid : {} preparing alloc. ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {}",userid,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
								}else if(status == 0){
									Constants.ONLINE_KF.remove(cmgroup+"-"+userid);
									
									//移除其正在接待的主播
									Iterator<Entry<Long, Long>> iterator = Constants.ALLOC_ANCHOR.entrySet().iterator();
									while(iterator.hasNext()){
										Entry<Long, Long> entry = iterator.next();
										Long value = entry.getValue();
										if(value.equals(userid)){
											Constants.ALLOC_ANCHOR.remove(entry.getKey());
										}
									}
									//更新接待表为默认接待主播
									mysqlDao.updateKFAnchor(userid);
									
									logger.info("kf uid : {} ending alloc. ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {} ",userid,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
								}
							}else if(type == 1){
								//添加移出组
								Integer group = doc.getInteger("group");
								if(status == 1){
									Constants.ANCHOR_GROUP.put(userid, group);
									logger.info("anchor uid : {} add to {}. ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {} ",userid,group,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
								}else if(status == 0){
									Constants.ANCHOR_GROUP.remove(userid);
									//接待当前主播的客服接待数减1
									Long allocKfUid = Constants.ALLOC_ANCHOR.get(userid);
									if(allocKfUid != null){
										Integer allocNum = Constants.ONLINE_KF.get(group+"-"+allocKfUid);
										if(allocNum != null){
											Constants.ONLINE_KF.put(group+"-"+allocKfUid,--allocNum);
										}
										//移出正在接待
										Constants.ALLOC_ANCHOR.remove(userid);
									}
									
									logger.info("anchor uid : {} remove from group :{}. ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {}",userid,group,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
								}
							}
							
							removeData.add(q.get_id());
						}
						
						//删除处理完的数据
						if(removeData.size() > 0){
							mongodbDao.removeQueueData(removeData);
						}

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
		
		},"dynamic-queue").start();
		
		return true;
	}

}
