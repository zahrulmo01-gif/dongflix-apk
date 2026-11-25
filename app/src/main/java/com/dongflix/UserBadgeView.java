package com.dongflix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserBadgeView extends LinearLayout {

	private final TextView tvLevelBadge;
	private final TextView tvAdminBadge;
	private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Matrix shimmerMatrix = new Matrix();
	private final RectF shimmerRect = new RectF();
	private LinearGradient shimmerGradient;
	private int lastShimmerWidth = -1;
	private float shimmerPhase = 0f;
	private ValueAnimator shimmerAnimator;

	public UserBadgeView(Context context) {
		super(context);
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		setClipChildren(false);
		setClipToPadding(false);

		float d = context.getResources().getDisplayMetrics().density;

		tvLevelBadge = new TextView(context);
		tvLevelBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
		tvLevelBadge.setTypeface(null, Typeface.BOLD);
		tvLevelBadge.setMaxLines(1);
		tvLevelBadge.setPadding((int) (6.5f * d), (int) (1.5f * d), (int) (6.5f * d), (int) (1.5f * d));

		tvAdminBadge = new TextView(context);
		tvAdminBadge.setText("★ ADMIN");
		tvAdminBadge.setTextColor(Color.WHITE);
		tvAdminBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
		tvAdminBadge.setTypeface(null, Typeface.BOLD);
		tvAdminBadge.setMaxLines(1);
		tvAdminBadge.setPadding((int) (6.5f * d), (int) (1.5f * d), (int) (6.5f * d), (int) (1.5f * d));
		LayoutParams admLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		admLp.leftMargin = (int) (5 * d);
		tvAdminBadge.setLayoutParams(admLp);
		tvAdminBadge.setVisibility(GONE);

		addView(tvLevelBadge, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		addView(tvAdminBadge);
	}

	public static int computeLevelFromExp(long exp) {
		if (exp <= 0L) return 1;
		return (int) ((exp - 1L) / 1000L) + 1;
	}

	public void bindUserBadges(long exp, boolean isAdmin) {
		float d = getContext().getResources().getDisplayMetrics().density;

		if (isAdmin) {
			tvLevelBadge.setText("Lv999+");
			tvLevelBadge.setTextColor(Color.parseColor("#451A03"));
			GradientDrawable lvBg = new GradientDrawable();
			lvBg.setCornerRadius(100f);
			lvBg.setColor(Color.parseColor("#FBBF24"));
			tvLevelBadge.setBackground(lvBg);

			GradientDrawable admBg = new GradientDrawable();
			admBg.setCornerRadius(100f);
			admBg.setColor(Color.parseColor("#EA580C"));
			tvAdminBadge.setBackground(admBg);
			tvAdminBadge.setVisibility(VISIBLE);
		} else {
			int level = computeLevelFromExp(exp);
			tvLevelBadge.setText("Lv" + level);
			tvLevelBadge.setTextColor(Color.parseColor("#38BDF8"));
			GradientDrawable lvBg = new GradientDrawable();
			lvBg.setCornerRadius(100f);
			lvBg.setColor(Color.parseColor("#2638BDF8"));
			lvBg.setStroke(Math.max(1, (int) (0.8f * d)), Color.parseColor("#5538BDF8"));
			tvLevelBadge.setBackground(lvBg);
			tvAdminBadge.setVisibility(GONE);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		startShimmer();
	}

	@Override
	protected void onDetachedFromWindow() {
		stopShimmer();
		super.onDetachedFromWindow();
	}

	private void startShimmer() {
		if (shimmerAnimator != null && shimmerAnimator.isRunning()) return;
		shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
		shimmerAnimator.setDuration(1500L);
		shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
		shimmerAnimator.setInterpolator(new LinearInterpolator());
		shimmerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				shimmerPhase = (float) animation.getAnimatedValue();
				invalidate();
			}
		});
		shimmerAnimator.start();
	}

	private void stopShimmer() {
		if (shimmerAnimator != null) {
			shimmerAnimator.cancel();
			shimmerAnimator = null;
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) return;

		if (shimmerGradient == null || lastShimmerWidth != w) {
			lastShimmerWidth = w;
			shimmerGradient = new LinearGradient(
				0f, 0f, w, 0f,
				new int[]{
					Color.TRANSPARENT,
					Color.parseColor("#40FFFFFF"),
					Color.parseColor("#99FFFFFF"),
					Color.parseColor("#40FFFFFF"),
					Color.TRANSPARENT
				},
				new float[]{0f, 0.35f, 0.5f, 0.65f, 1f},
				Shader.TileMode.CLAMP
			);
			shimmerPaint.setShader(shimmerGradient);
		}

		float tx = -w + (2f * w * shimmerPhase);
		shimmerMatrix.setTranslate(tx, 0f);
		shimmerGradient.setLocalMatrix(shimmerMatrix);

		drawShimmerOnChild(canvas, tvLevelBadge);
		if (tvAdminBadge.getVisibility() == VISIBLE) {
			drawShimmerOnChild(canvas, tvAdminBadge);
		}
	}

	private void drawShimmerOnChild(Canvas canvas, TextView child) {
		if (child == null || child.getVisibility() != VISIBLE || child.getWidth() <= 0 || child.getHeight() <= 0) {
			return;
		}
		float left = child.getLeft();
		float top = child.getTop();
		float right = child.getRight();
		float bottom = child.getBottom();
		float radius = (bottom - top) / 2f;

		shimmerRect.set(left, top, right, bottom);
		canvas.drawRoundRect(shimmerRect, radius, radius, shimmerPaint);
	}
}
