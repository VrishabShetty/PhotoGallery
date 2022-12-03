package com.example.photogallery;

import androidx.fragment.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    /* access modifiers changed from: protected */
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
