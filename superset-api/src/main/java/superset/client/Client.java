package superset.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import lombok.AllArgsConstructor;
import lombok.Data;
import superset.client.api.Api;
import superset.client.exception.UnexceptedResponseException;

public class Client {

    private String host;
    private int port;
    private String authToken;
    private String csrfToken;
    private CloseableHttpClient client;

    public Client(String host, int port, String username, String password) throws Exception {
        this(host, port, username, password, null);
    }

    public Client(String host, int port, String username, String password, CloseableHttpClient client)
            throws Exception {
        if (client == null) {
            this.client = HttpClientBuilder.create().build();
        } else {
            this.client = client;
        }
        HttpUriRequest request = Api.getAuthTokenRequest(host, port, username, password);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        String token = respBody.getAsJsonObject().get("access_token").getAsString();
        this.authToken = token;
        this.host = host;
        this.port = port;
    }

    public JsonElement dashboards()
            throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.geListDashboardsRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        return JsonParser.parseString(resp.getBody());
    }

    public File exportDashboard(int dashboardId, File destination)
            throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.getExportDashboardRequest(host, port, authToken, dashboardId);

        File downloadFile = client.execute(request, new FileDownloadResponseHandler(destination));
        return downloadFile;
    }

    public void importDashboard(File dashboardFile, JsonElement password, boolean override)
            throws ClientProtocolException, URISyntaxException, IOException {
        csrf();
        HttpUriRequest request = Api.getImportDashboardRequest(host, port, authToken, csrfToken, dashboardFile,
                override, password);
        executeRequest(request);
    }

    private void csrf() throws URISyntaxException, ClientProtocolException, IOException {
        HttpUriRequest request = Api.getCsrfTokenRequest(host, port, authToken);
        ApiResponse resp = executeRequest(request);
        JsonElement respBody = JsonParser.parseString(resp.getBody());
        this.csrfToken = respBody.getAsJsonObject().get("result").getAsString();
    }

    private ApiResponse executeRequest(HttpUriRequest request)
            throws ClientProtocolException, IOException, UnexceptedResponseException {
        try (CloseableHttpResponse response = client.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (code >= 300 || code < 200) {
                throw new UnexceptedResponseException(request.getURI().toString(), code, bodyAsString);
            }
            return new ApiResponse(code, bodyAsString);
        }
    }

    @AllArgsConstructor
    private static class FileDownloadResponseHandler implements ResponseHandler<File> {
        private File target;

        @Override
        public File handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            int code = response.getStatusLine().getStatusCode();
            if (code != 200) {
                throw new UnexceptedResponseException("", code, EntityUtils.toString(response.getEntity()));
            }
            try (InputStream source = response.getEntity().getContent()) {
                FileUtils.copyInputStreamToFile(source, this.target);
            }
            return this.target;
        }
    }

    @AllArgsConstructor
    @Data
    public static class ApiResponse {
        private int code;
        private String body;
    }

}
