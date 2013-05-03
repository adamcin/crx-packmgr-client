package net.adamcin.crxpackage.client;

/**
 * The basic service response interface.
 */
public interface ServiceResponse {
    boolean isSuccess();
    String getMessage();
}
