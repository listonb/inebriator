package com.inebriator;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.inebriator.listener.BaseSensorChangeListener;
import com.phidgets.InterfaceKitPhidget;
import com.phidgets.Phidget;
import com.phidgets.PhidgetException;
import com.phidgets.event.AttachEvent;
import com.phidgets.event.AttachListener;
import com.phidgets.event.DetachEvent;
import com.phidgets.event.DetachListener;
import com.phidgets.event.ErrorEvent;
import com.phidgets.event.ErrorListener;
import com.phidgets.event.SensorChangeListener;

public class PhidgetSolenoidController implements SolenoidController {

	private static final Logger LOG = LoggerFactory.getLogger(PhidgetSolenoidController.class);

	private final InterfaceKitPhidget[] phidgets;
	private final Set<BaseSensorChangeListener> sensorChangeListeners;

	public PhidgetSolenoidController(Integer[] serialNumbers, Set<BaseSensorChangeListener> sensorChangeListeners) {
		this.phidgets = new InterfaceKitPhidget[serialNumbers.length];
		this.sensorChangeListeners = sensorChangeListeners;

		for (int i = 0; i < serialNumbers.length; i++) {
			phidgets[i] = openAndAttachPhidget(serialNumbers[i]);
		}
	}

	@Override
	public void openSolenoid(Solenoid solenoid) {
		setSolenoidState(solenoid, true);
	}

	@Override
	public void closeSolenoid(Solenoid solenoid) {
		setSolenoidState(solenoid, false);
	}

	@Override
	public void disconnect() {
		for (int i = 0; i < phidgets.length; i++) {
			try {
				phidgets[i].close();
			} catch (PhidgetException e) {
				LOG.warn("Exception while closing Phidget {}", i, e);
			}
		}
	}

	public InterfaceKitPhidget[] getPhidgets() {
		return phidgets;
	}

	public Set<BaseSensorChangeListener> getSensorChangeListeners() {
		return sensorChangeListeners;
	}

	private class AttachListenerImpl implements AttachListener {
		@Override
		public void attached(AttachEvent attachEvent) {
			String serialNumber = getSerialNumberFromPhidgetForLogging(attachEvent.getSource());
			LOG.info("Phidget [{}] attached", serialNumber);
		}
	}

	private class DetachListenerImpl implements DetachListener {
		@Override
		public void detached(DetachEvent detachEvent) {
			String serialNumber = getSerialNumberFromPhidgetForLogging(detachEvent.getSource());
			LOG.info("Phidget [{}] detached", serialNumber);
		}
	}

	private class ErrorListenerImpl implements ErrorListener {
		@Override
		public void error(ErrorEvent errorEvent) {
			String serialNumber = getSerialNumberFromPhidgetForLogging(errorEvent.getSource());
			LOG.error("Received error event from phidget {}", serialNumber, errorEvent.getException());
		}
	}

	protected static int getSerialNumberFromPhidget(Phidget phidget) {
		int serialNumber;

		try {
			serialNumber = phidget.getSerialNumber();
		} catch (PhidgetException e) {
			throw new RuntimeException("Can't get serial number from phidget", e);
		}
		
		return serialNumber;
	}

	protected static String getSerialNumberFromPhidgetForLogging(Phidget phidget) {
		String serialNumber;

		try {
			serialNumber = Integer.toString(phidget.getSerialNumber());
		} catch (PhidgetException e) {
			serialNumber = "(unknown)";
		} catch (NumberFormatException e) {
			serialNumber = "(unknown)";
		}

		return serialNumber;
	}

	private InterfaceKitPhidget openAndAttachPhidget(int serialNumber) {
		InterfaceKitPhidget phidget;

		Phidget.enableLogging(Phidget.PHIDGET_LOG_INFO, null);

		try {
			phidget = new InterfaceKitPhidget();
			phidget.addAttachListener(new AttachListenerImpl());
			phidget.addDetachListener(new DetachListenerImpl());
			phidget.addErrorListener(new ErrorListenerImpl());

			for (SensorChangeListener sensorChangeListener : sensorChangeListeners) {
				phidget.addSensorChangeListener(sensorChangeListener);
			}

			LOG.info("Connecting to Phidget with serial number [{}]", serialNumber);
			phidget.open(serialNumber);

			LOG.info("Waiting for attachment to Phidget with serial number [{}]", serialNumber);
			phidget.waitForAttachment();

			LOG.info("Attached to Phidget with serial number [{}]", serialNumber);
		} catch (PhidgetException e) {
			throw new RuntimeException("Unable to connect to Phidget with serial number [" + serialNumber + "]", e);
		}

		return phidget;
	}

	private void setSolenoidState(Solenoid solenoid, boolean state) {
		LOG.debug("Setting state for {} to {}", solenoid, state);

		try {
			if (solenoid.getPhidgetId() + 1 > phidgets.length) {
				throw new RuntimeException("Phidget does not exist for " + solenoid);
			} else if (solenoid.getSolenoidId() + 1 > phidgets[solenoid .getPhidgetId()].getOutputCount()) {
				throw new RuntimeException("Output does not exist for " + solenoid);
			}

			phidgets[solenoid.getPhidgetId()].setOutputState(solenoid.getSolenoidId(), state);
		} catch (PhidgetException e) {
			throw new RuntimeException("Unable to set " + solenoid + " to state " + state, e);
		}
	}
}
