package eu.dirk.haases.spring.session;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.events.SessionCreatedEvent;
import org.springframework.session.events.SessionDeletedEvent;
import org.springframework.session.events.SessionExpiredEvent;

import java.time.Duration;

abstract class AbstractSessionRepository implements SessionRepository<Session>, ApplicationEventPublisherAware {

    private final SessionRepository<Session> delegateSessionRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private Integer defaultMaxInactiveInterval;

    AbstractSessionRepository(final ApplicationEventPublisher applicationEventPublisher) {
        this.delegateSessionRepository = this;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    AbstractSessionRepository(final SessionRepository<Session> delegateSessionRepository, final ApplicationEventPublisher applicationEventPublisher) {
        this.delegateSessionRepository = delegateSessionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public final Session createSession() {
        final Session session = this.delegateSessionRepository.createSession();
        if (this.defaultMaxInactiveInterval != null) {
            session.setMaxInactiveInterval(Duration.ofSeconds(this.defaultMaxInactiveInterval));
        }
        publishCreatedEvent(session);
        return session;
    }

    @Override
    public final void deleteById(final String id) {
        publishDeletedEvent(id);
        this.delegateSessionRepository.deleteById(id);
    }

    @Override
    public final Session findById(final String id) {
        final Session saved = this.delegateSessionRepository.findById(id);
        if (saved == null) {
            return null;
        }
        if (saved.isExpired()) {
            publishExpiredEvent(saved);
            deleteById(saved.getId());
            return null;
        }
        return new DefaultSession(saved);
    }

    private void publishCreatedEvent(final Session session) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new SessionCreatedEvent(this, session));
        }
    }

    private void publishDeletedEvent(final String id) {
        if (this.applicationEventPublisher != null) {
            publishDeletedEvent(this.delegateSessionRepository.findById(id));
        }
    }

    private void publishDeletedEvent(final Session session) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new SessionDeletedEvent(this, session));
        }
    }

    private void publishExpiredEvent(final Session session) {
        if (this.applicationEventPublisher != null) {
            this.applicationEventPublisher.publishEvent(new SessionExpiredEvent(this, session));
        }
    }

    @Override
    public final void save(final Session session) {
        final String originalId = (session instanceof DefaultSession
                ? ((DefaultSession) session).getOriginalId()
                : session.getId());
        if (!session.getId().equals(originalId)) {
            publishDeletedEvent(session);
            this.delegateSessionRepository.deleteById(originalId);
        }
        final DefaultSession clonedSession = new DefaultSession(session);
        this.delegateSessionRepository.save(clonedSession);
    }

    @Override
    public final void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public final void setDefaultMaxInactiveInterval(final int defaultMaxInactiveInterval) {
        this.defaultMaxInactiveInterval = defaultMaxInactiveInterval;
    }


}