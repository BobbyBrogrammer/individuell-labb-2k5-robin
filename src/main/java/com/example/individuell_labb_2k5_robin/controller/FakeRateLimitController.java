package com.example.individuell_labb_2k5_robin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fake")
public class FakeRateLimitController {

    @PostMapping("/analyze")
    public ResponseEntity<String> analyze() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit was exceeded");
    }

    // Step 6: Framtvinga Hallucination
    @PostMapping("/hallucinate")
    public ResponseEntity<String> hallucinate() {
        String fakeGroqResponse = """
                {
                    "choices": [{
                        "message": {
                            "content": "Sure! The sentiment is VERY POSITIVE and I think the score is like... a lot?
                            Maybe 99 out of 100!? Great product definitely!!"
                        }
                    }]
                }
                """;
        return ResponseEntity.ok(fakeGroqResponse);
    }
}
