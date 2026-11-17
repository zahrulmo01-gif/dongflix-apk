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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.*;
import androidx.core.*;
import androidx.core.ktx.*;
import androidx.customview.*;
import androidx.customview.poolingcontainer.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.runtime.*;
import androidx.media.*;
import androidx.recyclerview.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.versionedparcelable.*;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.common.*;
import com.google.android.exoplayer2.container.*;
import com.google.android.exoplayer2.core.*;
import com.google.android.exoplayer2.database.*;
import com.google.android.exoplayer2.decoder.*;
import com.google.android.exoplayer2.extractor.*;
import com.google.android.exoplayer2.source.dash.*;
import com.google.android.exoplayer2.source.hls.*;
import com.google.android.exoplayer2.source.rtsp.*;
import com.google.android.exoplayer2.source.smoothstreaming.*;
import com.google.android.exoplayer2.ui.*;
import com.google.android.exoplayer2.upstream.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
	
	private ActivityMainBinding binding;
	private String cover = "";
	
	private ArrayList<HashMap<String, Object>> listMap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> listPopularMap = new ArrayList<>();
	
	private RequestNetwork dongflix_req;
	private RequestNetwork.RequestListener _dongflix_req_request_listener;
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		dongflix_req = new RequestNetwork(this);
		
		binding.gridview1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
				
				binding.gridview1.setAdapter(new Gridview1Adapter(listMap));
				
				binding.gridview1.post(new Runnable() {
					@Override
					public void run() {
						android.widget.ListAdapter adapter = binding.gridview1.getAdapter();
						if (adapter == null || adapter.getCount() == 0) return;
						
						android.view.View item = adapter.getView(0, null, binding.gridview1);
						int columnWidth = binding.gridview1.getWidth() / 3;
						int widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(columnWidth, android.view.View.MeasureSpec.EXACTLY);
						int heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);
						
						item.measure(widthSpec, heightSpec);
						int itemHeight = item.getMeasuredHeight();
						int totalRows = (int) Math.ceil((double) adapter.getCount() / 3.0);
						
						int verticalSpacing = 0;
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
							verticalSpacing = binding.gridview1.getVerticalSpacing();
						}
						
						android.view.ViewGroup.LayoutParams params = binding.gridview1.getLayoutParams();
						params.height = (itemHeight * totalRows) + (verticalSpacing * (totalRows - 1));
						binding.gridview1.setLayoutParams(params);
						binding.gridview1.requestLayout();
					}
				});
				
				if (listMap != null && listMap.size() >= 3) {
					binding.textHeroTitle.setBackgroundColor(Color.TRANSPARENT);
					binding.textHeroDesc.setBackgroundColor(Color.TRANSPARENT);
					binding.imgHeroBanner.setBackgroundColor(Color.TRANSPARENT);
					
					android.graphics.drawable.GradientDrawable playActiveBg = new android.graphics.drawable.GradientDrawable();
					playActiveBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
					playActiveBg.setColor(Color.WHITE);
					playActiveBg.setCornerRadius(100f);
					
					binding.btnHeroPlay.setBackground(playActiveBg);
					binding.btnHeroPlay.setEnabled(true);
					binding.imgPlayIcon.setVisibility(View.VISIBLE);
					binding.textPlayLabel.setVisibility(View.VISIBLE);
					binding.indicatorCapsule.setVisibility(View.VISIBLE);
					
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
							
							binding.textHeroTitle.setText(heroTitle);
							binding.textHeroDesc.setText(heroSynopsis);
							
							Glide.with(getApplicationContext())
							.load(Uri.parse(heroCover))
							.centerCrop()
							.placeholder(R.drawable.default_image)
							.error(R.drawable.default_image)
							.into(binding.imgHeroBanner);
							
							binding.btnHeroPlay.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(heroLink));
									startActivity(intent);
								}
							});
							
							View[] dots = {binding.dot0, binding.dot1, binding.dot2};
							for (int i = 0; i < dots.length; i++) {
								boolean isActive = (i == pos);
								LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dots[i].getLayoutParams();
								
								params.width = isActive ? activeWidth : inactiveWidth;
								params.height = fixedHeight;
								params.leftMargin = (i == 0) ? 0 : marginSpacing;
								
								dots[i].setLayoutParams(params);
								dots[i].setBackground(isActive ? activeDotDrawable : inactiveDotDrawable);
							}
							
							heroCurrentIndex[0] = (pos + 1) % 3;
							heroHandler.postDelayed(this, 4000);
						}
					};
					
					heroHandler.removeCallbacksAndMessages(null);
					heroHandler.post(heroRunnable);
				}
				
				binding.recyclerPopular.setAdapter(new RecyclerPopularAdapter(listMap));
				binding.listPopular.setAdapter(new ListPopularAdapter(listMap));
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
		
		binding.gridview1.setAdapter(new Gridview1Adapter(dummyList));
		
		binding.gridview1.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return event.getAction() == MotionEvent.ACTION_MOVE;
			}
		});
		
		binding.gridview1.post(new Runnable() {
			@Override
			public void run() {
				setGridViewHeightBasedOnChildren(binding.gridview1, 3);
			}
		});
		
		binding.textHeroTitle.setText("");
		binding.textHeroDesc.setText("");
		binding.textHeroTitle.setBackgroundColor(Color.parseColor("#111111"));
		binding.textHeroDesc.setBackgroundColor(Color.parseColor("#111111"));
		
		binding.imgHeroBanner.setImageResource(android.R.color.transparent);
		binding.imgHeroBanner.setBackgroundColor(Color.parseColor("#111111"));
		
		binding.btnHeroPlay.setEnabled(false);
		binding.indicatorCapsule.setVisibility(View.INVISIBLE);
		
		android.graphics.drawable.GradientDrawable skeletonBtnBg = new android.graphics.drawable.GradientDrawable();
		skeletonBtnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
		skeletonBtnBg.setColor(Color.parseColor("#111111"));
		skeletonBtnBg.setCornerRadius(100f);
		
		binding.btnHeroPlay.setBackground(skeletonBtnBg);
		binding.btnHeroPlay.setEnabled(false);
		binding.imgPlayIcon.setVisibility(View.GONE);
		binding.textPlayLabel.setVisibility(View.INVISIBLE);
		binding.indicatorCapsule.setVisibility(View.INVISIBLE);
		
		dongflix_req.startRequestNetwork(RequestNetworkController.GET, "https://dongflix-api1.zahrulmo01.workers.dev/", "", _dongflix_req_request_listener);
		
		androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
		new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
		binding.recyclerPopular.setLayoutManager(layoutManager);
		
		if (binding.recyclerPopular.getOnFlingListener() == null) {
			androidx.recyclerview.widget.PagerSnapHelper snapHelper = new androidx.recyclerview.widget.PagerSnapHelper();
			snapHelper.attachToRecyclerView(binding.recyclerPopular);
		}
		
		binding.recyclerPopular.setAdapter(new RecyclerPopularAdapter(dummyList));
		binding.listPopular.setNestedScrollingEnabled(false);
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
		params.height = totalHeight + (rows * (int)(8 * getResources().getDisplayMetrics().density)) + (int)(100 * getResources().getDisplayMetrics().density);
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
			ItemPopularDonghuaBinding bindingItem = ItemPopularDonghuaBinding.bind(_view);
			
			if (_data.get(_position).containsKey("is_loading")) {
				bindingItem.textPopularTitle.setText("");
				bindingItem.textPopularDesc.setText("");
				bindingItem.textPopularGenre.setText("");
				bindingItem.textPopularStatus.setText("                    ");
				bindingItem.textPopularTitle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)555, 0xFF111111));
				bindingItem.textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)555, 0xFF111111));
				bindingItem.textPopularDesc.setBackgroundColor(0xFF111111);
				bindingItem.textPopularGenre.setBackgroundColor(0xFF111111);
				bindingItem.imgPopularCover.setImageResource(android.R.color.transparent);
				bindingItem.imgPopularCover.setBackgroundColor(Color.parseColor("#111111"));
			} else {
				bindingItem.textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				bindingItem.textPopularTitle.setText(_data.get(_position).get("title").toString());
				bindingItem.textPopularDesc.setText(_data.get(_position).get("synopsis").toString());
				bindingItem.textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));
				
				bindingItem.textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
				Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(bindingItem.imgPopularCover);
				bindingItem.linear35.setOnClickListener(new View.OnClickListener() {
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
	
	public class Gridview1Adapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> _data;
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
		
		@Override public int getCount() { return _data.size(); }
		@Override public HashMap<String, Object> getItem(int _index) { return _data.get(_index); }
		@Override public long getItemId(int _index) { return _index; }
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			CustomAnimeBinding bindingItem = CustomAnimeBinding.inflate(getLayoutInflater());
			View _view = bindingItem.getRoot();
			
			HashMap<String, Object> item = _data.get(_position);
			
			if (item.containsKey("is_loading")) {
				bindingItem.textTitle.setText("");
				bindingItem.textStatus.setText("");
				bindingItem.textEpisode.setText("");
				
				bindingItem.imgCover.clearAnimation();
				_view.clearAnimation();
				_view.setAlpha(1.0f);
				
				bindingItem.textTitle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
				bindingItem.textStatus.setBackgroundColor(Color.parseColor("#111111"));
				bindingItem.textEpisode.setBackgroundColor(Color.parseColor("#111111"));
				bindingItem.imgCover.setImageResource(android.R.color.transparent);
				bindingItem.imgCover.setBackgroundColor(Color.parseColor("#111111"));
				bindingItem.view2.setVisibility(View.GONE);
			} else {
				bindingItem.imgCover.clearAnimation();
				_view.clearAnimation();
				_view.setAlpha(1.0f);
				
				bindingItem.imgCover.setBackgroundColor(Color.TRANSPARENT);
				bindingItem.textTitle.setBackgroundColor(Color.TRANSPARENT);
				bindingItem.textEpisode.setBackgroundColor(Color.TRANSPARENT);
				bindingItem.view2.setVisibility(View.VISIBLE);
				bindingItem.textStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF3B82F6));
				
				android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
					android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
					new int[] { Color.parseColor("#E6000000"), Color.parseColor("#80000000"), Color.TRANSPARENT }
				);
				bindingItem.view2.setBackground(gradient);
				
				bindingItem.textTitle.setText(_data.get(_position).get("title").toString());
				bindingItem.textStatus.setText(_data.get(_position).get("status").toString());
				bindingItem.textEpisode.setText(_data.get(_position).get("episode").toString());
				
				String cover = _data.get(_position).get("cover") != null ? _data.get(_position).get("cover").toString().trim() : "";
				if (cover.contains("i.ibb.co.com")) {
					cover = cover.replace("i.ibb.co.com", "i.ibb.co");
				}
				if (cover.startsWith("http://")) {
					cover = cover.replace("http://", "https://");
				}
				
				Glide.with(getApplicationContext()).load(Uri.parse(cover)).into(bindingItem.imgCover);
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
			ItemPopularDonghuaBinding bindingItem = ItemPopularDonghuaBinding.bind(_view);
			
			bindingItem.textPopularStatus.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)50, 0xFF111111));
			bindingItem.textPopularTitle.setText(_data.get(_position).get("title").toString());
			bindingItem.textPopularDesc.setText(_data.get(_position).get("synopsis").toString());
			bindingItem.textPopularGenre.setText(_data.get(_position).get("genres").toString().replace("[", "").replace("]", ""));
			
			bindingItem.textPopularStatus.setText(_data.get(_position).get("episode").toString().concat(" / ".concat(_data.get(_position).get("status").toString())));
			Glide.with(getApplicationContext()).load(Uri.parse(_data.get(_position).get("cover").toString())).into(bindingItem.imgPopularCover);
			bindingItem.linear35.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					i.setClass(getApplicationContext(), WatchActivity.class);
					i.putExtra("slug", _data.get(_position).get("slug").toString());
					startActivity(i);
				}
			});
		}
		
		@Override
		public int getItemCount() { return _data.size(); }
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) { super(v); }
		}
	}
}
