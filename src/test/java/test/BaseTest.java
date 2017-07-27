package test;

import org.bson.Document;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import cn.ispeak.dynamics.real.util.Constants;

public class BaseTest {
	
	public static MongoTemplate mongoTemplate;
	public static JdbcTemplate myzoneJdbcTemplate;
	public static JdbcTemplate userJdbcTemplate;
	public static JdbcTemplate giftJdbcTemplate;

	@BeforeClass
	public static void initialize() {
		@SuppressWarnings("resource")
		ApplicationContext ctx = new FileSystemXmlApplicationContext(Constants.SPRING_FILE);
		mongoTemplate = ctx.getBean(MongoTemplate.class);
		myzoneJdbcTemplate = (JdbcTemplate) ctx.getBean("myzoneJdbcTemplate");
		userJdbcTemplate = (JdbcTemplate) ctx.getBean("userJdbcTemplate");
		giftJdbcTemplate = (JdbcTemplate) ctx.getBean("giftJdbcTemplate");

		//先打开连接，否则使用时打开，时间会有偏差
		FindIterable<Document> find = mongoTemplate.getCollection("comments").find().limit(1);
		MongoCursor<Document> cursor = find.iterator();
		while (cursor.hasNext()) {
			Document document = cursor.next();
			System.out.println(document);
		}
		
		myzoneJdbcTemplate.execute("select now()");
		userJdbcTemplate.execute("select now()");
		giftJdbcTemplate.execute("select now()");
	}
}
