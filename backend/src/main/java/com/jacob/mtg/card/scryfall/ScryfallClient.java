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

        BulkDataInfo info = ScryfallJson.MAPPER.readValue(response.body(), BulkDataInfo.BulkDataList.class).data().stream()
            .filter(entry -> properties.bulkType().equals(entry.type()))
            .findFirst()
            .orElseThrow(() -> new IOException("Scryfall has no bulk file of type " + properties.bulkType()));
        if (info.fileUri() == null) {
            throw new IOException("Scryfall bulk entry has no download URL (API format changed?)");
        }
        return info;
    }

    /**
     * Opens the bulk file as a stream. Nothing is written to disk and the whole file is never held
     * in memory: the importer reads one card at a time from this stream.
     */
    public InputStream openBulkFile(BulkDataInfo info) throws IOException, InterruptedException {
        HttpRequest request = request(URI.create(info.fileUri()))
            .setHeader("Accept", "*/*") // a .jsonl.gz file, not an API response
            .timeout(Duration.ofMinutes(30))
            .build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            ensureOk(response.statusCode(), "bulk file");
        }
        return decompressIfGzip(response.body());
    }

    /**
     * The file itself is a .jsonl.gz (gzip is the file format, not a transfer encoding, so the HTTP
     * client will not unpack it). Detect gzip by its magic bytes 1F 8B, so a plain file works too.
     */
    public static InputStream decompressIfGzip(InputStream raw) throws IOException {
        BufferedInputStream in = new BufferedInputStream(raw, 1 << 16);
        in.mark(2);
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        boolean gzip = b1 == 0x1f && b2 == 0x8b;
        return gzip ? new GZIPInputStream(in, 1 << 16) : in;
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
