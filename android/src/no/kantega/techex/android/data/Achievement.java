package no.kantega.techex.android.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class for storing achievement related data
 */
public class Achievement  implements Parcelable{
    private String id;

    private String title;

    private String description;

    private boolean achieved = false;

    public Achievement() {
        id="";
        title="";
        description="";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public void setAchieved(boolean achieved) {
        this.achieved = achieved;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Achievement(Parcel in) {
        setId(in.readString());
        setTitle(in.readString());
        setDescription(in.readString());
        setAchieved(in.readInt() ==1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(getTitle());
        dest.writeString(getDescription());
        dest.writeInt(isAchieved()?1:0);
    }

    /**
     * For creating list of parcelable
     */
    public static final Parcelable.Creator<Achievement> CREATOR = new Parcelable.Creator<Achievement>(){
        public Achievement createFromParcel(Parcel in) {
            return new Achievement(in);
        }

        public Achievement[] newArray(int size) {
            return new Achievement[size];
        }
    };
}
