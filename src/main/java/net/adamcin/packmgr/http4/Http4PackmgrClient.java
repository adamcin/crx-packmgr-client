package net.adamcin.packmgr.http4;

import net.adamcin.packmgr.ACHandling;
import net.adamcin.packmgr.AbstractPackmgrClient;
import net.adamcin.packmgr.PackId;
import net.adamcin.packmgr.SimpleResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public final class Http4PackmgrClient extends AbstractPackmgrClient {

    public static final UsernamePasswordCredentials DEFAULT_CREDENTIALS =
            new UsernamePasswordCredentials(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    private static final ResponseHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new ResponseHandler<SimpleResponse>() {
                @Override public SimpleResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    StatusLine statusLine = response.getStatusLine();
                    return parseSimpleResponse(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase(),
                            response.getEntity().getContent(),
                            getResponseEncoding(response));
                }
            };

    private static final ResponseHandler<HttpResponse> AUTHORIZED_RESPONSE_HANDLER =
            new ResponseHandler<HttpResponse>() {
                @Override public HttpResponse handleResponse(final HttpResponse response)
                        throws ClientProtocolException, IOException {
                    if (response.getStatusLine().getStatusCode() == 401) {
                        throw new IOException("401 Unauthorized");
                    } else {
                        return response;
                    }
                }
            };

    private final AbstractHttpClient client;
    private HttpContext httpContext = new BasicHttpContext();
    private final AuthCache preemptAuthCache = new BasicAuthCache();

    public Http4PackmgrClient() {
        this(new DefaultHttpClient());
        getClient().getCredentialsProvider().setCredentials(AuthScope.ANY, DEFAULT_CREDENTIALS);
        httpContext.setAttribute(ClientContext.AUTH_CACHE, preemptAuthCache);
        try {
            preemptAuthCache.put(URIUtils.extractHost(new URI(DEFAULT_BASE_URL)), new BasicScheme());
        } catch (URISyntaxException e) {
            // shouldn't happen since we are parsing a valid constant, right?
        }
    }

    public Http4PackmgrClient(AbstractHttpClient client) {
        this.client = client;
    }

    public AbstractHttpClient getClient() {
        return client;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    private static String getResponseEncoding(HttpResponse response) {
        Header encoding = response.getFirstHeader("Content-Encoding");

        if (encoding != null) {
            return encoding.getValue();
        } else {
            Header contentType = response.getFirstHeader("Content-Type");
            if (contentType != null) {
                String _contentType = contentType.getValue();
                int charsetBegin = _contentType.toLowerCase().indexOf(";charset=");
                if (charsetBegin >= 0) {
                    return _contentType.substring(charsetBegin + ";charset=".length());
                }
            }
        }

        return "UTF-8";
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        super.setBaseUrl(baseUrl);
        try {
            this.preemptAuthCache.put(URIUtils.extractHost(new URI(baseUrl)), new BasicScheme());
        } catch (URISyntaxException e) {
            LOGGER.warn("[setBaseUrl] failed to parse URL for setup of preemptive authentication", e);
        }
    }

    public void setBasicCredentials(String username, String password) {
        getClient().getCredentialsProvider().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    }

    @Override
    protected Either<? extends Exception, Boolean> checkServiceAvailability(boolean checkTimeout,
                                                                            long timeoutRemaining) {
        HttpUriRequest request = new HttpGet(getJsonUrl());

        if (checkTimeout) {
            HttpConnectionParams.setConnectionTimeout(request.getParams(), (int) timeoutRemaining);
            HttpConnectionParams.setSoTimeout(request.getParams(), (int) timeoutRemaining);
        }

        try {
            HttpResponse response = getClient().execute(request, AUTHORIZED_RESPONSE_HANDLER, getHttpContext());
            return right(Exception.class, response.getStatusLine().getStatusCode() == 405);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private SimpleResponse executeSimpleRequest(HttpUriRequest request) throws Exception {
        return getClient().execute(request, SIMPLE_RESPONSE_HANDLER, getHttpContext());
    }

    @Override
    public boolean existsOnServer(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_CONTENTS));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request).isSuccess();
    }

    @Override
    public SimpleResponse upload(final File file, final PackId packageId, final boolean force) throws Exception {
        if (file == null) {
            throw new NullPointerException("file");
        }

        PackId _id = packageId == null ? identify(file) : packageId;

        HttpPost request = new HttpPost(getJsonUrl(_id) +
                "?" + KEY_CMD + "=" + CMD_UPLOAD +
                "&" + KEY_FORCE + "=" + Boolean.toString(force));

        MultipartEntity entity = new MultipartEntity();

        entity.addPart(KEY_PACKAGE, new FileBody(file, MIME_ZIP));

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse install(final PackId packageId,
                                  final boolean recursive,
                                  final int autosave,
                                  final ACHandling acHandling) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_INSTALL));
        pairs.add(new BasicNameValuePair(KEY_RECURSIVE, Boolean.toString(recursive)));
        pairs.add(new BasicNameValuePair(KEY_AUTOSAVE, Integer.toString(Math.max(MIN_AUTOSAVE, autosave))));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse build(final PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_BUILD));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse rewrap(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_REWRAP));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse uninstall(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_UNINSTALL));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse delete(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_DELETE));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse dryRun(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_DRY_RUN));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }

    @Override
    public SimpleResponse replicate(PackId packageId) throws Exception {
        if (packageId == null) {
            throw new NullPointerException("packageId");
        }

        HttpPost request = new HttpPost(getJsonUrl(packageId));

        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();

        pairs.add(new BasicNameValuePair(KEY_CMD, CMD_REPLICATE));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);

        request.setEntity(entity);

        return executeSimpleRequest(request);
    }
}
