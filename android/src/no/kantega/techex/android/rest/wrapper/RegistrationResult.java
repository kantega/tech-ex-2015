package no.kantega.techex.android.rest.wrapper;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for the results of registration through REST
 */
public class RegistrationResult {
    private RegistrationResultStatus resultStatus;
    private String nickname;
    private String id;
    private Map<String,String> preferences;
    private List<String> quests;

    public RegistrationResult() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RegistrationResultStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(RegistrationResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Map<String, String> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public List<String> getQuests() {
        return quests;
    }

    public void setQuests(List<String> quests) {
        this.quests = quests;
    }
}
