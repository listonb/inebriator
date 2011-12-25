package com.inebriator;

public class Solenoid {

	private final int phidgetId;
	private final int solenoidId;
	
	public Solenoid(int phidgetId, int solenoidId) {
		this.phidgetId = phidgetId;
		this.solenoidId = solenoidId;
	}
	
	@Override
	public String toString() {
		return "Solenoid [phidgetId=" + phidgetId + ", solenoidId="
				+ solenoidId + "]";
	}

	public Solenoid(String ids) {
		String[] parts = ids.split(",");
		if (parts.length != 2) {
			throw new RuntimeException("Invalid solenoid config: [" + ids + "]");
		}

		this.phidgetId = parseInteger(parts[0]);
		this.solenoidId = parseInteger(parts[1]);
	}

	public int getPhidgetId() {
		return phidgetId;
	}

	public int getSolenoidId() {
		return solenoidId;
	}
	
	private static int parseInteger(String s) {
		int value;

		try {
			value = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new RuntimeException("Can't parse string [" + s + "] as an integer");
		}

		return value;
	}
}
