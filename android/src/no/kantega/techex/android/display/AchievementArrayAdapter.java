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
 * Created by zsuhor on 23.02.2015.
 */
public class AchievementArrayAdapter extends ArrayAdapter<Achievement> {
    public AchievementArrayAdapter(Context context, List<Achievement> objects) {
        super(context, R.layout.achievement_list_row, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Item to display
        Achievement achievement = getItem(position);

        ViewHolder viewHolder; //cache
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.achievement_list_row, parent, false);
            viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.tvAchievementTitle);
            viewHolder.tvDescription = (TextView) convertView.findViewById(R.id.tvAchievementDesc);
            viewHolder.ivBadge = (ImageView) convertView.findViewById(R.id.achievementBadge);
            convertView.setTag(viewHolder);
        } else {
            //View is generated based on a cached (already built) row
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.tvTitle.setText(achievement.getTitle());
        viewHolder.tvDescription.setText(achievement.getDescription());
        viewHolder.ivBadge.setImageResource(achievement.isAchieved()? R.drawable.badge_achievement : R.drawable.badge_no_achievement);

        return convertView;
    }

    class ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        ImageView ivBadge;
    }
}
