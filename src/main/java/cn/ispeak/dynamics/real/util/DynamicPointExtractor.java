/**
 * 
 */
package cn.ispeak.dynamics.real.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import cn.ispeak.dynamics.real.entity.DynamicPoint;

/**
 * 点赞结果集转换器
 * @author Mr Yin
 *
 */
public class DynamicPointExtractor implements ResultSetExtractor<List<DynamicPoint>> {

	@Override
	public List<DynamicPoint> extractData(ResultSet rs) throws SQLException, DataAccessException {
		
		List<DynamicPoint> list = new ArrayList<>();
		while(rs.next()){
			
			DynamicPoint point = new DynamicPoint();
			point.setDynamic_id(rs.getInt("dynamic_id"));
			point.setPoint_in(rs.getInt("point_in"));
			point.setPoint_out(rs.getInt("point_out"));
			list.add(point);
		}
		
		return list;
	}
}
