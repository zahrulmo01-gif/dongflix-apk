package com.dongflix;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.*;
import androidx.core.ktx.*;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ui.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
	
	private String cover = "";
	
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> listPopularMap = new ArrayList<>();
	
	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;
	private Intent i = new Intent();
	
	// Deklarasi Komponen View Layout Utama
	private GridView gridview1;
	private RecyclerView recyclerPopular, listPopular;
	private TextView textHeroTitle, textHeroDesc;
	private ImageView imgHeroBanner, imgPlayIcon;
	private LinearLayout btnHeroPlay, indicatorCapsule;
	private View dot0, dot1, dot2;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		dongflix_req = new RequestNetwork(this);
		
		gridview1 = findViewById(R.id.gridview1);
		recyclerPopular = findViewById(R.id.recyclerPopular);
		listPopular = findViewById(R.id.listPopular);
		textHeroTitle = findViewById(R.id.text_hero_title);
		textHeroDesc = findViewById(R.id.text_hero_desc);
		imgHeroBanner = findViewById(R.id.img_hero_banner);
		btnHeroPlay = findViewById(R.id.btn_hero_play);
		imgPlayIcon = findViewById(R.id.img_play_icon);
		indicatorCapsule = findViewById(R.id.indicator_capsule);
		dot0 = findViewById(R.id.dot_0);
		dot1 = findViewById(R.id.dot_1);
		dot2 = findViewById(R.id.dot_2);
		
		if (gridview1 != null) {
			gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
					final int _position = _param3;
					if (listMap != null && _position < listMap.size()) {
						HashMap<String, Object> selectedData = listMap.get(_position);
						
						String animeUrl = selectedData.containsKey("url") ? selectedData.get("url").toString() : "";
						String animeTitle = selectedData.containsKey("title") ? selectedData.get("title").toString() : "";
						String animeCover = selectedData.containsKey("image") ? selectedData.get("image").toString() : "";
						
						Intent intent = new Intent(MainActivity.this, DetailActivity.class);
						intent.putExtra("url", animeUrl);
						intent.putExtra("title", animeTitle);
						intent.putExtra("image", animeCover);
						startActivity(intent);
					}
				}
			});
		}
		
		_dongflix_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				listMap = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				
				if (listMap.size() > 12) {
					listMap = new ArrayList<>(listMap.subList(0, 12));
				}
				
				if (gridview1 != null) {
					gridview1.setAdapter(new Gridview1Adapter(listMap));
					
					gridview1.post(new Runnable() {
						@Override
						public void run() {
							android.widget.ListAdapter adapter = gridview1.getAdapter();
							if (adapter == null || adapter.getCount() == 0) return;
							
							android.view.View item = adapter.getView(0, null, gridview1);
							int columnWidth = gridview1.getWidth() / 3;
							int widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(columnWidth, android.view.View.MeasureSpec.EXACTLY);
							int heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);
							
							item.measure(widthSpec, heightSpec);
							int itemHeight = item.getMeasuredHeight();
							int totalRows = (int) Math.ceil((double) adapter.getCount() / 3.0);
							
							int verticalSpacing = 0;
							if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
								verticalSpacing = gridview1.getVerticalSpacing();
							}
							
							android.view.ViewGroup.LayoutParams params = gridview1.getLayoutParams();
							params.height = (itemHeight * totalRows) + (verticalSpacing * (totalRows - 1));
							gridview1.setLayoutParams(params);
							gridview1.requestLayout();
						}
					});
				}
				
				if (listMap != null && listMap.size() >= 3) {
					if (textHeroTitle != null) textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					if (textHeroDesc != null) textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					if (imgHeroBanner != null) imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);
					
					android.graphics.drawable.GradientDrawable playActiveBg = new android.graphics.drawable.GradientDrawable();
					playActiveBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					playActiveBg.setColor(Color.WHITE);
					playActiveBg.setCornerRadius(100f);
					
					if (btnHeroPlay != null) {
						btnHeroPlay.setBackground(playActiveBg);
						btnHeroPlay.setEnabled(true);
					}
					if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.VISIBLE);
					if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.VISIBLE);
					
					final float density = getResources().getDisplayMetrics().density;
					final int activeWidth = (int) (16 * density);
					final int inactiveWidth = (int) (5 * density);
					final int fixedHeight = (int) (5 * density);
					final int marginSpacing = (int) (5 * density);
					
					final android.graphics.drawable.GradientDrawable activeDotDrawable = new android.graphics.drawable.GradientDrawable();
					activeDotDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					activeDotDrawable.setColor(Color.WHITE);
					activeDotDrawable.setCornerRadius(100f);
					
					final android.graphics.drawable.GradientDrawable inactiveDotDrawable = new android.graphics.drawable.GradientDrawable();
					inactiveDotDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					inactiveDotDrawable.setColor(Color.parseColor("#80FFFFFF"));
					inactiveDotDrawable.setCornerRadius(100f);
					
					final int[] heroCurrentIndex = {0};
					final android.os.Handler heroHandler = new android.os.Handler(android.os.Looper.getMainLooper());
					
					Runnable heroRunnable = new Runnable() {
						@Override
						public void run() {
							int pos = heroCurrentIndex[0];
							HashMap<String, Object> heroItem = listMap.get(pos);
							
							String heroTitle = heroItem.get("title") != null ? heroItem.get("title").toString() : "";
							String heroSynopsis = heroItem.get("synopsis") != null ? heroItem.get("synopsis").toString() : "";
							String heroCover = heroItem.get("cover") != null ? heroItem.get("cover").toString().trim() : "";
							final String heroLink = heroItem.get("link") != null ? heroItem.get("link").toString() : "";
							
							if (heroCover.contains("i.ibb.co.com")) {
								heroCover = heroCover.replace("i.ibb.co.com", "i.ibb.co");
							}
							if (heroCover.startsWith("http://")) {
								heroCover = heroCover.replace("http://", "https://");
							}
							
							if (textHeroTitle != null) textHeroTitle.setText(heroTitle);
							if (textHeroDesc != null) textHeroDesc.setText(heroSynopsis);
							
							if (imgHeroBanner != null) {
								Glide.with(getApplicationContext())
								.load(Uri.parse(heroCover))
								.centerCrop()
								.placeholder(R.drawable.default_image)
								.error(R.drawable.default_image)
								.into(imgHeroBanner);
							}
							
							if (btnHeroPlay != null) {
								btnHeroPlay.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(heroLink));
										startActivity(intent);
									}
								});
							}
							
							View[] dots = {dot0, dot1, dot2};
							for (int i = 0; i < dots.length; i++) {
								if (dots[i] != null) {
									boolean isActive = (i == pos);
									LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
									params.width = isActive ? activeWidth : inactiveWidth;
									params.height = fixedHeight;
									params.leftMargin = (i == 0) ? 0 : marginSpacing;
									dots[i].setLayoutParams(params);
									dots[i].setBackground(isActive ? activeDotDrawable : inactiveDotDrawable);
								}
							}
							
							heroCurrentIndex[0] = (pos + 1) % 3;
							heroHandler.postDelayed(this, 4000);
						}
					};
					
					heroHandler.removeCallbacksAndMessages(null);
					heroHandler.post(heroRunnable);
				}
				
				if (recyclerPopular != null) recyclerPopular.setAdapter(new RecyclerPopularAdapter(listMap));
				if (listPopular != null) listPopular.setAdapter(new ListPopularAdapter(listMap));
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
			}
		};
	}
	
	private void initializeLogic() {
		_UI();
		_Nav();
		
		ArrayList<HashMap<String, Object>> dummyList = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("is_loading", true);
			dummyList.add(map);
		}
		
		if (gridview1 != null) {
			gridview1.setAdapter(new Gridview1Adapter(dummyList));
			gridview1.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return event.getAction() == MotionEvent.ACTION_MOVE;
				}
			});
			gridview1.post(new Runnable() {
				@Override
				public void run() {
					setGridViewHeightBasedOnChildren(gridview1, 3);
				}
			});
		}
		
		if (textHeroTitle != null) {
			textHeroTitle.setText("");
			textHeroTitle.setBackgroundColor(Color.parseColor("#111111"));
		}
		if (textHeroDesc != null) {
			textHeroDesc.setText("");
			textHeroDesc.setBackgroundColor(Color.parseColor("#111111"));
		}
		
		if (imgHeroBanner != null) {
			imgHeroBanner.setImageResource(android.R.color.transparent);
			imgHeroBanner.setBackgroundColor(Color.parseColor("#111111"));
		}
		
		if (btnHeroPlay != null) {
			btnHeroPlay.setEnabled(false);
			android.graphics.drawable.GradientDrawable skeletonBtnBg = new android.graphics.drawable.GradientDrawable();
			skeletonBtnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
			skeletonBtnBg.setColor(Color.parseColor("#111111"));
			skeletonBtnBg.setCornerRadius(100f);
			btnHeroPlay.setBackground(skeletonBtnBg);
		}
		
		if (indicatorCapsule != null) indicatorCapsule.setVisibility(View.INVISIBLE);
		if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);
		
		dongflix_req.startRequestNetwork("GET", "https://dongflix-api1.zahrulmo01.workers.dev/", "", _dongflix_req_request_listener);
		
		if (recyclerPopular != null) {
			LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
			recyclerPopular.setLayoutManager(layoutManager);
			if (recyclerPopular.getOnFlingListener() == null) {
				PagerSnapHelper snapHelper = new PagerSnapHelper();
				snapHelper.attachToRecyclerView(recyclerPopular);
			}
			recyclerPopular.setAdapter(new RecyclerPopularAdapter(dummyList));
		}
		
		if (listPopular != null) {
			listPopular.setLayoutManager(new LinearLayoutManager(this));
			listPopular.setNestedScrollingEnabled(false);
		}
	}
	
	public void _UI() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
			android.view.Window window = getWindow();
			window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		
		// Setup Gradasi Atas (Header Shadow)
		View headerShadow = findViewById(R.id.header_shadow);
		if (headerShadow != null) {
			GradientDrawable topGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.parseColor("#E6000000"), Color.TRANSPARENT }
			);
			headerShadow.setBackground(topGradient);
		}
		
		// Setup Gradasi Bawah (Hero Shadow)
		View heroShadow = findViewById(R.id.hero_shadow);
		if (heroShadow != null) {
			GradientDrawable bottomGradient = new GradientDrawable(
				GradientDrawable.Orientation.TOP_BOTTOM,
				new int[] { Color.TRANSPARENT, Color.parseColor("#FF000000") }
			);
			heroShadow.setBackground(bottomGradient);
		}
		
		// Setup Floating Navbar Rounded
		LinearLayout navbar = findViewById(R.id.navbar);
		if (navbar != null) {
			GradientDrawable navbarBg = new GradientDrawable();
			navbarBg.setShape(GradientDrawable.RECTANGLE);
			navbarBg.setColor(Color.parseColor("#111111"));
			navbarBg.setCornerRadius(100f);
			navbarBg.setStroke(2, Color.parseColor("#333B82F6"));
			navbar.setBackground(navbarBg);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				navbar.setElevation(20f);
			}
		}
	}
	
	public void _Nav() {
		final View pageHome = findViewById(getResources().getIdentifier("main_scroll_view", "id", getPackageName()));
		final View pageSearch = findViewById(getResources().getIdentifier("layout_search", "id", getPackageName()));
		final View pageFavorite = findViewById(getResources().getIdentifier("layout_favorite", "id", getPackageName()));
		final View pageProfile = findViewById(getResources().getIdentifier("layout_profile", "id", getPackageName()));
		
		final ImageView imgHome = (ImageView) findViewById(getResources().getIdentifier("img_home", "id", getPackageName()));
		final ImageView imgSearch = (ImageView) findViewById(getResources().getIdentifier("img_search", "id", getPackageName()));
		final ImageView imgFavorite = (ImageView) findViewById(getResources().getIdentifier("img_favorite", "id", getPackageName()));
		final ImageView imgProfile = (ImageView) findViewById(getResources().getIdentifier("img_profile", "id", getPackageName()));
		
		final int colorActive = Color.parseColor("#3B82F6");
		final int colorInactive = Color.parseColor("#64748B");
		
		View.OnClickListener tabClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int id = v.getId();
				
				if (pageHome != null) pageHome.setVisibility(View.GONE);
				if (pageSearch != null) pageSearch.setVisibility(View.GONE);
				if (pageFavorite != null) pageFavorite.setVisibility(View.GONE);
				if (pageProfile != null) pageProfile.setVisibility(View.GONE);
				
				if (imgHome != null) imgHome.setColorFilter(colorInactive);
				if (imgSearch != null) imgSearch.setColorFilter(colorInactive);
				if (imgFavorite != null) imgFavorite.setColorFilter(colorInactive);
				if (imgProfile != null) imgProfile.setColorFilter(colorInactive);
				
				if (imgHome != null && id == imgHome.getId()) {
					if (pageHome != null) pageHome.setVisibility(View.VISIBLE);
					imgHome.setColorFilter(colorActive);
				} else if (imgSearch != null && id == imgSearch.getId()) {
					if (pageSearch != null) pageSearch.setVisibility(View.VISIBLE);
					imgSearch.setColorFilter(colorActive);
				} else if (imgFavorite != null && id == imgFavorite.getId()) {
					if (pageFavorite != null) pageFavorite.setVisibility(View.VISIBLE);
					imgFavorite.setColorFilter(colorActive);
				} else if (imgProfile != null && id == imgProfile.getId()) {
					if (pageProfile != null) pageProfile.setVisibility(View.VISIBLE);
					imgProfile.setColorFilter(colorActive);
				}
			}
		};
		
		if (imgHome != null) imgHome.setOnClickListener(tabClickListener);
		if (imgSearch != null) imgSearch.setOnClickListener(tabClickListener);
		if (imgFavorite != null) imgFavorite.setOnClickListener(tabClickListener);
		if (imgProfile != null) imgProfile.setOnClickListener(tabClickListener);
		
		if (imgHome != null) imgHome.performClick();
	}
	
	public void setGridViewHeightBasedOnChildren(GridView gridView, int columns) {
		android.widget.ListAdapter listAdapter = gridView.getAdapter();
		if (listAdapter == null) return;
		
		int totalHeight = 0;
		int items = listAdapter.getCount();
		int rows = (int) Math.ceil((double) items / columns);
		
		if (rows > 0) {
			View listItem = listAdapter.getView(0, null, gridView);
			listItem.measure(
				View.MeasureSpec.makeMeasureSpec(gridView.getWidth(), View.MeasureSpec.AT_MOST),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			);
			totalHeight = listItem.getMeasuredHeight() * rows;
		}
		
		ViewGroup.LayoutParams params = gridView.getLayoutParams();
		params.height = totalHeight + (rows * (int)(12 * getResources().getDisplayMetrics().density)) + (int)(40 * getResources().getDisplayMetrics().density);
		gridView.setLayoutParams(params);
		gridView.requestLayout();
	}
	
	public class RecyclerPopularAdapter extends RecyclerView.Adapter<RecyclerPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public RecyclerPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_popular_donghua, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			TextView textPopularTitle = _view.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView textPopularDesc = _view.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView textPopularGenre = _view.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView textPopularStatus = _view.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgPopularCover = _view.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = _view.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));
			
			if (_data.get(_position).containsKey("is_loading")) {
				if (textPopularTitle != null) {
					textPopularTitle.setText("");
					textPopularTitle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)555, 0xFF111111));
				}
				if (textPopularDesc != null) textPopularDesc.setBackgroundColor(0xFF111111);
				if (textPopularGenre != null) textPopularGenre.setBackgroundColor(0xFF111111);
				if (textPopularStatus != null) {
					textPopularStatus.setText("                    ");
					textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)555, 0xFF111111));
				}
				if (imgPopularCover != null) {
					imgPopularCover.setImageResource(android.R.color.transparent);
					imgPopularCover.setBackgroundColor(Color.parseColor("#111111"));
				}
			} else {
				if (textPopularStatus != null) textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				if (textPopularTitle != null) textPopularTitle.setText(_data.get(_position).get("title").toString());
				if (textPopularDesc != null) textPopularDesc.setText(_data.get(_position).get("synopsis").toString());
				if (textPopularGenre != null) textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));
				
				if (textPopularStatus != null) {
					textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
				}
				if (imgPopularCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(imgPopularCover);
				}
				if (linear35 != null) {
					linear35.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View _view) {
							i.setClass(getApplicationContext(), WatchActivity.class);
							i.putExtra("slug", _data.get(_position).get("slug").toString());
							startActivity(i);
						}
					});
				}
			}
		}
		
		@Override
		public int getItemCount() { return _data.size(); }
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
	
	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		
		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater inflater = getLayoutInflater();
			View _view = inflater.inflate(R.layout.custom_anime, _container, false);
			
			TextView textTitle = _view.findViewById(R.id.text_title);
			TextView textStatus = _view.findViewById(R.id.text_status);
			TextView textEpisode = _view.findViewById(R.id.text_episode);
			ImageView imgCover = _view.findViewById(R.id.img_cover);
			View view2 = _view.findViewById(R.id.view2);
			
			HashMap<String, Object> item = _data.get(_position);
			
			if (item.containsKey("is_loading")) {
				if (textTitle != null) {
					textTitle.setText("");
					textTitle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				}
				if (textStatus != null) textStatus.setBackgroundColor(Color.parseColor("#111111"));
				if (textEpisode != null) textEpisode.setBackgroundColor(Color.parseColor("#111111"));
				if (imgCover != null) {
					imgCover.setImageResource(android.R.color.transparent);
					imgCover.setBackgroundColor(Color.parseColor("#111111"));
				}
				if (view2 != null) view2.setVisibility(View.GONE);
			} else {
				if (imgCover != null) imgCover.setBackgroundColor(Color.TRANSPARENT);
				if (textTitle != null) textTitle.setBackgroundColor(Color.TRANSPARENT);
				if (textEpisode != null) textEpisode.setBackgroundColor(Color.TRANSPARENT);
				if (view2 != null) view2.setVisibility(View.VISIBLE);
				
				if (textStatus != null) {
					textStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF3B82F6));
				}
				
				if (view2 != null) {
					android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
						android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
						new int[] { Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
					);
					view2.setBackground(gradient);
				}
				
				if (textTitle != null) textTitle.setText(_data.get(_position).get("title").toString());
				if (textStatus != null) textStatus.setText(_data.get(_position).get("status").toString());
				if (textEpisode != null) textEpisode.setText(_data.get(_position).get("episode").toString());
				
				String cover = _data.get(_position).get("cover") != null ? _data.get(_position).get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) {
					cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (cover.startsWith("http://")) {
					cover = cover.replace("http://", "https://");
				}
				
				if (imgCover != null) {
					Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(imgCover);
				}
			}
			return _view;
		}
	}
	
	public class ListPopularAdapter extends RecyclerView.Adapter<ListPopularAdapter.ViewHolder> {
		ArrayList<HashMap<String, Object>> _data;
		public ListPopularAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_popular_donghua, parent, false);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			TextView textPopularTitle = _view.findViewById(getResources().getIdentifier("text_popular_title", "id", getPackageName()));
			TextView textPopularDesc = _view.findViewById(getResources().getIdentifier("text_popular_desc", "id", getPackageName()));
			TextView textPopularGenre = _view.findViewById(getResources().getIdentifier("text_popular_genre", "id", getPackageName()));
			TextView textPopularStatus = _view.findViewById(getResources().getIdentifier("text_popular_status", "id", getPackageName()));
			ImageView imgPopularCover = _view.findViewById(getResources().getIdentifier("img_popular_cover", "id", getPackageName()));
			View linear35 = _view.findViewById(getResources().getIdentifier("linear35", "id", getPackageName()));
			
			if (textPopularStatus != null) textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
			if (textPopularTitle != null) textPopularTitle.setText(_data.get(_position).get("title").toString());
			if (textPopularDesc != null) textPopularDesc.setText(_data.get(_position).get("synopsis").toString());
			if (textPopularGenre != null) textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));
			
			if (textPopularStatus != null) {
				textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
			}
			if (imgPopularCover != null) {
				Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(imgPopularCover);
			}
			if (linear35 != null) {
				linear35.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View _view) {
						i.setClass(getApplicationContext(), WatchActivity.class);
						i.putExtra("slug", _data.get(_position).get("slug").toString());
						startActivity(i);
					}
				});
			}
		}
		
		@Override
		public int getItemCount() { return _data.size(); }
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
}
