package net.adamcin.packmgr.async;

import com.ning.http.client.*;
import com.ning.http.multipart.FilePart;
import net.adamcin.packmgr.ACHandling;
import net.adamcin.packmgr.AbstractPackmgrClient;
import net.adamcin.packmgr.PackId;
import net.adamcin.packmgr.SimpleResponse;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AsyncPackmgrClient extends AbstractPackmgrClient {

    public static final Realm DEFAULT_REALM = new Realm.RealmBuilder()
            .setPrincipal(DEFAULT_USERNAME)
            .setPassword(DEFAULT_PASSWORD)
            .setUsePreemptiveAuth(true)
            .setScheme(Realm.AuthScheme.BASIC)
            .build();


    private static final AsyncCompletionHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new AsyncCompletionHandler<SimpleResponse>() {
                @Override public SimpleResponse onCompleted(Response response) throws Exception {
                    return AbstractPackmgrClient.parseSimpleResponse(
                            response.getStatusCode(),
                            response.getStatusText(),
                            response.getResponseBodyAsStream(),
                            getResponseEncoding(response));
                }
            };

    private final AsyncCompletionHandler<Response> AUTHORIZED_RESPONSE_HANDLER =
            new AuthorizedResponseHandler<Response>() {
                @Override protected Response onAuthorized(Response response) throws Exception {
                    return response;
                }
            };

    private final AsyncHttpClient client;

    private Realm realm = DEFAULT_REALM;

    public AsyncPackmgrClient() {
        this(new AsyncHttpClient());
    }

    public AsyncPackmgrClient(final AsyncHttpClient client) {
        if (client == null) {
            throw new NullPointerException("client cannot be null");
        }
        this.client = client;
    }

    public AsyncHttpClient getClient() {
        return this.client;
    }

    public void setBasicCredentials(final String username, final String password) {
        this.setRealm(new Realm.RealmBuilder()
                .setPrincipal(username)
                .setPassword(password)
                .setUsePreemptiveAuth(true)
                .setScheme(Realm.AuthScheme.BASIC)
                .build());
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    private ListenableFuture<Response> executeRequest(Request request) throws IOException {
        return this.client.executeRequest(request, AUTHORIZED_RESPONSE_HANDLER);
    }

    private ListenableFuture<SimpleResponse> executeSimpleRequest(Request request) throws IOException {
        return this.client.executeRequest(request, SIMPLE_RESPONSE_HANDLER);
    }

    /**
     * @inheritDoc
     */
    protected final Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                                  final long timeoutRemaining) {
        final Request request = new RequestBuilder("GET").setUrl(getJsonUrl()).setRealm(realm).build();

        try {
            final ListenableFuture<Response> future = executeRequest(request);

            Response response = null;
            if (checkTimeout) {
                response = future.get(timeoutRemaining, TimeUnit.MILLISECONDS);
            } else {
                response = future.get();
            }

            return right(Exception.class, response.getStatusCode() == 405);
        } catch (TimeoutException e) {
            return left(new IOException("Service timeout exceeded."), Boolean.class);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private RequestBuilder buildSimpleRequest(PackId packageId) {
        RequestBuilder builder = new RequestBuilder("POST").setRealm(realm);
        if (packageId != null) {
            return builder.setUrl(getJsonUrl(packageId));
        } else {
            return builder.setUrl(getJsonUrl());
        }
    }

    private static String getResponseEncoding(Response response) {
        String encoding = response.getHeader("Content-Encoding");

        if (encoding == null) {
            String contentType = response.getContentType();
            int charsetBegin = contentType.toLowerCase().indexOf(";charset=");
            if (charsetBegin >= 0) {
                encoding = contentType.substring(charsetBegin + ";charset=".length());
            }
        }

        return encoding;
    }

    @Override
    public boolean existsOnServer(final PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId).addParameter(KEY_CMD, CMD_CONTENTS).build();

        return executeSimpleRequest(request).get().isSuccess();
    }

    @Override
    public SimpleResponse upload(final File file, final PackId packageId, final boolean force) throws Exception {
        if (file == null) {
            throw new NullPointerException("file");
        }

        PackId _id = packageId == null ? identify(file) : packageId;

        Request request = buildSimpleRequest(_id)
                .addQueryParameter(KEY_CMD, CMD_UPLOAD)
                .addQueryParameter(KEY_FORCE, Boolean.toString(force))
                .addBodyPart(new FilePart(KEY_PACKAGE, file, MIME_ZIP, null)).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse install(final PackId packageId,
                                  final boolean recursive,
                                  final int autosave,
                                  final ACHandling acHandling) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        RequestBuilder request = buildSimpleRequest(packageId)
                .addParameter(KEY_CMD, CMD_INSTALL)
                .addParameter(KEY_RECURSIVE, Boolean.toString(recursive))
                .addParameter(KEY_AUTOSAVE, Integer.toString(Math.max(autosave, MIN_AUTOSAVE)));

        if (acHandling != null) {
            request.addParameter(KEY_ACHANDLING, acHandling.name().toLowerCase());
        }

        return executeSimpleRequest(request.build()).get();
    }

    @Override
    public SimpleResponse build(final PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId)
                .addParameter(KEY_CMD, CMD_BUILD).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse rewrap(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId)
                .addParameter(KEY_CMD, CMD_REWRAP).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse uninstall(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId).addParameter(KEY_CMD, CMD_UNINSTALL).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse delete(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId).addParameter(KEY_CMD, CMD_DELETE).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse dryRun(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId).addParameter(KEY_CMD, CMD_DRY_RUN).build();

        return executeSimpleRequest(request).get();
    }

    @Override
    public SimpleResponse replicate(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        Request request = buildSimpleRequest(packageId).addParameter(KEY_CMD, CMD_REPLICATE).build();

        return executeSimpleRequest(request).get();
    }

    abstract class AuthorizedResponseHandler<T> extends AsyncCompletionHandler<T> {
        protected abstract T onAuthorized(Response response) throws Exception;

        @Override
        public final T onCompleted(Response response) throws Exception {
            if (response.getStatusCode() == 401) {
                throw new IOException(Integer.toString(response.getStatusCode()) + " " + response.getStatusText());
            } else {
                return onAuthorized(response);
            }
        }

        @Override
        public void onThrowable(Throwable t) {
            AsyncPackmgrClient.this.LOGGER.debug("Caught throwable: {}", t);
        }
    }

}
