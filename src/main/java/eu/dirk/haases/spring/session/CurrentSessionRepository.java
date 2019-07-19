package eu.dirk.haases.spring.session;

import org.springframework.session.Session;

public interface CurrentSessionRepository<S extends Session> {

    S currentSession();

}
