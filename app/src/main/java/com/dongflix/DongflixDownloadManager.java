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
	private static final String PREFS_NAME = "dongflix_bg_downloads";
	private static final String KEY_TASKS = "tasks_json";
	private static final String CHANNEL_ID = "dongflix_downloads";

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
		String raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TASKS, "");
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

	private synchronized void saveTasks(Context context, List<HashMap<String, Object>> list) {
		if (context == null || list == null) return;
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(KEY_TASKS, gson.toJson(list))
			.apply();
	}

	public synchronized void removeTask(Context context, String taskId) {
		if (context == null || taskId == null || taskId.isEmpty()) return;
		List<HashMap<String, Object>> current = getTasks(context);
		List<HashMap<String, Object>> next = new ArrayList<>();
		for (Map<String, Object> item : current) {
			if (!taskId.equals(String.valueOf(item.get("id")))) {
				next.add(new HashMap<String, Object>(item));
			}
		}
		saveTasks(context, next);
	}

	public void startDownload(Context context, String title, String episode, String streamUrl) {
		startDownload(context, title, episode, streamUrl, "", "");
	}

	public void startDownload(
		final Context context,
		final String title,
		final String episode,
		final String streamUrl,
		final String slug,
		final String referer
	) {
		if (context == null || streamUrl == null || streamUrl.trim().isEmpty()) return;
		final Context appCtx = context.getApplicationContext();
		final String safeTitle = (title == null || title.trim().isEmpty()) ? "Donghua" : title.trim();
		final String safeEp = (episode == null || episode.trim().isEmpty()) ? "Episode" : episode.trim();
		final String safeSlug = slug != null ? slug.trim() : "";
		final String safeReferer = (referer != null && !referer.trim().isEmpty())
			? referer.trim() : "https://anichin.co/";
		final String taskId = String.valueOf(Math.abs((safeTitle + "_" + safeEp).hashCode()));

		File dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		if (dir == null) dir = appCtx.getFilesDir();
		if (!dir.exists()) dir.mkdirs();

		String baseName = (safeTitle + "_" + safeEp)
			.replaceAll("[^a-zA-Z0-9._-]", "_")
			.replaceAll("_+", "_");
		if (baseName.length() > 64) baseName = baseName.substring(0, 64);
		final File targetFile = new File(dir, baseName + ".mp4");

		HashMap<String, Object> task = new HashMap<>();
		task.put("id", taskId);
		task.put("slug", safeSlug);
		task.put("title", safeTitle);
		task.put("episode", safeEp);
		task.put("url", streamUrl.trim());
		task.put("file_path", targetFile.getAbsolutePath());
		task.put("downloaded", 0L);
		task.put("total", 0L);
		task.put("progress", 0);
		task.put("status", "downloading");

		upsertTask(appCtx, task);
		broadcastUpdate(appCtx, taskId, 0, "downloading");

		executor.execute(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				InputStream in = null;
				FileOutputStream out = null;
				try {
					URL url = new URL(streamUrl.trim());
					conn = (HttpURLConnection) url.openConnection();
					conn.setInstanceFollowRedirects(true);
					conn.setConnectTimeout(15000);
					conn.setReadTimeout(25000);
					conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36");
					conn.setRequestProperty("Referer", safeReferer);
					conn.connect();

					int code = conn.getResponseCode();
					if (code < 200 || code >= 300) {
						throw new Exception("HTTP " + code);
					}

					long total = conn.getContentLengthLong();
					in = conn.getInputStream();
					out = new FileOutputStream(targetFile);

					byte[] buf = new byte[16 * 1024];
					long downloaded = 0L;
					int lastPct = -1;
					long lastTime = 0L;
					int n;

					while ((n = in.read(buf)) != -1) {
						out.write(buf, 0, n);
						downloaded += n;

						int pct = total > 0L
							? (int) Math.min(99L, (downloaded * 100L) / total)
							: Math.min(95, (int) (downloaded / (512 * 1024L)));

						long now = System.currentTimeMillis();
						if (pct != lastPct || (now - lastTime) > 650L) {
							lastPct = pct;
							lastTime = now;
							updateTask(appCtx, taskId, downloaded, total, pct, "downloading");
							postSystemNotification(appCtx, taskId.hashCode(), safeTitle + " • " + safeEp, pct, false);
							broadcastUpdate(appCtx, taskId, pct, "downloading");
						}
					}
					out.flush();

					long finalTotal = Math.max(total, downloaded);
					updateTask(appCtx, taskId, downloaded, finalTotal, 100, "completed");
					postSystemNotification(appCtx, taskId.hashCode(), safeTitle + " • " + safeEp, 100, true);
					broadcastUpdate(appCtx, taskId, 100, "completed");
				} catch (Exception e) {
					updateTask(appCtx, taskId, 0L, 0L, 0, "failed");
					broadcastUpdate(appCtx, taskId, 0, "failed");
				} finally {
					try { if (in != null) in.close(); } catch (Exception ignored) {}
					try { if (out != null) out.close(); } catch (Exception ignored) {}
					try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
				}
			}
		});
	}

	private synchronized void upsertTask(Context ctx, HashMap<String, Object> item) {
		List<HashMap<String, Object>> tasks = getTasks(ctx);
		String id = String.valueOf(item.get("id"));
		boolean replaced = false;
		for (int i = 0; i < tasks.size(); i++) {
			if (id.equals(String.valueOf(tasks.get(i).get("id")))) {
				tasks.set(i, item);
				replaced = true;
				break;
			}
		}
		if (!replaced) tasks.add(0, item);
		saveTasks(ctx, tasks);
	}

	private synchronized void updateTask(Context ctx, String taskId, long dl, long tot, int pct, String status) {
		List<HashMap<String, Object>> tasks = getTasks(ctx);
		for (Map<String, Object> t : tasks) {
			if (taskId.equals(String.valueOf(t.get("id")))) {
				t.put("downloaded", dl);
				t.put("total", tot);
				t.put("progress", pct);
				t.put("status", status);
				break;
			}
		}
		saveTasks(ctx, tasks);
	}

	private void broadcastUpdate(Context ctx, String taskId, int pct, String status) {
		Intent i = new Intent(ACTION_DOWNLOAD_PROGRESS);
		i.setPackage(ctx.getPackageName());
		i.putExtra("task_id", taskId);
		i.putExtra("progress", pct);
		i.putExtra("status", status);
		ctx.sendBroadcast(i);
	}

	private void postSystemNotification(Context ctx, int id, String label, int pct, boolean done) {
		try {
			NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm == null) return;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				nm.createNotificationChannel(new NotificationChannel(
					CHANNEL_ID, "Unduhan Dongflix", NotificationManager.IMPORTANCE_LOW
				));
			}
			NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
				.setSmallIcon(done ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_sys_download)
				.setContentTitle(done ? "Unduhan Selesai" : "Mengunduh Video")
				.setContentText(done ? label + " siap diputar offline" : String.format(Locale.US, "%s (%d%%)", label, pct))
				.setOnlyAlertOnce(true)
				.setOngoing(!done)
				.setAutoCancel(done);
			if (!done) b.setProgress(100, pct, false);
			nm.notify(id, b.build());
		} catch (Throwable ignored) {}
	}
}
