package org.zeith.gson.stream.itf;

import java.lang.annotation.*;

/**
 * Marks interface function as exported to be enrolled with the {@link ExportedInterface}/{@link ImportedInterface} chain.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Exported
{
}