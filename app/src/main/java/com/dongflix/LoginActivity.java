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

	private static final String FIREBASE_API_KEY = "AIzaSyAddo6FcJmixeNsGN3XACI_ghqGYzigMds";[span_7](start_span)[span_7](end_span)
	private static final String FIREBASE_DB_URL = "https://donghu-91808-default-rtdb.asia-southeast1.firebasedatabase.app";[span_8](start_span)[span_8](end_span)
	private static final String AUTH_SIGN_IN_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY;
	private static final String AUTH_SIGN_UP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FIREBASE_API_KEY;

	private FrameLayout logoBadgeBox, btnSubmitAuth;
	private LinearLayout tabSwitchContainer, authFormCard, groupRegisterFields;
	private TextView textAuthHeading, textAuthSub, tabModeLogin, tabModeRegister, textSubmitAuth, textToggleHint;
	private EditText inputRegName, inputRegUsername, inputAuthEmail, inputAuthPassword;
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
		inputAuthPassword = findViewById(R.id.input_auth_password);

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

		EditText[] inputs = {inputRegName, inputRegUsername, inputAuthEmail, inputAuthPassword};
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
			if (textAuthSub != null) textAuthSub.setText("Daftar dengan email dan password untuk menyimpan favorit & riwayat tontonan kamu.");
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
			if (textAuthSub != null) textAuthSub.setText("Masuk menggunakan email dan password akun Dongflix kamu.");
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

	private String parseAuthError(String errJson) {
		try {
			JSONObject obj = new JSONObject(errJson);
			String msg = obj.optJSONObject("error").optString("message", "");
			if (msg.contains("EMAIL_NOT_FOUND") || msg.contains("INVALID_LOGIN_CREDENTIALS")) {
				return "Email atau password salah";
			}
			if (msg.contains("INVALID_PASSWORD")) {
				return "Password salah";
			}
			if (msg.contains("EMAIL_EXISTS")) {
				return "Email sudah terdaftar di Authentication";
			}
			if (msg.contains("WEAK_PASSWORD")) {
				return "Password minimal 6 karakter";
			}
			if (msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
				return "Terlalu banyak percobaan, coba lagi nanti";
			}
		} catch (Exception ignored) {}
		return "Autentikasi gagal";
	}

	private void handleAuthSubmit() {
		final String rawEmail = inputAuthEmail != null ? inputAuthEmail.getText().toString().trim().toLowerCase() : "";
		final String rawPassword = inputAuthPassword != null ? inputAuthPassword.getText().toString() : "";

		if (rawEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(rawEmail).matches()) {
			Toast.makeText(this, "Masukkan alamat email yang valid", Toast.LENGTH_SHORT).show();
			return;
		}

		if (rawPassword.length() < 6) {
			Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
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
					JSONObject payload = new JSONObject();
					payload.put("email", rawEmail);
					payload.put("password", rawPassword);
					payload.put("returnSecureToken", true);

					String endpoint = isRegisterMode ? AUTH_SIGN_UP_URL : AUTH_SIGN_IN_URL;
					String authResponse = httpPostJson(endpoint, payload.toString());
					JSONObject authJson = new JSONObject(authResponse);
					String idToken = authJson.optString("idToken", "");

					String encodedKey = URLEncoder.encode(userKey, "UTF-8");
					String dbUserUrl = FIREBASE_DB_URL + "/users/" + encodedKey + ".json"
						+ (idToken.isEmpty() ? "" : "?auth=" + URLEncoder.encode(idToken, "UTF-8"));

					if (!isRegisterMode) {
						// Baca data profil dari node users di Realtime Database
						String existingDb = httpGet(dbUserUrl);
						String name = rawEmail.split("@")[0];
						String username = rawEmail.split("@")[0];
						String bio = "Dongflix Member";
						String photoURL = "";
						int exp = 0;

						if (existingDb != null && !existingDb.trim().equals("null") && !existingDb.trim().isEmpty()) {
							JSONObject userObj = new JSONObject(existingDb);
							name = userObj.optString("name", name);
							username = userObj.optString("username", username);
							bio = userObj.optString("bio", bio);
							photoURL = userObj.optString("photoURL", "");
							exp = userObj.optInt("exp", 0);
						}

						saveSessionAndProceed(rawEmail, userKey, idToken, name, username, bio, photoURL, exp);
					} else {
						// Daftar baru: simpan profil ke node users di Realtime Database
						String cleanUsername = regUsername.replaceAll("[^a-z0-9_]", "");
						if (cleanUsername.isEmpty()) cleanUsername = rawEmail.split("@")[0];

						JSONObject newUser = new JSONObject();
						newUser.put("email", rawEmail);
						newUser.put("name", regName);
						newUser.put("username", cleanUsername);
						newUser.put("bio", "Dongflix — Streaming Donghua Sub Indo");
						newUser.put("exp", 100);
						newUser.put("followersCount", 0);
						newUser.put("followingCount", 0);
						newUser.put("photoURL", "");
						newUser.put("updatedAt", System.currentTimeMillis());

						httpPutJson(dbUserUrl, newUser.toString());

						saveSessionAndProceed(
							rawEmail,
							userKey,
							idToken,
							regName,
							cleanUsername,
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
							Toast.makeText(LoginActivity.this, parseAuthError(e.getMessage()), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
		}).start();
	}

	private void saveSessionAndProceed(final String email, final String userKey, final String idToken,
									   final String name, final String username, final String bio,
									   final String photoURL, final int exp) {
		sessionPrefs.edit()
			.putBoolean("is_logged_in", true)
			.putString("email", email)
			.putString("user_key", userKey)
			.putString("id_token", idToken)
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

	private String httpPostJson(String urlStr, String jsonBody) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

		OutputStream os = conn.getOutputStream();
		os.write(jsonBody.getBytes("UTF-8"));
		os.flush();
		os.close();

		int code = conn.getResponseCode();
		BufferedReader br = new BufferedReader(new InputStreamReader(
			code >= 400 ? conn.getErrorStream() : conn.getInputStream(), "UTF-8"
		));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();
		conn.disconnect();

		if (code >= 400) {
			throw new Exception(sb.toString());
		}
		return sb.toString();
	}

	private void httpPutJson(String urlStr, String jsonBody) throws Exception {
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
