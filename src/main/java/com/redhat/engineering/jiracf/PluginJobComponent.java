package com.redhat.engineering.jiracf;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * Base class for scheduled plugin jobs, providing convenient functionality missing from
 * {@link com.atlassian.sal.api.scheduling.PluginJob}.
 * <p>
 * Differences between this class and PluginJob include:
 * <ul>
 *     <li>the job is a first-class plugin component with a single instance re-used at each execution</li>
 *     <li>the job is self-registering; no need to make a separate plugin component just to register the job</li>
 *     <li>dependency injection works as usual, rather than passing all needed objects as a map</li>
 *     <li>all exceptions and errors from within the job are caught and logged - as opposed to PluginJob, where
 *         errors raised can silently kill the scheduler's worker thread and prevent further executions</li>
 *     <li>the current thread's context classloader is always set to the class of the job, meaning OSGi imports
 *         work as expected during the job's execution</li>
 *     <li>the job will not allow itself to be run from separate threads simultaneously</li>
 * </ul>
 * <p>
 * To use this class, extend it, implement at least {@link #execute()} and {@link #jobInterval()}, and add a snippet
 * like the following to atlassian-plugin.xml:
 * <pre>{@code
 *     <component key="my-job" class="com.my.SomeJob" public="true">
 *         <interface>com.atlassian.sal.api.lifecycle.LifecycleAware</interface>
 *     </component>
 * }</pre>
 */
public abstract class PluginJobComponent implements InitializingBean, DisposableBean, LifecycleAware {
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginJobComponent.class);

	private final PluginScheduler pluginScheduler;
	private final Semaphore sem = new Semaphore(1);

	private boolean initialized;
	private boolean started;
	private boolean scheduled;

	/**
	 * @param pluginScheduler to be injected
	 */
	protected PluginJobComponent(PluginScheduler pluginScheduler) {
		this.pluginScheduler = pluginScheduler;
	}

	@Override
	public void destroy() {
		if (!scheduled) {
			return;
		}
		final String jobKey = jobKey();
		pluginScheduler.unscheduleJob(jobKey);
		scheduled = false;
		LOGGER.info("unscheduled {}", jobKey);
	}

	@Override
	public void afterPropertiesSet() {
		initialized = true;
		scheduleJob();
	}

	@Override
	public void onStart() {
		started = true;
		scheduleJob();
	}

	private void scheduleJob() {
		if (scheduled) {
			return;
		}

		// According to PluginScheduler docs, pluginscheduler must not be used until Lifecycle.onStart was called
		if (!initialized || !started) {
			return;
		}

		final String jobKey = jobKey();
		pluginScheduler.scheduleJob(
				jobKey,
				DelegatingPluginJob.class,
				Collections.<String, Object> singletonMap("component", this),
				jobStartTime(),
				jobInterval()
		);
		scheduled = true;
		LOGGER.info("scheduled {}", jobKey);
	}

	/**
	 * @return The time at which the job should first run. Default implementation returns now, starting the job immediately.
	 */
	protected Date jobStartTime() {
		return new Date();
	}

	/**
	 * @return The key used to register the job in the plugin scheduler.  Default implementation returns a value derived
	 * from the job's class.
	 */
	protected String jobKey() {
		return String.format("%s:job", getClass().getName());
	}

	/**
	 * @return The desired interval, in milliseconds, between executions of the job.
	 */
	protected abstract long jobInterval();
	protected abstract void execute();

	protected void executeJob() {
		final Class<?> klass = getClass();
		LOGGER.debug("Executing {}", klass);

		final ClassLoader classLoader = klass.getClassLoader();
		final Thread currentThread = Thread.currentThread();
		final ClassLoader oldClassLoader = currentThread.getContextClassLoader();

		if (!sem.tryAcquire()) {
			LOGGER.warn("Detected attempt to execute multiple {} concurrently, ignored.", klass);
			return;
		}
		try {
			currentThread.setContextClassLoader(classLoader);
			execute();
		} catch (Throwable t) {
			// we catch all Throwable because Atlassian's plugin scheduler seemingly doesn't do anything smart
			// if a plugin job raises anything; instead the scheduler thread silently dies, blocking any future
			// runs. This isn't acceptable, so catch and log everything ourselves
			LOGGER.error("Error executing {}", jobKey(), t);
		} finally {
			sem.release();
			currentThread.setContextClassLoader(oldClassLoader);
		}
	}

	@Override
	public void onStop() {
		this.destroy();
	}
}
