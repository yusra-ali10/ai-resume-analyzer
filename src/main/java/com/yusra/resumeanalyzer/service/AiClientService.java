package com.yusra.resumeanalyzer.service;

import com.yusra.resumeanalyzer.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class AiClientService {

    private final RestClient client;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public AiClientService(@Value("${ai.api-key}") String apiKey,
                           @Value("${ai.url}") String url) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(url)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public AnalysisResult analyze(String resumeText, String jobDescription) {
        String prompt = """
                You are an expert recruiter and ATS specialist.
                Compare the resume with the job description.

                Return ONLY valid JSON, no extra text, no markdown, in exactly this format:
                {
                  "matchScore": <integer 0-100>,
                  "strengths": [<string>, ...],
                  "missingSkills": [<string>, ...],
                  "suggestions": [<string>, ...]
                }

                RESUME:
                %s

                JOB DESCRIPTION:
                %s
                """.formatted(resumeText, jobDescription);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        String response = callWithRetry(body);

        JsonNode root = mapper.readTree(response);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asString();

        // Agar AI ne ```json fences laga diye hon to hata do
        text = text.replaceAll("```json|```", "").trim();

        return mapper.readValue(text, AnalysisResult.class);
    }

    private String callWithRetry(Map<String, Object> body) {
        int attempts = 0;
        while (true) {
            try {
                return client.post().body(body).retrieve().body(String.class);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                attempts++;
                if (attempts >= 3) {
                    throw e;
                }
                try {
                    Thread.sleep(3000L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}