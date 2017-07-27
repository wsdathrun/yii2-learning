/**
 * 
 */
package cn.ispeak.dynamics.real.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import cn.ispeak.dynamics.real.dao.MongodbDao;
import cn.ispeak.dynamics.real.dao.MysqlDao;
import cn.ispeak.dynamics.real.entity.AccountGrade;
import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.entity.Dynamics_onlines_history;
import cn.ispeak.dynamics.real.entity.UserInfo;
import cn.ispeak.dynamics.real.entity.VisitorCacheVo;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * 访客等级任务
 * @author Mr Yin
 *
 */
@Service
@Scope("prototype")
public class VisitorTask implements Callable<Boolean> {
	
	private static final Logger logger = LoggerFactory.getLogger(VisitorTask.class);
	
	@Autowired
	private MongodbDao mongodbDao;
	
	@Autowired
	private MysqlDao mysqlDao;
	
	private List<Integer> dynamicList;
	private Long nextTs;
	private Long preTs;

	public void initData(List<Integer> dynamicList,Long preTs,Long nextTs) {
		this.dynamicList = dynamicList;
		this.preTs = preTs;
		this.nextTs = nextTs;
	}

	@Override
	public Boolean call() throws Exception {
		
		List<Dynamics_onlines_history> visitorList = mongodbDao.queryVisitorList(dynamicList, preTs, nextTs);
		if(!CollectionUtils.isEmpty(visitorList)){
			int size = visitorList.size();
			List<Long> didList = new ArrayList<>();
			Map<Long,List<Integer>> dynamicIdMap = new HashMap<>();
			List<BatchUpdateOptions> visitorOptions = new ArrayList<>();
			for(int i = 1;i<=size;i++){
				
				Dynamics_onlines_history v = visitorList.get(i-1);
				BatchUpdateOptions o = new BatchUpdateOptions();
				o.setQuery(query(where("uid").is(v.getUserid()).and("dynamic_id").is(v.getDynamic_id())));
				o.setUpdate(update("last_review_time",new Date(v.getLast_review_time()*1000))
						.set("reply", v.getReply())
						.set("reply_time", v.getReply_time())
						.set("create_time", v.getCreate_time()));
				o.setUpsert(true);
				visitorOptions.add(o);
				
				didList.add(v.getUserid());
				
				//动态id
				List<Integer> list = dynamicIdMap.get(v.getUserid());
				if(list == null){
					List<Integer> cList = new ArrayList<>();
					cList.add(v.getDynamic_id());
					dynamicIdMap.put(v.getUserid(), cList);
				}else{
					list.add(v.getDynamic_id());
				}
				
				if((i%1000 == 0) || i == size){
					
					//规则：有则更新最后访问时间，没有则添加
					int batchRow = mongodbDao.doBatchUpdate(visitorOptions,"dynamics_visitor","last_review_time");
					logger.debug("visitor : batch update or insert dynamics_visitor {} row.",batchRow);
					
					//更新昵称
					updateNickName(didList,dynamicIdMap);
					
					//更新等级
					updateGrade(didList,dynamicIdMap);
					
					didList = new ArrayList<>();
					dynamicIdMap = new HashMap<>();
					visitorOptions = new ArrayList<>();
				}
			}
		}
		
		return true;
	}

	private void updateGrade(List<Long> didList,Map<Long,List<Integer>> dynamicIdMap) {
		List<AccountGrade> gradeList = mysqlDao.queryAccountGradeInfo(didList);
		List<BatchUpdateOptions> gradeOptions = new ArrayList<>();
		List<VisitorCacheVo> updateCacheList = new ArrayList<>();
		for(AccountGrade s:gradeList){
			
			List<Integer> list = dynamicIdMap.get(Long.valueOf(s.getUserid()));
			
			for(Integer dyId : list){
				//使用缓存,从缓存中查询到并值匹配,不更新数据库，否则更新数据库，查询不到则更新数据库
				VisitorCacheVo cacheVo = Constants.VISITOR_CACHE.getIfPresent(s.getUserid()+"-"+dyId);
				if(cacheVo != null){
					if(cacheVo.getDiannum_grade() != s.getDiannum_grade()
							|| cacheVo.getTotal_xf_grade() != s.getTotal_xf_grade()){
						BatchUpdateOptions go = new BatchUpdateOptions();
						go.setUid(s.getUserid());
						go.setQuery(query(where("uid").is(s.getUserid())));
						go.setUpdate(update("diannum_grade",s.getDiannum_grade())
								.set("total_xf_grade", s.getTotal_xf_grade()));
						go.setMulti(true);
						if(!gradeOptions.contains(go)){
							gradeOptions.add(go);
						}
						
						VisitorCacheVo v = new VisitorCacheVo();
						v.setUserid(s.getUserid());
						v.setDynamic_id(dyId);
						v.setDiannum_grade(s.getDiannum_grade());
						v.setTotal_xf_grade(s.getTotal_xf_grade());
						updateCacheList.add(v);
					}
				}else{
					BatchUpdateOptions go = new BatchUpdateOptions();
					go.setUid(s.getUserid());
					go.setQuery(query(where("uid").is(s.getUserid())));
					go.setUpdate(update("diannum_grade",s.getDiannum_grade())
							.set("total_xf_grade", s.getTotal_xf_grade()));
					go.setMulti(true);
					if(!gradeOptions.contains(go)){
						gradeOptions.add(go);
					}
					
					VisitorCacheVo v = new VisitorCacheVo();
					v.setUserid(s.getUserid());
					v.setDynamic_id(dyId);
					v.setDiannum_grade(s.getDiannum_grade());
					v.setTotal_xf_grade(s.getTotal_xf_grade());
					updateCacheList.add(v);
				}
			}
		}
		
		int gradeBatchRow = mongodbDao.doBatchUpdate(gradeOptions,"dynamics_visitor","diannum_grade,total_xf_grade");
		logger.debug("visitor grade : batch update or insert dynamics_visitor {} row.",gradeBatchRow);
		
		//更新缓存中的等级
		for(VisitorCacheVo vo : updateCacheList){
			VisitorCacheVo preCache = Constants.VISITOR_CACHE.getIfPresent(vo.getUserid()+"-"+vo.getDynamic_id());
			if(preCache != null){
				preCache.setDiannum_grade(vo.getDiannum_grade());
				preCache.setTotal_xf_grade(vo.getTotal_xf_grade());
				Constants.VISITOR_CACHE.put(preCache.getUserid()+"-"+preCache.getDynamic_id(), preCache);
			}else{
				Constants.VISITOR_CACHE.put(vo.getUserid()+"-"+vo.getDynamic_id(), vo);
			}
		}
	}

	private void updateNickName(List<Long> didList,Map<Long,List<Integer>> dynamicIdMap) {
		List<UserInfo> userList = mysqlDao.queryUserInfo(didList);
		List<BatchUpdateOptions> userOptions = new ArrayList<>();
		List<VisitorCacheVo> updateCacheList = new ArrayList<>();
		for(UserInfo s:userList){
			
			List<Integer> list = dynamicIdMap.get(Long.valueOf(s.getUid()));
			
			for(Integer dyId : list){
				//使用缓存,从缓存中查询到并值匹配,不更新数据库，否则更新数据库，查询不到则更新数据库
				VisitorCacheVo cacheVo = Constants.VISITOR_CACHE.getIfPresent(s.getUid()+"-"+dyId);
				if(cacheVo != null){
					if(!s.getNickname().equals(cacheVo.getNickname())){
						BatchUpdateOptions uo = new BatchUpdateOptions();
						uo.setUid(s.getUid());
						uo.setQuery(query(where("uid").is(s.getUid())));
						uo.setUpdate(update("nickname",s.getNickname()));
						uo.setMulti(true);
						if(!userOptions.contains(uo)){
							userOptions.add(uo);
						}
						
						VisitorCacheVo v = new VisitorCacheVo();
						v.setUserid(s.getUid());
						v.setDynamic_id(dyId);
						v.setNickname(s.getNickname());
						updateCacheList.add(v);
					}
				}else{
					BatchUpdateOptions uo = new BatchUpdateOptions();
					uo.setUid(s.getUid());
					uo.setQuery(query(where("uid").is(s.getUid())));
					uo.setUpdate(update("nickname",s.getNickname()));
					uo.setMulti(true);
					if(!userOptions.contains(uo)){
						userOptions.add(uo);
					}
					
					VisitorCacheVo v = new VisitorCacheVo();
					v.setUserid(s.getUid());
					v.setDynamic_id(dyId);
					v.setNickname(s.getNickname());
					updateCacheList.add(v);
				}
			}
		}
		
		int userBatchRow = mongodbDao.doBatchUpdate(userOptions,"dynamics_visitor","nickname");
		logger.debug("visitor nickname : batch update or insert dynamics_visitor {} row.",userBatchRow);
		
		//更新缓存中的昵称
		for(VisitorCacheVo vo : updateCacheList){
			VisitorCacheVo preCache = Constants.VISITOR_CACHE.getIfPresent(vo.getUserid()+"-"+vo.getDynamic_id());
			if(preCache != null){
				preCache.setNickname(vo.getNickname());
				Constants.VISITOR_CACHE.put(preCache.getUserid()+"-"+preCache.getDynamic_id(), preCache);
			}else{
				Constants.VISITOR_CACHE.put(vo.getUserid()+"-"+vo.getDynamic_id(), vo);
			}
		}
	}
}
