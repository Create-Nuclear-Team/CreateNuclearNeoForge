package net.nuclearteam.createnuclear.foundation.telemetry;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.nuclearteam.createnuclear.foundation.telemetry.api.BuiltinCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.ITelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetryModule;
import net.nuclearteam.createnuclear.foundation.telemetry.api.RegisterTelemetrySink;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryCategory;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryIds;
import net.nuclearteam.createnuclear.foundation.telemetry.config.TelemetryConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

/**
 * Discovers telemetry modules and sinks, and indexes their categories.
 * <p>
 * {@link #bootstrap(ModContainer)} scans every loaded mod, addons included, for classes annotated
 * with {@link RegisterTelemetryModule} or {@link RegisterTelemetrySink}. Invalid entries are logged
 * and skipped: a faulty addon never prevents the game from starting.
 * <p>
 * After bootstrap, every collection is unmodifiable and can be read from any thread.
 */
public final class TelemetryRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateNuclear/Telemetry");

    private static Map<String, ModuleEntry> modules = Map.of();
    private static Map<String, SinkEntry> sinks = Map.of();
    private static Map<TelemetryCategory, ModuleEntry> categoryOwners = Map.of();
    private static boolean bootstrapped;

    private TelemetryRegistry() {}

    /**
     * Scans all loaded mods and builds the registry. Called once from {@code CNConfigs.register}.
     * <p>
     * Modules and sinks of addons are instantiated during Create Nuclear's construction, possibly
     * before the addon's own constructor: their no-arg constructor must stay trivial.
     *
     * @param container Create Nuclear's mod container, used to register the telemetry configs (from step 5)
     * @throws IllegalStateException if called twice
     */
    public static synchronized void bootstrap(ModContainer container) {
        if (bootstrapped) throw new IllegalStateException("TelemetryRegistry already bootstrapped");

        List<ModuleEntry> foundModules = new ArrayList<>();
        for (ITelemetryModule module : instantiateAll(RegisterTelemetryModule.class, ITelemetryModule.class)) {
            try {
                foundModules.add(new ModuleEntry(module));
            } catch (Throwable t) {
                LOGGER.error("Skipping telemetry module {}: cannot read its id or categories", module.getClass().getName(), t);
            }
        }

        List<SinkEntry> foundSinks = new ArrayList<>();
        for (Class<? extends ITelemetrySink> type : findClass(RegisterTelemetrySink.class, ITelemetrySink.class)) {
            SinkEntry entry = SinkEntry.of(type);
            if (entry != null) foundSinks.add(entry);
        }

        index(foundModules, foundSinks);

        TelemetryConfigs.register(container);

        LOGGER.info("Telemetry registry ready: {} module(s) {}, {} sink(s) {}",
                modules.size(), modules.keySet(), sinks.size(), sinks.keySet());
    }

    /**
     * Validates and indexes the given entries, replacing any previous content.
     * <p>
     * Package-private so game tests can build a registry without scanning.
     *
     * @param foundModules discovered modules; invalid or duplicate ones are logged and skipped
     * @param foundSinks   discovered sinks; invalid or duplicate ones are logged and skipped
     */
    static synchronized void index(List<ModuleEntry> foundModules, List<SinkEntry> foundSinks) {
        Map<String, ModuleEntry> moduleMap = new TreeMap<>();
        Map<TelemetryCategory, ModuleEntry> owners = new IdentityHashMap<>();

        for (ModuleEntry entry : foundModules) {
            String id = entry.id();
            if (!TelemetryIds.isValid(id)) {
                LOGGER.error("Skipping telemetry module {}: invalid id '{}'", entry.module().getClass().getName(), id);
            } else if (TelemetrySession.SESSION_MODULE.equals(id)) {
                LOGGER.error("Skipping telemetry module {}: id '{}' is reserved for session events",
                        entry.module().getClass().getName(), id);
            } else if (moduleMap.containsKey(id)) {
                LOGGER.error("Skipping telemetry module {}: id '{}' already used by {}",
                        entry.module().getClass().getName(), id, moduleMap.get(id).module().getClass().getName());
            } else if (validateCategories(entry, owners)) {
                moduleMap.put(id, entry);
                for (TelemetryCategory c : entry.ownCategories()) owners.put(c, entry);
            }
        }

        Map<String, SinkEntry> sinkMap = new TreeMap<>();
        for (SinkEntry entry : foundSinks) {
            if (!TelemetryIds.isValid(entry.id())) {
                LOGGER.error("Skipping telemetry sink {}: invalid id '{}'", entry.type().getName(), entry.id());
            } else if (sinkMap.containsKey(entry.id())) {
                LOGGER.error("Skipping telemetry sink {}: id '{}' already used by {}",
                        entry.type().getName(), entry.id(), sinkMap.get(entry.id()).type().getName());
            } else {
                sinkMap.put(entry.id(), entry);
            }
        }

        modules = Collections.unmodifiableMap(new LinkedHashMap<>(moduleMap));
        sinks = Collections.unmodifiableMap(new LinkedHashMap<>(sinkMap));
        categoryOwners = Collections.unmodifiableMap(owners);
        bootstrapped = true;
    }

    private static boolean validateCategories(ModuleEntry entry, Map<TelemetryCategory, ModuleEntry> owners) {
        String module = entry.id();
        Set<String> seen = new HashSet<>();
        for (TelemetryCategory c : entry.ownCategories()) {
            String reason = null;
            if (c == null) reason = "null category";
            else if (!TelemetryIds.isValid(c.id())) reason = "invalid category id '" + c.id() + "'";
            else if (BuiltinCategory.isReserved(c.id())) reason = "category id '" + c.id() + "' is reserved";
            else if (!seen.add(c.id())) reason = "duplicate category id '" + c.id() + "'";
            else if (c.defaultLevel() == null) reason = "category '" + c.id() + "' has no default level";
            else if (owners.containsKey(c)) reason = "category '" + c.id() + "' already belongs to module '" + owners.get(c).id() + "'";

            if (reason != null) {
                LOGGER.error("Skipping telemetry module '{}': {}", module, reason);
                return false;
            }
        }
        return true;
    }

    // ---- Discovery ------------------------------------------------------------------------------
    private static <T> List<Class<? extends T>> findClass(Class<? extends Annotation> annotation, Class<T> expected) {
        Set<String> names = new TreeSet<>();
        for (ModFileScanData scan : ModList.get().getAllScanData())
            scan.getAnnotatedBy(annotation, ElementType.TYPE).forEach(a -> names.add(a.clazz().getClassName()));

        List<Class<? extends T>> classes = new ArrayList<>();
        ClassLoader loader = TelemetryRegistry.class.getClassLoader();
        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, true, loader);
                if (!expected.isAssignableFrom(type)) {
                    LOGGER.error("Skipping {}: annotated with @{} but does not implement {}",
                            name, annotation.getSimpleName(), expected.getSimpleName());
                    continue;
                }
                classes.add(type.asSubclass(expected));
            } catch (Throwable t) {
                LOGGER.error("Skipping {}: cannot load class", name, t);
            }
        }
        return classes;
    }

    private static <T> List<T> instantiateAll(Class<? extends Annotation> annotation, Class<T> expected) {
        List<T> instances = new ArrayList<>();
        for (Class<? extends T> type : findClass(annotation, expected)) {
            T instance = newInstance(type);
            if (instance != null) instances.add(instance);
        }
        return instances;
    }

    @Nullable
    private static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> ctor = type.getConstructor(); // public no-arg only
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            LOGGER.error("Skipping {}: needs a public no-arg constructor", type.getName());
        } catch (Throwable t) {
            LOGGER.error("Skipping {}: constructor failed", type.getName(), t);
        }

        return null;
    }

    // ---- Queries --------------------------------------------------------------------------------

    /**
     * Tells whether {@link #bootstrap(ModContainer)} has run.
     *
     * @return {@code true} once the registry is built
     */
    public static boolean isBootstrapped() {
        return bootstrapped;
    }

    /**
     * Returns every registered module.
     *
     * @return an unmodifiable view, sorted by id
     */
    public static Collection<ModuleEntry> modules() {
        return modules.values();
    }

    /**
     * Looks up a module by id.
     *
     * @param id module id
     * @return the module entry, or {@code null} if unknown
     */
    @Nullable
    public static ModuleEntry module(String id) {
        return modules.get(id);
    }

    /**
     * Returns every registered sink.
     *
     * @return an unmodifiable view, sorted by id
     */
    public static Collection<SinkEntry> sinks() {
        return sinks.values();
    }

    /**
     * Looks up a sink by id.
     *
     * @param id sink id
     * @return the sink entry, or {@code null} if unknown
     */
    @Nullable
    public static SinkEntry sink(String id) {
        return sinks.get(id);
    }

    /**
     * Finds the module owning a category, by instance identity.
     * Built-in categories are shared by every module and have no single owner.
     *
     * @param category a category instance returned by some {@link ITelemetryModule#categories()}
     * @return the owning module, or {@code null} for a built-in or unregistered category
     */
    @Nullable
    public static ModuleEntry ownerOf(TelemetryCategory category) {
        return categoryOwners.get(category);
    }

    // ---- Entries --------------------------------------------------------------------------------

    /**
     * A registered module, with its own categories followed by the built-in ones.
     * <p>
     * Categories are read once, when the entry is created.
     */
    public static final class ModuleEntry {
        private final ITelemetryModule module;
        private final List<TelemetryCategory> ownCategories;
        private final List<TelemetryCategory> categories;
        private final Map<String, TelemetryCategory> byId;

        /**
         * Creates an entry and reads the module's categories.
         * A {@code null} category list is treated as empty.
         *
         * @param module the module instance
         * @throws RuntimeException any exception thrown by {@link ITelemetryModule#categories()}
         */
        public ModuleEntry(ITelemetryModule module) {
            this.module = module;
            List<? extends TelemetryCategory> declared = module.categories();
            this.ownCategories = declared == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(declared));

            List<TelemetryCategory> all = new ArrayList<>(ownCategories);
            all.addAll(List.of(BuiltinCategory.values()));
            this.categories = Collections.unmodifiableList(all);

            Map<String, TelemetryCategory> map = new LinkedHashMap<>();
            for (TelemetryCategory c : all) if (c != null) map.putIfAbsent(c.id(), c);
            this.byId = Collections.unmodifiableMap(map);
        }

        /**
         * Returns the module id.
         *
         * @return the id returned by {@link ITelemetryModule#id()}
         */
        public String id() {
            return module.id();
        }

        /**
         * Returns the module instance.
         *
         * @return the single instance created at bootstrap
         */
        public ITelemetryModule module() {
            return module;
        }

        /**
         * Returns the categories declared by the module.
         *
         * @return an unmodifiable list, without the built-in categories
         */
        public List<TelemetryCategory> ownCategories() {
            return ownCategories;
        }

        /**
         * Returns every category of the module.
         *
         * @return an unmodifiable list: own categories, then {@link BuiltinCategory#HEALTH}
         *         and {@link BuiltinCategory#METRICS}
         */
        public List<TelemetryCategory> categories() {
            return categories;
        }

        /**
         * Looks up a category by id, built-in ones included.
         *
         * @param id category id
         * @return the category, or {@code null} if unknown
         */
        @Nullable
        public TelemetryCategory category(String id) {
            return byId.get(id);
        }
    }

    /**
     * A registered sink: its metadata plus a factory creating a new instance per session.
     *
     * @param id               sink id
     * @param description      config comment
     * @param enabledByDefault initial value of {@code sink_<id>}
     * @param type             implementation class, for error messages
     * @param factory          creates a fresh sink for each session; throws {@link IllegalStateException}
     *                         if the sink can no longer be instantiated
     */
    public record SinkEntry(String id, String description, boolean enabledByDefault, Class<? extends ITelemetrySink> type, Supplier<ITelemetrySink> factory) {
        /**
         * Builds an entry from a sink class, using a template instance to read its metadata.
         *
         * @param type the sink class
         * @return the entry, or {@code null} if the class cannot be instantiated
         */
        @Nullable
        static SinkEntry of(Class<? extends ITelemetrySink> type) {
            ITelemetrySink template = newInstance(type);
            if (template == null) return null;
            Supplier<ITelemetrySink> factory = () -> {
                ITelemetrySink sink = newInstance(type);
                if (sink == null) throw new IllegalStateException("Cannot instantiate sink " + type.getName());
                return sink;
            };
            try {
                return new SinkEntry(template.id(), template.description(), template.enabledByDefault(), type, factory);
            } catch (Throwable t) {
                LOGGER.error("Skipping telemetry sink {}: cannot read its metadata", type.getName(), t);
                return null;
            }
        }
    }
}
