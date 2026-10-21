package com.collabx.codeexecution.service;

import com.collabx.codeexecution.dto.ExecuteCodeRequest;
import com.collabx.codeexecution.dto.ExecutionResultResponse;
import com.collabx.codeexecution.dto.Judge0SubmissionRequest;
import com.collabx.codeexecution.dto.Judge0SubmissionResponse;
import com.collabx.common.exception.BadRequestException;
import com.collabx.config.Judge0Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class Judge0Service {

    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);

    private static final Map<String, Integer> LANGUAGE_ID_MAP = new HashMap<>();

    static {
        LANGUAGE_ID_MAP.put("c", 50);
        LANGUAGE_ID_MAP.put("c99", 50);
        LANGUAGE_ID_MAP.put("cpp", 54);
        LANGUAGE_ID_MAP.put("c++", 54);
        LANGUAGE_ID_MAP.put("cpp14", 54);
        LANGUAGE_ID_MAP.put("cpp17", 54);
        LANGUAGE_ID_MAP.put("cpp20", 54);
        LANGUAGE_ID_MAP.put("java", 62);
        LANGUAGE_ID_MAP.put("javascript", 63);
        LANGUAGE_ID_MAP.put("js", 63);
        LANGUAGE_ID_MAP.put("node", 63);
        LANGUAGE_ID_MAP.put("nodejs", 63);
        LANGUAGE_ID_MAP.put("python", 71);
        LANGUAGE_ID_MAP.put("py", 71);
        LANGUAGE_ID_MAP.put("python3", 71);
        LANGUAGE_ID_MAP.put("typescript", 74);
        LANGUAGE_ID_MAP.put("ts", 74);
        LANGUAGE_ID_MAP.put("sql", 82);
        LANGUAGE_ID_MAP.put("postgresql", 82);
    }

    private final Judge0Properties properties;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public Judge0Service(Judge0Properties properties,
                         RestTemplateBuilder restTemplateBuilder,
                         SimpMessagingTemplate messagingTemplate) {
        this.properties = properties;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getFetchTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getFetchTimeoutMs()))
                .build();
    }

    public ExecutionResultResponse executeCode(ExecuteCodeRequest request) {
        String language = request.getLanguage() != null ? request.getLanguage().trim().toLowerCase() : "";
        String code = request.getCode() != null ? request.getCode() : "";
        String roomId = request.getRoomId();

        // Broadcast 'running' status to room if roomId is present
        if (roomId != null && !roomId.trim().isEmpty()) {
            Map<String, Object> runningUpdate = new HashMap<>();
            runningUpdate.put("roomId", roomId);
            runningUpdate.put("status", "running");
            runningUpdate.put("language", language);
            runningUpdate.put("running", true);
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".execution", runningUpdate);
        }

        // HTML is processed directly for browser live-preview
        if (isHtmlLanguage(language)) {
            ExecutionResultResponse response = ExecutionResultResponse.htmlPreview(code);
            response.setRoomId(roomId);
            response.setLanguage("html");
            broadcastExecutionResult(roomId, response);
            return response;
        }

        try {
            int languageId = resolveLanguageId(request);
            ExecutionResultResponse result = runViaJudge0(code, languageId, request.getStdin());
            result.setRoomId(roomId);
            result.setLanguage(language);
            broadcastExecutionResult(roomId, result);
            return result;
        } catch (Exception ex) {
            log.error("Execution error via Judge0: {}", ex.getMessage(), ex);
            ExecutionResultResponse errorResult = ExecutionResultResponse.error(
                    "Error",
                    ex.getMessage(),
                    null,
                    "Code execution service encountered an error: " + ex.getMessage()
            );
            errorResult.setRoomId(roomId);
            errorResult.setLanguage(language);
            broadcastExecutionResult(roomId, errorResult);
            return errorResult;
        }
    }

    private ExecutionResultResponse runViaJudge0(String code, int languageId, String stdin) {
        String apiUrl = properties.getApiUrl().replaceAll("/+$", "");
        String submitUrl = apiUrl + "/submissions?base64_encoded=false&wait=false";

        Judge0SubmissionRequest submissionRequest = new Judge0SubmissionRequest(code, languageId, stdin != null ? stdin : "");
        HttpHeaders headers = createHeaders();
        HttpEntity<Judge0SubmissionRequest> entity = new HttpEntity<>(submissionRequest, headers);

        ResponseEntity<Judge0SubmissionResponse> submitResponse;
        try {
            submitResponse = restTemplate.postForEntity(submitUrl, entity, Judge0SubmissionResponse.class);
        } catch (RestClientException ex) {
            log.error("Failed to submit code to Judge0 at {}: {}", submitUrl, ex.getMessage());
            throw new RuntimeException("Could not connect to code execution service: " + ex.getMessage(), ex);
        }

        Judge0SubmissionResponse submitBody = submitResponse.getBody();
        if (submitBody == null || submitBody.getToken() == null) {
            throw new RuntimeException("Code execution service did not return a submission token");
        }

        String token = submitBody.getToken();
        log.debug("Judge0 submission token: {}", token);

        // Polling Judge0
        String pollUrl = apiUrl + "/submissions/" + token + "?base64_encoded=false";
        Judge0SubmissionResponse finalResult = null;

        for (int attempt = 1; attempt <= properties.getMaxPollAttempts(); attempt++) {
            try {
                Thread.sleep(properties.getPollIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Execution polling interrupted", e);
            }

            try {
                HttpEntity<Void> pollEntity = new HttpEntity<>(headers);
                ResponseEntity<Judge0SubmissionResponse> pollResponse =
                        restTemplate.exchange(pollUrl, HttpMethod.GET, pollEntity, Judge0SubmissionResponse.class);
                Judge0SubmissionResponse pollBody = pollResponse.getBody();

                if (pollBody != null && pollBody.getStatus() != null) {
                    int statusId = pollBody.getStatus().getId();
                    // statusId > 2 means finished (1: In Queue, 2: Processing, 3+: Finished/Accepted/Error)
                    if (statusId > 2) {
                        finalResult = pollBody;
                        break;
                    }
                }
            } catch (RestClientException ex) {
                log.warn("Judge0 poll attempt {} failed: {}", attempt, ex.getMessage());
            }
        }

        if (finalResult == null) {
            return ExecutionResultResponse.error(
                    "Timeout",
                    "Execution timed out while waiting for result",
                    null,
                    "Execution exceeded the maximum polling limit"
            );
        }

        int statusId = finalResult.getStatus() != null ? finalResult.getStatus().getId() : 0;
        String statusDesc = finalResult.getStatus() != null ? finalResult.getStatus().getDescription() : "Unknown";

        if (statusId == 3) {
            // Accepted / Successful execution
            return ExecutionResultResponse.success(
                    finalResult.getStdout(),
                    finalResult.getTime(),
                    finalResult.getMemory(),
                    "judge0"
            );
        } else {
            // Compile Error or Runtime Error
            return ExecutionResultResponse.error(
                    statusDesc,
                    finalResult.getStderr(),
                    finalResult.getCompileOutput(),
                    finalResult.getMessage()
            );
        }
    }

    private void broadcastExecutionResult(String roomId, ExecutionResultResponse response) {
        if (roomId != null && !roomId.trim().isEmpty()) {
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".execution", response);
        }
    }

    private boolean isHtmlLanguage(String language) {
        return "html".equalsIgnoreCase(language) || "htm".equalsIgnoreCase(language) || "htmlmixed".equalsIgnoreCase(language);
    }

    private int resolveLanguageId(ExecuteCodeRequest request) {
        if (request.getLanguageId() != null && request.getLanguageId() > 0) {
            return request.getLanguageId();
        }
        String lang = request.getLanguage() != null ? request.getLanguage().trim().toLowerCase() : "";
        Integer mapped = LANGUAGE_ID_MAP.get(lang);
        if (mapped == null) {
            throw new BadRequestException("Unsupported programming language for execution: " + request.getLanguage());
        }
        return mapped;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = properties.getApiKey();
        String apiHost = properties.getApiHost();

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.set("X-RapidAPI-Key", apiKey.trim());
            headers.set("X-Auth-Token", apiKey.trim());
        }
        if (apiHost != null && !apiHost.trim().isEmpty()) {
            headers.set("X-RapidAPI-Host", apiHost.trim());
        }

        return headers;
    }
}
