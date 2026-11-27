package com.dongflix;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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

	public static String computeTaskId(String title, String episode) {
		String safeTitle = (title == null || title.trim().isEmpty()) ? "Donghua" : title.trim();
		String safeEp = (episode == null || episode.trim().isEmpty()) ? "Episode" : episode.trim();
		return String.valueOf(Math.abs((safeTitle + "_" + safeEp).hashCode()));
	}

	public synchronized HashMap<String, Object> getTaskById(Context context, String taskId) {
		if (context == null || taskId == null || taskId.isEmpty()) return null;
		List<HashMap<String, Object>> tasks = getTasks(context);
		for (HashMap<String, Object> t : tasks) {
			if (taskId.equals(String.valueOf(t.get("id")))) {
				return new HashMap<>(t);
			}
		}
		return null;
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
		startDownload(context, title, episode, streamUrl, "", "", "");
	}

	public void startDownload(
		Context context,
		String title,
		String episode,
		String streamUrl,
		String slug,
		String referer
	) {
		startDownload(context, title, episode, streamUrl, slug, referer, "");
	}

	public void startDownload(
		final Context context,
		final String title,
		final String episode,
		final String streamUrl,
		final String slug,
		final String referer,
		final String cookie
	) {
		if (context == null || streamUrl == null || streamUrl.trim().isEmpty()) return;
		final Context appCtx = context.getApplicationContext();
		final String safeTitle = (title == null || title.trim().isEmpty()) ? "Donghua" : title.trim();
		final String safeEp = (episode == null || episode.trim().isEmpty()) ? "Episode" : episode.trim();
		final String safeSlug = slug != null ? slug.trim() : "";
		final String cleanUrl = streamUrl.trim();
		final String safeReferer = resolveReferer(cleanUrl, referer);
		final String safeCookie = cookie != null ? cookie.trim() : "";
		final String taskId = computeTaskId(safeTitle, safeEp);

		HashMap<String, Object> existing = getTaskById(appCtx, taskId);
		if (existing != null) {
			String curStatus = String.valueOf(existing.get("status"));
			String existingPath = String.valueOf(existing.get("file_path"));
			if ("downloading".equals(curStatus)) {
				return;
			}
			if ("completed".equals(curStatus) && existingPath != null && new File(existingPath).exists() && new File(existingPath).length() > 0L) {
				broadcastUpdate(appCtx, taskId, 100, "completed", safeSlug, safeTitle, safeEp);
				return;
			}
		}

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
		task.put("url", cleanUrl);
		task.put("file_path", targetFile.getAbsolutePath());
		task.put("downloaded", 0L);
		task.put("total", 0L);
		task.put("progress", 0);
		task.put("status", "downloading");

		upsertTask(appCtx, task);
		broadcastUpdate(appCtx, taskId, 0, "downloading", safeSlug, safeTitle, safeEp);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					String lowerUrl = cleanUrl.toLowerCase(Locale.US);
					if (lowerUrl.contains(".m3u8")) {
						downloadHlsToFile(appCtx, taskId, safeSlug, safeTitle, safeEp, cleanUrl, safeReferer, safeCookie, targetFile);
					} else {
						downloadDirectFile(appCtx, taskId, safeSlug, safeTitle, safeEp, cleanUrl, safeReferer, safeCookie, targetFile);
					}

					if (!targetFile.exists() || targetFile.length() <= 1024L) {
						throw new Exception("File kosong atau tidak valid");
					}

					long fileLen = targetFile.length();
					updateTask(appCtx, taskId, fileLen, fileLen, 100, "completed");
					postSystemNotification(appCtx, taskId.hashCode(), safeTitle + " • " + safeEp, 100, true);
					broadcastUpdate(appCtx, taskId, 100, "completed", safeSlug, safeTitle, safeEp);
				} catch (Exception e) {
					try { if (targetFile.exists()) targetFile.delete(); } catch (Exception ignored) {}
					updateTask(appCtx, taskId, 0L, 0L, 0, "failed");
					postSystemNotification(appCtx, taskId.hashCode(), safeTitle + " • " + safeEp, 0, false);
					broadcastUpdate(appCtx, taskId, 0, "failed", safeSlug, safeTitle, safeEp);
				}
			}
		});
	}

	private String resolveReferer(String url, String referer) {
		if (referer != null && !referer.trim().isEmpty()) {
			return referer.trim();
		}
		String lower = url.toLowerCase(Locale.US);
		if (lower.contains("ok.ru") || lower.contains("odnoklassniki") || lower.contains("okcdn")) {
			return "https://ok.ru/";
		}
		if (lower.contains("dailymotion") || lower.contains("dmcdn")) {
			return "https://geo.dailymotion.com/";
		}
		if (lower.contains("rumble")) {
			return "https://rumble.com/";
		}
		if (lower.contains("odysee") || lower.contains("odycdn")) {
			return "https://odysee.com/";
		}
		return "https://dongflix.my.id/";
	}

	private HttpURLConnection openStreamConnection(String targetUrl, String referer, String cookie) throws Exception {
		String current = targetUrl;
		for (int i = 0; i < 6; i++) {
			URL url = new URL(current);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(25000);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
			if (referer != null && !referer.isEmpty()) {
				conn.setRequestProperty("Referer", referer);
				try {
					Uri rUri = Uri.parse(referer);
					if (rUri.getScheme() != null && rUri.getHost() != null) {
						conn.setRequestProperty("Origin", rUri.getScheme() + "://" + rUri.getHost());
					}
				} catch (Exception ignored) {}
			}
			if (cookie != null && !cookie.isEmpty()) {
				conn.setRequestProperty("Cookie", cookie);
			}

			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
				|| code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				conn.disconnect();
				if (loc == null || loc.isEmpty()) break;
				if (loc.startsWith("//")) loc = "https:" + loc;
				else if (loc.startsWith("/")) loc = url.getProtocol() + "://" + url.getHost() + loc;
				current = loc;
				continue;
			}
			return conn;
		}
		throw new Exception("Terlalu banyak redirect");
	}

	private void downloadDirectFile(
		Context appCtx,
		String taskId,
		String safeSlug,
		String safeTitle,
		String safeEp,
		String targetUrl,
		String referer,
		String cookie,
		File targetFile
	) throws Exception {
		HttpURLConnection conn = null;
		InputStream in = null;
		FileOutputStream out = null;
		try {
			conn = openStreamConnection(targetUrl, referer, cookie);
			int code = conn.getResponseCode();
			if (code < 200 || code >= 300) {
				throw new Exception("HTTP " + code);
			}

			String contentType = conn.getContentType();
			if (contentType != null && contentType.toLowerCase(Locale.US).contains("text/html")) {
				throw new Exception("Respons berupa halaman HTML, bukan stream video");
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
					broadcastUpdate(appCtx, taskId, pct, "downloading", safeSlug, safeTitle, safeEp);
				}
			}
			out.flush();
		} finally {
			try { if (in != null) in.close(); } catch (Exception ignored) {}
			try { if (out != null) out.close(); } catch (Exception ignored) {}
			try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
		}
	}

	private void downloadHlsToFile(
		Context appCtx,
		String taskId,
		String safeSlug,
		String safeTitle,
		String safeEp,
		String playlistUrl,
		String referer,
		String cookie,
		File targetFile
	) throws Exception {
		String activePlaylist = playlistUrl;
		List<String> segments = parseM3u8Segments(activePlaylist, referer, cookie);

		if (segments.isEmpty()) {
			throw new Exception("Segmen HLS kosong atau terenkripsi/diblokir");
		}

		if (segments.size() == 1 && segments.get(0).toLowerCase(Locale.US).contains(".m3u8")) {
			activePlaylist = segments.get(0);
			segments = parseM3u8Segments(activePlaylist, referer, cookie);
		}

		if (segments.isEmpty()) {
			throw new Exception("Tidak ada segmen video yang bisa diunduh");
		}

		FileOutputStream out = null;
		long totalBytes = 0L;
		int totalSegs = segments.size();
		try {
			out = new FileOutputStream(targetFile);
			byte[] buf = new byte[16 * 1024];

			for (int i = 0; i < totalSegs; i++) {
				String segUrl = segments.get(i);
				HttpURLConnection sConn = null;
				InputStream sIn = null;
				try {
					sConn = openStreamConnection(segUrl, referer, cookie);
					int sCode = sConn.getResponseCode();
					if (sCode < 200 || sCode >= 300) continue;
					sIn = sConn.getInputStream();
					int n;
					while ((n = sIn.read(buf)) != -1) {
						out.write(buf, 0, n);
						totalBytes += n;
					}
				} catch (Exception ignored) {
				} finally {
					try { if (sIn != null) sIn.close(); } catch (Exception ignored) {}
					try { if (sConn != null) sConn.disconnect(); } catch (Exception ignored) {}
				}

				int pct = Math.min(99, (int) (((i + 1) * 100L) / totalSegs));
				updateTask(appCtx, taskId, totalBytes, 0L, pct, "downloading");
				postSystemNotification(appCtx, taskId.hashCode(), safeTitle + " • " + safeEp, pct, false);
				broadcastUpdate(appCtx, taskId, pct, "downloading", safeSlug, safeTitle, safeEp);
			}
			out.flush();
		} finally {
			try { if (out != null) out.close(); } catch (Exception ignored) {}
		}
	}

	private List<String> parseM3u8Segments(String m3u8Url, String referer, String cookie) throws Exception {
		List<String> list = new ArrayList<>();
		HttpURLConnection conn = null;
		BufferedReader reader = null;
		try {
			conn = openStreamConnection(m3u8Url, referer, cookie);
			int code = conn.getResponseCode();
			if (code < 200 || code >= 300) {
				throw new Exception("HTTP " + code);
			}
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line;
			String lastVariantOrSegment = "";
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (trimmed.isEmpty()) continue;
				if (trimmed.startsWith("#")) continue;
				String resolved = resolveUrl(m3u8Url, trimmed);
				if (resolved.toLowerCase(Locale.US).contains(".m3u8")) {
					lastVariantOrSegment = resolved;
				} else {
					list.add(resolved);
				}
			}
			if (list.isEmpty() && !lastVariantOrSegment.isEmpty()) {
				list.add(lastVariantOrSegment);
			}
			return list;
		} finally {
			try { if (reader != null) reader.close(); } catch (Exception ignored) {}
			try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
		}
	}

	private String resolveUrl(String base, String rel) {
		if (rel.startsWith("http://") || rel.startsWith("https://")) return rel;
		if (rel.startsWith("//")) return "https:" + rel;
		try {
			URL baseUrl = new URL(base);
			return new URL(baseUrl, rel).toString();
		} catch (Exception e) {
			return rel;
		}
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

	private void broadcastUpdate(Context ctx, String taskId, int pct, String status, String slug, String title, String ep) {
		Intent i = new Intent(ACTION_DOWNLOAD_PROGRESS);
		i.setPackage(ctx.getPackageName());
		i.putExtra("task_id", taskId);
		i.putExtra("progress", pct);
		i.putExtra("status", status);
		i.putExtra("slug", slug);
		i.putExtra("title", title);
		i.putExtra("episode", ep);
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
