package harman.com.monitor.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import harman.com.R;
import harman.com.monitor.models.VideoItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created on 2015-7-20.
 */
public class GalleryViewAdapter extends ArrayAdapter<VideoItem> {
    private DisplayImageOptions options;

    public GalleryViewAdapter(Context context, ArrayList<VideoItem> items) {
        super(context, R.layout.video_grid_item, items);
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.mipmap.thumbnail_placeholder)
                .showImageForEmptyUri(R.mipmap.thumbnail_placeholder)
                .showImageOnFail(R.mipmap.thumbnail_placeholder)
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .considerExifParams(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .defaultDisplayImageOptions(options)
                .build();

        ImageLoader.getInstance().init(config);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if(view == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.video_grid_item, parent, false);

            // initialize the view holder
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            // recycle the already inflated view
            holder = (ViewHolder) view.getTag();
        }

        // update the item view
        final VideoItem item = getItem(position);

        Bitmap vdoCapture = createVideoThumbnail(item.getUrl());
        if(vdoCapture != null){
            holder.thumbnail.setImageBitmap(vdoCapture);
            holder.indicator.setVisibility(View.VISIBLE);
            //holder.lock.setVisibility(item.locked ? View.VISIBLE : View.GONE);
        }
        else{
            holder.indicator.setVisibility(View.GONE);
            //holder.lock.setVisibility(View.GONE);
        }
        if(item.uploaded)
            holder.upload.setVisibility(View.VISIBLE);
        holder.date.setText(item.date.toString());
        return view;
    }

    static class ViewHolder {
        @Bind(R.id.video_grid_thumbnail) ImageView thumbnail;
        @Bind(R.id.video_indicator) ImageView indicator;
        @Bind(R.id.video_date) TextView date;
        @Bind(R.id.video_lock) ImageView lock;
        @Bind(R.id.video_uploaded) ImageView upload;
        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }


    private Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
//            retriever.setMode(MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        } catch(IllegalArgumentException ex) {
// Assume this is a corrupt video file
        } catch (RuntimeException ex) {
// Assume this is a corrupt video file.
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
// Ignore failures while cleaning up.
            }
        }
        return bitmap;
    }
}