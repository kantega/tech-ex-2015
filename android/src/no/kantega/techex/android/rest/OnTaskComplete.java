package no.kantega.techex.android.rest;

/**
 * Interface for Activities to have a callback method that the asynchronous REST tasks can call on completion
 */
public interface OnTaskComplete<T> {
    public void onTaskComplete(T result);
}
