package com.ruscello.core.transport.http.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    public StatsController() {

    }

    @GetMapping("/stats")
    public void stats() {

    }

    @GetMapping("/stats/replication")
    public void repliactionStats() {

    }

    @GetMapping("/stats/tcp")
    public void tcpStats() {

    }

    @GetMapping("/stats/{stat}")
    public void getStat() {

    }
}
