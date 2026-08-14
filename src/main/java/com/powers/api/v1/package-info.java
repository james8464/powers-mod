/**
 * POWERS public integration API 1.0. All mutation methods are server-thread-only.
 * Extensions register through the {@code powers:v1} Fabric entrypoint during SERVER_STARTING;
 * registrations close before SERVER_STARTED and are removed after SERVER_STOPPING. Invalid,
 * duplicate, or late registrations never replace live state. Protection is unanimous and fails
 * closed on any exception or linkage failure. The package contains no client-only references.
 */
package com.powers.api.v1;
