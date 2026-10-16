package com.yusra.resumeanalyzer.model;

import java.util.List;

public record AnalysisResult(
        int matchScore,
        List<String> strengths,
        List<String> missingSkills,
        List<String> suggestions
) {}
