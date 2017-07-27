/**
 * 
 */
package cn.ispeak.dynamics.real.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import cn.ispeak.dynamics.real.entity.AccountGrade;
import cn.ispeak.dynamics.real.entity.Community_anchor_group;
import cn.ispeak.dynamics.real.entity.DynamicPoint;
import cn.ispeak.dynamics.real.entity.Community_kf_login_out;
import cn.ispeak.dynamics.real.entity.UserInfo;
import cn.ispeak.dynamics.real.util.AccountGradeExtractor;
import cn.ispeak.dynamics.real.util.Constants;
import cn.ispeak.dynamics.real.util.DynamicPointExtractor;
import cn.ispeak.dynamics.real.util.UserInfoExtractor;

/**
 * @author Mr Yin
 *
 */
@Repository
public class MysqlDao {
	
	private static final Logger logger = LoggerFactory.getLogger(MysqlDao.class);
	
	private static final String POINT_SQL = "SELECT SUM(CASE t.publish_platform WHEN 0 THEN 1 ELSE 0 END) AS point_out,"
			  +"SUM(CASE t.publish_platform WHEN 1 THEN 1 ELSE 0 END) AS point_in,"
			  +"t.`dynamic_id`FROM myzone.dynamic_point t where t.created_time > ? and t.created_time <= ? "
			  + "and t.to_uid in (qmark) GROUP BY t.`dynamic_id`";
	
	private static final String POINT_SQL_BY_DYNAMIC = "SELECT SUM(CASE t.publish_platform WHEN 0 THEN 1 ELSE 0 END) AS point_out,"
			  +"SUM(CASE t.publish_platform WHEN 1 THEN 1 ELSE 0 END) AS point_in,"
			  +"t.`dynamic_id`FROM myzone.dynamic_point t where t.created_time > ? and t.created_time <= ? "
			  + "and t.dynamic_id in (qmark) GROUP BY t.`dynamic_id`";

	private static final String ANCHOR_SQL = "SELECT anchoruid FROM myzone.community_kf_anchor";
	
	private static final String USERINFO_SQL = "SELECT t.uid,t.nickname FROM user_db.userinfo t WHERE t.uid in (qmark)";
	
	private static final String ACCOUNT_SQL = "SELECT t.userid,t.diannum,t.totalXF FROM gift_db.is_account t WHERE t.userid in (qmark)";
	
	private static final Long defaultKfUid = Long.valueOf(Constants.getProperty("default.kf.uid"));
	
	@Autowired
	private JdbcTemplate myzoneJdbcTemplate;
	
	@Autowired
	private JdbcTemplate userJdbcTemplate;
	
	@Autowired
	private JdbcTemplate giftJdbcTemplate;
	
	/**
	 * 查询区间内点赞数
	 * @param didSet
	 * @param preTs
	 * @param nextTs
	 * @return
	 */
	public List<DynamicPoint> queryPointCountList(Set<Integer> didSet,Long preTs,Long nextTs){
		
		List<DynamicPoint> result = new ArrayList<>();
		List<Integer> uidList = null;
		
		if(didSet == null){
			uidList = queryUserList();
			int size = uidList.size();
			int fromIndex = 0;
			int toIndex = 0;
			if(size > 1000){
				do{
					if(toIndex + 1000 < size){
						toIndex += 1000;
					}else{
						toIndex = size;
					}
					List<Integer> subList = uidList.subList(fromIndex, toIndex);
					fromIndex += 1000;
					List<DynamicPoint> subResultList = queryPointCount(preTs, nextTs, subList);
					
					result.addAll(subResultList);
				}while(toIndex != size);
			}else{
				List<DynamicPoint> subResultList = queryPointCount(preTs, nextTs, uidList);
				result = subResultList;
			}
		}else{
			StringBuilder idStrB = new StringBuilder();
			int size = didSet.size();
			for(int i = 0;i<size;i++){
				idStrB.append("?").append(",");
			}
			
			if(idStrB.length() == 0){
				return new ArrayList<>();
			}
			
			String idStr = idStrB.substring(0, idStrB.length() - 1);
			Integer[] integers = didSet.toArray(new Integer[1]);
			
			String temSql = POINT_SQL_BY_DYNAMIC.replace("qmark", idStr);
			result = myzoneJdbcTemplate.query(temSql,new PreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement ps) throws SQLException {
					ps.setTimestamp(1, new Timestamp(preTs*1000));
					ps.setTimestamp(2, new Timestamp(nextTs*1000));
					for(int i = 0;i<size;i++){
						ps.setInt(3+i, integers[i]);
					}
				}
			},new DynamicPointExtractor());
		}
		
		return result;
	}

	private List<DynamicPoint> queryPointCount(Long preTs, Long nextTs, List<Integer> uidList) {
		StringBuilder idStrB = new StringBuilder();
		int size = uidList.size();
		for(int i = 0;i<size;i++){
			idStrB.append("?").append(",");
		}
		
		if(idStrB.length()==0){
			return new ArrayList<>();
		}
		String idStr = idStrB.substring(0, idStrB.length() - 1);
		
		String temSql = POINT_SQL.replace("qmark", idStr);
		
		List<DynamicPoint> list = myzoneJdbcTemplate.query(temSql,new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setTimestamp(1, new Timestamp(preTs*1000));
				ps.setTimestamp(2, new Timestamp(nextTs*1000));
				for(int i = 0;i<size;i++){
					ps.setInt(3+i, uidList.get(i));
				}
			}
		},new DynamicPointExtractor());
		
		return list;
	}
	
	/**
	 * 查询接待主播id列表
	 * @return
	 */
	public List<Integer> queryUserList(){
		
		List<Integer> list = myzoneJdbcTemplate.queryForList(ANCHOR_SQL,Integer.class);
		
		return list;
	}

	/**
	 * 查询用户信息
	 * @param didList
	 * @return
	 */
	public List<UserInfo> queryUserInfo(List<Long> uidList) {
		StringBuilder idStrB = new StringBuilder();
		int size = uidList.size();
		for(int i = 0;i<size;i++){
			idStrB.append("?").append(",");
		}
		
		if(idStrB.length() == 0){
			return new ArrayList<>();
		}
		
		String idStr = idStrB.substring(0, idStrB.length() - 1);
		
		String temSql = USERINFO_SQL.replace("qmark", idStr);
		
		long beginTime = System.currentTimeMillis();
		List<UserInfo> list = userJdbcTemplate.query(temSql, new PreparedStatementSetter(){
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				for(int i = 0;i<size;i++){
					ps.setLong(1+i, uidList.get(i));
				}
			}
		}, new UserInfoExtractor());
		long endTime = System.currentTimeMillis();
		logger.info("{} milliseconds spending",endTime-beginTime);
		return list;
	}

	/**
	 * 查询账户等级
	 * @param didList
	 * @return
	 */
	public List<AccountGrade> queryAccountGradeInfo(List<Long> uidList) {
		
		StringBuilder idStrB = new StringBuilder();
		int size = uidList.size();
		for(int i = 0;i<size;i++){
			idStrB.append("?").append(",");
		}
		
		if(idStrB.length() == 0){
			return new ArrayList<>();
		}
		
		String idStr = idStrB.substring(0, idStrB.length() - 1);
		
		String temSql = ACCOUNT_SQL.replace("qmark", idStr);
		
		List<AccountGrade> list = giftJdbcTemplate.query(temSql, new PreparedStatementSetter(){
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				for(int i = 0;i<size;i++){
					ps.setLong(1+i, uidList.get(i));
				}
			}
		},new AccountGradeExtractor());
		return list;
	}

	/**
	 * 客服下班时，更新其接待主播为默认接待主播
	 * @param userid
	 */
	public void updateKFAnchor(Long userid) {
		myzoneJdbcTemplate.update("update myzone.community_kf_anchor set uid=?,kfuid=? where kfuid = ?", new PreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement pss) throws SQLException {
				pss.setLong(1, defaultKfUid);
				pss.setLong(2, defaultKfUid);
				pss.setLong(3, userid);
			}
			
		});
	}

	/**
	 * 更新某主播为某一接待客服
	 * @param kfid
	 * @param anchorid
	 */
	public void updateAnchorAlloc(Long kfid, Long anchorid) {
		myzoneJdbcTemplate.update("update myzone.community_kf_anchor set uid=?,kfuid=? where anchoruid = ?", new PreparedStatementSetter(){

			@Override
			public void setValues(PreparedStatement pss) throws SQLException {
				pss.setLong(1, kfid);
				pss.setLong(2, kfid);
				pss.setLong(3, anchorid);
			}
			
		});
	}

	/**
	 * 查询客服已接待数量
	 * @return
	 */
	public Map<Long, Integer> queryKfAllocNum() {
		String sql = "SELECT t.kfuid,COUNT(1) FROM myzone.community_kf_anchor t GROUP BY t.kfuid";
		return myzoneJdbcTemplate.query(sql, new ResultSetExtractor<Map<Long,Integer>>(){

			@Override
			public Map<Long, Integer> extractData(ResultSet rs) throws SQLException, DataAccessException {
				
				Map<Long,Integer> map = new HashMap<>();
				while(rs.next()){
					long kfuid = rs.getLong(1);
					int num = rs.getInt(2);
					map.put(kfuid, num);
				}
				
				return map;
			}
		});
	}

	/**
	 * 查询所有分配
	 * @return
	 */
	public Map<Long, Long> queryAllocMap() {
		String sql = "SELECT t.anchoruid,t.kfuid FROM myzone.community_kf_anchor t";
		return myzoneJdbcTemplate.query(sql, new ResultSetExtractor<Map<Long, Long>>(){

			@Override
			public Map<Long, Long> extractData(ResultSet rs) throws SQLException, DataAccessException {
				
				Map<Long, Long> map = new HashMap<>();
				while(rs.next()){
					long anchoruid = rs.getInt(1);
					long kfuid = rs.getInt(2);
					map.put(anchoruid, kfuid);
				}
				
				return map;
			}
		});
	}
	
	/**
	 * 查询主播所在组
	 * @return
	 */
	public List<Community_anchor_group> queryAnchorGroup() {
		String sql = "select t.gid,t.auid from myzone.community_anchor_group t";
		return myzoneJdbcTemplate.query(sql, new ResultSetExtractor<List<Community_anchor_group>>(){

			@Override
			public List<Community_anchor_group> extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<Community_anchor_group> list = new ArrayList<>();
				while(rs.next()){
					Community_anchor_group c = new Community_anchor_group();
					c.setAuid(rs.getInt("auid"));
					c.setGid(rs.getInt("gid"));
					list.add(c);
				}
				return list;
			}
			
		});
	}

	/**
	 * 查询客服当时上班情况
	 * @return
	 */
	public List<Community_kf_login_out> queryKfLoginOut() {
		
		//重启时默认查询两天的客服上下班记录
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date time = new Date();
		String date = sdf.format(new Date());
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		c.set(Calendar.DATE, c.get(Calendar.DATE) - 1);
		Date dateBegin = c.getTime();
		String datePre = sdf.format(dateBegin);
		
		String beginDate = datePre + " 00:00:00";
		String endDate = date + " 23:59:59";
		String sql = "select t.uid,t.status,t.gid as cmgroup from myzone.community_kf_login_out t WHERE t.time >= str_to_date('"+beginDate+"','%Y%m%d %H:%i:%s') and t.time <= str_to_date('"+endDate+"','%Y%m%d %H:%i:%s') order by t.time asc";
		return myzoneJdbcTemplate.query(sql,new ResultSetExtractor<List<Community_kf_login_out>>(){

			@Override
			public List<Community_kf_login_out> extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<Community_kf_login_out> list = new ArrayList<>();
				while(rs.next()){
					Community_kf_login_out c = new Community_kf_login_out();
					c.setUid(rs.getInt("uid"));
					c.setStatus(rs.getInt("status"));
					c.setCmgroup(rs.getInt("cmgroup"));
					list.add(c);
				}
				return list;
			}
			
		});
	}
}
