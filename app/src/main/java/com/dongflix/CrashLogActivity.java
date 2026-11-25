package com.dongflix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CrashLogActivity extends AppCompatActivity {

	public static final String EXTRA_CRASH_LOG = "extra_crash_log";

	private String crashLogText = "";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(Color.parseColor("#000000"));
			w.setNavigationBarColor(Color.parseColor("#000000"));
		}

		String rawLog = getIntent() != null ? getIntent().getStringExtra(EXTRA_CRASH_LOG) : null;
		if (rawLog == null || rawLog.trim().isEmpty()) {
			rawLog = "Tidak ada detail stack trace yang tersimpan.";
		}
		crashLogText = buildFormattedCrashReport(rawLog.trim());

		float d = getResources().getDisplayMetrics().density;

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.parseColor("#000000"));
		root.setPadding((int) (16 * d), (int) (20 * d), (int) (16 * d), (int) (20 * d));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));

		// Badge status
		TextView tvBadge = new TextView(this);
		tvBadge.setText("✦ APLIKASI MENGALAMI FORCE CLOSE");
		tvBadge.setTextColor(Color.parseColor("#F87171"));
		tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
		tvBadge.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
		LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		tvBadge.setLayoutParams(badgeLp);
		GradientDrawable badgeBg = new GradientDrawable();
		badgeBg.setColor(Color.parseColor("#24F87171"));
		badgeBg.setCornerRadius(100f);
		badgeBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#55F87171"));
		tvBadge.setBackground(badgeBg);
		root.addView(tvBadge);

		// Judul halaman
		TextView tvTitle = new TextView(this);
		tvTitle.setText("Laporan Error Aplikasi");
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f);
		LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		titleLp.topMargin = (int) (10 * d);
		tvTitle.setLayoutParams(titleLp);
		root.addView(tvTitle);

		TextView tvSub = new TextView(this);
		tvSub.setText("Salin log error di bawah untuk melacak penyebab crash.");
		tvSub.setTextColor(Color.parseColor("#94A3B8"));
		tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		subLp.topMargin = (int) (4 * d);
		subLp.bottomMargin = (int) (14 * d);
		tvSub.setLayoutParams(subLp);
		root.addView(tvSub);

		// Kotak log yang bisa di-scroll
		FrameLayout logBox = new FrameLayout(this);
		LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		);
		boxLp.bottomMargin = (int) (16 * d);
		logBox.setLayoutParams(boxLp);
		GradientDrawable boxBg = new GradientDrawable();
		boxBg.setColor(Color.parseColor("#0F172A"));
		boxBg.setCornerRadius(18 * d);
		boxBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#26FFFFFF"));
		logBox.setBackground(boxBg);

		ScrollView scrollView = new ScrollView(this);
		scrollView.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
		));
		scrollView.setPadding((int) (14 * d), (int) (14 * d), (int) (14 * d), (int) (14 * d));
		scrollView.setClipToPadding(false);

		TextView tvLog = new TextView(this);
		tvLog.setText(crashLogText);
		tvLog.setTextColor(Color.parseColor("#E2E8F0"));
		tvLog.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tvLog.setTypeface(Typeface.MONOSPACE);
		tvLog.setTextIsSelectable(true);
		tvLog.setLineSpacing(3 * d, 1.05f);
		tvLog.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		scrollView.addView(tvLog);
		logBox.addView(scrollView);
		root.addView(logBox);

		// Tombol Salin Log
		TextView btnCopy = new TextView(this);
		btnCopy.setText("Salin Log Error");
		btnCopy.setTextColor(Color.WHITE);
		btnCopy.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
		btnCopy.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, (int) (46 * d)
		);
		copyLp.bottomMargin = (int) (10 * d);
		btnCopy.setLayoutParams(copyLp);
		GradientDrawable copyBg = new GradientDrawable(
			GradientDrawable.Orientation.LEFT_RIGHT,
			new int[]{Color.parseColor("#2563EB"), Color.parseColor("#0284C7")}
		);
		copyBg.setCornerRadius(100f);
		btnCopy.setBackground(copyBg);
		btnCopy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				copyCrashLogToClipboard();
			}
		});
		root.addView(btnCopy);

		// Baris tombol Buka Ulang & Tutup
		LinearLayout bottomRow = new LinearLayout(this);
		bottomRow.setOrientation(LinearLayout.HORIZONTAL);
		bottomRow.setGravity(Gravity.CENTER_VERTICAL);
		bottomRow.setLayoutParams(new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		));

		TextView btnRestart = new TextView(this);
		btnRestart.setText("Buka Ulang");
		btnRestart.setTextColor(Color.WHITE);
		btnRestart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnRestart.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(0, (int) (44 * d), 1f);
		resLp.rightMargin = (int) (8 * d);
		btnRestart.setLayoutParams(resLp);
		GradientDrawable secBg = new GradientDrawable();
		secBg.setColor(Color.parseColor("#12141A"));
		secBg.setCornerRadius(100f);
		secBg.setStroke(Math.max(1, (int) (1 * d)), Color.parseColor("#26FFFFFF"));
		btnRestart.setBackground(secBg);
		btnRestart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				restartAppSafely();
			}
		});

		TextView btnClose = new TextView(this);
		btnClose.setText("Tutup");
		btnClose.setTextColor(Color.parseColor("#94A3B8"));
		btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
		btnClose.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams clsLp = new LinearLayout.LayoutParams(0, (int) (44 * d), 0.75f);
		btnClose.setLayoutParams(clsLp);
		btnClose.setBackground(secBg.getConstantState() != null ? secBg.getConstantState().newDrawable() : secBg);
		btnClose.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finishAffinity();
			}
		});

		bottomRow.addView(btnRestart);
		bottomRow.addView(btnClose);
		root.addView(bottomRow);

		setContentView(root);
	}

	private String buildFormattedCrashReport(String stackTrace) {
		String verName = "unknown";
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (pi != null && pi.versionName != null) verName = pi.versionName;
		} catch (Throwable ignored) {}

		return "Package : " + getPackageName() + "\n"
			+ "Version : " + verName + "\n"
			+ "Device  : " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
			+ "Android : " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n"
			+ "----------------------------------------\n"
			+ stackTrace;
	}

	private void copyCrashLogToClipboard() {
		try {
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (cm != null) {
				ClipData clip = ClipData.newPlainText("Dongflix Crash Log", crashLogText);
				cm.setPrimaryClip(clip);
				Toast.makeText(this, "Log error berhasil disalin", Toast.LENGTH_SHORT).show();
			}
		} catch (Throwable e) {
			Toast.makeText(this, "Gagal menyalin log", Toast.LENGTH_SHORT).show();
		}
	}

	private void restartAppSafely() {
		try {
			Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
			if (launch != null) {
				launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(launch);
			}
		} catch (Throwable ignored) {}
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}
