package com.dongflix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DongflixToast {

	private static final Handler mainHandler = new Handler(Looper.getMainLooper());
	private static android.app.Dialog activeDialog;
	private static Runnable dismissRunnable;

	public static void show(Context context, String message) {
		show(context, message, 2200L);
	}

	public static void show(final Context context, final String message, final long durationMs) {
		if (context == null || message == null || message.trim().isEmpty()) return;

		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (context instanceof Activity) {
					Activity act = (Activity) context;
					if (act.isFinishing() || act.isDestroyed()) return;
				}

				try {
					dismissCurrent();
					float d = context.getResources().getDisplayMetrics().density;

					final android.app.Dialog dialog = new android.app.Dialog(context);
					dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
					dialog.setCancelable(true);
					dialog.setCanceledOnTouchOutside(false);

					LinearLayout pill = new LinearLayout(context);
					pill.setOrientation(LinearLayout.HORIZONTAL);
					pill.setGravity(Gravity.CENTER_VERTICAL);
					pill.setPadding((int) (16 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
					LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
					);
					pillLp.leftMargin = (int) (16 * d);
					pillLp.rightMargin = (int) (16 * d);
					pill.setLayoutParams(pillLp);

					GradientDrawable bg = new GradientDrawable();
					bg.setColor(Color.parseColor("#24262E"));
					bg.setCornerRadius(100f);
					pill.setBackground(bg);

					TextView tv = new TextView(context);
					tv.setText(message.trim());
					tv.setTextColor(Color.parseColor("#F1F5F9"));
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
					tv.setMaxLines(3);
					LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
					);
					tvLp.rightMargin = (int) (10 * d);
					tv.setLayoutParams(tvLp);

					FrameLayout closeBtn = new FrameLayout(context);
					int btnPx = (int) (24 * d);
					closeBtn.setLayoutParams(new LinearLayout.LayoutParams(btnPx, btnPx));
					GradientDrawable btnBg = new GradientDrawable();
					btnBg.setShape(GradientDrawable.OVAL);
					btnBg.setColor(Color.parseColor("#343742"));
					closeBtn.setBackground(btnBg);

					ImageView closeIcon = new ImageView(context);
					int iconSize = (int) (14 * d);
					closeIcon.setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
					closeIcon.setImageDrawable(createCloseCrossDrawable(Color.parseColor("#E2E8F0")));
					closeBtn.addView(closeIcon);

					closeBtn.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dismissCurrent();
						}
					});

					pill.addView(tv);
					pill.addView(closeBtn);
					dialog.setContentView(pill);

					Window win = dialog.getWindow();
					if (win != null) {
						win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
						win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
						win.addFlags(
							WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
								| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
								| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
						);
						win.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
						WindowManager.LayoutParams wlp = win.getAttributes();
						wlp.y = (int) (84 * d);
						wlp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
						wlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
						win.setAttributes(wlp);
					}

					activeDialog = dialog;
					dialog.show();

					long delay = durationMs > 0L ? durationMs : 2200L;
					dismissRunnable = new Runnable() {
						@Override
						public void run() {
							if (activeDialog == dialog) {
								dismissCurrent();
							}
						}
					};
					mainHandler.postDelayed(dismissRunnable, delay);
				} catch (Throwable ignored) {}
			}
		});
	}

	public static void dismissCurrent() {
		if (dismissRunnable != null) {
			mainHandler.removeCallbacks(dismissRunnable);
			dismissRunnable = null;
		}
		if (activeDialog != null) {
			try {
				if (activeDialog.isShowing()) {
					activeDialog.dismiss();
				}
			} catch (Throwable ignored) {}
			activeDialog = null;
		}
	}

	private static Drawable createCloseCrossDrawable(final int color) {
		return new Drawable() {
			private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

			{
				paint.setColor(color);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeCap(Paint.Cap.ROUND);
			}

			@Override
			public void draw(Canvas canvas) {
				android.graphics.Rect b = getBounds();
				float w = b.width();
				float h = b.height();
				if (w <= 0 || h <= 0) return;

				paint.setStrokeWidth(Math.max(2f, w * 0.12f));
				float pad = w * 0.24f;
				canvas.drawLine(pad, pad, w - pad, h - pad, paint);
				canvas.drawLine(w - pad, pad, pad, h - pad, paint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}
}
