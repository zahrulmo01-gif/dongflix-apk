package com.dongflix;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.view.*;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;

/**
 * Helper "liquid glass" ala iOS 26 untuk BlurView.
 * Butuh: com.github.Dimezis:BlurView:2.0.x (ada RenderEffectBlur)
 *        androidx.dynamicanimation:dynamicanimation:1.0.0
 */
public final class LiquidGlass {

	private LiquidGlass() {}

	/* ------------------------------------------------------------------ */
	/*  BLUR                                                               */
	/* ------------------------------------------------------------------ */

	public static void setupBlur(Context ctx, View target, ViewGroup root, Window window, float radius) {
		if (!(target instanceof BlurView) || root == null) return;
		BlurView bv = (BlurView) target;

		// Android 12+ pakai RenderEffect (halus, tanpa kotak-kotak), sisanya RenderScript
		BlurAlgorithm algo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
			? new RenderEffectBlur()
			: new RenderScriptBlur(ctx);

		bv.setupWith(root, algo)
			.setFrameClearDrawable(window.getDecorView().getBackground())
			.setBlurRadius(radius);

		// overlay putih sangat tipis, BUKAN hitam pekat
		bv.setOverlayColor(Color.parseColor("#14FFFFFF"));

		bv.setOutlineProvider(new ViewOutlineProvider() {
			@Override
			public void getOutline(View v, Outline o) {
				o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getHeight() / 2f);
			}
		});
		bv.setClipToOutline(true);

		// saturasi: warna di balik kaca terlihat lebih hidup seperti iOS
		try {
			ColorMatrix cm = new ColorMatrix();
			cm.setSaturation(1.5f);
			Paint p = new Paint();
			p.setColorFilter(new ColorMatrixColorFilter(cm));
			bv.setLayerType(View.LAYER_TYPE_HARDWARE, p);
		} catch (Exception ignored) {}
	}

	/* ------------------------------------------------------------------ */
	/*  STYLE KACA (tint + rim light)                                      */
	/* ------------------------------------------------------------------ */

	public static void style(View blurTarget, View container, float d) {
		// Fallback kalau view bukan BlurView
		if (blurTarget != null && !(blurTarget instanceof BlurView)) {
			GradientDrawable glass = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#66202533"), Color.parseColor("#80101420")});
			glass.setCornerRadius(1000f);
			blurTarget.setBackground(glass);
		}
		if (container != null) {
			// Foreground = tint terang dari atas + rim light diagonal
			container.setForeground(new LayerDrawable(new Drawable[]{tint(), rim(d)}));
			// Elevasi 0: bayangan elevasi bikin kaca terlihat kotor/abu-abu
			container.setElevation(0f);
		}
	}

	/** Tint: atas lebih terang, bawah hampir bening (efek cahaya jatuh dari atas). */
	private static Drawable tint() {
		GradientDrawable g = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#26FFFFFF"), Color.parseColor("#0AFFFFFF")});
		g.setCornerRadius(1000f);
		return g;
	}

	/** Rim light: tepi kiri-atas dan kanan-bawah memantulkan cahaya, tengah bening. */
	public static Drawable rim(final float d) {
		return new Drawable() {
			private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
			{
				p.setStyle(Paint.Style.STROKE);
				p.setStrokeWidth(1.2f * d);
			}

			@Override
			public void draw(Canvas c) {
				RectF r = new RectF(getBounds());
				float half = p.getStrokeWidth() / 2f;
				r.inset(half, half);
				if (r.width() <= 0 || r.height() <= 0) return;
				p.setShader(new LinearGradient(
					r.left, r.top, r.right, r.bottom,
					new int[]{0xB3FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x80FFFFFF},
					new float[]{0f, 0.32f, 0.68f, 1f},
					Shader.TileMode.CLAMP));
				float rad = r.height() / 2f;
				c.drawRoundRect(r, rad, rad, p);
			}

			@Override public void setAlpha(int a) {}
			@Override public void setColorFilter(ColorFilter f) {}
			@Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
		};
	}

	/* ------------------------------------------------------------------ */
	/*  PILL (indikator tab yang bisa di-drag)                             */
	/* ------------------------------------------------------------------ */

	public static Drawable pill(float d) {
		GradientDrawable fill = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#47FFFFFF"), Color.parseColor("#1CFFFFFF")});
		fill.setCornerRadius(1000f);
		return new LayerDrawable(new Drawable[]{fill, rim(d)});
	}

	/** Saat disentuh: pill membesar seperti tetesan cair. */
	public static void press(View pill) {
		pill.animate().cancel();
		pill.animate().scaleX(1.14f).scaleY(1.10f)
			.setDuration(140).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
	}

	/** Saat digeser: melar sedikit sesuai jarak geser. */
	public static void stretch(View pill, float dxPx, float d) {
		float extra = Math.min(0.16f, Math.abs(dxPx) / (420f * d));
		pill.setScaleX(1.14f + extra);
		pill.setScaleY(1.10f - extra * 0.35f);
	}

	/** Saat dilepas: kembali ke ukuran normal dengan pegas kenyal. */
	public static void release(View pill) {
		pill.animate().cancel();
		spring(pill, DynamicAnimation.SCALE_X, 1f, 320f, 0.52f);
		spring(pill, DynamicAnimation.SCALE_Y, 1f, 320f, 0.52f);
	}

	/** Pindah ke tab dengan pegas. */
	public static void springX(View pill, float tx) {
		pill.animate().cancel();
		spring(pill, DynamicAnimation.TRANSLATION_X, tx, 380f, 0.62f);
	}

	private static void spring(View v, DynamicAnimation.ViewProperty prop, float target, float stiffness, float damping) {
		SpringAnimation anim = new SpringAnimation(v, prop, target);
		SpringForce force = new SpringForce(target);
		force.setStiffness(stiffness);
		force.setDampingRatio(damping);
		anim.setSpring(force);
		anim.start();
	}
}
