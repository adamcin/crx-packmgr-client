package net.adamcin.packmgr;

import java.util.List;

/**
 *
 */
public interface DetailedResponse extends ServiceResponse {
    long getDuration();
    List<String> getProgressErrors();
}
