package com.ticketrush.catalog.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    /** Other TicketRush services, authenticated with client credentials. */
    public static final String SERVICE = "ROLE_SERVICE";

    private AuthoritiesConstants() {}
}
