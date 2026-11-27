package com.dongflix;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class StreamExtractorHelper {

	public interface ExtractionCallback {
		void onSuccess(ArrayList<String> qualityLabels, ArrayList<String> streamUrls, String referer, String dmCookie);
		void onNeedSniffer(String snifferUrl, String referer);
	}

	public static String normalizeResolutionToP(String raw) {
		if (raw == null) return "720p";
		String s = raw.trim().toLowerCase(Locale.US);
		if (s.isEmpty()) return "720p";

		if (s.equals("mobile") || s.equals("lowest")) return "144p";
		if (s.equals("low")) return "360p";
		if (s.equals("medium") || s.equals("sd") || s.equals("standard")) return "480p";
		if (s.equals("high") || s.equals("hd")) return "720p";
		if (s.equals("full") || s.equals("fullhd") || s.equals("full hd") || s.equals("fhd")) return "1080p";
		if (s.equals("2k") || s.equals("qhd")) return "1440p";
		if (s.equals("4k") || s.equals("uhd")) return "2160p";

		Matcher m = Pattern.compile("(2160|1440|1080|720|540|480|380|360|240|144)").matcher(s);
		if (m.find()) {
			String num = m.group(1);
			if ("380".equals(num)) num = "360";
			return num + "p";
		}

		String digitsOnly = s.replaceAll("[^0-9]", "");
		if (!digitsOnly.isEmpty()) {
			try {
				int val = Integer.parseInt(digitsOnly);
				if (val >= 144 && val <= 4320) {
					return val + "p";
				}
			} catch (Exception ignored) {}
		}

		return "720p";
	}

	public static void extract(final String server, final String targetUrl, final ExtractionCallback callback) {
		if (targetUrl == null || targetUrl.trim().isEmpty() || callback == null) return;

		String cleanUrl = targetUrl.trim();
		if (cleanUrl.startsWith("//")) {
			cleanUrl = "https:" + cleanUrl;
		} else if (!cleanUrl.startsWith("http")) {
			cleanUrl = "https://" + cleanUrl;
		}

		final String finalTargetUrl = cleanUrl;
		final String serverKey = (server != null) ? server.trim().toLowerCase(Locale.US) : "";
		final String lowerUrl = finalTargetUrl.toLowerCase(Locale.US);
		final Handler mainHandler = new Handler(Looper.getMainLooper());

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (serverKey.contains("ok") || serverKey.contains("odnoklassniki") || lowerUrl.contains("ok.ru")) {
						extractOkRu(finalTargetUrl, mainHandler, callback);
					} else if (serverKey.contains("daily") || lowerUrl.contains("dailymotion") || lowerUrl.contains("dai.ly") || lowerUrl.contains("dongflix-dailymotion")) {
						extractDailymotion(finalTargetUrl, mainHandler, callback);
					} else if (serverKey.contains("d-tube") || serverKey.contains("dtube") || lowerUrl.contains("d.tube")) {
						extractDtube(finalTargetUrl, mainHandler, callback);
					} else if (serverKey.contains("rumble") || lowerUrl.contains("rumble.com")) {
						extractRumble(finalTargetUrl, mainHandler, callback);
					} else if (serverKey.contains("odysee") || lowerUrl.contains("odysee.com") || lowerUrl.contains("odycdn.com")) {
						extractOdysee(finalTargetUrl, mainHandler, callback);
					} else {
						extractGeneric(finalTargetUrl, lowerUrl, mainHandler, callback);
					}
				} catch (Exception e) {
					postSniffer(mainHandler, callback, finalTargetUrl, finalTargetUrl);
				}
			}
		}).start();
	}

	private static void extractOkRu(String finalTargetUrl, Handler mainHandler, ExtractionCallback callback) throws Exception {
		String vidId = "";
		if (finalTargetUrl.contains("ok.ru/videoembed/") || finalTargetUrl.contains("ok.ru/video/")) {
			vidId = finalTargetUrl.substring(finalTargetUrl.lastIndexOf("/") + 1).replaceAll("[^0-9]", "");
		}

		String embedPageUrl = !vidId.isEmpty() ? "https://ok.ru/videoembed/" + vidId : finalTargetUrl;
		HttpFetchResult res = fetchHttp(embedPageUrl, "https://ok.ru/", true);
		String html = res.body;
		if (html.isEmpty() && !vidId.isEmpty()) {
			HttpFetchResult mobRes = fetchHttp("https://m.ok.ru/video/" + vidId, "https://ok.ru/", true);
			html = mobRes.body;
			if (!mobRes.cookie.isEmpty()) res = mobRes;
		}

		ArrayList<String> tempNames = new ArrayList<>();
		ArrayList<String> tempUrls = new ArrayList<>();

		String foundOptions = "";
		int idx = html.indexOf("data-options=");
		if (idx != -1) {
			int qStart = idx + 13;
			char quote = html.charAt(qStart);
			int qEnd = html.indexOf(quote, qStart + 1);
			if (qEnd != -1) {
				foundOptions = html.substring(qStart + 1, qEnd);
			}
		}

		if (!foundOptions.isEmpty()) {
			try {
				String cleanJson = foundOptions
					.replace("&quot;", "\"")
					.replace("&amp;", "&")
					.replace("&#39;", "'")
					.replace("&lt;", "<")
					.replace("&gt;", ">");

				JSONObject optJson = new JSONObject(cleanJson);
				JSONObject metadata = null;
				if (optJson.has("flashvars")) {
					JSONObject fv = optJson.getJSONObject("flashvars");
					if (fv.has("metadata")) {
						Object mObj = fv.get("metadata");
						metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
					}
				} else if (optJson.has("metadata")) {
					Object mObj = optJson.get("metadata");
					metadata = (mObj instanceof JSONObject) ? (JSONObject) mObj : new JSONObject(mObj.toString());
				}

				if (metadata != null && metadata.has("videos")) {
					JSONArray videos = metadata.getJSONArray("videos");
					for (int k = 0; k < videos.length(); k++) {
						JSONObject v = videos.getJSONObject(k);
						if (v.has("url")) {
							String rawUrl = v.getString("url").replace("\\/", "/").replace("\\u0026", "&");
							if (rawUrl.startsWith("//")) rawUrl = "https:" + rawUrl;
							String labelP = normalizeResolutionToP(v.optString("name", ""));
							addUniqueQuality(tempNames, tempUrls, labelP, rawUrl);
						}
					}
				}
			} catch (Exception ignored) {}
		}

		if (tempUrls.isEmpty() && !html.isEmpty()) {
			String unescaped = html
				.replace("\\/", "/")
				.replace("\\u0026", "&")
				.replace("&quot;", "\"")
				.replace("&amp;", "&")
				.replace("&#39;", "'");

			String[] resOrder = {"1080p", "720p", "480p", "360p", "240p"};
			Matcher mJsonUrl = Pattern.compile("\"(?:url|hlsMasterPlaylistUrl|ondemandUrl|manifestUrl)\"\\s*:\\s*\"(https?://[^\"]+)\"").matcher(unescaped);
			while (mJsonUrl.find()) {
				String u = mJsonUrl.group(1).replace("\\/", "/").replace("\\u0026", "&");
				if (u.startsWith("//")) u = "https:" + u;
				if (!tempUrls.contains(u) && (u.contains("okcdn.ru") || u.contains(".mp4") || u.contains(".m3u8"))) {
					String guessed = normalizeResolutionToP(u);
					if ("720p".equals(guessed) && tempNames.size() < resOrder.length) {
						guessed = resOrder[tempNames.size()];
					}
					addUniqueQuality(tempNames, tempUrls, guessed, u);
				}
				if (tempUrls.size() >= 5) break;
			}

			if (tempUrls.isEmpty()) {
				Matcher mOk = Pattern.compile("(https?://[^\"'\\s<>\\\\]*(?:okcdn\\.ru|\\.mp4|\\.m3u8)[^\"'\\s<>\\\\]*)").matcher(unescaped);
				while (mOk.find()) {
					String u = mOk.group(1);
					if (!tempUrls.contains(u)) {
						String guessed = normalizeResolutionToP(u);
						if ("720p".equals(guessed) && tempNames.size() < resOrder.length) {
							guessed = resOrder[tempNames.size()];
						}
						addUniqueQuality(tempNames, tempUrls, guessed, u);
					}
					if (tempUrls.size() >= 4) break;
				}
			}
		}

		if (!tempUrls.isEmpty()) {
			postSuccess(mainHandler, callback, tempNames, tempUrls, "https://ok.ru/", res.cookie);
		} else {
			postSniffer(mainHandler, callback, embedPageUrl, "https://ok.ru/");
		}
	}

	private static void extractDailymotion(String finalTargetUrl, Handler mainHandler, ExtractionCallback callback) {
		try {
			String dailyId = "";
			String playerId = "xid0t";
			Uri parsedUri = Uri.parse(finalTargetUrl);

			if (parsedUri.getQueryParameter("id") != null && !parsedUri.getQueryParameter("id").trim().isEmpty()) {
				dailyId = parsedUri.getQueryParameter("id").trim();
			} else if (parsedUri.getQueryParameter("video") != null && !parsedUri.getQueryParameter("video").trim().isEmpty()) {
				dailyId = parsedUri.getQueryParameter("video").trim();
			} else if (finalTargetUrl.contains("/video/")) {
				String[] parts = finalTargetUrl.split("/video/");
				if (parts.length > 1) {
					dailyId = parts[1].split("[?&#_]")[0].trim();
				}
			} else if (finalTargetUrl.contains("dai.ly/")) {
				String[] parts = finalTargetUrl.split("dai.ly/");
				if (parts.length > 1) {
					dailyId = parts[1].split("[?&#]")[0].trim();
				}
			} else {
				String cleanPath = (parsedUri.getPath() != null) ? parsedUri.getPath() : "";
				if (!cleanPath.isEmpty() && !cleanPath.equals("/")) {
					String lastSeg = cleanPath.substring(cleanPath.lastIndexOf('/') + 1).trim();
					if (!lastSeg.equals("worker.js") && !lastSeg.equals("workers.js")) {
						dailyId = lastSeg;
					}
				}
			}

			String path = parsedUri.getPath();
			if (path != null && path.contains("/player/")) {
				String afterPlayer = path.substring(path.indexOf("/player/") + 8);
				if (afterPlayer.endsWith(".html")) {
					afterPlayer = afterPlayer.substring(0, afterPlayer.length() - 5);
				}
				if (!afterPlayer.trim().isEmpty()) {
					playerId = afterPlayer.trim();
				}
			}

			String geoUrl = !dailyId.isEmpty()
				? "https://geo.dailymotion.com/player/" + playerId + ".html?video=" + Uri.encode(dailyId)
				: finalTargetUrl;

			if (!dailyId.isEmpty()) {
				String metaUrl = "https://www.dailymotion.com/player/metadata/video/" + Uri.encode(dailyId);
				HttpFetchResult metaRes = fetchHttp(metaUrl, "https://geo.dailymotion.com/", true);
				if (metaRes.body != null && !metaRes.body.trim().isEmpty()) {
					ArrayList<String> names = new ArrayList<>();
					ArrayList<String> urls = new ArrayList<>();

					JSONObject root = new JSONObject(metaRes.body);
					JSONObject qualitiesObj = root.optJSONObject("qualities");
					if (qualitiesObj != null) {
						Iterator<String> keys = qualitiesObj.keys();
						while (keys.hasNext()) {
							String qKey = keys.next();
							JSONArray arr = qualitiesObj.optJSONArray(qKey);
							if (arr == null) continue;
							for (int i = 0; i < arr.length(); i++) {
								JSONObject item = arr.optJSONObject(i);
								if (item == null) continue;
								String m3u8Url = item.optString("url", "").replace("\\/", "/");
								if (!m3u8Url.isEmpty()) {
									if (m3u8Url.startsWith("//")) m3u8Url = "https:" + m3u8Url;
									String label = "auto".equalsIgnoreCase(qKey) ? "720p" : normalizeResolutionToP(qKey);
									addUniqueQuality(names, urls, label, m3u8Url);
								}
							}
						}
					}

					if (!urls.isEmpty()) {
						postSuccess(mainHandler, callback, names, urls, "https://geo.dailymotion.com/", metaRes.cookie);
						return;
					}
				}
			}

			postSniffer(mainHandler, callback, geoUrl, "https://geo.dailymotion.com/");
		} catch (Exception e) {
			postSniffer(mainHandler, callback, finalTargetUrl, "https://geo.dailymotion.com/");
		}
	}

	private static void extractDtube(String finalTargetUrl, Handler mainHandler, ExtractionCallback callback) {
		try {
			ArrayList<String> names = new ArrayList<>();
			ArrayList<String> urls = new ArrayList<>();

			Uri uri = Uri.parse(finalTargetUrl);
			String vParam = uri.getQueryParameter("v");
			String videoId = (vParam != null && !vParam.trim().isEmpty()) ? vParam.trim() : "";

			Pattern pDirectQm = Pattern.compile("(Qm[a-zA-Z0-9]{44})");
			Matcher mDirectQm = pDirectQm.matcher(finalTargetUrl);
			if (mDirectQm.find()) {
				addUniqueQuality(names, urls, "720p", "https://dweb.link/ipfs/" + mDirectQm.group(1));
			}

			if (urls.isEmpty() && finalTargetUrl.contains("/v/")) {
				String afterV = finalTargetUrl.substring(finalTargetUrl.indexOf("/v/") + 3).split("[?#]")[0];
				String[] parts = afterV.split("/");
				if (parts.length >= 2) {
					String author = parts[0].trim();
					String permlink = parts[1].trim();
					String rpcBody = "{\"jsonrpc\":\"2.0\",\"method\":\"condenser_api.get_content\",\"params\":[\"" + author + "\",\"" + permlink + "\"],\"id\":1}";
					String rpcRes = postJsonHttp("https://api.hive.blog", rpcBody);
					JSONObject rpcJson = new JSONObject(rpcRes);
					if (rpcJson.has("result")) {
						JSONObject resObj = rpcJson.getJSONObject("result");
						String metaStr = resObj.optString("json_metadata", "");
						if (!metaStr.isEmpty()) {
							JSONObject metaJson = new JSONObject(metaStr);
							if (metaJson.has("video")) {
								JSONObject videoObj = metaJson.getJSONObject("video");
								if (videoObj.has("files")) {
									JSONObject filesObj = videoObj.getJSONObject("files");
									if (filesObj.has("ipfs")) {
										JSONObject ipfsObj = filesObj.getJSONObject("ipfs");
										if (ipfsObj.has("vid")) {
											JSONObject vidObj = ipfsObj.getJSONObject("vid");
											String[] keys = {"1080", "720", "480", "360", "240", "src"};
											for (String k : keys) {
												String hash = vidObj.optString(k, "");
												if (!hash.isEmpty()) {
													addUniqueQuality(names, urls, normalizeResolutionToP(k), "https://dweb.link/ipfs/" + hash);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}

			if (urls.isEmpty() && !videoId.isEmpty()) {
				String[] apiCandidates = {
					"https://api.d.tube/v/" + videoId,
					"https://d.tube/api/v/" + videoId,
					"https://emb.d.tube/#!/" + videoId
				};
				for (String apiUrl : apiCandidates) {
					try {
						String body = fetchHttp(apiUrl, "https://d.tube/", false).body;
						if (body == null || body.trim().isEmpty()) continue;
						String unescaped = body.replace("\\/", "/").replace("\\u0026", "&");

						Matcher mMedia = Pattern.compile("(https?://[^\"'\\s<>]+\\.(?:mp4|m3u8)[^\"'\\s<>]*)").matcher(unescaped);
						while (mMedia.find()) {
							String mediaUrl = mMedia.group(1);
							addUniqueQuality(names, urls, normalizeResolutionToP(mediaUrl), mediaUrl);
							if (urls.size() >= 3) break;
						}

						if (urls.isEmpty()) {
							Matcher mQm = pDirectQm.matcher(unescaped);
							if (mQm.find()) {
								addUniqueQuality(names, urls, "720p", "https://dweb.link/ipfs/" + mQm.group(1));
							}
						}
						if (!urls.isEmpty()) break;
					} catch (Exception ignored) {}
				}
			}

			if (urls.isEmpty()) {
				String html = fetchHttp(finalTargetUrl, "https://emb.d.tube/", false).body;
				if (html != null && !html.isEmpty()) {
					String unescaped = html.replace("\\/", "/").replace("\\u0026", "&");
					Matcher mMedia = Pattern.compile("(https?://[^\"'\\s<>]+\\.(?:mp4|m3u8)[^\"'\\s<>]*)").matcher(unescaped);
					while (mMedia.find()) {
						String mediaUrl = mMedia.group(1);
						addUniqueQuality(names, urls, normalizeResolutionToP(mediaUrl), mediaUrl);
						if (urls.size() >= 3) break;
					}
					if (urls.isEmpty()) {
						Matcher mQm = pDirectQm.matcher(unescaped);
						if (mQm.find()) {
							addUniqueQuality(names, urls, "720p", "https://dweb.link/ipfs/" + mQm.group(1));
						}
					}
				}
			}

			if (!urls.isEmpty()) {
				postSuccess(mainHandler, callback, names, urls, "https://emb.d.tube/", "");
			} else {
				String snifferUrl = !videoId.isEmpty()
					? "https://emb.d.tube/#!/" + videoId
					: finalTargetUrl;
				postSniffer(mainHandler, callback, snifferUrl, "https://emb.d.tube/");
			}
		} catch (Exception e) {
			postSniffer(mainHandler, callback, finalTargetUrl, "https://emb.d.tube/");
		}
	}

	private static void extractRumble(String finalTargetUrl, Handler mainHandler, ExtractionCallback callback) throws Exception {
		String embedId = "";
		if (finalTargetUrl.contains("/embed/")) {
			String[] parts = finalTargetUrl.split("/embed/");
			if (parts.length > 1) embedId = parts[1].split("[/?#]")[0];
		}

		ArrayList<String> rNames = new ArrayList<>();
		ArrayList<String> rUrls = new ArrayList<>();

		if (embedId.isEmpty()) {
			String pageHtml = fetchHttp(finalTargetUrl, "https://rumble.com/", false).body;
			Matcher mEmbed = Pattern.compile("rumble\\.com/embed/([a-zA-Z0-9]+)").matcher(pageHtml);
			if (mEmbed.find()) {
				embedId = mEmbed.group(1);
			}
		}

		if (!embedId.isEmpty()) {
			String apiUrl = "https://rumble.com/embedJS/u3/?request=video&ver=2&v=" + embedId;
			String jsonStr = fetchHttp(apiUrl, "https://rumble.com/", false).body;
			JSONObject rootObj = new JSONObject(jsonStr);

			if (rootObj.has("ua")) {
				JSONObject uaObj = rootObj.getJSONObject("ua");
				String[] resKeys = new String[]{"1080", "720", "480", "360", "240"};

				if (uaObj.has("mp4")) {
					JSONObject mp4Obj = uaObj.getJSONObject("mp4");
					for (String rk : resKeys) {
						if (mp4Obj.has(rk)) {
							String u = mp4Obj.getJSONObject(rk).optString("url");
							if (!u.isEmpty()) {
								addUniqueQuality(rNames, rUrls, normalizeResolutionToP(rk), u);
							}
						}
					}
				}

				if (rUrls.isEmpty()) {
					for (String rk : resKeys) {
						if (uaObj.has(rk)) {
							JSONObject resObj = uaObj.optJSONObject(rk);
							if (resObj != null) {
								String u = resObj.optString("url");
								if (!u.isEmpty()) {
									addUniqueQuality(rNames, rUrls, normalizeResolutionToP(rk), u);
								}
							}
						}
					}
				}

				if (rUrls.isEmpty() && uaObj.has("hls")) {
					JSONObject hlsObj = uaObj.getJSONObject("hls");
					if (hlsObj.has("auto")) {
						String u = hlsObj.getJSONObject("auto").optString("url");
						if (!u.isEmpty()) {
							addUniqueQuality(rNames, rUrls, "720p", u);
						}
					}
				}
			}
		}

		if (rUrls.isEmpty()) {
			String html = fetchHttp(finalTargetUrl, "https://rumble.com/", false).body;
			Matcher mMp4 = Pattern.compile("(https?://[a-zA-Z0-9.-]+\\.rumble\\.com/[^\"'\\s]+\\.mp4)").matcher(html);
			if (mMp4.find()) {
				String u = mMp4.group(1);
				addUniqueQuality(rNames, rUrls, normalizeResolutionToP(u), u);
			}
		}

		if (!rUrls.isEmpty()) {
			postSuccess(mainHandler, callback, rNames, rUrls, "https://rumble.com/", "");
		} else {
			postSniffer(mainHandler, callback, finalTargetUrl, "https://rumble.com/");
		}
	}

	private static void extractOdysee(String finalTargetUrl, Handler mainHandler, ExtractionCallback callback) throws Exception {
		String streamOdysee = "";
		if (finalTargetUrl.contains("odycdn.com") && (finalTargetUrl.contains(".mp4") || finalTargetUrl.contains(".m3u8") || finalTargetUrl.contains("/streams/"))) {
			streamOdysee = finalTargetUrl;
		} else if (finalTargetUrl.contains("signature=")) {
			Uri uri = Uri.parse(finalTargetUrl);
			String sig = uri.getQueryParameter("signature");
			String sigTs = uri.getQueryParameter("signature_ts");
			String path = uri.getPath();
			if (path != null && path.contains("/embed/")) {
				String cleanClaim = path.replace("/$/embed/", "").replace("/embed/", "");
				streamOdysee = "https://player.odycdn.com/api/v4/streams/free/" + cleanClaim + "/" + sig + "/" + sigTs;
			}
		}

		if (streamOdysee.isEmpty()) {
			String html = fetchHttp(finalTargetUrl, "https://odysee.com/", false).body;
			Matcher mOdy = Pattern.compile("(https?://player\\.odycdn\\.com/api/v4/streams/[^\"'\\s]+)").matcher(html);
			if (mOdy.find()) {
				streamOdysee = mOdy.group(1).replace("\\/", "/");
			} else {
				Matcher mMedia = Pattern.compile("(https?://[^\"'\\s]+\\.(?:mp4|m3u8)[^\"'\\s]*)").matcher(html);
				if (mMedia.find()) {
					streamOdysee = mMedia.group(1).replace("\\/", "/");
				}
			}
		}

		if (streamOdysee.isEmpty() && finalTargetUrl.contains("/embed/")) {
			String afterEmbed = finalTargetUrl.substring(finalTargetUrl.indexOf("/embed/") + 7).split("[?#]")[0];
			String lbryUri = "lbry://" + afterEmbed.replace("/", "#");
			String lbryReq = "{\"method\":\"get\",\"params\":{\"uri\":\"" + lbryUri + "\"}}";
			String lbryRes = postJsonHttp("https://api.na-backend.odysee.com/api/v1/proxy", lbryReq);
			JSONObject lbryJson = new JSONObject(lbryRes);
			if (lbryJson.has("result")) {
				streamOdysee = lbryJson.getJSONObject("result").optString("streaming_url", "");
			}
		}

		if (!streamOdysee.isEmpty()) {
			ArrayList<String> names = new ArrayList<>();
			ArrayList<String> urls = new ArrayList<>();
			addUniqueQuality(names, urls, normalizeResolutionToP(streamOdysee), streamOdysee);
			postSuccess(mainHandler, callback, names, urls, "https://odysee.com/", "");
		} else {
			postSniffer(mainHandler, callback, finalTargetUrl, "https://odysee.com/");
		}
	}

	private static void extractGeneric(String finalTargetUrl, String lowerUrl, Handler mainHandler, ExtractionCallback callback) throws Exception {
		if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8")) {
			ArrayList<String> names = new ArrayList<>();
			ArrayList<String> urls = new ArrayList<>();
			addUniqueQuality(names, urls, normalizeResolutionToP(finalTargetUrl), finalTargetUrl);
			postSuccess(mainHandler, callback, names, urls, finalTargetUrl, "");
			return;
		}

		String refererToUse = finalTargetUrl;
		String html = fetchHttp(finalTargetUrl, finalTargetUrl, false).body;
		String unpacked = unpackDeanEdwards(html);
		String combinedSource = html + "\n" + unpacked;
		String streamFound = "";

		Matcher mSource = Pattern.compile("<(?:source|video)[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(combinedSource);
		if (mSource.find()) {
			streamFound = mSource.group(1).replace("&amp;", "&");
		}

		if (streamFound.isEmpty()) {
			Matcher mMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE).matcher(combinedSource);
			if (mMedia.find()) {
				streamFound = mMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
			}
		}

		if (streamFound.isEmpty()) {
			Matcher mIframe = Pattern.compile("<iframe[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(combinedSource);
			if (mIframe.find()) {
				String innerUrl = mIframe.group(1).replace("&amp;", "&");
				refererToUse = innerUrl;
				String innerHtml = fetchHttp(innerUrl, finalTargetUrl, false).body;
				String innerUnpacked = unpackDeanEdwards(innerHtml);
				String innerCombined = innerHtml + "\n" + innerUnpacked;
				Matcher mInnerMedia = Pattern.compile("(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:m3u8|mp4)[^\"'\\s<>]*)", Pattern.CASE_INSENSITIVE).matcher(innerCombined);
				if (mInnerMedia.find()) {
					streamFound = mInnerMedia.group(1).replace("\\/", "/").replace("\\u0026", "&");
				}
			}
		}

		if (!streamFound.isEmpty()) {
			ArrayList<String> names = new ArrayList<>();
			ArrayList<String> urls = new ArrayList<>();
			addUniqueQuality(names, urls, normalizeResolutionToP(streamFound), streamFound);
			postSuccess(mainHandler, callback, names, urls, refererToUse, "");
		} else {
			postSniffer(mainHandler, callback, finalTargetUrl, refererToUse);
		}
	}

	private static void addUniqueQuality(ArrayList<String> names, ArrayList<String> urls, String label, String url) {
		if (url == null || url.trim().isEmpty()) return;
		String cleanLabel = normalizeResolutionToP(label);
		int existingIdx = names.indexOf(cleanLabel);
		if (existingIdx >= 0) {
			urls.set(existingIdx, url);
		} else {
			names.add(cleanLabel);
			urls.add(url);
		}
	}

	private static void postSuccess(
		Handler handler,
		final ExtractionCallback callback,
		final ArrayList<String> names,
		final ArrayList<String> urls,
		final String referer,
		final String cookie
	) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onSuccess(names, urls, referer, cookie);
			}
		});
	}

	private static void postSniffer(
		Handler handler,
		final ExtractionCallback callback,
		final String snifferUrl,
		final String referer
	) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				callback.onNeedSniffer(snifferUrl, referer);
			}
		});
	}

	private static class HttpFetchResult {
		final String body;
		final String cookie;
		HttpFetchResult(String body, String cookie) {
			this.body = body;
			this.cookie = cookie;
		}
	}

	private static HttpFetchResult fetchHttp(String targetUrl, String referer, boolean saveDmCookie) throws Exception {
		String currentUrl = targetUrl;
		HttpURLConnection conn = null;
		String collectedCookie = "";

		for (int redirectCount = 0; redirectCount < 6; redirectCount++) {
			URL url = new URL(currentUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8");
			conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,id;q=0.8");
			conn.setRequestProperty("Sec-Fetch-Dest", "iframe");
			conn.setRequestProperty("Sec-Fetch-Mode", "navigate");
			if (referer != null && !referer.isEmpty()) {
				conn.setRequestProperty("Referer", referer);
			}

			if (saveDmCookie) {
				Map<String, List<String>> headerFields = conn.getHeaderFields();
				List<String> cookiesHeader = headerFields.get("Set-Cookie");
				if (cookiesHeader != null && !cookiesHeader.isEmpty()) {
					StringBuilder cookieSb = new StringBuilder();
					for (String c : cookiesHeader) {
						String mainPart = c.split(";")[0];
						if (cookieSb.length() > 0) cookieSb.append("; ");
						cookieSb.append(mainPart);
					}
					collectedCookie = cookieSb.toString();
				}
			}

			int code = conn.getResponseCode();
			if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
				|| code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
				String loc = conn.getHeaderField("Location");
				conn.disconnect();
				if (loc == null || loc.isEmpty()) break;
				if (loc.startsWith("//")) loc = "https:" + loc;
				else if (loc.startsWith("/")) loc = url.getProtocol() + "://" + url.getHost() + loc;
				currentUrl = loc;
				continue;
			}
			break;
		}

		if (conn == null) return new HttpFetchResult("", collectedCookie);
		int respCode = conn.getResponseCode();
		if (respCode < 200 || respCode >= 300) {
			conn.disconnect();
			return new HttpFetchResult("", collectedCookie);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		reader.close();
		conn.disconnect();
		return new HttpFetchResult(sb.toString(), collectedCookie);
	}

	private static String postJsonHttp(String apiUrl, String jsonPayload) throws Exception {
		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36");

		OutputStream os = conn.getOutputStream();
		os.write(jsonPayload.getBytes("UTF-8"));
		os.flush();
		os.close();

		int respCode = conn.getResponseCode();
		if (respCode < 200 || respCode >= 300) {
			conn.disconnect();
			return "";
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		conn.disconnect();
		return sb.toString();
	}

	private static String unpackDeanEdwards(String html) {
		try {
			Pattern p = Pattern.compile("eval\\(function\\(p,a,c,k,e,[dr]\\)\\{.*?\\}\\('(.*?)',(\\d+),(\\d+),'(.*?)'\\.split\\('\\|'\\)", Pattern.DOTALL);
			Matcher m = p.matcher(html);
			if (m.find()) {
				String payload = m.group(1);
				int radix = Integer.parseInt(m.group(2));
				String[] symtab = m.group(4).split("\\|");

				Pattern wordPat = Pattern.compile("\\b\\w+\\b");
				Matcher wm = wordPat.matcher(payload);
				StringBuffer sb = new StringBuffer();
				while (wm.find()) {
					String word = wm.group();
					try {
						int idx = Integer.parseInt(word, radix);
						if (idx >= 0 && idx < symtab.length && !symtab[idx].isEmpty()) {
							wm.appendReplacement(sb, Matcher.quoteReplacement(symtab[idx]));
						}
					} catch (Exception ignored) {}
				}
				wm.appendTail(sb);
				return sb.toString();
			}
		} catch (Exception ignored) {}
		return "";
	}
}

