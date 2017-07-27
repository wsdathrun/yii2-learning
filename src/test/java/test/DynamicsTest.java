/**
 * 
 */
package test;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import java.util.Date;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Mr Yin
 *
 */
public class DynamicsTest extends BaseTest{
	
	//添加动态并上广场
	@Ignore
	@Test
	public void testAddAndUp(){
		
		//create
		Document d = new Document();
		d.append("dynamic_id", 6666);
		d.append("pub_id", Long.valueOf(303));
		d.append("create_time", new Date().getTime()/1000);
		d.append("opt_time", new Date().getTime()/1000);
		d.append("userid", Long.valueOf(80062101));
		d.append("contents", new Document().append("txt", "测试"));
		d.append("partnerid", 0);
		d.append("sub_partnerid", 0);
		d.append("ip", "");
		d.append("public", true);
		d.append("status", 1);
		d.append("type", 2);
		d.append("pub_type", 1);
		
		mongoTemplate.insert(d, "square_dynamics");
		
		//up square
		Document r = new Document();
		r.append("_id", 1);
		r.append("dynamic_id", 6666);
		r.append("createtime", new Date().getTime()/1000);
		r.append("status", 1);
		mongoTemplate.insert(r,"dynamics_record");
	}

	//动态下广场
	@Ignore
	@Test
	public void testDown() {
		//udpate status
		mongoTemplate.updateFirst(
				query(where("dynamic_id").is(6666)),
				update("status",2).set("opt_time", new Date().getTime()/1000), 
				"square_dynamics");
		
		//down square
		Document r = new Document();
		r.append("_id", 1);
		r.append("dynamic_id", 6666);
		r.append("createtime", new Date().getTime()/1000);
		r.append("status", 0);
		mongoTemplate.insert(r,"dynamics_record");
	}
	
	//动态上广场
	@Test
	public void testUp() {
		//udpate status
		mongoTemplate.updateFirst(
				query(where("dynamic_id").is(6666)),
				update("status",1).set("opt_time", new Date().getTime()/1000), 
				"square_dynamics");
		
		//down square
		Document r = new Document();
		r.append("_id", 1);
		r.append("dynamic_id", 6666);
		r.append("createtime", new Date().getTime()/1000);
		r.append("status", 1);
		mongoTemplate.insert(r,"dynamics_record");
	}
	
	
}
