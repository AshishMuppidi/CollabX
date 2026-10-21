package com.collabx.codeexecution.controller;

import com.collabx.codeexecution.dto.ExecuteCodeRequest;
import com.collabx.codeexecution.dto.ExecutionResultResponse;
import com.collabx.codeexecution.service.Judge0Service;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeExecutionController {

    private final Judge0Service judge0Service;

    public CodeExecutionController(Judge0Service judge0Service) {
        this.judge0Service = judge0Service;
    }

    @PostMapping({"/api/execute", "/run"})
    public ResponseEntity<ExecutionResultResponse> execute(@Valid @RequestBody ExecuteCodeRequest request) {
        ExecutionResultResponse result = judge0Service.executeCode(request);
        return ResponseEntity.ok(result);
    }
}
