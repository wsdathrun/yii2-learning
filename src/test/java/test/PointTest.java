/**
 * 
 */
package test;

import org.junit.Test;

/**
 * @author Mr Yin
 *
 */
public class PointTest extends BaseTest{
	
	//添加点赞
	@Test
	public void testInsert(){
		myzoneJdbcTemplate.execute("INSERT INTO myzone.dynamic_point(dynamic_id,follow_uid,to_uid,created_time) VALUES(6666,80000075,80062101,NOW())");
	}
}
