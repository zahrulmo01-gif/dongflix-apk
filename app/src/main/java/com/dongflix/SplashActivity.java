package com.dongflix;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

	private final Handler handler = new Handler(Looper.getMainLooper());
	private Typeface outfitFont;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window w = getWindow();
			w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			w.setStatusBarColor(Color.TRANSPARENT);
		}

		ImageView imgLogo = findViewById(R.id.img_splash_logo);
		TextView tvBrand = findViewById(R.id.text_splash_brand);
		TextView tvStatus = findViewById(R.id.text_splash_status);
		TextView tvVersion = findViewById(R.id.text_splash_version);

		int appIconId = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
		if (imgLogo != null && appIconId > 0) {
			imgLogo.setImageResource(appIconId);
		}

		Typeface tf = getOutfitTypeface();
		if (tvBrand != null) {
			tvBrand.setTypeface(tf, Typeface.NORMAL);
			tvBrand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
		}
		if (tvStatus != null) {
			tvStatus.setTypeface(tf, Typeface.NORMAL);
		}
		if (tvVersion != null) {
			tvVersion.setTypeface(tf, Typeface.NORMAL);
			tvVersion.setText(resolveAppVersionText());
		}

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (isFinishing() || isDestroyed()) return;
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
				Intent next = new Intent(SplashActivity.this, user != null ? MainActivity.class : LoginActivity.class);
				next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(next);
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				finish();
			}
		}, 1200L);
	}

	private String resolveAppVersionText() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			String vName = info.versionName != null ? info.versionName : "6.0";
			int vCode = info.versionCode;
			return "V" + vName + " (" + vCode + ")";
		} catch (Exception e) {
			return "V6.0";
		}
	}

	private Typeface getOutfitTypeface() {
		if (outfitFont != null) return outfitFont;
		String[] candidates = {
			"fonts/outfit_regular.ttf",
			"fonts/outfit_medium.ttf",
			"fonts/outfit.ttf",
			"fonts/Outfit-Regular.ttf",
			"fonts/productsans_reg.ttf"
		};
		for (String path : candidates) {
			try {
				outfitFont = Typeface.createFromAsset(getAssets(), path);
				if (outfitFont != null) return outfitFont;
			} catch (Exception ignored) {}
		}
		outfitFont = Typeface.DEFAULT;
		return outfitFont;
	}

	@Override
	protected void onDestroy() {
		handler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}
}
