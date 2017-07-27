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

import cn.ispeak.dynamics.real.entity.UserInfo;

/**
 * @author Mr Yin
 *
 */
public class UserInfoExtractor implements ResultSetExtractor<List<UserInfo>> {

	@Override
	public List<UserInfo> extractData(ResultSet rs) throws SQLException, DataAccessException {
		List<UserInfo> list = new ArrayList<>();
		
		while(rs.next()){
			UserInfo u = new UserInfo();
			u.setUid(rs.getInt("uid"));
			u.setNickname(rs.getString("nickname"));
			list.add(u);
		}
		
		return list;
	}

}
