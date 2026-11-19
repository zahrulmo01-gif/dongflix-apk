package com.dongflix;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.HashMap;

public class DongflixPlayerManager {

	public static final String CHANNEL_ID = "dongflix_playback_channel";
	public static final int NOTIF_ID = 7788;
	public static final String ACTION_TOGGLE_PLAY = "com.dongflix.ACTION_TOGGLE_PLAY";
	public static final String ACTION_CLOSE_PLAYER = "com.dongflix.ACTION_CLOSE_PLAYER";

	private static DongflixPlayerManager instance;

	public MediaPlayer sharedMediaPlayer;
	public boolean isMiniPlayerActive = false;

	public String activeSlug = "";
	public String activeTitle = "";
	public String activeEpisodeLabel = "";
	public String activeCoverUrl = "";
	public String activeStreamUrl = "";
	public int activeEpisodeIndex = 0;
	public ArrayList<HashMap<String, Object>> activeEpisodeList = new ArrayList<>();

	public String cachedAllCatalogJson = "";
	public String cachedEpisodesJson = "";
	public String activeServerName = "";
	public int activeSelectedServerIndex = 0;
	public ArrayList<String> activeServerNames = new ArrayList<>();
	public ArrayList<String> activeServerUrls = new ArrayList<>();
	public ArrayList<String> activeQualityNames = new ArrayList<>();
	public ArrayList<String> activeQualityUrls = new ArrayList<>();
	public int activeSelectedQualityIndex = 0;
	public boolean activeIsAutoQuality = true;

	public boolean isScrolledDownCompact = false;

	private boolean isReceiverRegistered = false;
	private Application appContext;
	private int startedActivityCount = 0;

	public static synchronized DongflixPlayerManager getInstance() {
		if (instance == null) {
			instance = new DongflixPlayerManager();
		}
		return instance;
	}

	public void initWithApplication(Application app) {
		if (appContext != null) return;
		appContext = app;
		createNotificationChannel(app);
		registerNotificationReceiver(app);

		app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
			@Override
			public void onActivityStarted(Activity activity) {
				startedActivityCount++;
			}

			@Override
			public void onActivityStopped(Activity activity) {
				startedActivityCount = Math.max(0, startedActivityCount - 1);
				if (startedActivityCount == 0 && sharedMediaPlayer != null) {
					try {
						if (sharedMediaPlayer.isPlaying()) {
							sharedMediaPlayer.pause();
							if (isMiniPlayerActive) {
								updatePlaybackNotification(activity);
							}
						}
					} catch (Exception ignored) {}
				}
			}

			@Override
			public void onActivityResumed(Activity activity) {
				if (activity instanceof WatchActivity) {
					removeMiniPlayerFromActivity(activity);
				} else if (isMiniPlayerActive && sharedMediaPlayer != null) {
					attachMiniPlayerToActivity(activity);
					updatePlaybackNotification(activity);
				}
			}

			@Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
			@Override public void onActivityPaused(Activity activity) {}
			@Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
			@Override public void onActivityDestroyed(Activity activity) {}
		});
	}

	private void createNotificationChannel(Context ctx) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"Pemutar Video Dongflix",
				NotificationManager.IMPORTANCE_LOW
			);
			channel.setDescription("Kontrol pemutaran Dongflix");
			channel.setShowBadge(false);
			NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			if (nm != null) nm.createNotificationChannel(channel);
		}
	}

	private void registerNotificationReceiver(Context ctx) {
		if (isReceiverRegistered) return;
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_TOGGLE_PLAY);
		filter.addAction(ACTION_CLOSE_PLAYER);

		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_TOGGLE_PLAY.equals(action)) {
					if (sharedMediaPlayer != null) {
						try {
							if (sharedMediaPlayer.isPlaying()) {
								sharedMediaPlayer.pause();
							} else {
								sharedMediaPlayer.start();
							}
							updatePlaybackNotification(context);
						} catch (Exception ignored) {}
					}
				} else if (ACTION_CLOSE_PLAYER.equals(action)) {
					stopAndCloseEverything(context);
				}
			}
		};

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			ctx.registerReceiver(receiver, filter);
		}
		isReceiverRegistered = true;
	}

	public void updatePlaybackNotification(Context ctx) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) return;

		boolean isPlaying = false;
		try {
			isPlaying = sharedMediaPlayer.isPlaying();
		} catch (Exception ignored) {}

		Intent openWatchIntent = new Intent(ctx, WatchActivity.class);
		openWatchIntent.putExtra("slug", activeSlug);
		openWatchIntent.putExtra("resume_mini_player", true);
		openWatchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
			? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
			: PendingIntent.FLAG_UPDATE_CURRENT;

		PendingIntent contentPi = PendingIntent.getActivity(ctx, 100, openWatchIntent, flags);

		PendingIntent togglePi = PendingIntent.getBroadcast(
			ctx, 101, new Intent(ACTION_TOGGLE_PLAY).setPackage(ctx.getPackageName()), flags
		);
		PendingIntent closePi = PendingIntent.getBroadcast(
			ctx, 102, new Intent(ACTION_CLOSE_PLAYER).setPackage(ctx.getPackageName()), flags
		);

		String subText = activeEpisodeLabel.isEmpty() ? "Sedang memutar" : "Episode " + activeEpisodeLabel;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_play1)
			.setContentTitle(activeTitle.isEmpty() ? "Dongflix" : activeTitle)
			.setContentText(subText)
			.setContentIntent(contentPi)
			.setOngoing(isPlaying)
			.setOnlyAlertOnce(true)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.addAction(
				isPlaying ? R.drawable.ic_pause : R.drawable.ic_play1,
				isPlaying ? "Jeda" : "Putar",
				togglePi
			)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", closePi);

		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIF_ID, builder.build());
		}
	}

	public void clearCache() {
		cachedAllCatalogJson = "";
		cachedEpisodesJson = "";
		activeServerName = "";
		activeSelectedServerIndex = 0;
		activeServerNames.clear();
		activeServerUrls.clear();
		activeQualityNames.clear();
		activeQualityUrls.clear();
		activeSelectedQualityIndex = 0;
		activeIsAutoQuality = true;
	}

	public void stopAndCloseEverything(Context ctx) {
		isMiniPlayerActive = false;
		isScrolledDownCompact = false;
		clearCache();
		if (sharedMediaPlayer != null) {
			try {
				if (sharedMediaPlayer.isPlaying()) sharedMediaPlayer.stop();
				sharedMediaPlayer.reset();
				sharedMediaPlayer.release();
			} catch (Exception ignored) {}
			sharedMediaPlayer = null;
		}
		NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm != null) nm.cancel(NOTIF_ID);

		if (ctx instanceof Activity) {
			removeMiniPlayerFromActivity((Activity) ctx);
			if (ctx instanceof MainActivity) {
				((MainActivity) ctx).resetNavbarToExpanded();
			}
		}
	}

	public void removeMiniPlayerFromActivity(Activity activity) {
		ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
		View existing = decor.findViewWithTag("DONGFLIX_MINI_PLAYER");
		if (existing != null && existing.getParent() instanceof ViewGroup) {
			((ViewGroup) existing.getParent()).removeView(existing);
		}
		FrameLayout inlineSlot = activity.findViewById(R.id.mini_player_inline_slot);
		if (inlineSlot != null) {
			inlineSlot.removeAllViews();
			inlineSlot.setVisibility(View.GONE);
		}
	}

	public void setCompactScrollMode(Activity activity, boolean compact) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) {
			isScrolledDownCompact = false;
			return;
		}
		if (isScrolledDownCompact == compact) return;
		isScrolledDownCompact = compact;
		attachMiniPlayerToActivity(activity);
	}

	private View buildMiniPlayerPillView(final Activity activity, final boolean isInlineCompact) {
		float d = activity.getResources().getDisplayMetrics().density;

		final FrameLayout glassRoot = new FrameLayout(activity);
		glassRoot.setTag("DONGFLIX_MINI_PLAYER");

		// Frosted glass universal (tanpa RenderEffect agar kompati di semua versi Android)
		GradientDrawable glassBg = new GradientDrawable(
			GradientDrawable.Orientation.TOP_BOTTOM,
			new int[]{Color.parseColor("#E6141822"), Color.parseColor("#F20A0C12")}
		);
		glassBg.setCornerRadius(100f);
		glassBg.setStroke((int)(1 * d), Color.parseColor("#33FFFFFF"));
		glassRoot.setBackground(glassBg);

		LinearLayout row = new LinearLayout(activity);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		int hPad = isInlineCompact ? (int)(8 * d) : (int)(10 * d);
		int vPad = isInlineCompact ? (int)(6 * d) : (int)(8 * d);
		row.setPadding(hPad, vPad, hPad, vPad);
		glassRoot.addView(row);

		androidx.cardview.widget.CardView thumbCard = new androidx.cardview.widget.CardView(activity);
		int thumbW = isInlineCompact ? (int)(44 * d) : (int)(56 * d);
		int thumbH = isInlineCompact ? (int)(44 * d) : (int)(44 * d);
		thumbCard.setLayoutParams(new LinearLayout.LayoutParams(thumbW, thumbH));
		thumbCard.setRadius(isInlineCompact ? 22 * d : 12 * d);
		thumbCard.setCardElevation(0f);
		thumbCard.setCardBackgroundColor(Color.BLACK);

		TextureView miniTexture = new TextureView(activity);
		miniTexture.setLayoutParams(new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));
		miniTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				if (sharedMediaPlayer != null) {
					try {
						sharedMediaPlayer.setSurface(new Surface(surface));
					} catch (Exception ignored) {}
				}
			}
			@Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {}
			@Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) { return true; }
			@Override public void onSurfaceTextureUpdated(SurfaceTexture s) {}
		});
		thumbCard.addView(miniTexture);
		row.addView(thumbCard);

		LinearLayout infoCol = new LinearLayout(activity);
		infoCol.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		infoLp.setMargins((int)(10 * d), 0, (int)(6 * d), 0);
		infoCol.setLayoutParams(infoLp);

		TextView tvTitle = new TextView(activity);
		tvTitle.setText(activeTitle.isEmpty() ? "Dongflix" : activeTitle);
		tvTitle.setTextColor(Color.WHITE);
		tvTitle.setTextSize(isInlineCompact ? 13f : 14f);
		tvTitle.setTypeface(null, Typeface.BOLD);
		tvTitle.setMaxLines(1);
		tvTitle.setEllipsize(TextUtils.TruncateAt.END);
		infoCol.addView(tvTitle);

		TextView tvSub = new TextView(activity);
		tvSub.setText(activeEpisodeLabel.isEmpty() ? "Dongflix" : "Episode " + activeEpisodeLabel);
		tvSub.setTextColor(Color.parseColor("#9CA3AF"));
		tvSub.setTextSize(isInlineCompact ? 11f : 12f);
		tvSub.setMaxLines(1);
		tvSub.setEllipsize(TextUtils.TruncateAt.END);
		infoCol.addView(tvSub);

		row.addView(infoCol);

		final ImageView btnMiniPlay = new ImageView(activity);
		LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams((int)(34 * d), (int)(34 * d));
		btnMiniPlay.setLayoutParams(playLp);
		btnMiniPlay.setPadding((int)(7 * d), (int)(7 * d), (int)(7 * d), (int)(7 * d));
		btnMiniPlay.setColorFilter(Color.WHITE);
		boolean playingNow = false;
		try { playingNow = sharedMediaPlayer.isPlaying(); } catch (Exception ignored) {}
		btnMiniPlay.setImageResource(playingNow ? R.drawable.ic_pause : R.drawable.ic_play1);

		btnMiniPlay.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (sharedMediaPlayer != null) {
					try {
						if (sharedMediaPlayer.isPlaying()) {
							sharedMediaPlayer.pause();
							btnMiniPlay.setImageResource(R.drawable.ic_play1);
						} else {
							sharedMediaPlayer.start();
							btnMiniPlay.setImageResource(R.drawable.ic_pause);
						}
						btnMiniPlay.setColorFilter(Color.WHITE);
						updatePlaybackNotification(activity);
					} catch (Exception ignored) {}
				}
			}
		});
		row.addView(btnMiniPlay);

		if (!isInlineCompact) {
			TextView btnClose = new TextView(activity);
			LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams((int)(30 * d), (int)(30 * d));
			closeLp.leftMargin = (int)(2 * d);
			btnClose.setLayoutParams(closeLp);
			btnClose.setGravity(Gravity.CENTER);
			btnClose.setText("✕");
			btnClose.setTextColor(Color.parseColor("#CBD5E1"));
			btnClose.setTextSize(13f);
			btnClose.setTypeface(null, Typeface.BOLD);
			btnClose.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					stopAndCloseEverything(activity);
				}
			});
			row.addView(btnClose);
		}

		glassRoot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, WatchActivity.class);
				intent.putExtra("slug", activeSlug);
				intent.putExtra("resume_mini_player", true);
				activity.startActivity(intent);
				activity.overridePendingTransition(R.anim.slide_up_enter, R.anim.no_anim);
			}
		});

		return glassRoot;
	}

	public void attachMiniPlayerToActivity(final Activity activity) {
		if (!isMiniPlayerActive || sharedMediaPlayer == null) return;

		removeMiniPlayerFromActivity(activity);
		float d = activity.getResources().getDisplayMetrics().density;

		FrameLayout inlineSlot = activity.findViewById(R.id.mini_player_inline_slot);
		View bottomNav = activity.findViewById(R.id.navbar_wrapper);

		if (isScrolledDownCompact && inlineSlot != null) {
			inlineSlot.setVisibility(View.VISIBLE);
			View inlinePill = buildMiniPlayerPillView(activity, true);
			inlineSlot.addView(inlinePill, new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			));
			return;
		}

		ViewGroup contentRoot = activity.findViewById(android.R.id.content);
		if (contentRoot == null) return;

		int bottomMarginPx = (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE)
			? (int) (92 * d)
			: (int) (16 * d);

		View floatingPill = buildMiniPlayerPillView(activity, false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			floatingPill.setElevation(24 * d);
		}

		FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			(int) (60 * d)
		);
		rootLp.gravity = Gravity.BOTTOM;
		rootLp.setMargins((int)(14 * d), 0, (int)(14 * d), bottomMarginPx);
		contentRoot.addView(floatingPill, rootLp);
	}
}
