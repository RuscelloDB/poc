package com.ruscello.core.transport.http.admin;

import com.ruscello.core.CurrentUser;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class AdminController {


    //@PreAuthorize("hasAnyAuthority('ADMINS', 'OPERATIONS')")
    @PostMapping("/admin/shutdown")
    public void shutdown(@CurrentUser UserDetails currentUser, HttpServletRequest request) {
        // request.isUserInRole()
//        if (entity.User != null && (entity.User.IsInRole(SystemRoles.Admins) || entity.User.IsInRole(SystemRoles.Operations)))
//        {
//            Log.Info("Request shut down of node because shutdown command has been received.");
//            Publish(new ClientMessage.RequestShutdown(exitProcess: true, shutdownHttp: true));
//            entity.ReplyStatus(HttpStatusCode.OK, "OK", LogReplyError);
//        }
//        else
//        {
//            entity.ReplyStatus(HttpStatusCode.Unauthorized, "Unauthorized", LogReplyError);
//        }
    }

    @PostMapping("/admin/scavenge")
    public void scavenge() {
//        if (entity.User != null && (entity.User.IsInRole(SystemRoles.Admins) || entity.User.IsInRole(SystemRoles.Operations)))
//        {
//            Log.Info("Request scavenging because /admin/scavenge request has been received.");
//            Publish(new ClientMessage.ScavengeDatabase(new NoopEnvelope(), Guid.Empty, entity.User));
//            entity.ReplyStatus(HttpStatusCode.OK, "OK", LogReplyError);
//        }
//        else
//        {
//            entity.ReplyStatus(HttpStatusCode.Unauthorized, "Unauthorized", LogReplyError);
//        }
    }

}
