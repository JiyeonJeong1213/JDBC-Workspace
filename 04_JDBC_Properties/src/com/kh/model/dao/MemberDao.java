package com.kh.model.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import com.kh.common.JDBCTemplate;
import com.kh.model.vo.Member;

/*
 * DAO(Data Access Object)
 * Controller를 통해서 호출
 * Controller에서 요청받은 실질적인 기능을 수행함.
 * DB에 직접 접근해서 SQL문을 실행하고, 수행결과 돌려받기 -> JDBC
 */

public class MemberDao {
	/*
	 * 기존의 방식 : DAO클래스에 사용자가 요청할 때마다 실행해야 하는 SQL문을 자바 소스코드 내에 직접 명시적으로 작성함
	 * 	 		  => 정적 코딩방식, 하드코딩
	 * 문제점 : SQL문을 수정해야 할 경우 자바 소스코드를 수정하는 셈. 
	 * 		  즉, 수정된 내용을 반영시키고자 한다면 프로그램을 재구동해야 함
	 * 해결방식 : SQL문들을 별도로 관리하는 외부파일(.XML)을 만들어서 실시간으로 이 파일에 기록된 SQL문들을 동적으로 읽어들여서 실행 => 동적 코딩방식
	 */
	
	private Properties prop = new Properties();
	// new MemberDao().xxx(); 객체를 생성할 때마다 MemberDao객체의 생성자 안에서 xml파일을 읽어들일 예정
	
	public MemberDao() {
		try {
			prop.loadFromXML(new FileInputStream("resources/query.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 사용자가 회원 추가 요청시 입력했던 값을 가지고 Insert문을 실행하는 메서드
	 * @param m : 사용자가 입력했던 아이디부터 취미까지의 값을 가지고 만든 vo객체
	 * @return : Insert문을 실행한 행의 결과값
	 */
	public int insertMember(Member m) {
		// Insert문 -> 처리된 행의 갯수 -> 트랜잭션 처리
		
		// 0) 필요한 변수 셋팅
		int result = 0; // 처리된 결과(처리된 행의 갯수)를 담아줄 변수
		Connection conn = null; // 접속된 DB의 연결정보를 담는 변수
		PreparedStatement pstmt = null; 
		
		// + 필요한 변수 : 실행시킬 SQL문(완성된 형태의 SQL문으로 만들기) => 끝에 세미콜론 절대 붙이지 말기.(자동으로 붙여줌)
		/*
		 * INSERT INTO MEMBER
		 * VALUES(SEQ_USERNO.NEXTVAL , 'XXX', 'XXXX', 'XXX', 'X', XX, 'XXX@XXXXX', 'XXX', 'XXXX', 'XXX', DEFUALT)
		 */
//		String sql = "INSERT INTO MEMBER "
//					+ "VALUES(SEQ_USERNO.NEXTVAL, ? , ? , ? , ? , ? , ? , ? , ? , ? , DEFAULT )";
		String sql = prop.getProperty("insertMember");
		
		try {
			// 1) JDBC 드라이버 등록.
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// 오타가 있을 경우, ojdbc6.jar이 없을경우 -> ClassNotFoundException이 발생함.
			
			// 2) Connection 객체 생성 -> DB와 연결시키겠다
			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:orcl","JDBC","JDBC");
			
			// 3_1) PreparedStatement 객체 생성(sql문 미리 넘겨줌)
			pstmt = conn.prepareStatement(sql);
			
			// 3_2) 미완성된 sql문을 완성형태로 바꿔주기.
			// pstmt.setXXX(?위치, 실제값);
			pstmt.setString(1, m.getUserId());
			pstmt.setString(2, m.getUserPwd());
			pstmt.setString(3, m.getUserName());
			pstmt.setString(4, m.getGender());
			pstmt.setInt(5, m.getAge());
			pstmt.setString(6, m.getEmail());
			pstmt.setString(7, m.getPhone());
			pstmt.setString(8, m.getAddress());
			pstmt.setString(9, m.getHobby());
			
			
			// 4,5) DB에 완성된 SQL문을 실행시키고, 결과값 받기
			result = pstmt.executeUpdate();
			
			// 6_2) 트랜잭션 처리
			if(result > 0) { // 성공 -> 커밋
				conn.commit();
			}else { // 실패 -> 롤백
				conn.rollback();
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// 7) 다쓴 자원 반납해주기 -> 생성된 순서의 역순으로.
			 try {
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		// 8) 결과 반환
		return result;
	}
	
	
	/**
	 * 사용자가 회원 전체 조회 요청 시 select문을 실행해주는 메서드
	 * @return
	 */
	public ArrayList<Member> selectAll() {
		// SELECT -> ResultSet => ArrayList로 반환

		// 0) 필요한 변수들 세팅
		// 조회된 결과를 뽑아서 담아줄 변수 => ArrayList<Member> -> 여러 회원에 대한 정보.
		ArrayList<Member> list = new ArrayList<>(); // 현재 텅빈 리스트.

		// Connection, Statement, ResultSet
		Connection conn = JDBCTemplate.getConnection();
		PreparedStatement pstmt = null;
		ResultSet rset = null; // SELECT문이 실행된 조회결과값들이 처음에 실질적으로 담길 객체

//		String sql = "SELECT * FROM MEMBER";
		String sql = prop.getProperty("selectAll");
		
		try {
			

			// 3_1) PreparedStatement 객체생성
			pstmt = conn.prepareStatement(sql);
			
			// 3_2) 미완성된 sql문이라면 완성시켜주기 -> 스킵 // ? 없기때문 

			// 4,5)
			rset = pstmt.executeQuery();

			// 6_1) 현재 조회결과가 담긴 ResultSet에서 한행씩 뽑아서 vo객체에 담기.
			// rset.next() : 커서를 한줄 아래로 옮겨주고 해당행이 존재할 경우  true, 아니면 false를 반환해주는 메서드
			while (rset.next()) {

				// 현재 rset의 커서가 가리키고 있는 해당행의 데이터를 하나씩 뽑아서 Member객체 담기
				Member m = new Member();

				// rset으로부터 어떤 컬럼에 있는 값을 뽑을건지 제시해야함.
				// 컬럼명(대소문자X), 컬럼순번
				// 권장사항 : 컬럼명으로 쓰고, 대문자로 쓰는 것을 권장함.
				// rset.getInt(컬럼명 또는 순번) : int형 값을 뽑아낼 때 사용
				// rset.getString(컬럼명 또는 컬럼순번) : String값을 뽑아낼 때 사용
				// rset.getDate(컬럼명 또는 컬럼순번) : Date값을 뽑아올 때 사용하는 메서드.

				m.setUserNo(rset.getInt("USERNO"));
				m.setUserId(rset.getString("USERID"));
				m.setUserPwd(rset.getString("USERPWD"));
				m.setUserName(rset.getString("USERNAME"));
				m.setGender(rset.getString("GENDER"));
				m.setAge(rset.getInt("AGE"));
				m.setEmail(rset.getString("EMAIL"));
				m.setPhone(rset.getString("PHONE"));
				m.setAddress(rset.getString("ADDRESS"));
				m.setHobby(rset.getString("HOBBY"));
				m.setEnrollDate(rset.getDate("ENROLLDATE"));
				// 한 행에 대한 모든 컬럼의 데이터값들을
				// 각각의 필드에 담아 하나의 Member객체에 옮겨 담아주기 끝.

				list.add(m); // 리스트에 해당 Member객체를 담아주기

			}

		}  catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rset.close();
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return list;
	}
	
	public Member selectByUserId(String userId) {
		
		// 0) 필요한 변수 세팅
		// 조회된 회원에 대한 정보를 담을 변수
		Member m = null;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		
		// 실행할 sql문(완성된 형태, 세미콜론 x)
		//String sql = "SELECT * FROM MEMBER WHERE USERID = '" + userId + "'";
		//String sql = "SELECT * FROM MEMBER WHERE USERID = '' OR 1=1 --' ";
		// ' OR 1=1 --     
		// preparedStatement가 홀따옴표를 알아서 제거해줌 검사해줌! 문자열로 받아들여서 검색결과없음으로 나옴
//		String sql = "SELECT * FROM MEMBER WHERE USERID = ?";
		String sql = prop.getProperty("selectByUserId");
		
		try {
			// 1) JDBC 드라이버 등록.
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// 오타가 있을 경우, ojdbc6.jar이 없을경우 -> ClassNotFoundException이 발생함.

			// 2) Connection 객체 생성 -> DB와 연결시키겠다
			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:orcl", "JDBC", "JDBC");

			// 3_1) PreparedStatement 객체생성
			pstmt = conn.prepareStatement(sql);
			
			// 3_2) 미완성된 sql문 완성시키기
			pstmt.setString(1, userId);

			// 4,5) SQL문 실행시켜서 결과받기
			rset = pstmt.executeQuery();

			// 6_1) 현재 조회결과가 담긴 ResultSet에서 한행씩 뽑아서 VO객체에 담기 => ID검색은 검색결과가 한행이거나 ,한행도 없을 것.
			if (rset.next()) { // 커서를 한행 아래로 슬쩍 움직여보고 조회결과가 있다면 true, 없다면 false

				// 조회된 한행에 대한 모든 열에 대한 데이터값을 뽑아서 하나의 Member객체에 담기
				m = new Member(rset.getInt("USERNO"),
						       rset.getString("USERID"),
						       rset.getString("USERPWD"),
						       rset.getString("USERNAME"),
						       rset.getString("GENDER"),
						       rset.getInt("AGE"),
						       rset.getString("EMAIL"),
						       rset.getString("PHONE"),
						       rset.getString("ADDRESS"),
						       rset.getString("HOBBY"),
						       rset.getDate("ENROLLDATE"));
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			
			try {
				rset.close();
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return m;
	}
	
	public ArrayList<Member> selectByUserName(String keyword){
		
		// 0) 필요한 변수 세팅
		ArrayList<Member> list = new ArrayList<>();
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		
		// 실행할 sql문
		// SELECT * FROM MEMBER WHERE USERNAME LIKE '%keyword%'
		
		//String sql = "SELECT * FROM MEMBER WHERE USERNAME LIKE '%"?"%'";
		// 정상실행 안됨. SELECT * FROM MEMBER WHERE USERNAME LIKE '%'경민'%'
		
		// 방법 1)
		// String sql = "SELECT * FROM MEMBER WHERE USERNAME LIKE '%' || ? || '%' "
		// SELECT * FROM MEMBER WHERE USERNAME LIKE '%경민%'
		
		// 방법 2)
		// String sql = "SELECT * FROM MEMBER WHERE USERNAME LIKE ?"
//		String sql = "SELECT * FROM MEMBER WHERE USERNAME LIKE ?";
		String sql = prop.getProperty("selectByUserName");
		
		try {
			// 1) JDBC 드라이버 등록.
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// 오타가 있을 경우, ojdbc6.jar이 없을경우 -> ClassNotFoundException이 발생함.

			// 2) Connection 객체 생성 -> DB와 연결시키겠다

			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:orcl", "JDBC", "JDBC");

			// 3) Statement 객체생성
			pstmt = conn.prepareStatement(sql);
			
			// 3_2) pstmt.setString(1, keyword)
			pstmt.setString(1, "%"+keyword+"%");
			
			
			// 4,5) SQL문 실행시켜서 결과받기
			rset = pstmt.executeQuery();

			// 6_1)
			while (rset.next()) {

				list.add(new Member(rset.getInt("USERNO"), 
									rset.getString("USERID"), 
									rset.getString("USERPWD"),
									rset.getString("USERNAME"), 
									rset.getString("GENDER"), 
									rset.getInt("AGE"),
									rset.getString("EMAIL"), 
									rset.getString("PHONE"), 
									rset.getString("ADDRESS"),
									rset.getString("HOBBY"), 
									rset.getDate("ENROLLDATE")));

			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {

			try {
				rset.close();
				pstmt.close();
				conn.close();
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}

		// 8)
		return list;
	}
	
	//////////////////////////////////////////////
	
	public int updateMember(Member m) {
		
		// 0)
		int result = 0;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		/*
		 * UPDATE MEMBER
		 * SET USERPWD = 'XXX',
		 *     EMAIL = 'XXX',
		 *     PHONE = 'XXX',
		 *     ADDRESS = 'XXX'
		 * WHERE USERID = 'XXXX'
		 */
//		String sql = "UPDATE MEMBER SET USERPWD = ? , EMAIL = ? , PHONE = ? , ADDRESS = ? "
//				+ "WHERE USERID = ? ";  
		String sql = prop.getProperty("updateMember");		

		// 1) JDBC 드라이버 등록.
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			// 오타가 있을 경우, ojdbc6.jar이 없을경우 -> ClassNotFoundException이 발생함.

			// 2) Connection 객체 생성 -> DB와 연결시키겠다
			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:orcl", "JDBC", "JDBC");

			// 3_1) PreparedStatement 객체 생성
			pstmt = conn.prepareStatement(sql);
			
			// 3_2) 미완성된 sql문 완성시키기
			pstmt.setString(1, m.getUserPwd());
			pstmt.setString(2, m.getEmail());
			pstmt.setString(3, m.getPhone());
			pstmt.setString(4, m.getAddress());
			pstmt.setString(5, m.getUserId());

			// 4,5)
			result = pstmt.executeUpdate();

			// 6_2) 트랜잭션 처리
			if (result > 0) { // 성공
				conn.commit();
			} else { // 실패
				conn.rollback();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
		// 8)
		return result;
	}
	
	public int deleteMember(String userId) {
		
		int result = 0;
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		// DELETE FROM MEMBER WHERE USERID = 'userId'
//		String sql = "DELETE FROM MEMBER WHERE USERID = ? ";
		String sql = prop.getProperty("deleteMember");

		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:orcl", "JDBC", "JDBC");
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, userId);
			result = pstmt.executeUpdate();
			
			// 트랜잭션 처리
			if(result > 0) {
				conn.commit();
			}else {
				conn.rollback();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			
			try {
				pstmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	
	
	
	
	
	
	
	
	
	
}
