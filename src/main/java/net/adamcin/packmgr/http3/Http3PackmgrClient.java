package net.adamcin.packmgr.http3;

import net.adamcin.packmgr.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class Http3PackmgrClient extends AbstractPackmgrClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http3PackmgrClient.class);

    public static final UsernamePasswordCredentials DEFAULT_CREDENTIALS =
            new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    private final HttpClient client;

    public Http3PackmgrClient() {
        this(new HttpClient());
        getClient().getParams().setAuthenticationPreemptive(true);
        getClient().getState().setCredentials(AuthScope.ANY, DEFAULT_CREDENTIALS);
    }

    public Http3PackmgrClient(final HttpClient client) {
        this.client = client;
    }

    public HttpClient getClient() {
        return this.client;
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                            final long timeoutRemaining) {

        final GetMethod request = new GetMethod(getJsonUrl());
        final int oldTimeout = getClient().getHttpConnectionManager().getParams().getConnectionTimeout();
        if (checkTimeout) {
            getClient().getHttpConnectionManager().getParams().setConnectionTimeout((int) timeoutRemaining);
            request.getParams().setSoTimeout((int) timeoutRemaining);
        }

        try {
            int status = getClient().executeMethod(request);

            if (status == 401) {
                throw new IOException("401 Unauthorized");
            } else {
                return right(Exception.class, status == 405);
            }
        } catch (IOException e) {
            return left(e, Boolean.class);
        } finally {
            request.releaseConnection();
            if (checkTimeout) {
                getClient().getHttpConnectionManager().getParams().setConnectionTimeout(oldTimeout);
            }
        }
    }

    public void setBasicCredentials(String username, String password) {
        getClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    private SimpleResponse executeSimpleRequest(final HttpMethodBase request) throws IOException {
        int status = getClient().executeMethod(request);
        return parseSimpleResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet());
    }

    private DetailedResponse executeDetailedRequest(final HttpMethodBase request, final ResponseProgressListener listener) throws IOException {
        int status = getClient().executeMethod(request);
        return parseDetailedResponse(status,
                request.getStatusText(),
                request.getResponseBodyAsStream(),
                request.getResponseCharSet(),
                listener);
    }

    @Override
    public boolean existsOnServer(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getJsonUrl(packageId));
        request.addParameter(KEY_CMD, CMD_CONTENTS);

        try {
            return executeSimpleRequest(request).isSuccess();
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception {
        if (file == null) {
            throw new NullPointerException("file");
        }

        final PackId _id = packageId == null ? identify(file) : packageId;
        final String url = getJsonUrl(_id) +
                "?" + KEY_CMD + "=" + CMD_UPLOAD +
                "&" + KEY_FORCE + "=" + Boolean.toString(force);
        final PostMethod request = new PostMethod(url);
        request.setRequestEntity(
                new MultipartRequestEntity(
                        new Part[]{ new FilePart(KEY_PACKAGE, file, MIME_ZIP, null)}, request.getParams()));
        try {
            return executeSimpleRequest(request);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public DetailedResponse install(final PackId packageId,
                                  final boolean recursive,
                                  final int autosave,
                                  final ACHandling acHandling) throws Exception {
        return this.install(packageId, recursive, autosave, acHandling, null);
    }

    @Override
    public DetailedResponse install(final PackId packageId,
                                    final boolean recursive,
                                    final int autosave,
                                    final ACHandling acHandling,
                                    final ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getHtmlUrl(packageId));

        request.addParameter(KEY_CMD, CMD_INSTALL);
        request.addParameter(KEY_RECURSIVE, Boolean.toString(recursive));
        request.addParameter(KEY_AUTOSAVE, Integer.toString(Math.max(MIN_AUTOSAVE, autosave)));

        if (acHandling != null) {
            request.addParameter(KEY_ACHANDLING, acHandling.name());
        }

        try {
            return executeDetailedRequest(request, listener);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public DetailedResponse build(PackId packageId) throws Exception {
        return this.build(packageId, null);
    }

    @Override
    public DetailedResponse build(final PackId packageId, final ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getHtmlUrl(packageId));

        request.addParameter(KEY_CMD, CMD_BUILD);

        try {
            return executeDetailedRequest(request, listener);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public DetailedResponse rewrap(PackId packageId) throws Exception {
        return this.rewrap(packageId, null);
    }

    @Override
    public DetailedResponse rewrap(final PackId packageId, final ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getHtmlUrl(packageId));

        request.addParameter(KEY_CMD, CMD_REWRAP);

        try {
            return executeDetailedRequest(request, listener);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public DetailedResponse uninstall(final PackId packageId) throws Exception {
        return this.uninstall(packageId, null);
    }

    @Override
    public DetailedResponse uninstall(final PackId packageId, final ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getHtmlUrl(packageId));

        request.addParameter(KEY_CMD, CMD_UNINSTALL);

        try {
            return executeDetailedRequest(request, listener);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public SimpleResponse delete(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getJsonUrl(packageId));

        request.addParameter(KEY_CMD, CMD_DELETE);

        try {
            return executeSimpleRequest(request);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public DetailedResponse dryRun(final PackId packageId) throws Exception {
        return this.dryRun(packageId, null);
    }

    @Override
    public DetailedResponse dryRun(final PackId packageId, final ResponseProgressListener listener) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getHtmlUrl(packageId));

        request.addParameter(KEY_CMD, CMD_DRY_RUN);

        try {
            return executeDetailedRequest(request, listener);
        } finally {
            request.releaseConnection();
        }
    }

    @Override
    public SimpleResponse replicate(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        final PostMethod request = new PostMethod(getJsonUrl(packageId));

        request.addParameter(KEY_CMD, CMD_REPLICATE);

        try {
            return executeSimpleRequest(request);
        } finally {
            request.releaseConnection();
        }
    }
}
