package com.eyesecurity.api;

import com.eyesecurity.common.IngestionRequest;
import com.eyesecurity.common.IngestionResponse;
import com.eyesecurity.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestionResponse> ingest(@RequestBody IngestionRequest request) {
        return ResponseEntity.ok(ingestionService.ingest(request.records()));
    }
}
