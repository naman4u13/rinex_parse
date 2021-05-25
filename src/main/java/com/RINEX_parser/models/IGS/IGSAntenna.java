package com.RINEX_parser.models.IGS;

import com.RINEX_parser.utility.StringUtil;

public class IGSAntenna {

	private double dazi;
	private double[] zen;
	private long[] valid_from;
	private long[] valid_until;
	private double[] eccXYZ;
	private double[] PCV_NOAZI;
	private double[][] PCV_AZI;

	public IGSAntenna(String dazi, String zen, String valid_from, String valid_until, String eccXYZ, String PCV_NOAZI,
			String PCV_AZI) {
		super();
		this.dazi = Double.parseDouble(dazi);
		this.zen = StringUtil.str2arr_D(zen);
		this.valid_from = StringUtil.str2arr_L(valid_from);
		if (!valid_until.equalsIgnoreCase("null")) {
			this.valid_until = StringUtil.str2arr_L(valid_until);
		}
		this.eccXYZ = StringUtil.str2arr_D(eccXYZ, 1e-3);

		this.PCV_NOAZI = StringUtil.str2arr_D(PCV_NOAZI, 1e-3);
		if (!PCV_AZI.equalsIgnoreCase("null")) {
			this.PCV_AZI = StringUtil.str2mArr_D(PCV_AZI, 1e-3);
		}
	}

	public IGSAntenna(double dazi, double[] zen, long[] valid_from, long[] valid_until, double[] eccXYZ,
			double[] PCV_NOAZI, double[][] PCV_AZI) {
		super();
		this.dazi = dazi;
		this.zen = zen;
		this.valid_from = valid_from;
		this.valid_until = valid_until;
		this.eccXYZ = eccXYZ;

		this.PCV_NOAZI = PCV_NOAZI;
		this.PCV_AZI = PCV_AZI;
	}

//	private double[] NEU2ENU(double[] NEU) {
//		double[] ENU = new double[3];
//		ENU[0] = NEU[1] * 1e-3;
//		ENU[1] = NEU[0] * 1e-3;
//		ENU[2] = NEU[2] * 1e-3;
//		return ENU;
//	}

	public boolean checkValidity(long[] time) {
		if (valid_until == null) {
			if ((time[1] > valid_from[1]) || (time[1] == valid_from[1] && time[0] >= valid_from[0])) {
				return true;
			}
		} else {
			if ((time[1] > valid_from[1]) || (time[1] == valid_from[1] && time[0] >= valid_from[0])) {
				if ((time[1] < valid_until[1]) || (time[1] == valid_until[1] && time[0] <= valid_from[0])) {
					return true;
				}
			}
		}

		return false;
	}

	public double[] getEccXYZ() {
		return eccXYZ;
	}

}
