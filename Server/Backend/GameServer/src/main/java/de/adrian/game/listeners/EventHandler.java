// Modified by danlp6: @Target(METHOD), ignoreCanceled doc
package de.adrian.game.listeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    /** true (default): skip this handler once a {@link CancelableEvent} is canceled. */
    boolean ignoreCanceled() default true;

}
