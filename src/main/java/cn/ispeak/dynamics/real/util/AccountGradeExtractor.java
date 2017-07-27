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

import cn.ispeak.dynamics.real.entity.AccountGrade;

/**
 * @author Mr Yin
 *
 */
public class AccountGradeExtractor implements ResultSetExtractor<List<AccountGrade>> {

	@Override
	public List<AccountGrade> extractData(ResultSet rs) throws SQLException, DataAccessException {

		List<AccountGrade> list = new ArrayList<>();
		while (rs.next()) {
			AccountGrade g = new AccountGrade();
			g.setUserid(rs.getInt("userid"));
			
			//消费等级
			long totalXF = rs.getLong("totalXF");
			int total_xf_grade = -99;

			if (totalXF >= 50000000000L) {
				total_xf_grade = 8;
			} else if (totalXF >= 10000000000L) {
				total_xf_grade = 7;
			} else if (totalXF >= 3000000000L) {
				total_xf_grade = 6;
			} else if (totalXF >= 1000000000) {
				total_xf_grade = 5;
			} else if (totalXF >= 500000000) {
				total_xf_grade = 4;
			} else if (totalXF >= 200000000) {
				total_xf_grade = 3;
			} else if (totalXF >= 100000000) {
				total_xf_grade = 2;
			} else if (totalXF >= 50000000) {
				total_xf_grade = 1;
			} else if (totalXF >= 10000000) {
				total_xf_grade = -1;
			} else if (totalXF >= 1000000) {
				total_xf_grade = -2;
			} else if (totalXF >= 100000) {
				total_xf_grade = -3;
			} else if (totalXF > 0) {
				total_xf_grade = -4;
			}
			
			g.setTotal_xf_grade(total_xf_grade);
			
			//余额等级
			long diannum = rs.getLong("diannum");
			int diannum_grade = 0;
			if(diannum >= 1000000){
				diannum_grade = 4;
			}else if(diannum >= 100000){
				diannum_grade = 3;
			}else if(diannum >= 10000){
				diannum_grade = 2;
			}else if(diannum >= 1000){
				diannum_grade = 1;
			}
			g.setDiannum_grade(diannum_grade);
			
			list.add(g);
		}

		return list;
	}

}
