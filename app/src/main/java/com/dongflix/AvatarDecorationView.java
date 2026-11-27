package com.dongflix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.Locale;

public class AvatarDecorationView extends FrameLayout {

	private final ImageView ivAvatar;
	private final TextView tvInitial;
	private final ImageView ivGifFrame;
	private final View onlineDot;
	private final int avatarSizePx;
	private final int frameSizePx;

	public AvatarDecorationView(Context context, int avatarSizeDp, boolean showOnlineDot) {
		super(context);
		float d = context.getResources().getDisplayMetrics().density;
		this.avatarSizePx = Math.max(1, Math.round(avatarSizeDp * d));
		// Proporsi 1.40f memberi ruang bingkai yang lebih lega di sekeliling lingkaran avatar
		this.frameSizePx = Math.round(this.avatarSizePx * 1.40f);

		setClipChildren(false);
		setClipToPadding(false);
		setLayoutParams(new FrameLayout.LayoutParams(this.frameSizePx, this.frameSizePx));

		ivAvatar = new ImageView(context);
		ivAvatar.setLayoutParams(new FrameLayout.LayoutParams(
			this.avatarSizePx, this.avatarSizePx, Gravity.CENTER
		));
		ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
		addView(ivAvatar);

		tvInitial = new TextView(context);
		tvInitial.setLayoutParams(new FrameLayout.LayoutParams(
			this.avatarSizePx, this.avatarSizePx, Gravity.CENTER
		));
		tvInitial.setGravity(Gravity.CENTER);
		tvInitial.setTextColor(Color.WHITE);
		tvInitial.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.avatarSizePx * 0.36f);
		tvInitial.setTypeface(Typeface.DEFAULT_BOLD);
		addView(tvInitial);

		ivGifFrame = new ImageView(context);
		ivGifFrame.setLayoutParams(new FrameLayout.LayoutParams(
			this.frameSizePx, this.frameSizePx, Gravity.CENTER
		));
		ivGifFrame.setScaleType(ImageView.ScaleType.FIT_CENTER);
		ivGifFrame.setVisibility(View.GONE);
		addView(ivGifFrame);

		onlineDot = new View(context);
		int dotPx = Math.max(8, Math.round(this.avatarSizePx * 0.17f));
		FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotPx, dotPx);
		dotLp.gravity = Gravity.BOTTOM | Gravity.END;
		int offset = Math.max(2, Math.round((this.frameSizePx - this.avatarSizePx) * 0.5f));
		dotLp.setMargins(0, 0, offset, offset);
		onlineDot.setLayoutParams(dotLp);

		GradientDrawable dotBg = new GradientDrawable();
		dotBg.setShape(GradientDrawable.OVAL);
		dotBg.setColor(Color.parseColor("#22C55E"));
		dotBg.setStroke(Math.max(2, Math.round(2 * d)), Color.parseColor("#181C28"));
		onlineDot.setBackground(dotBg);
		onlineDot.setVisibility(showOnlineDot ? View.VISIBLE : View.GONE);
		addView(onlineDot);
	}

	public void bind(String avatarSrc, String displayName, String gifFrameUrl) {
		bind(avatarSrc, displayName, gifFrameUrl, 1.0f, 0f, 0f);
	}

	public void bind(
		String avatarSrc,
		String displayName,
		String gifFrameUrl,
		float frameScale,
		float offsetXDp,
		float offsetYDdp
	) {
		String initial = (displayName != null && !displayName.trim().isEmpty())
			? displayName.trim().substring(0, 1).toUpperCase(Locale.US) : "R";

		if (avatarSrc != null && !avatarSrc.trim().isEmpty()) {
			String clean = avatarSrc.trim();
			ivAvatar.setVisibility(View.VISIBLE);
			tvInitial.setVisibility(View.GONE);

			if (clean.startsWith("data:image")) {
				try {
					String b64 = clean.substring(clean.indexOf(",") + 1);
					byte[] raw = Base64.decode(b64, Base64.DEFAULT);
					Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
					if (bmp != null) {
						ivAvatar.setImageBitmap(makeCircleBitmap(bmp, avatarSizePx));
					}
				} catch (Exception ignored) {}
			} else {
				Glide.with(getContext().getApplicationContext())
					.load(Uri.parse(clean))
					.circleCrop()
					.placeholder(R.drawable.ic_profile)
					.error(R.drawable.ic_profile)
					.into(ivAvatar);
			}
		} else {
			ivAvatar.setVisibility(View.GONE);
			tvInitial.setVisibility(View.VISIBLE);
			tvInitial.setText(initial);
			GradientDrawable bg = new GradientDrawable();
			bg.setShape(GradientDrawable.OVAL);
			bg.setColor(Color.parseColor("#1D4ED8"));
			tvInitial.setBackground(bg);
		}

		float d = getResources().getDisplayMetrics().density;
		float safeScale = frameScale > 0f ? frameScale : 1.0f;
		ivGifFrame.setScaleX(safeScale);
		ivGifFrame.setScaleY(safeScale);
		ivGifFrame.setTranslationX(offsetXDp * d);
		ivGifFrame.setTranslationY(offsetYDdp * d);

		if (gifFrameUrl != null && !gifFrameUrl.trim().isEmpty()) {
			ivGifFrame.setVisibility(View.VISIBLE);
			Glide.with(getContext().getApplicationContext())
				.load(Uri.parse(gifFrameUrl.trim()))
				.into(ivGifFrame);
		} else {
			ivGifFrame.setVisibility(View.GONE);
			ivGifFrame.setImageDrawable(null);
		}
	}

	private Bitmap makeCircleBitmap(Bitmap src, int sizePx) {
		int edge = Math.min(src.getWidth(), src.getHeight());
		Bitmap sq = Bitmap.createBitmap(
			src, (src.getWidth() - edge) / 2, (src.getHeight() - edge) / 2, edge, edge
		);
		Bitmap scaled = Bitmap.createScaledBitmap(sq, sizePx, sizePx, true);
		Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(out);
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		Rect r = new Rect(0, 0, sizePx, sizePx);
		c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);
		p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		c.drawBitmap(scaled, r, r, p);
		return out;
	}
}
