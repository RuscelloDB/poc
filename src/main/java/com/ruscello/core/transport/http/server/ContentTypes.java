package com.ruscello.core.transport.http.server;

public class ContentTypes {

    public static final String ANY = "*/*";

    public static final String RUSCELLO_ATOM_JSON = "application/vnd.ruscello.atom+json";
    public static final String RUSCELLO_ATOM_SERVICE_DOC_JSON = "application/vnd.ruscello.atomsvc+json";

    public static final String RUSCELLO_EVENT_JSON = "application/vnd.ruscello.event+json";
    public static final String RUSCELLO_EVENT_XML = "application/vnd.ruscello.event+xml";

    public static final String RUSCELLO_EVENTS_JSON = "application/vnd.ruscello.events+json";
    public static final String RUSCELLO_EVENTS_XML = "application/vnd.ruscello.events+xml";

    public static final String RUSCELLO_DESCRIPTION_DOC_JSON = "application/vnd.ruscello.streamdesc+json";

    public static final String RUSCELLO_COMPETING = "application/vnd.ruscello.competingatom+xml";
    public static final String RUSCELLO_COMPETING_JSON = "application/vnd.ruscello.competingatom+json";


    private ContentTypes() {
        // statics only
    }
}
