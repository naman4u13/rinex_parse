package com.RINEX_parser.models.SBAS;

public class FastCorr extends SbasRoot {

	private double PRC = 0;
	private int IODP;
	private int IODF;
	private int UDREI;

	public FastCorr(long GPSTime, long weekNo, double PRC, int IODP, int IODF, int UDREI) {
		super(GPSTime, weekNo);
		// TODO Auto-generated constructor stub
		this.PRC = PRC;
		if (UDREI == 15) {
			this.PRC = 0;
		}
		this.IODP = IODP;
		this.IODF = IODF;
		this.UDREI = UDREI;
	}

	public double getPRC() {
		return PRC;
	}

	public void setPRC(double pRC) {
		PRC = pRC;
	}

	public int getIODP() {
		return IODP;
	}

	public void setIODP(int iODP) {
		IODP = iODP;
	}

	public long getToA() {
		return getGPStime();
	}

}
