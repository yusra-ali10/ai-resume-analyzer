package com.yusra.resumeanalyzer.controller;

import com.yusra.resumeanalyzer.model.AnalysisResult;
import com.yusra.resumeanalyzer.service.AiClientService;
import com.yusra.resumeanalyzer.service.PdfParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ResumeController {

    private final PdfParserService pdfParser;
    private final AiClientService aiClient;

    public ResumeController(PdfParserService pdfParser, AiClientService aiClient) {
        this.pdfParser = pdfParser;
        this.aiClient = aiClient;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("resume") MultipartFile resume,
                          @RequestParam("jobDescription") String jobDescription,
                          Model model) {
        try {
            if (resume.isEmpty() || !"application/pdf".equals(resume.getContentType())) {
                model.addAttribute("error", "Please upload a valid PDF file.");
                return "index";
            }
            String text = pdfParser.extractText(resume);
            AnalysisResult result = aiClient.analyze(text, jobDescription);
            model.addAttribute("result", result);
            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Something went wrong: " + e.getMessage());
            return "index";
        }
    }
}
