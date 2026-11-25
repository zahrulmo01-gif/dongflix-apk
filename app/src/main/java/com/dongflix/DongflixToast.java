package com.dongflix;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DongflixToast {

	private static final Handler mainHandler = new Handler(Looper.getMainLooper());
	private static Toast activeToast;

	public static void show(Context context, String message) {
		show(context, message, Toast.LENGTH_SHORT);
	}

	public static void show(final Context context, final String message, final int duration) {
		if (context == null || message == null || message.trim().isEmpty()) return;

		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (context instanceof Activity) {
					Activity act = (Activity) context;
					if (act.isFinishing() || act.isDestroyed()) return;
				}

				try {
					Context appCtx = context.getApplicationContext();
					float d = context.getResources().getDisplayMetrics().density;

					if (activeToast != null) {
						try {
							activeToast.cancel();
						} catch (Throwable ignored) {}
					}

					LinearLayout pill = new LinearLayout(context);
					pill.setOrientation(LinearLayout.HORIZONTAL);
					pill.setGravity(Gravity.CENTER_VERTICAL);
					pill.setPadding((int) (16 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));

					// Kapsul abu gelap bulat tanpa stroke
					GradientDrawable bg = new GradientDrawable();
					bg.setColor(Color.parseColor("#24262E"));
					bg.setCornerRadius(100f);
					pill.setBackground(bg);

					TextView tv = new TextView(context);
					tv.setText(message.trim());
					tv.setTextColor(Color.parseColor("#F1F5F9"));
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
					LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
					);
					tvLp.rightMargin = (int) (10 * d);
					tv.setLayoutParams(tvLp);

					ImageView closeIcon = new ImageView(context);
					int iconSize = (int) (18 * d);
					closeIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
					closeIcon.setImageDrawable(createCloseCrossDrawable(Color.parseColor("#CBD5E1")));

					pill.addView(tv);
					pill.addView(closeIcon);

					Toast toast = new Toast(appCtx != null ? appCtx : context);
					toast.setDuration(duration == Toast.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, (int) (84 * d));
					toast.setView(pill);

					activeToast = toast;
					toast.show();
				} catch (Throwable ignored) {}
			}
		});
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

				paint.setStrokeWidth(Math.max(2f, w * 0.11f));
				float pad = w * 0.25f;
				canvas.drawLine(pad, pad, w - pad, h - pad, paint);
				canvas.drawLine(w - pad, pad, pad, h - pad, paint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}
}
