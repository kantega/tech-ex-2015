package no.kantega.techex.android.data;

/**
 * Visibility of Quest
 * //TODO what types are there?
 */
public enum Visibility {
    PUBLIC("public"), PRIVATE("private"),UNDEFINED("undefined");

    /**
     * Identification used in JSON
     */
    private String id;

    Visibility(String id) {
        this.id = id;
    }

    public static Visibility getType(String id) {
        for (Visibility qv : values()) {
            if (qv.id.equalsIgnoreCase(id))
                return qv;
        }
        //If not found by id
        return UNDEFINED;
    }

    public String getId() {
        return id;
    }
}