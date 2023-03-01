package superset.client.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Api {
    private static final String LOGIN_API_ENDPOINT = "/api/v1/security/login";
    private static final String CSRF_TOKEN_API_ENDPOINT = "/api/v1/security/csrf_token/";
    private static final String LIST_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/";
    private static final String EXPORT_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/export/";
    private static final String IMPORT_DASHBOARD_API_ENDPOINT = "/api/v1/dashboard/import/";

    public static HttpUriRequest getAuthTokenRequest(String host, int port, String username, String password)
            throws URISyntaxException, UnsupportedEncodingException {
        URI apiUri = buildUri(host, port, LOGIN_API_ENDPOINT, null);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        body.addProperty("provider", "db");
        body.addProperty("refresh", true);
        StringEntity entity = new StringEntity(body.toString());

        HttpUriRequest post = RequestBuilder.post() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType()) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .setEntity(entity) //
                .build();
        return post;
    }

    public static HttpUriRequest getCsrfTokenRequest(String host, int port, String authToken)
            throws URISyntaxException {
        URI apiUri = buildUri(host, port, CSRF_TOKEN_API_ENDPOINT, null);

        HttpUriRequest get = RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
        return get;
    }

    public static HttpUriRequest geListDashboardsRequest(String host, int port, String authToken)
            throws URISyntaxException {
        JsonArray columns = new JsonArray();
        columns.add("dashboard_title");
        columns.add("id");
        JsonObject param = new JsonObject();
        param.add("columns", columns);

        URI apiUri = buildUri(host, port, LIST_DASHBOARD_API_ENDPOINT,
                List.of(Pair.of("q", param.toString())));

        HttpUriRequest get = RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                .build();
        return get;
    }

    public static HttpUriRequest getExportDashboardRequest(String host, int port, String authToken, int dashboardId)
            throws URISyntaxException {
        JsonArray ids = new JsonArray();
        ids.add(dashboardId);

        URI apiUri = buildUri(host, port, EXPORT_DASHBOARD_API_ENDPOINT,
                List.of(Pair.of("q", ids.toString())));

        HttpUriRequest get = RequestBuilder.get() //
                .setUri(apiUri) //
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) //
                .setHeader(HttpHeaders.ACCEPT, ContentType.TEXT_PLAIN.getMimeType()) //
                .build();
        return get;
    }

    public static HttpUriRequest getImportDashboardRequest(String host, int port, String authToken,
            String csrfToken, File compressedDashboardFile, boolean isOverride, JsonElement password)
            throws URISyntaxException, IOException {
        URI apiUri = buildUri(host, port, IMPORT_DASHBOARD_API_ENDPOINT, null);

        try (FileInputStream fis = new FileInputStream(compressedDashboardFile)) {
            byte[] arr = new byte[(int) compressedDashboardFile.length()];
            fis.read(arr);

            ContentType contentType = ContentType.create("multipart/form-data", Charset.forName("UTF-8"));
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("formData", arr, ContentType.DEFAULT_BINARY, "dashboard.zip");
            builder.addTextBody("overwrite", String.valueOf(isOverride), contentType);
            builder.addTextBody("passwords", password.getAsString(), contentType);

            HttpUriRequest post = RequestBuilder.post() //
                    .setUri(apiUri) //
                    .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken) //
                    .setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()) //
                    .setHeader("X-CSRF-Token", csrfToken) //
                    .setEntity(builder.build()) //
                    .build();
            return post;
        }

    }

    private static URI buildUri(String host, int port, String endpoint, List<Pair<String, String>> params)
            throws URISyntaxException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost(host).setPort(port).setPath(endpoint);

        if (!CollectionUtils.isEmpty(params)) {
            params.stream().forEach(p -> {
                builder.addParameter(p.getKey(), p.getValue());
            });
        }

        return builder.build();
    }
}
