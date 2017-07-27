/**
 * 
 */
package cn.ispeak.dynamics.real.dao;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapreduce.GroupBy;
import org.springframework.data.mongodb.core.mapreduce.GroupByResults;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.mongodb.BasicDBObject;

import cn.ispeak.dynamics.real.entity.BatchUpdateOptions;
import cn.ispeak.dynamics.real.entity.Comments;
import cn.ispeak.dynamics.real.entity.Dynamics_data;
import cn.ispeak.dynamics.real.entity.Dynamics_data_ts;
import cn.ispeak.dynamics.real.entity.Dynamics_onlines_history;
import cn.ispeak.dynamics.real.entity.Dynamics_record;
import cn.ispeak.dynamics.real.entity.Kf_anchor_queue;
import cn.ispeak.dynamics.real.entity.Square_dynamics;
import cn.ispeak.dynamics.real.util.Constants;

/**
 * @author Mr Yin
 *
 */
@Repository
public class MongodbDao {
	
	private static final Logger logger = LoggerFactory.getLogger(MongodbDao.class);
	
	private static final String COMMENTS_FUN = "function(doc,prev){if(doc.pub_type == 0){prev.comments_out += 1;}"
			+ "if(doc.pub_type == 1){prev.comments_in += 1;}}";

	private static final String VISITS_FUN = "function(doc,prev){if(doc.reply == 0){prev.visits_aud += 1;}prev.visits +=1;}";
	
	private int batchSize = Integer.parseInt(Constants.getProperty("dynamics.batch.size"));
	private int minute = Integer.parseInt(Constants.getProperty("visit.remain.min"));
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	/**
	 * 插入统计更新时间
	 * @param nowTs
	 */
	public void insertTsRecord(Long nowTs) {
		List<Dynamics_data_ts> tsList = new ArrayList<>();
		
		//动态广场更新时间
		Dynamics_data_ts square_doc = new Dynamics_data_ts();
		square_doc.set_id(new ObjectId());
		square_doc.setTs_doc("square_dynamics");
		square_doc.setTs(nowTs);
		tsList.add(square_doc);
		
		//评论更新时间
		Dynamics_data_ts comments_doc = new Dynamics_data_ts();
		comments_doc.set_id(new ObjectId());
		comments_doc.setTs_doc("comments");
		comments_doc.setTs(nowTs);
		tsList.add(comments_doc);
		
		Dynamics_data_ts point_doc = new Dynamics_data_ts();
		point_doc.set_id(new ObjectId());
		point_doc.setTs_doc("dynamic_point");
		point_doc.setTs(nowTs);
		tsList.add(point_doc);
		mongoTemplate.insert(tsList, "dynamics_data_ts");
	}
	
	public void insertTs(Dynamics_data_ts ts) {
		mongoTemplate.insert(ts,"dynamics_data_ts");
	}

	/**
	 * 更新某一统计的更新时间
	 * 
	 * @param type
	 * @param newTs
	 */
	public void updateTs(String type, Long newTs) {
		mongoTemplate.updateFirst(query(where("ts_doc").is(type)), update("ts", newTs), Dynamics_data_ts.class);
	}

	/**
	 * 查询所有最后更新时间
	 * 
	 * @return
	 */
	public List<Dynamics_data_ts> queryAllTs() {
		return mongoTemplate.find(query(new Criteria()), Dynamics_data_ts.class);
	}
	
	/**
	 * 查询指定类型的更新时间
	 * @param type
	 * @return
	 */
	public Long findTs(String type){
		List<Dynamics_data_ts> find = mongoTemplate.find(query(where("ts_doc").is(type)), Dynamics_data_ts.class);
		
		if(CollectionUtils.isEmpty(find)){
			return null;
		}
		return find.get(0).getTs();
	}

	/**
	 * 批量更新统计表
	 * @param options
	 * @param upTab
	 * @param type
	 * @return
	 */
	public int doBatchUpdate(List<BatchUpdateOptions> options,String upTab,String column) {
		
		if(options.isEmpty()){
			return 0;
		}
		
		Document command = new Document();
		command.put("update", upTab);
		List<BasicDBObject> updateList = new ArrayList<>();
		for (BatchUpdateOptions option : options) {
			BasicDBObject update = new BasicDBObject();
			update.put("q", option.getQuery().getQueryObject());
			update.put("u", option.getUpdate().getUpdateObject());
			update.put("upsert", option.isUpsert());
			update.put("multi", option.isMulti());
			updateList.add(update);
		}
		
		logger.info("execute batch update for {} row on {} for column {} at {}",updateList.size(),upTab,column,new Date().getTime());
		
		command.put("updates", updateList);
		Document commandResult = mongoTemplate.getDb().runCommand(command);
		logger.info("{}",command);
		
		return Integer.parseInt(commandResult.get("n").toString());
	}

	/**
	 * 查询区间内各动态的评论统计数
	 * @param didSet
	 * @param preTs
	 * @param nextTs
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Document> queryCommentsList(Set<Integer> didSet,Long preTs, Long nextTs) {
		
		List<Integer> didList=new ArrayList<>();
		//查询正在接待的主播动态列表
		if(didSet == null){
			didList = queryValidDynamicList();
		}else{
			Iterator<Integer> iterator = didSet.iterator();
			while(iterator.hasNext()){
				didList.add(iterator.next());
			}
		}
		
		//分批查询动态评论统计数
		List<Document> list = new ArrayList<>();
		int size = didList.size();
		int fromIndex = 0;
		int toIndex = 0;
		if(size >batchSize){
			do{
				if(toIndex + batchSize < size){
					toIndex += batchSize;
				}else{
					toIndex = size;
				}
				List<Integer> subList = didList.subList(fromIndex, toIndex);
				fromIndex += batchSize;
				GroupByResults<Comments> results = mongoTemplate.group(
						where("dynamic_id").in(subList).and("create_time").gt(preTs).lte(nextTs),
						"comments",
						 GroupBy.key("dynamic_id")
						 		.initialDocument("{comments_in:0,comments_out:0}")
						 		.reduceFunction(COMMENTS_FUN),
						 Comments.class);
				if(results.getCount()>0){
					Document rawResults = results.getRawResults();
					list.addAll((List<Document>)rawResults.get("retval"));
				}
			}while(toIndex != size);
		}else{
			GroupByResults<Comments> results = mongoTemplate.group(
					where("dynamic_id").in(didList).and("create_time").gt(preTs).lte(nextTs),
					"comments", 
					 GroupBy.key("dynamic_id")
					 		.initialDocument("{comments_in:0,comments_out:0}")
					 		.reduceFunction(COMMENTS_FUN),
					 Comments.class);
			if(results.getCount()>0){
				Document rawResults = results.getRawResults();
				list = (List<Document>) rawResults.get("retval");
			}
		}
		
		return list;
	}

	/**
	 * 查询正在接待的主播动态列表
	 * @return
	 */
	public List<Integer> queryValidDynamicList() {
		
		if(Constants.DYNAMICID.isEmpty()){
			Document fieldObject = new Document();
			fieldObject.put("dynamic_id", true);
			Document queryObject = new Document();
			queryObject.put("status", 1);
			Query query = new BasicQuery(queryObject,fieldObject);
			List<Dynamics_data> dynamicList = mongoTemplate.find(query,Dynamics_data.class);
			dynamicList.forEach(d->Constants.DYNAMICID.add(d.getDynamic_id()));
		}
		
		return Constants.DYNAMICID;
	}

	/**
	 * 查询最近一小时动态访客数
	 * @param dynamicList
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Document> queryOnlineList(List<Integer> dynamicList) {
		List<Document> list = new ArrayList<>();
		Long nowTs = new Date().getTime()/1000;
		Long preTs = nowTs - 60*minute;
		
		//添加date查询条件，使用索引
		String preDate = sdf.format(new Date(preTs*1000));
		String nowDate = sdf.format(new Date(nowTs*1000));
		
		if(preDate.equals(nowDate)){
			GroupByResults<Dynamics_onlines_history> results = mongoTemplate.group(
					where("last_review_time").gt(preTs).lte(nowTs)
							.and("dynamic_id").in(dynamicList)
							.and("date").is(preDate),
					"dynamics_onlines_history",
					 GroupBy.key("dynamic_id")
					 		.initialDocument("{visits:0,visits_aud:0}")
					 		.reduceFunction(VISITS_FUN),
					 Dynamics_onlines_history.class);
			if(results.getCount()>0){
				Document rawResults = results.getRawResults();
				list = (List<Document>) rawResults.get("retval");
			}
		}else{
			
			//前一天
			GroupByResults<Dynamics_onlines_history> results = mongoTemplate.group(
					where("last_review_time").gt(preTs).lte(nowTs)
							.and("dynamic_id").in(dynamicList)
							.and("date").is(preDate),
					"dynamics_onlines_history",
					 GroupBy.key("dynamic_id")
					 		.initialDocument("{visits:0,visits_aud:0}")
					 		.reduceFunction(VISITS_FUN),
					 Dynamics_onlines_history.class);
			if(results.getCount()>0){
				Document rawResults = results.getRawResults();
				list.addAll((List<Document>) rawResults.get("retval"));
			}
			
			//今天
			GroupByResults<Dynamics_onlines_history> results1 = mongoTemplate.group(
					where("last_review_time").gt(preTs).lte(nowTs)
							.and("dynamic_id").in(dynamicList)
							.and("date").is(preDate),
					"dynamics_onlines_history",
					 GroupBy.key("dynamic_id")
					 		.initialDocument("{visits:0,visits_aud:0}")
					 		.reduceFunction(VISITS_FUN),
					 Dynamics_onlines_history.class);
			if(results.getCount()>0){
				Document rawResults = results1.getRawResults();
				list.addAll((List<Document>) rawResults.get("retval"));
			}
		}
		
		return list;
	}
	
	/**
	 * 查询区间内访客列表
	 * @param dynamicList
	 * @param preTs
	 * @param nextTs
	 * @return
	 */
	public List<Dynamics_onlines_history> queryVisitorList(List<Integer> dynamicList,Long preTs,Long nextTs) {
		
		List<Dynamics_onlines_history> list = new ArrayList<>();
		
		//添加date查询条件，使用索引
		String preDate = sdf.format(new Date(preTs*1000));
		String nextDate = sdf.format(new Date(nextTs*1000));
		
		if(preDate.equals(nextDate)){
			list = mongoTemplate.find(query(where("last_review_time").gt(preTs).lte(nextTs)
					.and("dynamic_id").in(dynamicList)
					.and("date").is(preDate)), Dynamics_onlines_history.class);
		}else{
			//前一天
			list.addAll(mongoTemplate.find(query(where("last_review_time").gt(preTs).lte(nextTs)
					.and("dynamic_id").in(dynamicList)
					.and("date").is(preDate)), Dynamics_onlines_history.class));
			//今天
			list.addAll(mongoTemplate.find(query(where("last_review_time").gt(preTs).lte(nextTs)
					.and("dynamic_id").in(dynamicList)
					.and("date").is(nextDate)), Dynamics_onlines_history.class));
		}
		
		return list;
	}

	/**
	 * 查询评论未处理数
	 * @param dynamicList
	 * @return
	 */
	public List<Comments> queryCommentsAudList(List<Integer> dynamicList) {
		
		Aggregation agg = newAggregation(
			    project("dynamic_id","review"),
			    match(where("dynamic_id").in(dynamicList).and("review").is(0)),
			    group("dynamic_id").count().as("comments_aud")
			    .last("dynamic_id").as("dynamic_id")
			);

		AggregationResults<Comments> results = mongoTemplate.aggregate(agg, "comments", Comments.class);
		return results.getMappedResults();
	}

	public List<Dynamics_data> queryDynamicsCache() {
		Document q = new Document();
		q.append("status", 1);
		Document f = new Document();
		f.append("dynamic_id", true);
		f.append("comments_aud", true);
		f.append("visits", true);
		f.append("visits_aud", true);
		Query query = new BasicQuery(q,f);
		
		List<Dynamics_data> list = mongoTemplate.find(query,Dynamics_data.class);
		return list == null?new ArrayList<>():list;
	}

	/**
	 * 更新不在接待动态里的评论review=1
	 * @param preDealTime
	 * @param dynamicList
	 */
	public void updateCommentsStatus(Long preDealTime, List<Integer> dynamicList) {
		if(preDealTime == null){
			preDealTime = 0L;
		}
		mongoTemplate.updateMulti(query(where("create_time").gt(preDealTime).and("dynamic_id").nin(dynamicList).and("review").is(0)), 
				update("review",1), "comments");
	}

	public List<Dynamics_record> queryDynamicsChangeList() {
		Query query=new BasicQuery(new Document());
		query.with(new Sort(Direction.ASC,"_id"));
		return mongoTemplate.find(query, Dynamics_record.class);
	}

	/**
	 * 根据动态Id查询动态列表
	 * @param dynamicsList
	 * @return
	 */
	public List<Square_dynamics> queryDynamicsList(Set<Integer> dynamicsList) {
		Document queryObject = new Document();
		queryObject.append("dynamic_id", new Document().append("$in", dynamicsList));
		Query query = new BasicQuery(queryObject);
		return mongoTemplate.find(query, Square_dynamics.class);
	}

	/**
	 * 下广场动态处理
	 * @param delDynamicsList
	 */
	public void downSquare(Set<Integer> delDynamicsList) {

		mongoTemplate.updateMulti(query(where("dynamic_id").in(delDynamicsList)),
									update("status",0).set("comments_aud", 0), Dynamics_data.class);
		
		if(!Constants.DYNAMICID.isEmpty()){
			delDynamicsList.forEach(d->{
				Constants.COMMENTS_AUD.remove(d);
				Constants.VISITS.remove(d);
				Constants.DYNAMICID.remove(d);
			});
		}
	}

	/**
	 * 删除上下广场表里的已处理数据
	 * @param removeData
	 */
	public void removeUpDownSquareData(List<Integer> removeData) {
		Document queryObject = new Document();
		queryObject.append("_id", new Document().append("$in", removeData));
		Query query = new BasicQuery(queryObject);
		mongoTemplate.remove(query , Dynamics_record.class);
	}

	/**
	 * 查询客服打卡、主播组变化数据
	 * @return
	 */
	public List<Kf_anchor_queue> queryDynamicQueue() {
		Query query=new BasicQuery(new Document());
		query.with(new Sort(Direction.ASC,"createtime"));
		return mongoTemplate.find(query, Kf_anchor_queue.class);
	}

	/**
	 * 删除客服打卡、主播组变化数据
	 * @param removeData
	 */
	public void removeQueueData(List<ObjectId> removeData) {
		Document queryObject = new Document();
		queryObject.append("_id", new Document().append("$in", removeData));
		Query query = new BasicQuery(queryObject);
		mongoTemplate.remove(query , Kf_anchor_queue.class);
	}
}
