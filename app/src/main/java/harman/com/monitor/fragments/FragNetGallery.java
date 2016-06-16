package harman.com.monitor.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import harman.com.R;
import harman.com.Recorder;
import harman.com.toolbarslidingtab.SlidingTabLayout;
import harman.com.util.adapter.MyFragPagerAdapter;
import harman.com.util.proxy.HashMapProxy;
import harman.com.util.widget.CustomViewPager;

;

/**
 * Created by root on 16-6-12.
 */
public class FragNetGallery extends Fragment implements ViewPager.OnPageChangeListener{
    @Bind(R.id.frg_d_tbar)
    SlidingTabLayout slidingTabLayout;
    @Bind(R.id.frg_d_custom_vp)
    CustomViewPager viewPager;

    private ArrayList<Fragment> fragments;
    MyFragPagerAdapter vpAdapter;
    public FragServer fragServer;
    public FragDown fragDown;
    ArrayList<Map<String, Object>> adaptParam;
    Recorder recorder;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_netgallery, container, false);
        ButterKnife.bind(this, view);

        viewPager.setOffscreenPageLimit(fragments.size());
        viewPager.setAdapter(vpAdapter);
        viewPager.setScanScroll(false);
        slidingTabLayout.setCustomTabView(R.layout.toolbaritem, 0, R.id.toolbar_txt, R.id.toolbar_under);
        slidingTabLayout.setViewPager(viewPager, recorder.mWidth);
        slidingTabLayout.setOnPageChangeListener(this);
        return view;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        recorder = (Recorder) getActivity();
        fragments = new ArrayList<Fragment>();
        fragments.add(fragServer = new FragServer());
        fragments.add(fragDown = new FragDown());
        adaptParam = new ArrayList<Map<String, Object>>();
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Server"));
        adaptParam.add(new HashMapProxy<String, Object>().putObject("title", "Down"));
        vpAdapter = new MyFragPagerAdapter(recorder.getSupportFragmentManager(), fragments, adaptParam);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
