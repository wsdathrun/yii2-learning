/**
 * 
 */
package test;

import java.util.Date;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Mr Yin
 *
 */
public class QueueTest extends BaseTest{

	//主播添加到组
	@Test
	public void testGroupAdd(){
		Document d = new Document();
		d.append("createtime", new Date().getTime()/1000);
		d.append("type", 1);
		d.append("doc", new Document().append("userid", Long.valueOf(80060697))
				.append("group", 10).append("status", 1));
		
		mongoTemplate.insert(d, "kf_anchor_queue");
	}
	
	//主播移除到组
	@Ignore
	@Test
	public void testGroupDel(){
		Document d = new Document();
		d.append("createtime", new Date().getTime()/1000);
		d.append("type", 1);
		d.append("doc", new Document().append("userid", Long.valueOf(80060697))
				.append("group", 10).append("status", 0));
		
		mongoTemplate.insert(d, "kf_anchor_queue");
	}
	
	//客服签到
	@Ignore
	@Test
	public void testKfAdd(){
		Document d = new Document();
		d.append("createtime", new Date().getTime()/1000);
		d.append("type", 2);
		d.append("doc", new Document().append("userid", Long.valueOf(80060697))
				.append("status", 1));
		
		mongoTemplate.insert(d, "kf_anchor_queue");
	}
	
	//客服签退
	@Ignore
	@Test
	public void testKfDel(){
		Document d = new Document();
		d.append("createtime", new Date().getTime()/1000);
		d.append("type", 2);
		d.append("doc", new Document().append("userid", Long.valueOf(80060697))
				.append("status", 0));
		
		mongoTemplate.insert(d, "kf_anchor_queue");
	}
}
