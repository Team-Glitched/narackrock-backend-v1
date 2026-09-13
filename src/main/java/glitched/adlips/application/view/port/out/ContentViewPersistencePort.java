package glitched.adlips.application.view.port.out;

import glitched.adlips.application.view.ContentViewKey;

public interface ContentViewPersistencePort {

    void increment(ContentViewKey key, long count);
}
