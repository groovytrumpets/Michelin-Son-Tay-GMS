package com.g42.platform.gms.feedback.api.controller;

import com.g42.platform.gms.feedback.app.service.FeedbackService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/feedback/")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

}
