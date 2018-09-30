package com.ruscello.core.transport.http.persistentSubscription;

import org.springframework.web.bind.annotation.*;

@RestController
public class PersistentSubscriptionController {

    public PersistentSubscriptionController() {

    }

    @GetMapping("/subscriptions")
    public void getAllSubscriptions() {

    }

    @GetMapping("/subscription/{stream}")
    public void getStreamSubscriptions() {

    }

    @PutMapping("/subscription/{stream}/{subscription}")
    public void putSubscription() {

    }

    @PostMapping("/subscription/{stream}/{subscription}")
    public void postSubscription() {

    }

    @DeleteMapping("/subscription/{stream}/{subscription}")
    public void deleteSubscription() {

    }

    @GetMapping("/subscription/{stream}/{subscription}")
    public void getNextMessages() {

    }

}
