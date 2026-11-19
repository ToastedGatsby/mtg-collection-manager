package com.jacob.mtg.card.importer;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jacob.mtg.common.exception.ApiException;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** /api/admin/** requires ROLE_ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/cards/import")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardImportController {

    private final ScryfallImportTrigger trigger;
    private final ImportLogRepository importLog;

    public AdminCardImportController(ScryfallImportTrigger trigger, ImportLogRepository importLog) {
        this.trigger = trigger;
        this.importLog = importLog;
    }

    /** 202 Accepted: the import continues in the background; poll GET for progress. */
    @PostMapping
    public ResponseEntity<Map<String, String>> start(@RequestParam(defaultValue = "false") boolean force) {
        if (!trigger.startAsync(force)) {
            throw ApiException.conflict("An import is already running");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "STARTED"));
    }

    @GetMapping
    public ImportStatus status() {
        return new ImportStatus(trigger.isRunning(), importLog.latest(10));
    }

    public record ImportStatus(boolean running, List<ImportLogRepository.ImportRun> recentRuns) {
    }
}
