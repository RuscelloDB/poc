package com.ruscello.core.transport.http.histogram;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistogramController {

    public HistogramController() {

    }

    @GetMapping("/histogram/{name}")
    public void getHistogram() {

    }
}
