package test;

import java.util.Date;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * 
 * @author Mr Yin
 *
 */
public class CommentsTest extends BaseTest{
	
	//新增评论
	@Test
	public void testInsert() {
		Document d = new Document();
		d.append("dynamic_id", 2231);
		d.append("create_time", new Date().getTime()/1000);
		d.append("userid", Long.valueOf(80000041));
		d.append("contents", "测试评论");
		d.append("ip", "172.20.23.198");
		d.append("pub_id", 0);
		d.append("pub_type", 0);
		d.append("review", 0);
		
		mongoTemplate.insert(d, "comments");
	}
	
	//修改评论回复状态
	@Ignore
	@Test
	public void testUpdateOne() {
		Document d = new Document();
		d.append("dynamic_id", 2231);
		Query query = new BasicQuery(d);
		Update update = new Update();
		update.set("review", 1);
		
		mongoTemplate.updateFirst(query , update, "comments");
	}
	
	//修改评论回复状态
	@Ignore
	@Test
	public void testUpdateAll() {
		Document d = new Document();
		d.append("dynamic_id", 2231);
		Query query = new BasicQuery(d);
		Update update = new Update();
		update.set("review", 1);
		
		mongoTemplate.updateMulti(query , update, "comments");
	}
}
