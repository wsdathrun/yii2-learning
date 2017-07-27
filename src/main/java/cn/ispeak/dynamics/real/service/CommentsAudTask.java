/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.dao.MysqlDao;
import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.entity.Comments;
import cn.ispeak.dynamics.real.entity.Square_dynamics;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * 接待主播评论未处理数任务
 * @author Mr Yin
 *
 */
@Service
@Scope("prototype")
public class CommentsAudTask implements Callable<Boolean>{
	
	private static final Logger logger = LoggerFactory.getLogger(CommentsAudTask.class);
	
	private static final String defaultKfUid = Constants.getProperty("default.kf.uid");
	private static final Long switchControl = Long.valueOf(Constants.getProperty("switch.control"));
	
	@Autowired
	private MongodbDao mongodbDao;
	@Autowired
	private MysqlDao mysqlDao;
	
	private List<Integer> dynamicList;

	public void initData(List<Integer> dynamicList) {
		List<Integer> list = new ArrayList<>();
		list.addAll(dynamicList);
		this.dynamicList = list;
	}

	@Override
	public Boolean call() throws Exception {
		
		try{
			//更新平台评论未处理数
			List<Comments> list = mongodbDao.queryCommentsAudList(dynamicList);
			List<BatchUpdateOptions> options = new ArrayList<>();
			Set<Integer> dynamicIdList = new HashSet<>();
			int i = 0;
			List<Integer> findList = new ArrayList<>();
			for(int k = 0,l = list.size();k<l;k++){
				Comments s = list.get(k);
				BatchUpdateOptions o = new BatchUpdateOptions();
				
				Integer dynamicId = s.getDynamic_id();
				Integer newVal = s.getComments_aud();
				if(dynamicList.contains(dynamicId)){
					findList.add(dynamicId);
				}
				
				Integer oldVal = Constants.COMMENTS_AUD.get(dynamicId);
				if(oldVal == null){
					i++;
					//没有原值，新的动态，更新数据库和map
					Constants.COMMENTS_AUD.put(dynamicId, newVal);
					o.setQuery(query(where("dynamic_id").is(dynamicId)));
					o.setUpdate(update("comments_aud",s.getComments_aud())
							.set("comments_time", new Date().getTime()/1000));
					options.add(o);
					dynamicIdList.add(dynamicId);
				}else{
					//仅变化时更新数据库及map
					if(!oldVal.equals(newVal)){
						i++;
						Constants.COMMENTS_AUD.put(dynamicId, newVal);
						o.setQuery(query(where("dynamic_id").is(dynamicId)));
						o.setUpdate(update("comments_aud",s.getComments_aud())
								.set("comments_time", new Date().getTime()/1000));
						options.add(o);
						
						//仅评论增加时分配
						if(oldVal.compareTo(newVal) < 0){
							dynamicIdList.add(dynamicId);
						}
					}
				}
				
				if(i == 1000){
					int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_aud");
					i = 0;
					allocAnchorToKf(dynamicIdList);
					options = new ArrayList<>();
					dynamicIdList = new HashSet<>();
					logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow);
				}
			}
			if(i != 0){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_aud");
				allocAnchorToKf(dynamicIdList);
				logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow);
			}
			
			//更新为0的数据
			dynamicList.removeAll(findList);
			updateZeroCommentsAud(dynamicList);
			
		}catch(Exception e){
			logger.error("此次 comments_aud 处理失败,"+e.getMessage(),e);
		}
		return true;
	}
	
	private void updateZeroCommentsAud(List<Integer> dynamicList) {
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(int k = 0,l = dynamicList.size();k<l;k++){
			Integer dynamicId = dynamicList.get(k);
			
			BatchUpdateOptions o = new BatchUpdateOptions();
			
			Integer oldVal = Constants.COMMENTS_AUD.get(dynamicId);
			if(oldVal == null){
				i++;
				//没有原值，新的动态，更新数据库和map
				Constants.COMMENTS_AUD.put(dynamicId, 0);
				o.setQuery(query(where("dynamic_id").is(dynamicId)));
				o.setUpdate(update("comments_aud",0));
				options.add(o);
			}else{
				//仅变化时更新数据库及map
				if(!oldVal.equals(0)){
					i++;
					Constants.COMMENTS_AUD.put(dynamicId, 0);
					o.setQuery(query(where("dynamic_id").is(dynamicId)));
					o.setUpdate(update("comments_aud",0));
					options.add(o);
				}
			}
			
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_aud");
				i = 0;
				options = new ArrayList<>();
				logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow);
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","comments_aud");
			logger.debug("comments run : batch update or insert dynamics_data {} row.",batchRow);
		}
	}

	/**
	 * 为主播分配接待客服
	 * @param options
	 */
	private void allocAnchorToKf(Set<Integer> dynamicIdList) {
		
		if(switchControl.equals(0L)){
			return;
		}
		
		if(dynamicIdList.isEmpty()){
			return;
		}
		
		List<Square_dynamics> list = mongodbDao.queryDynamicsList(dynamicIdList);
		
		list.forEach(s->{
			Long anchorId = Long.valueOf(s.getUserid());
			Integer group = Constants.ANCHOR_GROUP.get(anchorId);
			
			if(group == null){
				logger.info("anchor id : {} is not in group,so don't alloc",anchorId);
				return ;
			}
			
			Long kfid = Constants.ALLOC_ANCHOR.get(anchorId);
			if(kfid == null || defaultKfUid.equals(kfid+"")){
				//未分配(包括已分配至默认客服)
				Iterator<Entry<String, Integer>> iterator = Constants.ONLINE_KF.entrySet().iterator();
				
				String kfgUid = "1-1";
				Integer value = Integer.MAX_VALUE;
				
				while(iterator.hasNext()){
					Entry<String, Integer> entry = iterator.next();
					//仅分配主播到同组客服
					if(!entry.getKey().equals(defaultKfUid) && entry.getKey().startsWith(group+"-")
							&& entry.getValue().compareTo(value) < 0){
						value = entry.getValue();
						kfgUid = entry.getKey();
					}
				}
				
				if(!"1-1".equals(kfgUid)){
					//更新接待主播表
					Long kfUid = Long.valueOf(kfgUid.split("-")[1]);
					mysqlDao.updateAnchorAlloc(kfUid,anchorId);
					
					//为该动态所在主播分配至目前接待最少的客服
					Constants.ALLOC_ANCHOR.put(anchorId,kfUid);
					Constants.ONLINE_KF.put(kfgUid,value+1);
					
					logger.info("alloc anchor id : {} group : {} to kf id : {} ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {}",anchorId,group,kfUid,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
				}else{
					//暂无客服在线且未分配到默认客服
					if(kfid == null){
						//更新接待主播表
						mysqlDao.updateAnchorAlloc(Long.valueOf(defaultKfUid),anchorId);
						Constants.ALLOC_ANCHOR.put(anchorId,Long.valueOf(defaultKfUid));
						
						logger.info("alloc anchor id : {} group : {} to default ONLINE_KF : {} ANCHOR_GROUP : {} ALLOC_ANCHOR : {}",anchorId,group,Constants.ONLINE_KF,Constants.ANCHOR_GROUP,Constants.ALLOC_ANCHOR);
					}
				}
			}
		});
	}
}
