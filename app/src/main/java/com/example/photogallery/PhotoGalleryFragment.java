package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    private static final String EXTRA_RECENT_PAGE_NUMBER = "RecentPageNumber";
    private static final String TAG = "PhotoGalleryFragment";
    /* access modifiers changed from: private */
    public List<GalleryItem> mGalleryItems;
    /* access modifiers changed from: private */
    public int mPageNo;
    /* access modifiers changed from: private */
    public RecyclerView mRecyclerView;
    /* access modifiers changed from: private */
    public boolean mScrollEnable;
    /* access modifiers changed from: private */
    public ThumbNailDownloader<PhotoHolder> mThumbNailDownloader;

    static /* synthetic */ void access$108(PhotoGalleryFragment x0) {
        int i = x0.mPageNo;
        x0.mPageNo = i + 1;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (savedInstanceState != null) {
            this.mPageNo = savedInstanceState.getInt(EXTRA_RECENT_PAGE_NUMBER, 1);
        } else {
            this.mPageNo = 1;
        }
        this.mGalleryItems = new ArrayList();
        updateItems(false);
        ThumbNailDownloader<PhotoHolder> thumbNailDownloader = new ThumbNailDownloader<>(new Handler(), requireActivity());
        this.mThumbNailDownloader = thumbNailDownloader;
        thumbNailDownloader.setThumbnailDownloadListener(new ThumbNailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                target.bind(new BitmapDrawable(PhotoGalleryFragment.this.getResources(), thumbnail));
            }
        });
        this.mThumbNailDownloader.start();
        this.mThumbNailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        this.mRecyclerView = recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(requireActivity(), 3));
        setUpAdapter();
        this.mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1) && PhotoGalleryFragment.this.mScrollEnable) {
                    PhotoGalleryFragment.access$108(PhotoGalleryFragment.this);
                    PhotoGalleryFragment.this.updateItems(true);
                }
            }
        });
        this.mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                PhotoGalleryFragment.this.mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ((GridLayoutManager) PhotoGalleryFragment.this.mRecyclerView.getLayoutManager()).setSpanCount(PhotoGalleryFragment.this.mRecyclerView.getWidth() / 300);
            }
        });
        return v;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_RECENT_PAGE_NUMBER, this.mPageNo);
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private boolean mAdd;
        private String mQuery;

        public FetchItemsTask(String query, boolean add) {
            this.mQuery = query;
            this.mAdd = add;
        }

        /* access modifiers changed from: protected */
        public void onPreExecute() {
            super.onPreExecute();
            boolean unused = PhotoGalleryFragment.this.mScrollEnable = false;
        }

        /* access modifiers changed from: protected */
        public List<GalleryItem> doInBackground(Integer... pages) {
            if (this.mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(pages[0].intValue());
            }
            return new FlickrFetchr().searchPhotos(this.mQuery, pages[0].intValue());
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(List<GalleryItem> galleryItems) {
            if (galleryItems != null) {
                if (!this.mAdd) {
                    PhotoGalleryFragment.this.mGalleryItems.clear();
                }
                final int oldSize = PhotoGalleryFragment.this.mGalleryItems.size();
                PhotoGalleryFragment.this.mGalleryItems.addAll(galleryItems);
                if (oldSize == 0) {
                    PhotoGalleryFragment.this.setUpAdapter();
                } else {
                    PhotoGalleryFragment.this.mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        public void onGlobalLayout() {
                            PhotoGalleryFragment.this.mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            PhotoGalleryFragment.this.mRecyclerView.smoothScrollToPosition(oldSize - 1);
                        }
                    });
                    PhotoGalleryFragment.this.mRecyclerView.getAdapter().notifyDataSetChanged();
                }
                boolean unused = PhotoGalleryFragment.this.mScrollEnable = true;
            }
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mItems;

        PhotoAdapter(List<GalleryItem> items) {
            this.mItems = items;
        }

        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new PhotoHolder(LayoutInflater.from(PhotoGalleryFragment.this.requireActivity()).inflate(R.layout.list_item_gallery, parent, false));
        }

        public void onBindViewHolder(PhotoHolder holder, int position) {
            holder.bind(PhotoGalleryFragment.this.getResources().getDrawable(R.drawable.bill_up_close));
            PhotoGalleryFragment.this.mThumbNailDownloader.queueThumbnail(holder, this.mItems.get(position).getUrl());
        }

        public int getItemCount() {
            return this.mItems.size();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            this.mImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bind(Drawable drawable) {
            this.mImageView.setImageDrawable(drawable);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        ((SearchView) menu.findItem(R.id.menu_item_search).getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String s) {
                Log.d(PhotoGalleryFragment.TAG, "QueryTextSubmit: " + s);
                QueryPreference.setStoredQuery(PhotoGalleryFragment.this.requireActivity(), s);
                int unused = PhotoGalleryFragment.this.mPageNo = 1;
                PhotoGalleryFragment.this.updateItems(false);
                return true;
            }

            public boolean onQueryTextChange(String s) {
                Log.d(PhotoGalleryFragment.TAG, "QueryTextChange: " + s);
                return false;
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear /*2131230974*/:
                QueryPreference.setStoredQuery(requireActivity(), (String) null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* access modifiers changed from: private */
    public void setUpAdapter() {
        if (isAdded()) {
            this.mRecyclerView.setAdapter(new PhotoAdapter(this.mGalleryItems));
        }
    }

    /* access modifiers changed from: private */
    public void updateItems(boolean add) {
        new FetchItemsTask(QueryPreference.getStoredQuery(requireActivity()), add).execute(new Integer[]{Integer.valueOf(this.mPageNo)});
    }

    public static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mThumbNailDownloader.clearQueue();
    }

    public void onDestroy() {
        super.onDestroy();
        this.mThumbNailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }
}
