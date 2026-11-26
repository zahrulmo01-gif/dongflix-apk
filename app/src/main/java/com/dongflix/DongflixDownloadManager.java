package com.dongflix;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DongflixDownloadManager {

	public static final String ACTION_DOWNLOAD_PROGRESS = "com.dongflix.ACTION_DOWNLOAD_PROGRESS";
	private static final String PREFS_NAME = "dongflix_downloads_state";
	private static final String KEY_TASKS = "tasks_json";
	private static final String CHANNEL_ID = "dongflix_download_channel";

	private static DongflixDownloadManager instance;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final Gson gson = new Gson();

	public static synchronized DongflixDownloadManager getInstance() {
		if (instance == null) {
			instance = new DongflixDownloadManager();
		}
		return instance;
	}

	public synchronized List<HashMap<String, Object>> getTasks(Context context) {
		if (context == null) return new ArrayList<>();
		SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String raw = sp.getString(KEY_TASKS, "");
		if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
		try {
			List<HashMap<String, Object>> list = gson.fromJson(
				raw, new TypeToken<List<HashMap<String, Object>>>(){}.getType()
			);
			return list != null ? list : new ArrayList<HashMap<String, Object>>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private synchronized void saveTasks(Context context, List<HashMap<String, Object>> tasks) {
		if (context == null || tasks == null) return;
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(KEY_TASKS, gson.toJson(tasks))
			.apply();
	}

	public synchronized void removeTask(Context context, String taskId) {
		if (context == null || taskId == null || taskId.isEmpty()) return;
		List<HashMap<String, Object>> tasks = getTasks(context);
		List<HashMap<String, Object>> next = new ArrayList<>();
		for (Map<String, Object> t : tasks) {
			if (!taskId.equals(String.valueOf(t.get("id")))) {
				next.add(new HashMap<String, Object>(t));
			}
		}
		saveTasks(context, next);
	}

	public void startDownload(final Context context, final String title, final String episode, final String coverUrl, final String videoUrl, final String referer) {
		if (context == null || videoUrl == null || videoUrl.trim().isEmpty()) return;
		final Context appCtx = context.getApplicationContext();
		final String safeTitle = (title == null || title.trim().isEmpty()) ? "Dongflix Video" : title.trim();
		final String safeEp = (episode == null || episode.trim().isEmpty()) ? "EP 1" : episode.trim();
		final String taskId = String.valueOf(Math.abs((safeTitle + "_" + safeEp + "_" + videoUrl).hashCode()));

		File dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		if (dir == null) dir = appCtx.getFilesDir();
		if (!dir.exists()) dir.mkdirs();

		String cleanName = (safeTitle + "_" + safeEp)
			.replaceAll("[^a-zA-Z0-9._-]", "_")
			.replaceAll("_+", "_");
		if (cleanName.length() > 60) cleanName = cleanName.substring(0, 60);
		final File outFile = new File(dir, cleanName + ".mp4");

		HashMap<String, Object> task = new HashMap<>();
		task.put("id", taskId);
		task.put("title", safeTitle);
		task.put("episode", safeEp);
		task.put("cover", coverUrl != null ? coverUrl : "");
		task.put("url", videoUrl.trim());
		task.put("file_path", outFile.getAbsolutePath());
		task.put("downloaded", 0L);
		task.put("total", 0L);
		task.put("progress", 0);
		task.put("status", "downloading");

		upsertTask(appCtx, task);
		broadcastProgress(appCtx, taskId, 0, "downloading");

		executor.execute(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				InputStream in = null;
				FileOutputStream out = null;
				try {
					URL url = new URL(videoUrl.trim());
					conn = (HttpURLConnection) url.openConnection();
					conn.setInstanceFollowRedirects(true);
					conn.setConnectTimeout(15000);
					conn.setReadTimeout(25000);
					conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
					if (referer != null && !referer.isEmpty()) {
						conn.setRequestProperty("Referer", referer);
					} else if (videoUrl.contains("ok.ru") || videoUrl.contains("okcdn")) {
						conn.setRequestProperty("Referer", "https://ok.ru/");
					}
					conn.connect();

					int code = conn.getResponseCode();
					if (code < 200 || code >= 300) {
						throw new Exception("HTTP " + code);
					}

					long totalBytes = conn.getContentLengthLong();
					in = conn.getInputStream();
					out = new FileOutputStream(outFile);

					byte[] buffer = new byte[16 * 1024];
					long downloaded = 0L;
					int lastPercent = -1;
					long lastNotifyTime = 0L;
					int read;

					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						downloaded += read;

						int percent = totalBytes > 0L
							? (int) Math.min(99L, (downloaded * 100L) / totalBytes)
							: Math.min(95, (int) (downloaded / (512 * 1024L)));

						long now = System.currentTimeMillis();
						if (percent != lastPercent || (now - lastNotifyTime) > 700L) {
							lastPercent = percent;
							lastNotifyTime = now;
							updateTaskProgress(appCtx, taskId, downloaded, totalBytes, percent, "downloading");
							showDownloadNotification(appCtx, taskId.hashCode(), safeTitle + " (" + safeEp + ")", percent, false);
							broadcastProgress(appCtx, taskId, percent, "downloading");
						}
					}
					out.flush();

					long finalTotal = Math.max(totalBytes, downloaded);
					updateTaskProgress(appCtx, taskId, downloaded, finalTotal, 100, "completed");
					showDownloadNotification(appCtx, taskId.hashCode(), safeTitle + " (" + safeEp + ")", 100, true);
					broadcastProgress(appCtx, taskId, 100, "completed");
				} catch (Exception e) {
					updateTaskProgress(appCtx, taskId, 0L, 0L, 0, "failed");
					broadcastProgress(appCtx, taskId, 0, "failed");
				} finally {
					try { if (in != null) in.close(); } catch (Exception ignored) {}
					try { if (out != null) out.close(); } catch (Exception ignored) {}
					try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
				}
			}
		});
	}

	private synchronized void upsertTask(Context ctx, HashMap<String, Object> newTask) {
		List<HashMap<String, Object>> tasks = getTasks(ctx);
		String id = String.valueOf(newTask.get("id"));
		boolean replaced = false;
		for (int i = 0; i < tasks.size(); i++) {
			if (id.equals(String.valueOf(tasks.get(i).get("id")))) {
				tasks.set(i, newTask);
				replaced = true;
				break;
			}
		}
		if (!replaced) tasks.add(0, newTask);
		saveTasks(ctx, tasks);
	}

	private synchronized void updateTaskProgress(
		Context ctx, String taskId, long downloaded, long total, int percent, String status
	) {
		List<HashMap<String, Object>> tasks = getTasks(ctx);
		for (Map<String, Object> t : tasks) {
			if (taskId.equals(String.valueOf(t.get("id")))) {
				t.put("downloaded", downloaded);
				t.put("total", total);
				t.put("progress", percent);
				t.put("status", status);
				break;
			}
		}
		saveTasks(ctx, tasks);
	}

	private void broadcastProgress(Context ctx, String taskId, int percent, String status) {
		Intent intent = new Intent(ACTION_DOWNLOAD_PROGRESS);
		intent.setPackage(ctx.getPackageName());
		intent.putExtra("task_id", taskId);
		intent.putExtra("progress", percent);
		intent.putExtra("status", status);
		ctx.sendBroadcast(intent);
	}

	private void showDownloadNotification(Context ctx, int notifId, String title, int progress, boolean done) {
		try {
			NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm == null) return;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel ch = new NotificationChannel(
					CHANNEL_ID, "Unduhan Video Dongflix", NotificationManager.IMPORTANCE_LOW
				);
				nm.createNotificationChannel(ch);
			}
			int iconRes = ctx.getResources().getIdentifier("ic_download", "drawable", ctx.getPackageName());
			if (iconRes <= 0) iconRes = android.R.drawable.stat_sys_download;

			NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
				.setSmallIcon(done ? android.R.drawable.stat_sys_download_done : iconRes)
				.setContentTitle(done ? "Unduhan Selesai" : "Mengunduh: " + title)
				.setContentText(done ? "Siap ditonton offline" : String.format(Locale.US, "Progress: %d%%", progress))
				.setOnlyAlertOnce(true)
				.setOngoing(!done)
				.setAutoCancel(done);
			if (!done) {
				b.setProgress(100, progress, false);
			}
			nm.notify(notifId, b.build());
		} catch (Throwable ignored) {}
	}
}
