package eu.dirk.haases.spring.session;

import org.springframework.session.Session;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * <p>
 * A {@link Session} implementation that is backed by a {@link java.util.Map}. The
 * defaults for the properties are:
 * </p>
 * <ul>
 * <li>id - a secure random generated id</li>
 * <li>creationTime - the moment the {@link DefaultSession} was instantiated</li>
 * <li>lastAccessedTimeMillis - the moment the {@link DefaultSession} was instantiated</li>
 * <li>maxInactiveInterval - 30 minutes</li>
 * </ul>
 * <p>
 * <p>
 * This implementation has no synchronization, so it is best to use the copy constructor
 * when working on multiple threads.
 * </p>
 */
final class DefaultSession implements Session, Serializable {

    private static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 30 * 60;

    private static final long serialVersionUID = 0L;
    private final String originalId;
    private final Supplier<String> sessionIdentifierSupplier;
    private Instant creationTime = Instant.now();
    private String id;
    private long lastAccessedTimeMillis = System.currentTimeMillis();
    private long maxInactiveTimeMillis = Duration.ofSeconds(DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS).toMillis();
    private Map<String, Object> sessionAttrs = new HashMap<>();

    DefaultSession() {
        this(DefaultSession::generateSessionId);
    }

    DefaultSession(final Supplier<String> sessionIdentifierSupplier) {
        this(sessionIdentifierSupplier, sessionIdentifierSupplier.get());
    }

    DefaultSession(final Supplier<String> sessionIdentifierSupplier, String id) {
        this.sessionIdentifierSupplier = sessionIdentifierSupplier;
        this.id = id;
        this.originalId = id;
    }

    DefaultSession(final String id) {
        this(DefaultSession::generateSessionId, generateSessionId());
    }

    DefaultSession(final Supplier<String> sessionIdentifierSupplier, final Session session) {
        if (session == null) {
            throw new IllegalArgumentException("session cannot be null");
        }
        this.sessionIdentifierSupplier = sessionIdentifierSupplier;
        this.id = session.getId();
        this.originalId = this.id;
        this.sessionAttrs = new HashMap<>(session.getAttributeNames().size());
        for (String attrName : session.getAttributeNames()) {
            Object attrValue = session.getAttribute(attrName);
            if (attrValue != null) {
                this.sessionAttrs.put(attrName, attrValue);
            }
        }
        this.lastAccessedTimeMillis = session.getLastAccessedTime().toEpochMilli();
        this.creationTime = session.getCreationTime();
        this.maxInactiveTimeMillis = session.getMaxInactiveInterval().toMillis();
    }


    DefaultSession(final Session session) {
        this(DefaultSession::generateSessionId, session);
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String changeSessionId() {
        return this.id = sessionIdentifierSupplier.get();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Session && this.id.equals(((Session) obj).getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String attributeName) {
        this.lastAccessedTimeMillis = System.currentTimeMillis();
        ;
        return (T) this.sessionAttrs.get(attributeName);
    }

    @Override
    public Set<String> getAttributeNames() {
        this.lastAccessedTimeMillis = System.currentTimeMillis();
        ;
        return new HashSet<>(this.sessionAttrs.keySet());
    }

    @Override
    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Instant getLastAccessedTime() {
        return Instant.ofEpochMilli(this.lastAccessedTimeMillis);
    }

    @Override
    public void setLastAccessedTime(final Instant lastAccessedTime) {
        this.lastAccessedTimeMillis = lastAccessedTime.toEpochMilli();
    }

    @Override
    public Duration getMaxInactiveInterval() {
        return Duration.ofMillis(this.maxInactiveTimeMillis);
    }

    @Override
    public void setMaxInactiveInterval(final Duration interval) {
        this.maxInactiveTimeMillis = interval.toMillis();
    }

    /**
     * Get the original session id.
     *
     * @return the original session id
     * @see #changeSessionId()
     */
    String getOriginalId() {
        return this.originalId;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean isExpired() {
        if (this.maxInactiveTimeMillis < 0) {
            return false;
        }
        final long inactiveTimeMillis = System.currentTimeMillis() - this.lastAccessedTimeMillis;
        return (inactiveTimeMillis - this.maxInactiveTimeMillis) >= 0;
    }

    @Override
    public void removeAttribute(final String attributeName) {
        this.lastAccessedTimeMillis = System.currentTimeMillis();
        this.sessionAttrs.remove(attributeName);
    }

    @Override
    public void setAttribute(final String attributeName, final Object attributeValue) {
        this.lastAccessedTimeMillis = System.currentTimeMillis();
        if (attributeValue == null) {
            removeAttribute(attributeName);
        } else {
            this.sessionAttrs.put(attributeName, attributeValue);
        }
    }

}