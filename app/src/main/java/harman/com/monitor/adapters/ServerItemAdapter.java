package harman.com.monitor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import harman.com.R;
import harman.com.monitor.models.ServerItem;

/**
 * Created by root on 16-6-12.
 */
public class ServerItemAdapter extends ArrayAdapter<ServerItem> {
    public ServerItemAdapter(Context context, int resource) {
        super(context, resource);
    }

    public ServerItemAdapter(Context context, ArrayList<ServerItem> items) {
        super(context, R.layout.video_grid_item, items);

    }

    void ServerItemAdapter() {

    }

    public void refreshItemList(){

    }
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.server_record_item, parent, false);

            // initialize the view holder
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            // recycle the already inflated view
            holder = (ViewHolder) view.getTag();
        }
        final ServerItem item = getItem(position);
        holder.recordName.setText(item.record_name);
        holder.downSizeLen.setText(Integer.toString(item.downSize)+"/\n"+Integer.toString(item.recordLen));
        return view;
    }

    static class ViewHolder {
        @Bind(R.id.record_name)
        TextView recordName;
        @Bind(R.id.down_size_len)
        TextView downSizeLen;


        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
