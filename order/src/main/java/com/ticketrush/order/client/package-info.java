/**
 * HTTP clients for other TicketRush services. Calls are load balanced through Consul and authenticated with this
 * service's client-credentials token, never with the end user's token.
 */
package com.ticketrush.order.client;
