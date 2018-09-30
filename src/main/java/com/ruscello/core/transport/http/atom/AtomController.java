package com.ruscello.core.transport.http.atom;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AtomController {

    public AtomController() {

    }

    @PostMapping("/streams/{stream}")
    public void postEvents() {

    }

    @DeleteMapping("/streams/{stream}")
    public void deleteStream() {

    }

    @PostMapping("/streams/{stream}/metadata")
    public void postMetastreamEvent() {

    }

    @GetMapping("/streams/{stream}/metadata/{event}")
    public void getMetasteramEvent() {

    }

    @GetMapping("/streams/$all/")
    public void getAll() {

    }

}
