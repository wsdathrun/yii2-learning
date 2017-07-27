/**
 * 
 */
package test;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author Mr Yin
 *
 */
public class OnlineTest extends BaseTest{

	//访客再次访问同一动态
	@Ignore
	@Test
	public void testUpdateTime() {
		mongoTemplate.updateFirst(new Query().addCriteria(new Criteria("dynamic_id").is(6666)
				.and("date").is("20170717").and("userid").is(Long.valueOf(80000073))), 
				new Update().set("last_review_time", new Date().getTime()/1000), 
				"dynamics_onlines_history");
	}

	//添加访客
	@Ignore
	@Test
	public void testInsert() {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		
		Document d = new Document();
		d.append("date", sdf.format(new Date()));
		d.append("dynamic_id", 6666);
		d.append("userid", Long.valueOf(80000073));
		d.append("create_time", new Date().getTime()/1000);
		d.append("last_review_time", new Date().getTime()/1000);
		d.append("reply", 0);
		
		mongoTemplate.insert(d, "dynamics_onlines_history");
	}

	//回复访客
	@Test
	public void testReply(){
		mongoTemplate.updateFirst(new Query().addCriteria(new Criteria("dynamic_id").is(6666)
				.and("userid").is(Long.valueOf(80000073))), 
				new Update().set("reply", 1).set("reply_time", new Date().getTime()/1000),
				"dynamics_onlines_history");
	}
}
