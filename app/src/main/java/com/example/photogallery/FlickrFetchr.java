package com.example.photogallery;

import android.net.Uri;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FlickrFetchr {
    private static final String API_KEY = "d76db9a7cbf9ccc44c0d1fe1e8568a3a";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/").buildUpon().appendQueryParameter("api_key", API_KEY).appendQueryParameter("format", "json").appendQueryParameter("nojsoncallback", "1").appendQueryParameter("extras", "url_s").build();
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final String TAG = "FlickrFetchr";
    private static int maxPageRecent = -1;

    public List<GalleryItem> fetchRecentPhotos(int page) {
        return downloadGalleryItems(buildUrl(FETCH_RECENTS_METHOD, (String) null, page));
    }

    public List<GalleryItem> searchPhotos(String query, int page) {
        return downloadGalleryItems(buildUrl(SEARCH_METHOD, query, page));
    }

    private String buildUrl(String method, String query, int page) {
        int i = maxPageRecent;
        if (i == -1) {
            page = 1;
        } else if (i < page) {
            return null;
        }
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method).appendQueryParameter("page", String.valueOf(page));
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private List<GalleryItem> downloadGalleryItems(String url) {
        List<GalleryItem> items = new ArrayList<>();
        if (url == null) {
            return null;
        }
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Fetched Items: " + jsonString);
            parseItems(items, new JSONObject(jsonString));
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items: ", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to Parse JSON: ", je);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException, IOException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        maxPageRecent = photosJsonObject.getInt("pages");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            GalleryItem item = new GalleryItem();
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            if (photoJsonObject.has("url_s")) {
                item.setUrl(photoJsonObject.getString("url_s"));
                items.add(item);
            }
        }
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlSpec).openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() == 200) {
                byte[] buffer = new byte[1024];
                while (true) {
                    int read = in.read(buffer);
                    int bytesRead = read;
                    if (read > 0) {
                        out.write(buffer, 0, bytesRead);
                    } else {
                        out.close();
                        return out.toByteArray();
                    }
                }
            } else {
                throw new IOException(connection.getResponseMessage() + " with " + urlSpec);
            }
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
}
