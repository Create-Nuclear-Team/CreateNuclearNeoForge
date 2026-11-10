package net.nuclearteam.createnuclear.foundation.telemetry.api;

import java.lang.annotation.*;

/** Discovered at mod construction by {@link net.nuclearteam.createnuclear.foundation.telemetry.TelemetryRegistry#bootstrap TelemetryRegistry.bootstrap}, in every loaded mod. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterTelemetrySink {
}
