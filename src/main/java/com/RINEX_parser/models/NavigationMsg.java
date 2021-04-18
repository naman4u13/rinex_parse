package com.RINEX_parser.models;

import com.RINEX_parser.utility.Time;

public class NavigationMsg {

	private String SVID;
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private double second;
	private double SV_clock_bias;
	private double SV_clock_drift;
	private double SV_clock_drift_rate;
	private int IODE;
	private double Crs;
	private double Delta_n;
	private double M0;
	private double Cuc;
	private double e;
	private double Cus;
	private double sqrt_A;
	private double TOE;
	private double Cic;
	private double OMEGA0;
	private double Cis;
	private double i0;
	private double Crc;
	private double omega;
	private double OMEGA_DOT;
	private double IDOT;
	private double codes_on_L2_channel;
	private double GPS_week_no;
	private double L2_P_data_flag;
	private double SV_Accuracy;
	private double Health;
	private double TGD;
	private int IODC;
	private double transmission_time_of_msg;
	private double fit_interval;
	private long TOC;

	public NavigationMsg(String[] arr) {
		SVID = arr[0].replaceAll("[a-zA-Z]", "");
		year = Integer.parseInt(arr[1]);
		month = Integer.parseInt(arr[2]);
		day = Integer.parseInt(arr[3]);
		hour = Integer.parseInt(arr[4]);
		minute = Integer.parseInt(arr[5]);
		second = Double.parseDouble(arr[6].replace('D', 'E'));
		SV_clock_bias = Double.parseDouble(arr[7].replace('D', 'E'));
		SV_clock_drift = Double.parseDouble(arr[8].replace('D', 'E'));
		SV_clock_drift_rate = Double.parseDouble(arr[9].replace('D', 'E'));
		IODE = (int) Double.parseDouble(arr[10].replace('D', 'E'));
		Crs = Double.parseDouble(arr[11].replace('D', 'E'));
		Delta_n = Double.parseDouble(arr[12].replace('D', 'E'));
		M0 = Double.parseDouble(arr[13].replace('D', 'E'));
		Cuc = Double.parseDouble(arr[14].replace('D', 'E'));
		e = Double.parseDouble(arr[15].replace('D', 'E'));
		Cus = Double.parseDouble(arr[16].replace('D', 'E'));
		sqrt_A = Double.parseDouble(arr[17].replace('D', 'E'));
		TOE = Double.parseDouble(arr[18].replace('D', 'E'));
		Cic = Double.parseDouble(arr[19].replace('D', 'E'));
		OMEGA0 = Double.parseDouble(arr[20].replace('D', 'E'));
		Cis = Double.parseDouble(arr[21].replace('D', 'E'));
		i0 = Double.parseDouble(arr[22].replace('D', 'E'));
		Crc = Double.parseDouble(arr[23].replace('D', 'E'));
		omega = Double.parseDouble(arr[24].replace('D', 'E'));
		OMEGA_DOT = Double.parseDouble(arr[25].replace('D', 'E'));
		IDOT = Double.parseDouble(arr[26].replace('D', 'E'));
		codes_on_L2_channel = Double.parseDouble(arr[27].replace('D', 'E'));
		GPS_week_no = Double.parseDouble(arr[28].replace('D', 'E'));
		L2_P_data_flag = Double.parseDouble(arr[29].replace('D', 'E'));
		SV_Accuracy = Double.parseDouble(arr[30].replace('D', 'E'));
		Health = Double.parseDouble(arr[31].replace('D', 'E'));
		TGD = Double.parseDouble(arr[32].replace('D', 'E'));
		IODC = (int) Double.parseDouble(arr[33].replace('D', 'E'));
		transmission_time_of_msg = Double.parseDouble(arr[34].replace('D', 'E'));
		TOC = Time.getGPSTime(year, month - 1, day, hour, minute, (int) second)[0];
		// fit_interval = Double.parseDouble(arr[35].replace('D', 'E'));
		// TODO Auto-generated constructor stub
	}

	public void set(String[] arr) {
		SVID = arr[0].replaceAll("[a-zA-Z]", "");
		year = Integer.parseInt(arr[1]);
		month = Integer.parseInt(arr[2]);
		day = Integer.parseInt(arr[3]);
		hour = Integer.parseInt(arr[4]);
		minute = Integer.parseInt(arr[5]);
		second = Double.parseDouble(arr[6].replace('D', 'E'));
		SV_clock_bias = Double.parseDouble(arr[7].replace('D', 'E'));
		SV_clock_drift = Double.parseDouble(arr[8].replace('D', 'E'));
		SV_clock_drift_rate = Double.parseDouble(arr[9].replace('D', 'E'));
		IODE = (int) Double.parseDouble(arr[10].replace('D', 'E'));
		Crs = Double.parseDouble(arr[11].replace('D', 'E'));
		Delta_n = Double.parseDouble(arr[12].replace('D', 'E'));
		M0 = Double.parseDouble(arr[13].replace('D', 'E'));
		Cuc = Double.parseDouble(arr[14].replace('D', 'E'));
		e = Double.parseDouble(arr[15].replace('D', 'E'));
		Cus = Double.parseDouble(arr[16].replace('D', 'E'));
		sqrt_A = Double.parseDouble(arr[17].replace('D', 'E'));
		TOE = Double.parseDouble(arr[18].replace('D', 'E'));
		Cic = Double.parseDouble(arr[19].replace('D', 'E'));
		OMEGA0 = Double.parseDouble(arr[20].replace('D', 'E'));
		Cis = Double.parseDouble(arr[21].replace('D', 'E'));
		i0 = Double.parseDouble(arr[22].replace('D', 'E'));
		Crc = Double.parseDouble(arr[23].replace('D', 'E'));
		omega = Double.parseDouble(arr[24].replace('D', 'E'));
		OMEGA_DOT = Double.parseDouble(arr[25].replace('D', 'E'));
		IDOT = Double.parseDouble(arr[26].replace('D', 'E'));
		codes_on_L2_channel = Double.parseDouble(arr[27].replace('D', 'E'));
		GPS_week_no = Double.parseDouble(arr[28].replace('D', 'E'));
		L2_P_data_flag = Double.parseDouble(arr[29].replace('D', 'E'));
		SV_Accuracy = Double.parseDouble(arr[30].replace('D', 'E'));
		Health = Double.parseDouble(arr[31].replace('D', 'E'));
		TGD = Double.parseDouble(arr[32].replace('D', 'E'));
		IODC = (int) Double.parseDouble(arr[33].replace('D', 'E'));
		transmission_time_of_msg = Double.parseDouble(arr[34].replace('D', 'E'));
		// fit_interval = Double.parseDouble(arr[35].replace('D', 'E'));

	}

	@Override
	public String toString() {
		return "NavigationMsg [SVID=" + SVID + ", year=" + year + ", month=" + month + ", day=" + day + ", hour=" + hour
				+ ", minute=" + minute + ", second=" + second + ", SV_clock_bias=" + SV_clock_bias + ", SV_clock_drift="
				+ SV_clock_drift + ", SV_clock_drift_rate=" + SV_clock_drift_rate + ", IODE=" + IODE + ", Crs=" + Crs
				+ ", Delta_n=" + Delta_n + ", M0=" + M0 + ", Cuc=" + Cuc + ", e=" + e + ", Cus=" + Cus + ", sqrt_A="
				+ sqrt_A + ", TOE=" + TOE + ", Cic=" + Cic + ", OMEGA0=" + OMEGA0 + ", Cis=" + Cis + ", i0=" + i0
				+ ", Crc=" + Crc + ", omega=" + omega + ", OMEGA_DOT=" + OMEGA_DOT + ", IDOT=" + IDOT
				+ ", codes_on_L2_channel=" + codes_on_L2_channel + ", GPS_week_no=" + GPS_week_no + ", L2_P_data_flag="
				+ L2_P_data_flag + ", SV_Accuracy=" + SV_Accuracy + ", Health=" + Health + ", TGD=" + TGD + ", IODC="
				+ IODC + ", transmission_time_of_msg=" + transmission_time_of_msg + ", fit_interval=" + fit_interval
				+ "]";
	}

	public String getSVID() {
		return SVID;
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}

	public int getHour() {
		return hour;
	}

	public int getMinute() {
		return minute;
	}

	public double getSecond() {
		return second;
	}

	public double getSV_clock_bias() {
		return SV_clock_bias;
	}

	public double getSV_clock_drift() {
		return SV_clock_drift;
	}

	public double getSV_clock_drift_rate() {
		return SV_clock_drift_rate;
	}

	public int getIODE() {
		return IODE;
	}

	public double getCrs() {
		return Crs;
	}

	public double getDelta_n() {
		return Delta_n;
	}

	public double getM0() {
		return M0;
	}

	public double getCuc() {
		return Cuc;
	}

	public double getE() {
		return e;
	}

	public double getCus() {
		return Cus;
	}

	public double getSqrt_A() {
		return sqrt_A;
	}

	public double getTOE() {
		return TOE;
	}

	public double getCic() {
		return Cic;
	}

	public double getOMEGA0() {
		return OMEGA0;
	}

	public double getCis() {
		return Cis;
	}

	public double getI0() {
		return i0;
	}

	public double getCrc() {
		return Crc;
	}

	public double getOmega() {
		return omega;
	}

	public double getOMEGA_DOT() {
		return OMEGA_DOT;
	}

	public double getIDOT() {
		return IDOT;
	}

	public double getCodes_on_L2_channel() {
		return codes_on_L2_channel;
	}

	public double getGPS_week_no() {
		return GPS_week_no;
	}

	public double getL2_P_data_flag() {
		return L2_P_data_flag;
	}

	public double getSV_Accuracy() {
		return SV_Accuracy;
	}

	public double getHealth() {
		return Health;
	}

	public double getTGD() {
		return TGD;
	}

	public int getIODC() {
		return IODC;
	}

	public double getTransmission_time_of_msg() {
		return transmission_time_of_msg;
	}

	public double getFit_interval() {
		return fit_interval;
	}

	public long getTOC() {
		return TOC;
	}
}
