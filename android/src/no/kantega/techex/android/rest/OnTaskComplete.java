package no.kantega.techex.android.rest;

/**
 * Interface for Activities to have a callback method that the REST tasks can call
 */
public interface OnTaskComplete<T> {
    public void onTaskComplete(T result);
}
