package com.jacob.mtg.card.scryfall;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.GZIPInputStream;

import org.springframework.stereotype.Component;

/**
 * Talks to Scryfall. Their rules: send a descriptive User-Agent and an Accept header,
 * and use the daily bulk files instead of thousands of single-card requests.
 */
@Component
public class ScryfallClient {

    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    private final ScryfallProperties properties;

    public ScryfallClient(ScryfallProperties properties) {
        this.properties = properties;
    }

    /** Metadata for the configured bulk file (download URL + when it was generated). */
    public BulkDataInfo fetchBulkInfo() throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
            request(URI.create(properties.baseUrl() + "/bulk-data")).build(),
            HttpResponse.BodyHandlers.ofString());
        ensureOk(response.statusCode(), "bulk-data list");

        return ScryfallJson.MAPPER.readValue(response.body(), BulkDataInfo.BulkDataList.class).data().stream()
            .filter(info -> properties.bulkType().equals(info.type()))
            .findFirst()
            .orElseThrow(() -> new IOException("Scryfall has no bulk file of type " + properties.bulkType()));
    }

    /**
     * Opens the (large) bulk file as a stream. Nothing is written to disk and the whole file is
     * never held in memory: the importer reads one card at a time from this stream.
     */
    public InputStream openBulkFile(BulkDataInfo info) throws IOException, InterruptedException {
        HttpRequest request = request(URI.create(info.downloadUri()))
            .header("Accept-Encoding", "gzip") // ~5x smaller download
            .timeout(Duration.ofMinutes(30))
            .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            ensureOk(response.statusCode(), "bulk file");
        }
        boolean gzip = response.headers().firstValue("Content-Encoding")
            .map(v -> v.equalsIgnoreCase("gzip"))
            .orElse(false);
        InputStream body = new BufferedInputStream(response.body(), 1 << 16);
        return gzip ? new GZIPInputStream(body, 1 << 16) : body; // java.net.http does not decompress by itself
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri)
            .header("User-Agent", properties.userAgent())
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET();
    }

    private static void ensureOk(int status, String what) throws IOException {
        if (status != 200) {
            throw new IOException("Scryfall " + what + " returned HTTP " + status);
        }
    }
}
