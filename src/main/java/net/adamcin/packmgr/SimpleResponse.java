package net.adamcin.packmgr;

public interface SimpleResponse {
    boolean isSuccess();
    String getMessage();
    String getPath();
}
