package com.example.yangxing.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import java.util.ArrayList;
import java.util.List;

import static android.support.v4.content.res.ResourcesCompat.getDrawable;

/**
 * Created by yangxing on 2016/9/5.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG="PhotoGalleryFragment";
    private RecyclerView mRecyclerView;
    private List<GalleryItems> mItems=new ArrayList<>();
    private ThumbnailDownloader<PhotoHodler> mThumbnailDownloader;
    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updataItems();

        Handler responseHandler=new Handler();
        mThumbnailDownloader=new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHodler>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHodler photoHodler, Bitmap bitmap) {
                        Drawable drawable=new BitmapDrawable(getResources(),bitmap);
                        photoHodler.bindDrawable(drawable);

                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "后台线程已经启动");
    }
    private class PhotoHodler extends RecyclerView.ViewHolder{
        private ImageView mImageView;
        public PhotoHodler(View itemView) {
            super(itemView);
            mImageView= (ImageView) itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
        }
        public void bindDrawable(Drawable drawable){
            mImageView.setImageDrawable(drawable);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHodler>{
        private List<GalleryItems> mGalleryItems;

        public PhotoAdapter(List<GalleryItems> galleryItems){
            mGalleryItems=galleryItems;
        }

        @Override
        public PhotoHodler onCreateViewHolder(ViewGroup parent, int viewType) {
           LayoutInflater inflater=LayoutInflater.from(getActivity());
           View view = inflater.inflate(R.layout.gallery_item,parent,false);
            return new PhotoHodler(view);
        }

        @Override
        public void onBindViewHolder(PhotoHodler holder, int position) {
            GalleryItems galleryItems=mGalleryItems.get(position);
            Drawable placeholder=getDrawable(getResources(),R.drawable.moon,null);
            holder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumnail(holder,galleryItems.getUrl());

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItems>>{
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery=query;
        }
        @Override
        protected List<GalleryItems> doInBackground(Void... voids) {

            if (mQuery==null){
                return new FlickrFetchr().fetchRecentPhotos();
            }
            return  new FlickrFetchr().searchPhotos(mQuery);
        }

        @Override
        protected void onPostExecute(List<GalleryItems> items) {
            mItems=items;
            setupAdapter();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mRecyclerView= (RecyclerView) v.findViewById
                (R.id.fragment_photo_gallery_recycle_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        setupAdapter();
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "后台线程已经结束");
    }
    //引用工具栏
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);
        MenuItem searchItem=menu.findItem(R.id.menu_item_search);
        final SearchView searchView= (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: "+query);
                QueryPreferences.setStoredQuery(getActivity(),query);
                updataItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: "+newText);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query=QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                //确保显示最新的搜索结果
                updataItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void updataItems() {
        String query=QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    public void setupAdapter(){
        if(isAdded()){
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
}
