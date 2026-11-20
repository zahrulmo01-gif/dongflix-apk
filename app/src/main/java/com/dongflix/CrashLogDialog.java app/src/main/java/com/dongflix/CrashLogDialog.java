package com.dongflix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CrashLogDialog {

	public static void showIfAvailable(final Activity activity) {
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

		SharedPreferences prefs = activity.getSharedPreferences("dongflix_crash_prefs", Context.MODE_PRIVATE);
		final String log = prefs.getString("last_crash_log", "");
		if (log == null || log.trim().isEmpty()) return;

		ScrollView scroll = new ScrollView(activity);
		int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
		scroll.setPadding(pad, pad, pad, pad);

		TextView tv = new TextView(activity);
		tv.setText(log);
		tv.setTextColor(Color.parseColor("#F8FAFC"));
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
		tv.setTextIsSelectable(true);
		scroll.addView(tv);

		new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
			.setTitle("Log Crash Terakhir")
			.setView(scroll)
			.setCancelable(false)
			.setPositiveButton("Salin Error", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
					if (cm != null) {
						ClipData clip = ClipData.newPlainText("Dongflix Crash Log", log);
						cm.setPrimaryClip(clip);
						Toast.makeText(activity, "Log error disalin ke clipboard", Toast.LENGTH_SHORT).show();
					}
					prefs.edit().remove("last_crash_log").apply();
				}
			})
			.setNegativeButton("Hapus Log", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					prefs.edit().remove("last_crash_log").apply();
				}
			})
			.show();
	}
}
