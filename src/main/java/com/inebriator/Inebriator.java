package com.inebriator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.OperationStatus;

public class Inebriator {

	private static final Logger LOG = LoggerFactory.getLogger(Inebriator.class);

	private static final long POUR_TIMEOUT_MILLIS = 30000;

	private static final String AIR_SOLENOID_NAME = "air";
	private static final String WATER_SOLENOID_NAME = "water";
	private static final String SPOUT_SOLENOID_NAME = "spout";
	private static final String DRAIN_SOLENOID_NAME = "drain";
	private static final String READY_LIGHT_SOLENOID_NAME = "ready_light";
	private static final String POURING_LIGHT_SOLENOID_NAME = "pouring_light";
	private static final String ERROR_LIGHT_SOLENOID_NAME = "error_light";

	private final SolenoidController solenoidController;
	private final Database drinksDb;
	private final Map<String, Solenoid> solenoidsByName;
	private final long pourMillisPerUnit;
	private final long airFlushDurationMillis;
	private final long waterFlushDurationMillis;
	private final long maxPourUnits;
	private final Solenoid airSolenoid;
	private final Solenoid waterSolenoid;
	private final Solenoid spoutSolenoid;
	private final Solenoid drainSolenoid;
	private final Solenoid readyLightSolenoid;
	private final Solenoid pouringLightSolenoid;
	// TODO write logic to use the error light
	@SuppressWarnings("unused")
	private final Solenoid errorLightSolenoid;

	public SolenoidController getSolenoidController() {
		return solenoidController;
	}

	private final AtomicLong drinkCount = new AtomicLong(0);
	private final Lock pourLock = new ReentrantLock();

	public Inebriator(SolenoidController solenoidController, Database drinksDb,
			Map<String, Solenoid> solenoidsByName, long pourMillisPerUnit, long airFlushDurationMillis, long waterFlushDurationMillis, long maxPourUnits) {
		this.solenoidController = solenoidController;
		this.drinksDb = drinksDb;
		this.solenoidsByName = solenoidsByName;
		this.pourMillisPerUnit = pourMillisPerUnit;
		this.airFlushDurationMillis = airFlushDurationMillis;
		this.waterFlushDurationMillis = waterFlushDurationMillis;
		this.maxPourUnits = maxPourUnits;

		this.airSolenoid = getRequiredSolenoid(solenoidsByName, AIR_SOLENOID_NAME);
		this.waterSolenoid = getRequiredSolenoid(solenoidsByName, WATER_SOLENOID_NAME);
		this.spoutSolenoid = getRequiredSolenoid(solenoidsByName, SPOUT_SOLENOID_NAME);
		this.drainSolenoid = getRequiredSolenoid(solenoidsByName, DRAIN_SOLENOID_NAME);
		this.readyLightSolenoid = getRequiredSolenoid(solenoidsByName, READY_LIGHT_SOLENOID_NAME);
		this.pouringLightSolenoid = getRequiredSolenoid(solenoidsByName, POURING_LIGHT_SOLENOID_NAME);
		this.errorLightSolenoid = getRequiredSolenoid(solenoidsByName, ERROR_LIGHT_SOLENOID_NAME);

		reset();
	}

	public Map<String, Solenoid> getSolenoidsByName() {
		return solenoidsByName;
	}

	public Set<String> getAvailableCocktailNames() {
		Cursor cursor = drinksDb.openCursor(null, null);
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();
		value.setPartial(true);
		
		Set<String> result = new HashSet<String>();

		for (;;) {
			OperationStatus status = cursor.getNext(key, value, null);
			if (!status.equals(OperationStatus.SUCCESS)) {
				break;
			}
			
			result.add(new String(key.getData()));
		}
		
		cursor.close();
		return result;
	}

	public void addCocktailDefinition(String name, Cocktail cocktail) {
		DatabaseEntry key = new DatabaseEntry(cocktail.getName().getBytes());
		Gson gson = new Gson();
		String drinkJson = gson.toJson(cocktail);
		DatabaseEntry value = new DatabaseEntry(drinkJson.getBytes());

		OperationStatus status = drinksDb.put(null, key, value);
		if (status.equals(OperationStatus.SUCCESS)) {
			LOG.info("Successfully added cocktail {} to the DB", name);
		} else {
			LOG.warn("Add cocktail {} = {} to the DB failed", name, cocktail);
		}
	}
	
	public Cocktail getCocktailDefinition(String name) {
		DatabaseEntry key = new DatabaseEntry(name.getBytes());
		DatabaseEntry value = new DatabaseEntry();
		OperationStatus status = drinksDb.get(null, key, value, null);
		if (status.equals(OperationStatus.NOTFOUND)) {
			throw new RuntimeException("Drink [" + name + "] not found");
		}

		Gson gson = new Gson();
		String drinkJson = new String(value.getData());
		return gson.fromJson(new String(drinkJson), Cocktail.class);
	}
	
	public void deleteCocktailDefinition(String name) {
		DatabaseEntry key = new DatabaseEntry(name.getBytes());
		OperationStatus status = drinksDb.delete(null, key);
		if (status.equals(OperationStatus.NOTFOUND)) {
			throw new RuntimeException("Drink [" + name + "] not found");
		}
	}

	public void pourCocktail(String name) {
		Cocktail cocktail = getCocktailDefinition(name);
		validateCocktail(cocktail);

		try {
			pourLock.lock();
			solenoidController.closeSolenoid(readyLightSolenoid);
			solenoidController.openSolenoid(pouringLightSolenoid);

			long drinkNumber = drinkCount.incrementAndGet();

			StopWatch stopWatch = new StopWatch();
			LOG.info("Pouring drink #{}: {}", drinkNumber, name);

			stopWatch.start();
			pourCocktail(cocktail);
			stopWatch.stop();

			LOG.info("Finished pouring drink #{} ({}) in {} ms", new Object[] { drinkNumber, name, stopWatch.getTime() });
		} finally {
			solenoidController.closeSolenoid(pouringLightSolenoid);
			solenoidController.openSolenoid(readyLightSolenoid);
			pourLock.unlock();
		}
	}

	public void pourStraight(String solenoidName, int units) {
		Solenoid solenoid = solenoidsByName.get(solenoidName);
		if (solenoid == null) {
			throw new RuntimeException("Beverage [" + solenoidName + "] is not defined");
		}

		if (units > maxPourUnits) {
			throw new RuntimeException("Pour request exceeds the maximum of [" + units + "]");
		}

		try {
			pourLock.unlock();
			solenoidController.closeSolenoid(readyLightSolenoid);
			solenoidController.openSolenoid(pouringLightSolenoid);

			// TODO sanity check that it's a beverage (i.e. don't let them pour
			// air or spout)

			long drinkNumber = drinkCount.incrementAndGet();

			StopWatch stopWatch = new StopWatch();
			LOG.info("Pouring drink #{}: {} ({} units)", new Object[] { drinkNumber, solenoidName, units });

			stopWatch.start();
			pourStraight(solenoid, units * pourMillisPerUnit);
			stopWatch.stop();

			LOG.info("Finished pouring drink #{} ({}) in {} ms", new Object[] { drinkNumber, solenoidName, stopWatch.getTime() });
		} finally {
			solenoidController.closeSolenoid(pouringLightSolenoid);
			solenoidController.openSolenoid(readyLightSolenoid);
			pourLock.unlock();
		}
	}

	public void reset() {
		LOG.info("Resetting all solenoids to closed state");

		for (Solenoid solenoid : solenoidsByName.values()) {
			solenoidController.closeSolenoid(solenoid);
		}
		solenoidController.openSolenoid(readyLightSolenoid);

		LOG.info("All solenoids have been reset to closed state");
	}

	private void pourStraight(Solenoid solenoid, long durationMillis) {
		LOG.debug("Pouring {} for {} ms", solenoid, durationMillis);
		solenoidController.openSolenoid(spoutSolenoid);
		pourSingleBeverage(solenoid, durationMillis);
		flush();
		LOG.debug("Pour {} for {} ms complete", solenoid, durationMillis);
	}

	private void pourCocktail(Cocktail cocktail) {
		int numTasks = cocktail.getIngredients().size();
		Set<PourTask> tasks = new HashSet<PourTask>(numTasks);

		for (Map.Entry<String, Integer> entry : cocktail.getIngredients().entrySet()) {
			String solenoidName = entry.getKey();
			Solenoid solenoid = solenoidsByName.get(solenoidName);
			if (solenoid == null) {
				throw new RuntimeException("Missing solenoid definition [" + solenoidName + "] for " + cocktail);
			}

			int units = entry.getValue();
			long durationMillis = units * pourMillisPerUnit;
			
			tasks.add(new PourTask(solenoid, durationMillis));
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(numTasks);
		List<Future<Object>> results = null;
		boolean abortMission = false;

		solenoidController.openSolenoid(spoutSolenoid);

		try {
			results = threadPool.invokeAll(tasks);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted while pouring!", e);
			abortMission = true;
		}

		if (results != null) {
			for (Future<Object> result : results) {
				try {
					result.get(POUR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					LOG.error("Interrupted while pouring! Abort mission!", e);
					abortMission = true;
					break;
				} catch (ExecutionException e) {
					LOG.error("Unable to pour! Abort mission!", e);
					abortMission = true;
					break;
				} catch (TimeoutException e) {
					LOG.error("Timeout while pouring! Abort mission!", e);
					abortMission = true;
					break;
				} catch (Exception e) {
					LOG.error("Internal error while pouring! Abort mission!", e);
					abortMission = true;
					break;
				}
			}
		}

		if (abortMission) {
			LOG.warn("Pour failed -- resetting all solenoids");
			reset();
		} else {
			flush();
		}
	}

	private void validateCocktail(Cocktail cocktail) {
		int sum = 0;

		for (Integer unitCount : cocktail.getIngredients().values()) {
			sum += unitCount;
		}

		if (sum > maxPourUnits) {
			throw new RuntimeException("Pour request exceeds the maximum of [" + maxPourUnits + "]");
		}
	}

	private void pourSingleBeverage(Solenoid solenoid, long durationMillis) {
		LOG.debug("Pouring {} for [{}] ms", solenoid, durationMillis);

		try {
			solenoidController.openSolenoid(solenoid);

			long targetOffMillis = System.currentTimeMillis() + durationMillis;

			for (;;) {
				long millisRemaining = targetOffMillis - System.currentTimeMillis();
				if (millisRemaining <= 0) {
					break;
				}

				try {
					Thread.sleep(millisRemaining);
				} catch (InterruptedException e) {
					// Ignore for now
				}
			}
		} finally {
			solenoidController.closeSolenoid(solenoid);
		}

		LOG.debug("Finished pouring {} for [{}] ms", solenoid, durationMillis);
	}

	private void flush() {
		LOG.debug("Flushing");

		pourSingleBeverage(airSolenoid, airFlushDurationMillis);
		snooze(500);
		solenoidController.closeSolenoid(spoutSolenoid);
		solenoidController.openSolenoid(drainSolenoid);
		snooze(500);
		pourSingleBeverage(waterSolenoid, waterFlushDurationMillis);
		snooze(500);
		pourSingleBeverage(airSolenoid, airFlushDurationMillis);
		snooze(500);
		solenoidController.closeSolenoid(drainSolenoid);

		LOG.debug("Flush complete");
	}

	private static Solenoid getRequiredSolenoid(Map<String, Solenoid> solenoidsByName, String name) {
		Solenoid solenoid = solenoidsByName.get(name);

		if (solenoid == null) {
			throw new RuntimeException("Missing required solenoid [" + name + "]");
		}

		return solenoid;
	}

	public static void snooze(long millis) {
		LOG.debug("Snoozing for [{}] ms", millis);

		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted", e);
		}
	}

	private class PourTask implements Callable<Object> {
		private final Solenoid solenoid;
		private final long durationMillis;

		public PourTask(Solenoid solenoid, long durationMillis) {
			this.solenoid = solenoid;
			this.durationMillis = durationMillis;
		}

		@Override
		public Object call() throws Exception {
			pourSingleBeverage(solenoid, durationMillis);
			return null;
		}
	}
}
