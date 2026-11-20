package com.dongflix;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import java.io.PrintWriter;
import java.io.StringWriter;

public class DongflixApp extends Application {

	public static final String PREFS_NAME = "dongflix_crash_prefs";
	public static final String KEY_CRASH_LOG = "last_crash_log";

	@Override
	public void onCreate() {
		super.onCreate();
		installGlobalCrashHandler();
	}

	private void installGlobalCrashHandler() {
		final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					throwable.printStackTrace(pw);

					String report = "Thread: " + thread.getName()
						+ "\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL
						+ " (Android " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT + ")\n\n"
						+ sw.toString();

					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putString(KEY_CRASH_LOG, report).apply();

					Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
					intent.putExtra("crash_log", report);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(intent);
				} catch (Throwable ignored) {
					if (defaultHandler != null) {
						defaultHandler.uncaughtException(thread, throwable);
						return;
					}
				}

				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(1);
			}
		});
	}
}
