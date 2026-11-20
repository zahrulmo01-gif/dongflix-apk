package com.dongflix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		float d = getResources().getDisplayMetrics().density;
		SharedPreferences prefs = getSharedPreferences(DongflixApp.PREFS_NAME, MODE_PRIVATE);
		String logFromIntent = getIntent() != null ? getIntent().getStringExtra("crash_log") : null;
		final String crashLog = (logFromIntent != null && !logFromIntent.trim().isEmpty())
			? logFromIntent
			: prefs.getString(DongflixApp.KEY_CRASH_LOG, "Tidak ada log error tersimpan.");

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setBackgroundColor(Color.parseColor("#090B10"));
		int pad = (int) (16 * d);
		root.setPadding(pad, (int) (36 * d), pad, (int) (24 * d));
		root.setLayoutParams(new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));

		TextView title = new TextView(this);
		title.setText("Terjadi Error pada Aplikasi");
		title.setTextColor(Color.WHITE);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		root.addView(title);

		TextView subtitle = new TextView(this);
		subtitle.setText("Salin detail error di bawah lalu kirimkan untuk diperbaiki.");
		subtitle.setTextColor(Color.parseColor("#94A3B8"));
		subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
		LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		subLp.topMargin = (int) (4 * d);
		subLp.bottomMargin = (int) (12 * d);
		subtitle.setLayoutParams(subLp);
		root.addView(subtitle);

		ScrollView scroll = new ScrollView(this);
		LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
		);
		scroll.setLayoutParams(scrollLp);

		GradientDrawable boxBg = new GradientDrawable();
		boxBg.setColor(Color.parseColor("#121622"));
		boxBg.setCornerRadius(12 * d);
		boxBg.setStroke((int) (1 * d), Color.parseColor("#26FFFFFF"));
		scroll.setBackground(boxBg);
		scroll.setPadding((int) (12 * d), (int) (12 * d), (int) (12 * d), (int) (12 * d));

		TextView logView = new TextView(this);
		logView.setText(crashLog);
		logView.setTextColor(Color.parseColor("#F8FAFC"));
		logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		logView.setTextIsSelectable(true);
		scroll.addView(logView);
		root.addView(scroll);

		LinearLayout actionRow = new LinearLayout(this);
		actionRow.setOrientation(LinearLayout.HORIZONTAL);
		actionRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
		);
		rowLp.topMargin = (int) (14 * d);
		actionRow.setLayoutParams(rowLp);

		Button btnCopy = new Button(this);
		btnCopy.setText("Salin Error");
		btnCopy.setAllCaps(false);
		btnCopy.setTextColor(Color.BLACK);
		GradientDrawable copyBg = new GradientDrawable();
		copyBg.setColor(Color.WHITE);
		copyBg.setCornerRadius(100f);
		btnCopy.setBackground(copyBg);
		LinearLayout.LayoutParams copyLp = new LinearLayout.LayoutParams(0, (int) (46 * d), 1f);
		copyLp.rightMargin = (int) (8 * d);
		btnCopy.setLayoutParams(copyLp);
		btnCopy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				if (cm != null) {
					ClipData clip = ClipData.newPlainText("Dongflix Error Log", crashLog);
					cm.setPrimaryClip(clip);
					Toast.makeText(CrashActivity.this, "Error disalin ke clipboard", Toast.LENGTH_SHORT).show();
				}
			}
		});

		Button btnRestart = new Button(this);
		btnRestart.setText("Buka Ulang");
		btnRestart.setAllCaps(false);
		btnRestart.setTextColor(Color.WHITE);
		GradientDrawable restartBg = new GradientDrawable();
		restartBg.setColor(Color.parseColor("#1E293B"));
		restartBg.setCornerRadius(100f);
		btnRestart.setBackground(restartBg);
		btnRestart.setLayoutParams(new LinearLayout.LayoutParams(0, (int) (46 * d), 1f));
		btnRestart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent mainIntent = new Intent(CrashActivity.this, MainActivity.class);
				mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(mainIntent);
				finish();
			}
		});

		actionRow.addView(btnCopy);
		actionRow.addView(btnRestart);
		root.addView(actionRow);

		setContentView(root);
	}
}
