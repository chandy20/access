/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package torniquete;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion
{
	static Connection conn;

	public static Connection getConnection()
	{
		
		try
		{
//			String uname = "fabio";
//			String pwd = "totem123";
//			String servidor = "jdbc:mysql://192.168.1.109/tiquetes";
                        
                        String uname = "root";
                       	String pwd = "";
			String servidor = "jdbc:mysql://localhost/tiquetes";			

//			String uname = "tiquetes";
//			String pwd = "tiquetes2";
//			String servidor = "jdbc:mysql://www.db4free.net/tiquetes";
                        
//			String uname = "totemgco_tiquet";
//			String pwd = "glEok_1dpVZ%";
//			String servidor = "jdbc:mysql://www.totemgroup.co/totemgco_tiquetes";

			Class.forName("com.mysql.jdbc.Driver");
			try
			{
				conn = DriverManager.getConnection(servidor,uname,pwd);
			}
			catch (SQLException ex)
			{
				ex.printStackTrace();
			}
		}
		catch(ClassNotFoundException e)
		{
			System.out.println(e);
		}
		return conn;
	}

}
