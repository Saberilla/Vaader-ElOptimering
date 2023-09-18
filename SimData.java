import java.text.SimpleDateFormat;
import java.util.Date;

public class SimData {
	
	private Date date;
	private String id;
	
	private double kostnad;
	private int tmin;
	private int tmax;
	private double tempUpp;
	private double tempNed;
	
	private int ordning;
	private int timme;
	private int min;
	private int paav;
	private double kostnadTotal;
	private double temperaturTotal;
	
	public SimData(Date date, int timme,double kostnad, int tmin, int tmax, double tempUpp, double tempNed,
			int ordning, int min, int paav, double kostnadTotal, double temperaturTotal, String ID) {
		this.date = date;
		

		this.tmin = tmin;	
		this.tmax = tmax;
		this.tempUpp = tempUpp;
		this.tempNed = tempNed;
		this.kostnadTotal = kostnadTotal;
		this.kostnad = kostnad;
		this.ordning = ordning;
		this.timme = timme;
		this.min = min;
		this.paav = paav;
		this.temperaturTotal = temperaturTotal;
		this.id = ID;
		
	}
	
	public String getID() {
		return this.id;
	}
	
	public double getKostnadTotal() {
		return this.kostnadTotal;
	}
	
	public double getKostnad() {
		return this.kostnad;
	}
	
	public String getDate() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH");
		String strDate = formatter.format(this.date);
		String dateNoHour = strDate.substring(0,8);
		
		return dateNoHour;
	}
	
	public int getTimme() {
		return this.timme;
	}
	
	
	public int getTmin() {
		return this.tmin;
	}
	
	public int getTmax() {
		return this.tmax;
	}
	
	public double getTempUpp() {
		return this.tempUpp;
	}
	
	public double getTempNed() {
		return this.tempNed;
	}
	
	public int getOrdning() {
		return ordning;
	}
	
	public int getMin() {
		return min;
	}
	
	public int getPaav() {
		return paav;
	}
	
	
	public double getTemperaturTotal() {
		return this.temperaturTotal;
	}
}
