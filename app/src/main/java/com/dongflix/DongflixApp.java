package com.dongflix;

import android.app.Application;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DongflixApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		installGlobalCrashHandler();
	}

	private void installGlobalCrashHandler() {
		final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					if (throwable != null) {
						throwable.printStackTrace(pw);
					} else {
						pw.println("Unknown uncaught exception on thread: " + (thread != null ? thread.getName() : "unknown"));
					}
					pw.flush();

					Intent intent = new Intent(DongflixApp.this, CrashLogActivity.class);
					intent.putExtra(CrashLogActivity.EXTRA_CRASH_LOG, sw.toString());
					intent.addFlags(
						Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_CLEAR_TASK
							| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
					);
					startActivity(intent);
				} catch (Throwable ignored) {
					if (previousHandler != null && throwable != null) {
						previousHandler.uncaughtException(thread, throwable);
						return;
					}
				}

				try {
					Thread.sleep(300);
				} catch (InterruptedException ignored) {}

				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(10);
			}
		});
	}
}
