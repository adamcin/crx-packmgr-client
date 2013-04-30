package net.adamcin.packmgr;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * The AbstractPackmgrClient provides constants and concrete implementations for generic method logic and response
 * handling.
 */
public abstract class AbstractPackmgrClient implements PackmgrClient {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final String SERVICE_BASE_PATH = "/crx/packmgr/service";
    public static final String HTML_SERVICE_PATH = SERVICE_BASE_PATH + "/console.html";
    public static final String JSON_SERVICE_PATH = SERVICE_BASE_PATH + "/exec.json";
    public static final String DEFAULT_BASE_URL = "http://localhost:4502";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";
    public static final int MIN_AUTOSAVE = 1024;

    public static final String MIME_ZIP = "application/zip";

    public static final String KEY_CMD = "cmd";
    public static final String KEY_FORCE = "force";
    public static final String KEY_PACKAGE = "package";
    public static final String KEY_SUCCESS = "success";
    public static final String KEY_MESSAGE = "msg";
    public static final String KEY_PATH = "path";
    public static final String KEY_RECURSIVE = "recursive";
    public static final String KEY_AUTOSAVE = "autosave";
    public static final String KEY_ACHANDLING = "acHandling";

    public static final String CMD_CONTENTS = "contents";
    public static final String CMD_INSTALL = "install";
    public static final String CMD_UNINSTALL = "uninstall";
    public static final String CMD_UPLOAD = "upload";
    public static final String CMD_BUILD = "build";
    public static final String CMD_REWRAP = "rewrap";
    public static final String CMD_DRY_RUN = "dryrun";
    public static final String CMD_DELETE = "delete";
    public static final String CMD_REPLICATE = "replicate";

    private String baseUrl = DEFAULT_BASE_URL;

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new NullPointerException("baseUrl");
        }

        String _baseUrl = baseUrl;
        while (_baseUrl.endsWith("/")) {
            _baseUrl = _baseUrl.substring(0, _baseUrl.length() - 1);
        }
        this.baseUrl = _baseUrl;
    }

    public final String getBaseUrl() {
        return this.baseUrl;
    }

    protected final String getHtmlUrl() {
        return getBaseUrl() + HTML_SERVICE_PATH;
    }

    protected final String getHtmlUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getHtmlUrl() + packageId.getInstallationPath() + ".zip";
    }

    protected final String getJsonUrl() {
        return getBaseUrl() + JSON_SERVICE_PATH;
    }

    protected final String getJsonUrl(PackId packageId) {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }
        return getJsonUrl() + packageId.getInstallationPath() + ".zip";
    }

    /**
     * The CRX PackageManagerServlet does not support GET requests. The only use for GET is to check service
     * availability. If anything other than 405 is returned, the service should be considered unavailable.
     * @param checkTimeout set to true to enforce a timeout
     * @param timeoutRemaining remaining timeout in milliseconds
     * @return either a throwable or a boolean
     */
    protected abstract Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout, long timeoutRemaining);

    protected static final SimpleResponse parseSimpleResponse(final int statusCode,
                                                       final String statusText,
                                                       final InputStream stream,
                                                       final String charset)
            throws IOException {
        if (statusCode == 400) {
            throw new IOException("Command not supported by service");
        } else if (statusCode / 100 != 2) {
            throw new IOException(Integer.toString(statusCode) + " " + statusText);
        } else {
            try {
                JSONTokener tokener = new JSONTokener(new InputStreamReader(stream, charset));
                final JSONObject json = new JSONObject(tokener);

                final boolean success = json.has(KEY_SUCCESS) && json.getBoolean(KEY_SUCCESS);
                final String message = json.has(KEY_MESSAGE) ? json.getString(KEY_MESSAGE) : "";
                final String path = json.has(KEY_PATH) ? json.getString(KEY_PATH) : "";

                return new SimpleResponse() {
                    @Override public boolean isSuccess() { return success; }
                    @Override public String getMessage() { return message; }
                    @Override public String getPath() { return path; }
                    @Override public String toString() {
                        return "{success:" + success + ", msg:\"" + message + "\", path:\"" + path + "\"}";
                    }
                };
            } catch (JSONException e) {
                throw new IOException("Exception encountered while parsing response.", e);
            }
        }
    }

    @Override
    public PackId identify(File file) throws IOException {
        return PackId.identifyPackage(file);
    }

    @Override
    public final void waitForService(final long serviceTimeout) throws Exception {
        boolean checkTimeout = serviceTimeout >= 0L;
        int tries = 0;
        final long stop = System.currentTimeMillis() + serviceTimeout;
        Either<? extends Exception, Boolean> resp;
        do {
            if (checkTimeout && stop <= System.currentTimeMillis()) {
                throw new IOException("Service timeout exceeded.");
            }
            Thread.sleep(Math.min(5, tries) * 1000L);
            resp = checkServiceAvailability(checkTimeout, stop - System.currentTimeMillis());
            if (resp.isLeft()) {
                throw resp.getLeft();
            }
            tries++;
        } while (!resp.isLeft() && !resp.getRight());
    }

    protected static abstract class Either<T, U> {
        abstract boolean isLeft();
        T getLeft() { return null; }
        U getRight() { return null; }
    }

    static final class Left<T, U> extends Either<T, U> {
        final T left;

        private Left(final T left) {
            if (left == null) {
                throw new NullPointerException("left");
            }
            this.left = left;
        }

        @Override boolean isLeft() { return true; }
        @Override T getLeft() { return left; }
    }

    static final class Right<T, U> extends Either<T, U> {
        final U right;

        private Right(final U right) {
            if (right == null) {
                throw new NullPointerException("right");
            }
            this.right = right;
        }

        @Override boolean isLeft() { return false; }
        @Override U getRight() { return right; }
    }

    protected static <T, U> Either<T, U> left(T left, Class<U> right) {
        return new Left<T, U>(left);
    }

    protected static <T, U> Either<T, U> right(Class<T> left, U right) {
        return new Right<T, U>(right);
    }
}
