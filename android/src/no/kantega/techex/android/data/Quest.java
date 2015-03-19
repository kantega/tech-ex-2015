package no.kantega.techex.android.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for storing quest data
 *
 * Parcelable for transfer between activities
 */
public class Quest implements Parcelable{

    private String id;

    private Visibility visibility;

    private String title;

    private String description;

    private List<Achievement> achievements;

    public Quest() {
        id = "";
        title = "";
        description = "";
        visibility = Visibility.UNDEFINED;
        achievements = new ArrayList<Achievement>();
    }

    public Quest(Parcel in) {
        setId(in.readString());
        setVisibility(in.readString());
        setTitle(in.readString());
        setDescription(in.readString());
        achievements = new ArrayList<Achievement>();
        in.readTypedList(achievements,Achievement.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(getVisibility().getId());
        dest.writeString(getTitle());
        dest.writeString(getDescription());
        dest.writeTypedList(achievements);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public void setVisibility(String name) {
        this.visibility = Visibility.getType(name);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    public void addAchievement(Achievement a) {
        achievements.add(a);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Quest> CREATOR = new Parcelable.Creator<Quest>() {
        public Quest createFromParcel(Parcel in) {
            return new Quest(in);
        }

        public Quest[] newArray(int size) {
            return new Quest[size];
        }
    };

    /**
     * Convenience function for the quest details UI. If this quest has an achievement with the given ID,
     * it will set its status to "achieved".
     * @param achievement
     * @return
     */
    public boolean updateAchievement(String achievement) {
        for (Achievement a : achievements) {
            if (a.getId().equalsIgnoreCase(achievement)) {
                a.setAchieved(true);
                return true;
            }
        }
        return false;
    }


}
