package no.kantega.techex.android.display;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import no.kantega.techex.android.R;
import no.kantega.techex.android.data.Achievement;
import no.kantega.techex.android.data.Quest;

import java.util.List;

/**
 * Adapter for displaying list of quests
 *
 * TODO ViewHandler for better performance
 * TODO ClickHandler on title
 * TODO Click handling on quest item
 */
public class QuestArrayAdapter extends ArrayAdapter<Quest> {

    private final String TAG = QuestArrayAdapter.class.getSimpleName();

    /**
     *
     * @param context Activity context
     * @param objects List objects
     */
    public QuestArrayAdapter(Context context, List<Quest> objects) {
        super(context, R.layout.quest_list_row, objects);
    }

    /**
     * Custom view generation for displaying a Quest item in the list
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Item to display
        Quest quest = getItem(position);

        ViewHolder viewHolder; //cache
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.quest_list_row, parent, false);
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.quest_title);
            viewHolder.llBadges = (LinearLayout)convertView.findViewById(R.id.badge_list);
            convertView.setTag(viewHolder);
        } else {
            //View is generated based on a cached (already built) row
            viewHolder = (ViewHolder)convertView.getTag();
            viewHolder.llBadges.removeAllViews(); //Removing all children because view needs to be dynamically generated
        }

        //Updates
        viewHolder.tvTitle.setText(quest.getTitle());

        //Calc padding
        int padding_in_dp = 2;  // TODO get from config
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

        //Iterate through achievements and display badge according to fulfillment
        for (Achievement a : quest.getAchievements()) {
            ImageView iv = new ImageView(getContext());
            if (a.isAchieved()) {
                iv.setImageResource(R.drawable.quest_achievement);
            } else {
                iv.setImageResource(R.drawable.quest_no_achievement);
            }
            iv.setPadding(0,0,padding_in_px,0);
           viewHolder.llBadges.addView(iv);
        }

        return convertView;
    }

    class ViewHolder {
        TextView tvTitle;
        LinearLayout llBadges;
    }
}
