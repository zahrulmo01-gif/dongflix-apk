package com.dongflix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ShimmerHelper {

	private ValueAnimator animator;
	private float phase = 0f;
	private final List<View> trackedViews = new ArrayList<>();

	public Drawable createDrawable(final float cornerRadiusPx) {
		return new Drawable() {
			private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			private final Matrix shaderMatrix = new Matrix();
			private LinearGradient gradient;
			private int lastWidth = -1;

			{
				basePaint.setColor(Color.parseColor("#12141A"));
			}

			@Override
			public void draw(Canvas canvas) {
				Rect b = getBounds();
				int w = b.width();
				int h = b.height();
				if (w <= 0 || h <= 0) return;

				RectF rect = new RectF(b);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint);

				if (gradient == null || lastWidth != w) {
					lastWidth = w;
					gradient = new LinearGradient(
						0, 0, w, 0,
						new int[]{
							Color.TRANSPARENT,
							Color.parseColor("#1AFFFFFF"),
							Color.parseColor("#33FFFFFF"),
							Color.parseColor("#1AFFFFFF"),
							Color.TRANSPARENT
						},
						new float[]{0f, 0.35f, 0.5f, 0.65f, 1f},
						Shader.TileMode.CLAMP
					);
					shimmerPaint.setShader(gradient);
				}

				float translateX = -w + (2f * w * phase);
				shaderMatrix.setTranslate(translateX, 0);
				gradient.setLocalMatrix(shaderMatrix);
				canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint);
			}

			@Override public void setAlpha(int alpha) {}
			@Override public void setColorFilter(ColorFilter colorFilter) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	public void applyToView(View v, float cornerRadiusDp) {
		if (v == null) return;
		float d = v.getResources().getDisplayMetrics().density;
		v.setBackground(createDrawable(cornerRadiusDp * d));
		if (!trackedViews.contains(v)) {
			trackedViews.add(v);
		}
	}

	public void start() {
		if (animator != null && animator.isRunning()) return;
		animator = ValueAnimator.ofFloat(0f, 1f);
		animator.setDuration(1250);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setInterpolator(new LinearInterpolator());
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				phase = (float) animation.getAnimatedValue();
				for (View v : trackedViews) {
					if (v != null) {
						v.invalidate();
						if (v instanceof ViewGroup) {
							invalidateChildren((ViewGroup) v);
						}
					}
				}
			}
		});
		animator.start();
	}

	private void invalidateChildren(ViewGroup vg) {
		for (int i = 0; i < vg.getChildCount(); i++) {
			View c = vg.getChildAt(i);
			c.invalidate();
			if (c instanceof ViewGroup) {
				invalidateChildren((ViewGroup) c);
			}
		}
	}

	public void stop() {
		if (animator != null) {
			animator.cancel();
			animator = null;
		}
		for (View v : trackedViews) {
			if (v != null) v.setBackground(null);
		}
		trackedViews.clear();
	}

	/**
	 * Adapter Grid 3 Kolom Berkilau untuk MainActivity
	 */
	public RecyclerView.Adapter<?> createGridAdapter(int itemCount) {
		return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
				Context ctx = parent.getContext();
				float d = ctx.getResources().getDisplayMetrics().density;

				LinearLayout box = new LinearLayout(ctx);
				box.setOrientation(LinearLayout.VERTICAL);
				GridLayoutManager.LayoutParams lp = new GridLayoutManager.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				);
				lp.setMargins((int)(4 * d), 0, (int)(4 * d), (int)(14 * d));
				box.setLayoutParams(lp);

				View coverStub = new View(ctx);
				coverStub.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(150 * d)));
				coverStub.setBackground(createDrawable(10 * d));
				box.addView(coverStub);

				View titleStub = new View(ctx);
				LinearLayout.LayoutParams titLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(14 * d));
				titLp.topMargin = (int)(8 * d);
				titleStub.setLayoutParams(titLp);
				titleStub.setBackground(createDrawable(5 * d));
				box.addView(titleStub);

				if (!trackedViews.contains(box)) trackedViews.add(box);
				return new RecyclerView.ViewHolder(box) {};
			}

			@Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
			@Override public int getItemCount() { return itemCount; }
		};
	}

	/**
	 * Adapter Horizontal Berkilau untuk MainActivity (Slider / Donghua Populer / Jadwal)
	 */
	public RecyclerView.Adapter<?> createHorizontalAdapter(int itemCount, final int widthDp, final int heightDp) {
		return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
				Context ctx = parent.getContext();
				float d = ctx.getResources().getDisplayMetrics().density;

				View cardStub = new View(ctx);
				RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams((int)(widthDp * d), (int)(heightDp * d));
				lp.setMarginEnd((int)(10 * d));
				cardStub.setLayoutParams(lp);
				cardStub.setBackground(createDrawable(12 * d));

				if (!trackedViews.contains(cardStub)) trackedViews.add(cardStub);
				return new RecyclerView.ViewHolder(cardStub) {};
			}

			@Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
			@Override public int getItemCount() { return itemCount; }
		};
	}
}
