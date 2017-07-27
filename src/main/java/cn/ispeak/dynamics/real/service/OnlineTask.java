/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * 接待主播动态访客数、访客未处理数任务
 * @author Mr Yin
 *
 */
@Service
@Scope("prototype")
public class OnlineTask implements Callable<Boolean> {
	
	private static final Logger logger = LoggerFactory.getLogger(OnlineTask.class);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	private List<Integer> dynamicList;
	
	public void initData(List<Integer> dynamicList) {
		List<Integer> list = new ArrayList<>();
		list.addAll(dynamicList);
		this.dynamicList = list;
	}
	
	@Override
	public Boolean call() throws Exception {
		try{
			//更新访客数、访客未处理数
			List<Document> list = mongodbDao.queryOnlineList(dynamicList);
			List<BatchUpdateOptions> options = new ArrayList<>();
			List<Integer> findList = new ArrayList<>();
			int i = 0;
			for(int k = 0,l = list.size();k<l;k++){
				Document s = list.get(k);
				BatchUpdateOptions o = new BatchUpdateOptions();
				
				Integer dynamicId = s.getDouble("dynamic_id").intValue();
				
				int newVisits = s.getDouble("visits").intValue();
				int newAud = s.getDouble("visits_aud").intValue();
				String newVal = newVisits+"-"+newAud;
				
				if(dynamicList.contains(dynamicId)){
					findList.add(dynamicId);
				}
				
				String oldVal = Constants.VISITS.get(dynamicId);
				if(oldVal == null){
					i++;
					//没有原值，新的动态，更新数据库和map
					Constants.VISITS.put(dynamicId, newVal);
					o.setQuery(query(where("dynamic_id").is(dynamicId)));
					o.setUpdate(update("visits",newVisits)
							.set("visits_aud", newAud));
					options.add(o);
				}else{
					//仅变化时更新数据库及map
					if(!oldVal.equals(newVal)){
						i++;
						Constants.VISITS.put(dynamicId, newVal);
						o.setQuery(query(where("dynamic_id").is(dynamicId)));
						o.setUpdate(update("visits",newVisits)
								.set("visits_aud",newAud));
						options.add(o);
					}
				}
				
				if(i == 1000){
					int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","visits,visits_aud");
					options = new ArrayList<>();
					i = 0;
					logger.debug("online run : batch update or insert dynamics_data {} row.",batchRow );
				}
			}
			if(i != 0){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","visits,visits_aud");
				logger.debug("online run : batch update or insert dynamics_data {} row.",batchRow);
			}
			
			//更新为0的数据
			dynamicList.removeAll(findList);
			updateZeroOnlineAud(dynamicList);
		}catch(Exception e){
			logger.error("此次 visits,visits_aud 处理失败,"+e.getMessage(),e);
		}
		return true;
	}

	private void updateZeroOnlineAud(List<Integer> dynamicList) {
		List<BatchUpdateOptions> options = new ArrayList<>();
		int i = 0;
		for(int k = 0,l = dynamicList.size();k<l;k++){
			Integer dynamicId = dynamicList.get(k);
			BatchUpdateOptions o = new BatchUpdateOptions();
			
			String oldVal = Constants.VISITS.get(dynamicId);
			if(oldVal == null){
				i++;
				//没有原值，新的动态，更新数据库和map
				Constants.VISITS.put(dynamicId, "0-0");
				o.setQuery(query(where("dynamic_id").is(dynamicId)));
				o.setUpdate(update("visits",0)
						.set("visits_aud", 0));
				options.add(o);
			}else{
				//仅变化时更新数据库及map
				if(!oldVal.equals("0-0")){
					i++;
					Constants.VISITS.put(dynamicId, "0-0");
					o.setQuery(query(where("dynamic_id").is(dynamicId)));
					o.setUpdate(update("visits",0)
							.set("visits_aud",0));
					options.add(o);
				}
			}
			
			if(i == 1000){
				int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","visits,visits_aud");
				options = new ArrayList<>();
				i = 0;
				logger.debug("online run : batch update or insert dynamics_data {} row.",batchRow );
			}
		}
		if(i != 0){
			int batchRow = mongodbDao.doBatchUpdate(options,"dynamics_data","visits,visits_aud");
			logger.debug("online run : batch update or insert dynamics_data {} row.",batchRow);
		}
	}
}
