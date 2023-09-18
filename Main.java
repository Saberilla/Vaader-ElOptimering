
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Main {
	
	private Connection connection = null;
	private java.sql.Statement stmt = null;
	private String url = "jdbc:mariadb://localhost:3306/vaader";
	private String user = "root";
	private String psw = "raspberry";
	private ResultSet rs = null;
	
	private String fromDate;
	private String toDate;
	private String toDateNoHour;
	private String fromDateNoHour;
	private String fromDateHour;
	
	

	public static void main(String[] args) {
		Main program = new Main();
		program.run();

	}
	
	
	
	private void openConnection() {
		try {
			
			connection = DriverManager.getConnection(url, user, psw);
			stmt = connection.createStatement();
			
			System.out.println("connected!");
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void closeConnection(){
		try {
			
			if (!(rs==null)) {
				rs.close();
			}
			
			stmt.close();
			connection.close();
			System.out.println("closed!");
			
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
	}
	
	private void run() {
		openConnection();
		
		//System.out.println(getTodayDatePlusHour()); //testa datum
		setDateValues();
		setAvailable();
		
		mainLoop();

		closeConnection();
	}
	

	
	private void mainLoop() {
		String mainSQL = createSimDataStatement();
		
		boolean run = true; //to run while loop
		boolean noResult = false; //if no more id
		
		while(run) {
			
			System.out.println("Looking for ID...");
			Integer ID = getId(); 
			System.out.println("ID: " +ID);
			
			if(ID == null) { //no more ids if ID is null
				noResult = true;
				//run = false; //leave loop
				break;
			}
			 //below will always return data
			String IDs = String.valueOf(ID);
			String mainSQLID = mainSQL.replaceAll("%", IDs);
			List<SimData> list = executeMainSQL(mainSQLID, IDs, getStartTemp()); //returns 12 data 

			boolean found = solutionFound(list); //see if there is a solution
			
			if(found == true) {
				System.out.println("SOLUTION FOUND: ID " + IDs);
				setResultat(list);
				//run = false; //solution found leaves while loop
				break;
			}
		}
		
		if(noResult == true) {
			setDefault(getDefaultTemp());
		}
	}
	
	private boolean solutionFound(List<SimData> list) {
		boolean found = true;
		
		List<SimData> failedList = new ArrayList<>();
		
		for (SimData data : list) {
			failedList.add(data);
			if(data.getTemperaturTotal() > data.getTmax() || data.getTemperaturTotal() < data.getTmin()) {
				found = false;
				System.out.println("FAILED AT NR:" + data.getOrdning() + " TEMP:" + data.getTemperaturTotal() + 
						" MAX:" + data.getTmax() + " MIN:"  + data.getTmin());
				System.out.println("________________________________________");
				
				setPaav(failedList);
				break; //leave for loop
			}
		}
		return found;
	}
	
	private void setDefault(double defaultTemp) {
		
		String time = getCurrentTime();
		String date = time.substring(0,8);
		String timme = time.substring(8,10);
		String min = time.substring(10,12);
		
		String sql = "INSERT INTO resultat (bertid,typ, alt_id ,data_date,Rtimme,Rmin,Rtemp) VALUES ("+ time + "," + "'TOSP'" + "," + 0 + "," + date + ","+ timme + "," + min + ","+ defaultTemp +");";
		System.out.println("NO SOLUTION FOUND: DEFAULT TEMP SET ON " + defaultTemp);
		try {
			rs = stmt.executeQuery(sql);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	private double getDefaultTemp() {
		
		String sql = "SELECT varde FROM Defaulter where typ=\"TOSP\";";
		double temp = 0;
		try {

			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				temp = rs.getDouble("varde");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return temp;
	}
	
	private void setResultat(List<SimData> list) {
		String time = getCurrentTime();
		for(SimData data : list) {
			executeResultat(time, data.getID(), data.getDate(), data.getTimme(), data.getMin(), data.getTemperaturTotal());
			System.out.println(data.getOrdning() + " TEMP:" + data.getTemperaturTotal() + 
						" MAX:" + data.getTmax() + " MIN:"  + data.getTmin());
		}	
	}
	
	private void executeResultat(String time, String id, String date, int timme, int minut, double temp) {
		String sql = "INSERT INTO resultat(bertid, alt_id ,data_date,Rtimme,Rmin,Rtemp) values ("+ time + "," + id+ "," + date + "," + timme + "," + minut + "," + temp +");";
		
		try {
			rs = stmt.executeQuery(sql);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	private void setPaav(List<SimData> list) {
		StringBuilder str = new StringBuilder();
		str.append("UPDATE alternativR set vald=0 where ");
		for (SimData data : list) {
			str.append("paav" + data.getOrdning() + "=" + data.getPaav() + " and ");
		}
		int len = str.length(); 
		str.delete(len-5, len); //removes last and in sql code
		str.append(";");
		executeSetPaav(str.toString());	
	}
	
	private void executeSetPaav(String sqlStatement) {

		try {
			String sql;
			sql = sqlStatement;
			rs = stmt.executeQuery(sql);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	private List<SimData> executeMainSQL(String SQLStatement, String ID, Double startTemp){
		
		List<SimData> list = new ArrayList<>(); 
		
		try {
			
			double paaAlfa = getpaaAlfa();
			double avBeta = getavBeta();
			
			String sql;
			sql = SQLStatement;
			rs = stmt.executeQuery(sql);
			
			double temperaturTotal = startTemp;
			System.out.println(temperaturTotal);			
			double kostnadTotal = 0;
			
			while(rs.next()) {

				int ordning = rs.getInt("ordning");
				Date dateS = rs.getDate("data_date");
				int timmeS = rs.getInt("timme"); 
				int min = rs.getInt("min");
				double kost = rs.getDouble("hNORDPpris");
				int tmin = rs.getInt("Tmin");
				int tmax = rs.getInt("Tmax");
				int paav = rs.getInt("paav");
				double SMHItemp = rs.getDouble("SMHItemp");	
				//double tempupp = rs.getDouble("TempUpp");
				//double tempned = rs.getDouble("TempNed");
				double tempupp = (paaAlfa - (temperaturTotal-SMHItemp)*avBeta)*0.5;
				double tempned = (temperaturTotal-SMHItemp)*avBeta*0.5; 
				
				
				if (paav == 1) {
					kostnadTotal += kost;
					temperaturTotal += tempupp;
					System.out.println("PA"+temperaturTotal);
				} 

				if (paav == 0) {
					temperaturTotal -= tempned;
					System.out.println("AV"+temperaturTotal);
				}
				
				list.add(new SimData(dateS, timmeS, kost, tmin, tmax, 
						tempupp, tempned, ordning, min, paav, kostnadTotal, temperaturTotal, ID));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list; 
	}
	
	private double getavBeta() {
		double avBeta = 0;
		try {
			
			String sql;
			sql = "SELECT avBeta from Parametrar;";
			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				avBeta = rs.getDouble("avBeta");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return avBeta;
	}
	
	private double getpaaAlfa() {
		double paaAlfa = 0;
		try {
			
			String sql;
			sql = "SELECT paaAlfa from Parametrar;";
			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				paaAlfa = rs.getDouble("paaAlfa");
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return paaAlfa;
	}
	
	private double getStartTemp() {
		
		double temp = 0;
		try {
			String sql;
			sql = "SELECT inne,mattid \n"
					+ "FROM Matdata \n"
					+ "ORDER BY mattid DESC limit 1 ;";

			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				temp = rs.getDouble("inne");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return temp;
	}
	
	
	private void setDateValues() {
		
		//this.fromDate  = getTodayDatePlusHour(); //has todays date
		this.fromDate  = getTodayDatePlusHourHARD(); 
		this.toDate = getSixHoursAddedDate(fromDate);
		this.toDateNoHour = toDate.substring(0,8);
		this.fromDateNoHour = fromDate.substring(0,8);
		this.fromDateHour = fromDate.substring(8,10);
		
	}
	
	private void setAvailable() {
		try {
			
			String sql;
			sql = "UPDATE alternativR SET vald=1;";
			rs = stmt.executeQuery(sql);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
			
	private String createSimDataStatement() {
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT x.nrtimme as ordning,f.data_date,g.timme,x.min,c.NORDPpris*0.5 as hNORDPpris,g.Tmin,g.Tmax,paav,e.SMHItemp\n"
				+ "FROM ( Kalender f)   \n"
				+ "JOIN (Dagtyp g,NORDP c,SMHI e, connect x,alternativ t)   \n"
				+ "ON (f.dagtyp=g.dagtyp and c.data_date=f.data_date and g.timme = c.NORDPtimme  and c.data_date=e.data_date and g.timme = e.SMHItimme and g.timme=x.timme and x.nrtimme=t.timme)   \n"
				+ "where  f.data_date*100+g.timme>=");
		str.append(this.fromDate + " and  f.data_date*100+g.timme<=" + this.toDate);
		str.append("  and x.starttimme=" + this.fromDateHour + " and t.altnr_id = " + "%");
		str.append(" and (f.data_date=" + this.fromDateNoHour + " or f.data_date=" + this.toDateNoHour + ")\n"
				+ "ORDER BY f.data_date,g.timme,x.min;");	
		return str.toString();
	}
	
	private String getCurrentTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		 String strDate = "";
		
		 Date todayDate = new Date();
		 strDate = formatter.format(todayDate);
		 return strDate;
	}
	
	private String getTodayDatePlusHourHARD() { //HARDKODA DATUM
		 SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH");
		 String strDate = "";
		
		String date = "2023080721"; //2023 08 07 21
		 
			Date dateAsDate;
			try {
				dateAsDate = formatter.parse(date);
				
				final int HOURS = 1; 
				
				Calendar calendar2 = Calendar.getInstance();
				calendar2.setTime(dateAsDate);
			    calendar2.add(Calendar.HOUR_OF_DAY, HOURS);
			    
			    Date newDate = calendar2.getTime();
			    strDate = formatter.format(newDate);
				
			} catch (ParseException e) {

				e.printStackTrace();
			}
			
		 
		return strDate;
	}
	
	private String getTodayDatePlusHour() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH");
		String strDate = "";
		
		Date todayDate = new Date(); //NU TID

		final int HOURS = 1; 
				
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(todayDate);
		calendar2.add(Calendar.HOUR_OF_DAY, HOURS); //add one hour	    
		Date newDate = calendar2.getTime();
		
		strDate = formatter.format(newDate);
				
		return strDate;
	}
	
	private String getSixHoursAddedDate(String date) {
		 SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH");
		 String strDate = "";
		
		 try {
			Date dateAsDate = formatter.parse(date);
			
			int HOURS = 6; 
			
			Calendar calendar2 = Calendar.getInstance();
		    calendar2.setTime(dateAsDate);
		    calendar2.add(Calendar.HOUR_OF_DAY, HOURS);
		    
		    Date newDate = calendar2.getTime();
		    strDate = formatter.format(newDate);
			
		} catch (ParseException e) {
			e.printStackTrace();
		} 
		 
		return strDate;
	}
	
	private Integer getId() {
		
		Integer id = null;
		try {
			String sql;
			sql = createIdStatement();
			//sql = idTest(); // hardkoda sqlID

			rs = stmt.executeQuery(sql);
			
			if(!(rs==null)) {
				
				while(rs.next()) {
				id = rs.getInt("altnr_id");
				}
			}
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	private String createIdStatement() {
		StringBuilder str = new StringBuilder();
		
		str.append("SELECT t.altnr_id ,SUM(paav*NORDPpris*0.5) as total_x FROM (Parametrar d, Kalender f)     \n"
				+ "JOIN (Dagtyp g,NORDP c,connect x,alternativ t,alternativR r)     \n"
				+ "ON (f.dagtyp=g.dagtyp and c.data_date=f.data_date and g.timme = c.NORDPtimme  "
				+ "and  g.timme=x.timme and x.nrtimme=t.timme and t.altnr_id=r.altnr_id)     \n"
				+ "WHERE  f.data_date*100+g.timme>=");
		str.append(this.fromDate);
		str.append(" and  f.data_date*100+g.timme<=" + this.toDate);
		str.append("  and x.starttimme=" + this.fromDateHour);
		str.append(" and (f.data_date=" + this.fromDateNoHour);
		str.append(" or f.data_date=" + this.toDateNoHour);
		str.append(") and r.vald=1 \n"
				+ "GROUP by t.altnr_id   \n"
				+ "ORDER by total_x ASC limit 1;");
		
		return str.toString();
	}
	
	private String idTest() { //for hardkoda
		StringBuilder str = new StringBuilder();
		str.append("SELECT t.altnr_id ,SUM(paav*NORDPpris*0.5) as total_x FROM (Parametrar d, Kalender f)     \n"
				+ "JOIN (Dagtyp g,NORDP c,connect x,alternativ t,alternativR r)     \n"
				+ "ON (f.dagtyp=g.dagtyp and c.data_date=f.data_date and g.timme = c.NORDPtimme  and  g.timme=x.timme and x.nrtimme=t.timme and t.altnr_id=r.altnr_id)     \n"
				+ "WHERE  f.data_date*100+g.timme>=2023080721 and  f.data_date*100+g.timme<=2023080803  and x.starttimme=21 and (f.data_date=20230807 or f.data_date=20230808) and r.vald=1 \n"
				+ "GROUP by t.altnr_id   \n"
				+ "ORDER by total_x ASC limit 1;");
		return str.toString();
	}
	

}
