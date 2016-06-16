package harman.com.monitor.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import harman.com.R;
import harman.com.Recorder;
import harman.com.cloud.Cloud;
import harman.com.cloud.Response;
import harman.com.monitor.adapters.GalleryViewAdapter;
import harman.com.monitor.models.DownRecord;
import harman.com.monitor.models.FileModel;
import harman.com.monitor.models.VideoItem;
import harman.com.videocapture.VideoFile;
import io.realm.Realm;
import io.realm.RealmResults;

public class FragDown extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    Recorder recorder;


    private static final String LAST_REFRESH_TIME = "com.harman.ctg.roadrecord.lastrefresh";
    @Bind(R.id.video_down_gv)
    GridView gallery;
    private Cloud cloud;
    private ArrayList<VideoItem> galleryItems = new ArrayList<VideoItem>();
    private GalleryViewAdapter galleryAdapter;
    private Realm realm;

    private ActionMode actionMenu = null;
    private VideoItem itemLongClicked = null;
    private int itemPositionLongClicked = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_down, container, false);
        ButterKnife.bind(this, view);

        if (recorder.myApp.recService != null)
            realm = recorder.myApp.recService.realm;
        galleryAdapter = new GalleryViewAdapter(recorder, galleryItems);
        gallery.setAdapter(galleryAdapter);
        gallery.setOnItemClickListener(this);
        gallery.setOnItemLongClickListener(this);

        initializeGallary();
        if (recorder.myApp.recService == null)
            ;
        else
            cloud = recorder.myApp.recService.cloud;
        return view;
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        recorder = (Recorder) getActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
//        realm.close();
    }

    public void addVideoItem(VideoItem vdoItm){
        if(galleryItems.contains(vdoItm))
            return;
        else {
            //galleryItems.add(vdoItm);
            galleryAdapter.add(vdoItm);
        }
    }

    public void updateGallery(VideoFile vf) {
        galleryAdapter.add(new VideoItem(vf.getFullPath(), null, vf.getDate(), vf.getLocked(), false));
//        galleryAdapter.notifyDataSetChanged();
    }

    public void initializeGallary() {
        galleryItems.clear();

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "down");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                });

                for (File file : files) {
                    String url = file.getAbsolutePath();

                    galleryItems.add(new VideoItem(url, null, new Date(file.lastModified()), false, false));
                }
            }
        }

        galleryAdapter.notifyDataSetChanged();


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        VideoItem item = (VideoItem) parent.getItemAtPosition(position);
        File file = new File(item.url);

        // replay with intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "video/*");
        startActivity(intent);
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        itemLongClicked = (VideoItem) parent.getItemAtPosition(position);
        itemPositionLongClicked = position;

        if (actionMenu == null) {
            actionMenu = recorder.startActionMode(actionMode);
        }

        return true;
    }

    //////////////////////////////////////////////////////////////////
    private ActionMode.Callback actionMode = new ActionMode.Callback() {
        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("");
            mode.getMenuInflater().inflate(R.menu.menu_popup, menu);
            return true;
        }

        // Called each time the action mode is shown.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (itemLongClicked != null) {
                mode.getMenu().getItem(1).setVisible(false);
                return true;
            } else
                return false;  // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_popup_delete:
                    delete();
                    mode.finish(); // Action picked, so close the contextual menu
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMenu = null;
        }
    };

    //////////////////////////////////////////////////////////////////////////////


    private void delete() {
        if (itemLongClicked != null) {
            File file = new File(itemLongClicked.url);
            boolean deleted = file.delete();
            if (deleted) {
                realm.beginTransaction();
                RealmResults<FileModel> r = realm.where(FileModel.class).equalTo("filename", file.getName()).findAll();
                if (r.size() > 0) {
                    r.first().removeFromRealm();
                }
                realm.commitTransaction();

                MemoryCacheUtils.removeFromCache(Uri.fromFile(new File(itemLongClicked.url)).toString(), ImageLoader.getInstance().getMemoryCache());

                galleryItems.remove(itemPositionLongClicked);
                galleryAdapter.notifyDataSetChanged();
            }
        }

        itemLongClicked = null;
    }


}