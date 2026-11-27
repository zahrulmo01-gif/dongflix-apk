package com.dongflix;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class UpdateCheckerDialog {

	public static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/zahrulmo01-gif/dongflixapk/refs/heads/main/update-apk.json";
	private static boolean isSheetShowing = false;
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	public static void checkForUpdate(final Activity activity) {
		checkForUpdate(activity, false);
	}

	public static void checkForUpdate(final Activity activity, final boolean notifyIfUpToDate) {
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection conn = null;
				try {
					URL url = new URL(UPDATE_JSON_URL);
					conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(8000);
					conn.setReadTimeout(8000);
					conn.setUseCaches(false);
					conn.setRequestProperty("Accept", "application/json");

					if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
						showToastOnUi(activity, notifyIfUpToDate, "Gagal memeriksa pembaruan");
						return;
					}

					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line);
					}
					br.close();

					String raw = sb.toString().trim();
					if (raw.isEmpty()) return;

					JSONObject obj;
					if (raw.startsWith("[")) {
						JSONArray arr = new JSONArray(raw);
						if (arr.length() == 0) return;
						obj = arr.getJSONObject(0);
					} else {
						obj = new JSONObject(raw);
					}

					final String remoteVersion = obj.optString("version", "").trim();
					final String updateNotes = obj.optString("update", "Perbaikan bug dan peningkatan performa.").trim();
					final String closedl = obj.optString("closedl", "false").trim();
					final String linkup = obj.optString("linkup", "dongflix.my.id").trim();
					final String localVersion = getAppVersionName(activity);

					if (remoteVersion.isEmpty()) return;

					if (compareVersions(remoteVersion, localVersion) > 0) {
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (!activity.isFinishing() && !activity.isDestroyed() && !isSheetShowing) {
									showBottomSheet(activity, localVersion, remoteVersion, updateNotes, closedl, linkup);
								}
							}
						});
					} else {
						showToastOnUi(activity, notifyIfUpToDate, "Aplikasi sudah versi terbaru (v" + localVersion + ")");
					}
				} catch (Throwable ignored) {
					showToastOnUi(activity, notifyIfUpToDate, "Tidak dapat memeriksa pembaruan");
				} finally {
					if (conn != null) {
						try { conn.disconnect(); } catch (Throwable ignored) {}
					}
				}
			}
		}).start();
	}

	private static void showToastOnUi(final Activity activity, final boolean shouldShow, final String msg) {
		if (!shouldShow || activity == null) return;
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (!activity.isFinishing() && !activity.isDestroyed()) {
					Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	public static String getAppVersionName(Activity activity) {
		try {
			PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
			if (pInfo != null && pInfo.versionName != null && !pInfo.versionName.trim().isEmpty()) {
				return pInfo.versionName.trim();
			}
		} catch (Throwable ignored) {}
		return "1.0";
	}

	public static int compareVersions(String remoteVer, String localVer) {
		String[] rParts = remoteVer.replaceAll("[^0-9.]", "").split("\\.");
		String[] lParts = localVer.replaceAll("[^0-9.]", "").split("\\.");
		int len = Math.max(rParts.length, lParts.length);
		for (int i = 0; i < len; i++) {
			long r = 0L;
			long l = 0L;
			if (i < rParts.length && !rParts[i].isEmpty()) {
				try { r = Long.parseLong(rParts[i]); } catch (Throwable ignored) {}
			}
			if (i < lParts.length && !lParts[i].isEmpty()) {
				try { l = Long.parseLong(lParts[i]); } catch (Throwable ignored) {}
			}
			if (r > l) return 1;
			if (r < l) return -1;
		}
		return 0;
	}

	private static Typeface loadFont(Activity activity) {
		String[] paths = {
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/productsans_reg.ttf"
		};
		for (String p : paths) {
			try {
				Typeface tf = Typeface.createFromAsset(activity.getAssets(), p);
				if (tf != null) return tf;
			} catch (Throwable ignored) {}
		}
		return Typeface.DEFAULT;
	}

	private static List<String> splitNotesByDot(String rawNotes) {
		List<String> out = new ArrayList<>();
		if (rawNotes == null || rawNotes.trim().isEmpty()) return out;

		String[] lines = rawNotes.split("\\n+");
		for (String line : lines) {
			String trimmedLine = line.trim();
			if (trimmedLine.isEmpty()) continue;

			// Pecah berdasarkan tanda titik (.) dengan menjaga versi angka (misal 1.0.3) dan domain/URL
			StringBuilder current = new StringBuilder();
			for (int i = 0; i < trimmedLine.length(); i++) {
				char ch = trimmedLine.charAt(i);
				if (ch == '.') {
					char prev = i > 0 ? trimmedLine.charAt(i - 1) : 0;
					char next = i + 1 < trimmedLine.length() ? trimmedLine.charAt(i + 1) : 0;
					boolean isDecimalOrVersion = Character.isDigit(prev) && Character.isDigit(next);
					boolean isDomainOrUrl = Character.isLetterOrDigit(prev) && Character.isLetter(next);
					if (isDecimalOrVersion || isDomainOrUrl) {
						current.append(ch);
					} else {
						String part = current.toString().trim();
						if (!part.isEmpty()) out.add(part);
						current.setLength(0);
					}
				} else {
					current.append(ch);
				}
			}
			String tail = current.toString().trim();
			if (!tail.isEmpty()) out.add(tail);
		}
		return out;
	}

	public static void showBottomSheet(
		final Activity activity,
		String currentVer,
		String remoteVer,
		String updateNotes,
		String closedl,
		final String rawLink
	) {
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

		try {
			isSheetShowing = true;
			final boolean forceUpdate = "true".equalsIgnoreCase(closedl) || "1".equals(closedl);
			final float d = activity.getResources().getDisplayMetrics().density;
			final Typeface tf = loadFont(activity);

			final Dialog sheetDialog = new Dialog(activity);
			sheetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			sheetDialog.setCancelable(!forceUpdate);
			sheetDialog.setCanceledOnTouchOutside(!forceUpdate);
			sheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dInt) {
					isSheetShowing = false;
				}
			});

			LinearLayout sheet = new LinearLayout(activity);
			sheet.setOrientation(LinearLayout.VERTICAL);
			sheet.setPadding((int) (20 * d), (int) (10 * d), (int) (20 * d), (int) (24 * d));
			sheet.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			));

			GradientDrawable sheetBg = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#121826"), Color.parseColor("#07090F")}
			);
			float topRadius = 28 * d;
			sheetBg.setCornerRadii(new float[]{
				topRadius, topRadius,
				topRadius, topRadius,
				0f, 0f,
				0f, 0f
			});
			sheet.setBackground(sheetBg);

			// Drag handle pill
			View handle = new View(activity);
			LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams((int) (40 * d), (int) (4 * d));
			hLp.gravity = Gravity.CENTER_HORIZONTAL;
			hLp.topMargin = (int) (2 * d);
			hLp.bottomMargin = (int) (14 * d);
			handle.setLayoutParams(hLp);
			GradientDrawable handleBg = new GradientDrawable();
			handleBg.setColor(Color.parseColor("#475569"));
			handleBg.setCornerRadius(100f);
			handle.setBackground(handleBg);
			sheet.addView(handle);

			// Badge versi
			LinearLayout badgeRow = new LinearLayout(activity);
			badgeRow.setOrientation(LinearLayout.HORIZONTAL);
			badgeRow.setGravity(Gravity.CENTER_VERTICAL);
			badgeRow.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			));

			TextView tvPill = new TextView(activity);
			tvPill.setText(forceUpdate ? "✦ PEMBARUAN WAJIB" : "✦ PEMBARUAN TERSEDIA");
			tvPill.setTextColor(Color.parseColor("#38BDF8"));
			tvPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
			tvPill.setTypeface(tf, Typeface.NORMAL);
			tvPill.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
			GradientDrawable pillBg = new GradientDrawable();
			pillBg.setColor(Color.parseColor("#1A38BDF8"));
			pillBg.setCornerRadius(100f);
			tvPill.setBackground(pillBg);

			TextView tvVersionChip = new TextView(activity);
			tvVersionChip.setText("v" + currentVer + " → v" + remoteVer);
			tvVersionChip.setTextColor(Color.parseColor("#CBD5E1"));
			tvVersionChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
			tvVersionChip.setTypeface(tf, Typeface.NORMAL);
			tvVersionChip.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
			LinearLayout.LayoutParams vcChipLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			vcChipLp.leftMargin = (int) (8 * d);
			tvVersionChip.setLayoutParams(vcChipLp);
			GradientDrawable chipBg = new GradientDrawable();
			chipBg.setColor(Color.parseColor("#1C2538"));
			chipBg.setCornerRadius(100f);
			tvVersionChip.setBackground(chipBg);

			badgeRow.addView(tvPill);
			badgeRow.addView(tvVersionChip);
			sheet.addView(badgeRow);

			// Judul
			TextView tvTitle = new TextView(activity);
			tvTitle.setText("Dongflix v" + remoteVer + " Telah Hadir");
			tvTitle.setTextColor(Color.WHITE);
			tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.5f);
			tvTitle.setTypeface(tf, Typeface.NORMAL);
			LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			titleLp.topMargin = (int) (12 * d);
			tvTitle.setLayoutParams(titleLp);
			sheet.addView(tvTitle);

			// Deskripsi
			TextView tvDesc = new TextView(activity);
			tvDesc.setText(forceUpdate
				? "Pembaruan ini diperlukan agar seluruh fitur pemutar dan katalog berjalan optimal."
				: "Nikmati peningkatan performa, stabilitas pemutar, dan fitur terbaru di versi ini.");
			tvDesc.setTextColor(Color.parseColor("#94A3B8"));
			tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvDesc.setTypeface(tf, Typeface.NORMAL);
			LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			descLp.topMargin = (int) (4 * d);
			tvDesc.setLayoutParams(descLp);
			sheet.addView(tvDesc);

			// Box catatan pembaruan (warna berbeda dari background sheet, tanpa stroke)
			LinearLayout notesBox = new LinearLayout(activity);
			notesBox.setOrientation(LinearLayout.VERTICAL);
			notesBox.setPadding((int) (14 * d), (int) (12 * d), (int) (14 * d), (int) (12 * d));
			LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			);
			boxLp.topMargin = (int) (14 * d);
			boxLp.bottomMargin = (int) (18 * d);
			notesBox.setLayoutParams(boxLp);
			GradientDrawable boxBg = new GradientDrawable();
			boxBg.setColor(Color.parseColor("#1D263B"));
			boxBg.setCornerRadius(18 * d);
			notesBox.setBackground(boxBg);

			TextView tvNotesTitle = new TextView(activity);
			tvNotesTitle.setText("Catatan Pembaruan");
			tvNotesTitle.setTextColor(Color.parseColor("#E2E8F0"));
			tvNotesTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
			tvNotesTitle.setTypeface(tf, Typeface.NORMAL);
			notesBox.addView(tvNotesTitle);

			ScrollView notesScroll = new ScrollView(activity);
			int maxNotesH = (int) (180 * d);
			LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, maxNotesH
			);
			scLp.topMargin = (int) (6 * d);
			notesScroll.setLayoutParams(scLp);
			notesScroll.setVerticalScrollBarEnabled(false);

			LinearLayout listCol = new LinearLayout(activity);
			listCol.setOrientation(LinearLayout.VERTICAL);

			List<String> items = splitNotesByDot(updateNotes);
			for (String clean : items) {
				TextView tvItem = new TextView(activity);
				tvItem.setText("•  " + clean);
				tvItem.setTextColor(Color.parseColor("#CBD5E1"));
				tvItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
				tvItem.setTypeface(tf, Typeface.NORMAL);
				tvItem.setLineSpacing(2 * d, 1.05f);
				LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
				);
				iLp.topMargin = (int) (6 * d);
				tvItem.setLayoutParams(iLp);
				listCol.addView(tvItem);
			}
			notesScroll.addView(listCol);
			notesBox.addView(notesScroll);
			sheet.addView(notesBox);

			// Baris tombol aksi
			LinearLayout btnRow = new LinearLayout(activity);
			btnRow.setOrientation(LinearLayout.HORIZONTAL);
			btnRow.setGravity(Gravity.CENTER_VERTICAL);
			btnRow.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			));

			if (!forceUpdate) {
				TextView btnLater = new TextView(activity);
				btnLater.setText("Nanti Saja");
				btnLater.setTextColor(Color.parseColor("#94A3B8"));
				btnLater.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
				btnLater.setTypeface(tf, Typeface.NORMAL);
				btnLater.setGravity(Gravity.CENTER);
				LinearLayout.LayoutParams laterLp = new LinearLayout.LayoutParams(0, (int) (46 * d), 0.85f);
				laterLp.rightMargin = (int) (10 * d);
				btnLater.setLayoutParams(laterLp);
				GradientDrawable laterBg = new GradientDrawable();
				laterBg.setColor(Color.parseColor("#182032"));
				laterBg.setCornerRadius(100f);
				btnLater.setBackground(laterBg);
				btnLater.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sheetDialog.dismiss();
					}
				});
				btnRow.addView(btnLater);
			}

			TextView btnUpdate = new TextView(activity);
			btnUpdate.setText("Unduh Pembaruan");
			btnUpdate.setTextColor(Color.WHITE);
			btnUpdate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
			btnUpdate.setTypeface(tf, Typeface.NORMAL);
			btnUpdate.setGravity(Gravity.CENTER);
			btnUpdate.setLayoutParams(new LinearLayout.LayoutParams(0, (int) (46 * d), 1.15f));
			GradientDrawable updBg = new GradientDrawable(
				GradientDrawable.Orientation.LEFT_RIGHT,
				new int[]{Color.parseColor("#2563EB"), Color.parseColor("#0284C7")}
			);
			updBg.setCornerRadius(100f);
			btnUpdate.setBackground(updBg);
			btnUpdate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (rawLink == null || rawLink.isEmpty()) {
						Toast.makeText(activity, "Tautan pembaruan belum tersedia", Toast.LENGTH_SHORT).show();
						return;
					}
					String url = (rawLink.startsWith("http://") || rawLink.startsWith("https://"))
						? rawLink : "https://" + rawLink;
					try {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						activity.startActivity(intent);
					} catch (Throwable e) {
						Toast.makeText(activity, "Gagal membuka tautan", Toast.LENGTH_SHORT).show();
					}
				}
			});
			btnRow.addView(btnUpdate);

			sheet.addView(btnRow);
			sheetDialog.setContentView(sheet);

			Window win = sheetDialog.getWindow();
			if (win != null) {
				win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
				win.setGravity(Gravity.BOTTOM);
				win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				win.setDimAmount(0.65f);
			}
			sheetDialog.show();
		} catch (Throwable ignored) {
			isSheetShowing = false;
		}
	}
}
