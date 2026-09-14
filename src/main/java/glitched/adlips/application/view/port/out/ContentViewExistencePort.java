package glitched.adlips.application.view.port.out;

import glitched.adlips.application.view.ContentViewTarget;

public interface ContentViewExistencePort {

    boolean exists(ContentViewTarget target, Long contentId);
}
