package com.dongflix;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.Queue;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";

	private FrameLayout logoBadgeBox, btnSubmitAuth;
	private LinearLayout tabSwitchContainer, authFormCard, groupRegisterFields;
	private TextView textAuthHeading, textAuthSub, tabModeLogin, tabModeRegister, textSubmitAuth, textToggleHint;
	private EditText inputRegName, inputRegUsername, inputAuthEmail;
	private ProgressBar progressAuth;

	private boolean isRegisterMode = false;
	private boolean isProcessing = false;
	private SharedPreferences sessionPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sessionPrefs = getSharedPreferences("dongflix_session", Context.MODE_PRIVATE);
		if (sessionPrefs.getBoolean("is_logged_in", false)) {
			goToMainActivity();
			return;
		}

		setContentView(R.layout.activity_login);
		initViews();
		setupStyle();
		applyCustomFonts();
		setupListeners();
	}

	private void initViews() {
		logoBadgeBox = findViewById(R.id.logo_badge_box);
		tabSwitchContainer = findViewById(R.id.tab_switch_container);
		authFormCard = findViewById(R.id.auth_form_card);
		groupRegisterFields = findViewById(R.id.group_register_fields);

		textAuthHeading = findViewById(R.id.text_auth_heading);
		textAuthSub = findViewById(R.id.text_auth_sub);
		tabModeLogin = findViewById(R.id.tab_mode_login);
		tabModeRegister = findViewById(R.id.tab_mode_register);
		textSubmitAuth = findViewById(R.id.text_submit_auth);
		textToggleHint = findViewById(R.id.text_toggle_hint);

		inputRegName = findViewById(R.id.input_reg_name);
		inputRegUsername = findViewById(R.id.input_reg_username);
		inputAuthEmail = findViewById(R.id.input_auth_email);

		btnSubmitAuth = findViewById(R.id.btn_submit_auth);
		progressAuth = findViewById(R.id.progress_auth);
	}

	private void setupStyle() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.setStatusBarColor(Color.parseColor("#000000"));
		}

		float d = getResources().getDisplayMetrics().density;

		if (logoBadgeBox != null) {
			GradientDrawable logoBg = new GradientDrawable();
			logoBg.setColor(Color.parseColor("#3B82F6"));
			logoBg.setCornerRadius(14 * d);
			logoBadgeBox.setBackground(logoBg);
		}

		if (tabSwitchContainer != null) {
			GradientDrawable switchBg = new GradientDrawable();
			switchBg.setColor(Color.parseColor("#12141A"));
			switchBg.setCornerRadius(100f);
			switchBg.setStroke((int)(1 * d), Color.parseColor("#1F2430"));
			tabSwitchContainer.setBackground(switchBg);
		}

		if (authFormCard != null) {
			GradientDrawable cardBg = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[]{Color.parseColor("#12141A"), Color.parseColor("#0B0D11")}
			);
			cardBg.setCornerRadius(20 * d);
			cardBg.setStroke((int)(1 * d), Color.parseColor("#24FFFFFF"));
			authFormCard.setBackground(cardBg);
		}

		EditText[] inputs = {inputRegName, inputRegUsername, inputAuthEmail};
		for (EditText et : inputs) {
			if (et != null) {
				GradientDrawable inputBg = new GradientDrawable();
				inputBg.setColor(Color.parseColor("#181B23"));
				inputBg.setCornerRadius(12 * d);
				inputBg.setStroke((int)(1 * d), Color.parseColor("#26FFFFFF"));
				et.setBackground(inputBg);
			}
		}

		if (btnSubmitAuth != null) {
			GradientDrawable btnBg = new GradientDrawable();
			btnBg.setColor(Color.parseColor("#3B82F6"));
			btnBg.setCornerRadius(100f);
			btnSubmitAuth.setBackground(btnBg);
		}

		updateModeUI();
	}

	private void updateModeUI() {
		GradientDrawable activeTabBg = new GradientDrawable();
		activeTabBg.setColor(Color.parseColor("#3B82F6"));
		activeTabBg.setCornerRadius(100f);

		if (isRegisterMode) {
			if (tabModeRegister != null) {
				tabModeRegister.setBackground(activeTabBg);
				tabModeRegister.setTextColor(Color.WHITE);
			}
			if (tabModeLogin != null) {
				tabModeLogin.setBackground(null);
				tabModeLogin.setTextColor(Color.parseColor("#94A3B8"));
			}
			if (groupRegisterFields != null) groupRegisterFields.setVisibility(View.VISIBLE);
			if (textAuthHeading != null) textAuthHeading.setText("Buat Akun Dongflix");
			if (textAuthSub != null) textAuthSub.setText("Daftar dengan email untuk menyimpan tontonan favorit & melacak progres episode kamu.");
			if (textSubmitAuth != null) textSubmitAuth.setText("Daftar Sekarang");
			if (textToggleHint != null) textToggleHint.setText("Sudah punya akun? Masuk di sini");
		} else {
			if (tabModeLogin != null) {
				tabModeLogin.setBackground(activeTabBg);
				tabModeLogin.setTextColor(Color.WHITE);
			}
			if (tabModeRegister != null) {
				tabModeRegister.setBackground(null);
				tabModeRegister.setTextColor(Color.parseColor("#94A3B8"));
			}
			if (groupRegisterFields != null) groupRegisterFields.setVisibility(View.GONE);
			if (textAuthHeading != null) textAuthHeading.setText("Selamat Datang Kembali");
			if (textAuthSub != null) textAuthSub.setText("Masuk menggunakan email yang terdaftar di Dongflix untuk menyinkronkan riwayat & pustaka kamu.");
			if (textSubmitAuth != null) textSubmitAuth.setText("Masuk Sekarang");
			if (textToggleHint != null) textToggleHint.setText("Belum punya akun? Daftar di sini");
		}
	}

	private void setupListeners() {
		if (tabModeLogin != null) {
			tabModeLogin.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = false;
					updateModeUI();
				}
			});
		}

		if (tabModeRegister != null) {
			tabModeRegister.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = true;
					updateModeUI();
				}
			});
		}

		if (textToggleHint != null) {
			textToggleHint.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					isRegisterMode = !isRegisterMode;
					updateModeUI();
				}
			});
		}

		if (btnSubmitAuth != null) {
			btnSubmitAuth.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isProcessing) return;
					handleAuthSubmit();
				}
			});
		}
	}

	private void setLoadingState(boolean loading) {
		isProcessing = loading;
		if (textSubmitAuth != null) textSubmitAuth.setVisibility(loading ? View.GONE : View.VISIBLE);
		if (progressAuth != null) progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
		if (btnSubmitAuth != null) btnSubmitAuth.setEnabled(!loading);
	}

	private String emailToFirebaseKey(String email) {
		return email.trim().toLowerCase().replace(".", ",");
	}

	private void handleAuthSubmit() {
		final String rawEmail = inputAuthEmail != null ? inputAuthEmail.getText().toString().trim().toLowerCase() : "";

		if (rawEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(rawEmail).matches()) {
			Toast.makeText(this, "Masukkan alamat email yang valid", Toast.LENGTH_SHORT).show();
			return;
		}

		final String regName = inputRegName != null ? inputRegName.getText().toString().trim() : "";
		final String regUsername = inputRegUsername != null ? inputRegUsername.getText().toString().trim().toLowerCase() : "";

		if (isRegisterMode) {
			if (regName.isEmpty()) {
				Toast.makeText(this, "Nama tampilan tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
			if (regUsername.isEmpty()) {
				Toast.makeText(this, "Username tidak boleh kosong", Toast.LENGTH_SHORT).show();
				return;
			}
		}

		setLoadingState(true);
		final String userKey = emailToFirebaseKey(rawEmail);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String encodedKey = URLEncoder.encode(userKey, "UTF-8");
					String userUrl = FIREBASE_DB_URL + "/users/" + encodedKey + ".json";
					String existingData = httpGet(userUrl);

					if (!isRegisterMode) {
						// MODE LOGIN: Cek apakah email ada di node users
						if (existingData == null || existingData.trim().equals("null") || existingData.trim().isEmpty()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									setLoadingState(false);
									Toast.makeText(LoginActivity.this, "Email belum terdaftar. Silakan Daftar Baru!", Toast.LENGTH_LONG).show();
									isRegisterMode = true;
									updateModeUI();
								}
							});
							return;
						}

						JSONObject userObj = new JSONObject(existingData);
						String name = userObj.optString("name", rawEmail.split("@")[0]);
						String username = userObj.optString("username", rawEmail.split("@")[0]);
						String bio = userObj.optString("bio", "Dongflix Member");
						String photoURL = userObj.optString("photoURL", "");
						int exp = userObj.optInt("exp", 0);

						saveSessionAndProceed(rawEmail, userKey, name, username, bio, photoURL, exp);
					} else {
						// MODE DAFTAR: Pastikan email belum terdaftar
						if (existingData != null && !existingData.trim().equals("null") && !existingData.trim().isEmpty()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									setLoadingState(false);
									Toast.makeText(LoginActivity.this, "Email sudah terdaftar, silakan langsung Masuk", Toast.LENGTH_SHORT).show();
									isRegisterMode = false;
									updateModeUI();
								}
							});
							return;
						}

						JSONObject newUser = new JSONObject();
						newUser.put("email", rawEmail);
						newUser.put("name", regName);
						newUser.put("username", regUsername.replaceAll("[^a-z0-9_]", ""));
						newUser.put("bio", "Dongflix — Streaming Donghua Sub Indo");
						newUser.put("exp", 100);
						newUser.put("followersCount", 0);
						newUser.put("followingCount", 0);
						newUser.put("photoURL", "");
						newUser.put("updatedAt", System.currentTimeMillis());

						httpPut(userUrl, newUser.toString());

						saveSessionAndProceed(
							rawEmail,
							userKey,
							regName,
							regUsername.replaceAll("[^a-z0-9_]", ""),
							"Dongflix — Streaming Donghua Sub Indo",
							"",
							100
						);
					}
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setLoadingState(false);
							Toast.makeText(LoginActivity.this, "Gagal terhubung ke server: " + e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).start();
	}

	private void saveSessionAndProceed(final String email, final String userKey, final String name,
									   final String username, final String bio, final String photoURL, final int exp) {
		sessionPrefs.edit()
			.putBoolean("is_logged_in", true)
			.putString("email", email)
			.putString("user_key", userKey)
			.putString("name", name)
			.putString("username", username)
			.putString("bio", bio)
			.putString("photoURL", photoURL)
			.putInt("exp", exp)
			.apply();

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				setLoadingState(false);
				Toast.makeText(LoginActivity.this, "Selamat datang, " + name + "!", Toast.LENGTH_SHORT).show();
				goToMainActivity();
			}
		});
	}

	private void goToMainActivity() {
		Intent intent = new Intent(LoginActivity.this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}

	private String httpGet(String urlStr) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		conn.disconnect();
		return sb.toString();
	}

	private void httpPut(String urlStr, String jsonBody) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("PUT");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		OutputStream os = conn.getOutputStream();
		os.write(jsonBody.getBytes("UTF-8"));
		os.flush();
		os.close();
		conn.getResponseCode();
		conn.disconnect();
	}

	private void applyCustomFonts() {
		try {
			final Typeface fontReg = Typeface.createFromAsset(getAssets(), "fonts/productsans_reg.ttf");
			final Typeface fontMed = Typeface.createFromAsset(getAssets(), "fonts/productsans_med.ttf");
			final Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/productsans_bold.ttf");

			Queue<View> viewQueue = new LinkedList<>();
			viewQueue.add(getWindow().getDecorView().findViewById(android.R.id.content));

			while (!viewQueue.isEmpty()) {
				View currentView = viewQueue.poll();
				if (currentView instanceof TextView) {
					TextView tv = (TextView) currentView;
					Typeface tf = tv.getTypeface();
					int style = (tf != null) ? tf.getStyle() : Typeface.NORMAL;

					if (style == Typeface.BOLD) {
						tv.setTypeface(fontBold, Typeface.NORMAL);
					} else if (style == Typeface.ITALIC) {
						tv.setTypeface(fontReg, Typeface.ITALIC);
					} else {
						tv.setTypeface(fontMed, Typeface.NORMAL);
					}
				} else if (currentView instanceof ViewGroup) {
					ViewGroup group = (ViewGroup) currentView;
					for (int k = 0; k < group.getChildCount(); k++) {
						viewQueue.add(group.getChildAt(k));
					}
				}
			}
		} catch (Exception ignored) {}
	}
}
