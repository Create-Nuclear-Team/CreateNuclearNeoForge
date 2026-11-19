package net.nuclearteam.createnuclear.foundation.telemetry.privacy;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.nuclearteam.createnuclear.foundation.telemetry.api.TelemetryEvent;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safety net that removes personal data from every event before it reaches a sink.
 * <p>
 * The rule stays: never pass personal data to telemetry. This filter catches mistakes:
 * <ul>
 *   <li>a {@link Player} or {@link GameProfile} value becomes {@value #PLAYER}, another {@link Entity}
 *       its type id, a {@link UUID} {@value #UUID_MASK}, an address {@value #IP_MASK};</li>
 *   <li>a {@link Path} is made relative to the game directory (or {@code ~}), or becomes {@value #PATH_MASK};</li>
 *   <li>in text, in this order: the game directory becomes {@code .}, the home directory {@code ~},
 *       UUIDs and IPv4 addresses are masked, then the names of online players and of the OS user are
 *       replaced as whole words of at least {@value #MIN_NAME_LENGTH} characters.</li>
 * </ul>
 * Known limits: IPv6 is not masked; an IPv4 address glued to a letter, a digit, {@code @} or
 * {@code -} is not masked, since it reads as a version ({@code jei@19.21.0.247}); only online
 * players are recognised by name; a player name that is also a common word is masked everywhere.
 * <p>
 * Thread-safe. Created once per session by {@link #forServer}.
 */
public final class PrivacyFilter {
    /** Replacement of a player. */
    public static final String PLAYER = "<player>";
    /** Replacement of a UUID. */
    public static final String UUID_MASK = "<uuid>";
    /** Replacement of a network address. */
    public static final String IP_MASK = "<ip>";
    /** Replacement of the OS user name. */
    public static final String USER_MASK = "<user>";
    /** Replacement of a path outside the game and home directories. */
    public static final String PATH_MASK = "<path>";
    /** Shorter names are not replaced, to avoid mangling ordinary text. */
    public static final int MIN_NAME_LENGTH = 3;

    private static final Pattern UUID_TEXT = Pattern.compile(
            "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    /** One IPv4 byte, 0 to 255. */
    private static final String IPV4_BYTE = "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    /**
     * An IPv4 address: four bytes, not glued to a word, '@', '-' or a longer dotted number, so that
     * versions such as {@code jei@19.21.0.247} or {@code mod-1.2.3.4} are kept. A final '.' ending a
     * sentence does not prevent the match.
     */
    private static final Pattern IPV4_TEXT = Pattern.compile(
            "(?<![\\w.@-])(?:" + IPV4_BYTE + "\\.){3}" + IPV4_BYTE + "(?![\\w-]|\\.\\d)");

    private final Path gameDirectory;
    @Nullable
    private final Path homeDirectory;
    private final List<Pattern> gameDirectoryText;
    private final List<Pattern> homeDirectoryText;
    @Nullable
    private final Pattern osUser;
    private final Supplier<String[]> playerNames;

    private String[] lastPlayerNames = new String[0];
    @Nullable
    private Pattern players;

    /**
     * Creates a filter. Use {@link #forServer} in the game; this constructor exists for tests.
     *
     * @param gameDirectory the game directory
     * @param homeDirectory the OS home directory, or {@code null}
     * @param osUserName    the OS user name, or {@code null}; ignored if shorter than {@value #MIN_NAME_LENGTH}
     * @param playerNames   names of the players online right now; read at each event
     */
    public PrivacyFilter(Path gameDirectory, @Nullable String homeDirectory, @Nullable String osUserName, Supplier<String[]> playerNames) {
        this.gameDirectory = gameDirectory.toAbsolutePath().normalize();
        this.homeDirectory = homeDirectory == null || homeDirectory.isBlank() ? null
                : Path.of(homeDirectory).toAbsolutePath().normalize();
        this.gameDirectoryText = pathPatterns(this.gameDirectory);
        this.homeDirectoryText = this.homeDirectory == null ? List.of() : pathPatterns(this.homeDirectory);

        this.osUser = osUserName == null || osUserName.length() < MIN_NAME_LENGTH ? null : wholeWords(List.of(osUserName));
        this.playerNames = playerNames;
    }

    /**
     * Creates the filter of a session.
     *
     * @param server the running server
     * @return a filter using the server directory, the JVM's {@code user.home} and {@code user.name},
     *         and the server's online players
     */
    public static PrivacyFilter forServer(MinecraftServer server) {
        return new PrivacyFilter(server.getServerDirectory(), System.getProperty("user.home"), System.getProperty("user.name"), server::getPlayerNames);
    }

    /**
     * Returns a cleaned copy of an event: its message and every attribute value. Attribute keys,
     * level, module, category, subject and tick are unchanged.
     *
     * @param event the event as emitted
     * @return the event safe to write
     */
    public TelemetryEvent clean(TelemetryEvent event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : event.attributes().entrySet())
            attributes.put(e.getKey(), value(e.getValue()));
        return event.withContent(text(event.message()), attributes);
    }

    /**
     * Cleans one attribute value. Objects are reduced before their {@code toString()} could leak anything.
     *
     * @param value the value, possibly {@code null}
     * @return {@code null}, a number or a boolean unchanged, an enum constant's {@code name()}, or a cleaned string
     */
    @Nullable
    public Object value(@Nullable Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof Player || value instanceof GameProfile) return PLAYER;
        if (value instanceof Entity entity) return EntityType.getKey(entity.getType()).toString();
        if (value instanceof UUID) return UUID_MASK;
        if (value instanceof InetAddress || value instanceof SocketAddress) return IP_MASK;
        if (value instanceof Path path) return path(path);
        if (value instanceof Component component) return text(component.getString());
        return text(String.valueOf(value));
    }

    /**
     * Cleans free text, applying the replacements in the order described on the class.
     *
     * @param text the text, possibly {@code null}
     * @return the cleaned text; {@code ""} for {@code null}
     */
    public String text(@Nullable String text) {
        if (text == null || text.isEmpty()) return "";
        String out = text;
        for (Pattern p : gameDirectoryText) out = p.matcher(out).replaceAll(".");
        for (Pattern p : homeDirectoryText) out = p.matcher(out).replaceAll("~");
        out = UUID_TEXT.matcher(out).replaceAll(UUID_MASK);
        out = IPV4_TEXT.matcher(out).replaceAll(IP_MASK);
        Pattern online = players();
        if (online != null) out = online.matcher(out).replaceAll(Matcher.quoteReplacement(PLAYER));
        if (osUser != null) out = osUser.matcher(out).replaceAll(Matcher.quoteReplacement(USER_MASK));
        return out;
    }

    /** Relative to the game directory, else {@code ~/…} under the home directory, else {@value #PATH_MASK}. */
    private String path(Path path) {
        Path abs = path.toAbsolutePath().normalize();
        if (abs.startsWith(gameDirectory)) {
            String rel = gameDirectory.relativize(abs).toString().replace('\\', '/');
            return rel.isEmpty() ? "." : rel;
        }
        if (homeDirectory != null && abs.startsWith(homeDirectory))
            return "~/" + homeDirectory.relativize(abs).toString().replace('\\', '/');
        return PATH_MASK;
    }

    /**
     * Pattern of the online players' names, rebuilt only when the list changes. If the list cannot
     * be read, the previous pattern is kept.
     *
     * @return the pattern, or {@code null} if no name is long enough
     */
    @Nullable
    private synchronized Pattern players() {
        String[] names;
        try {
            names = playerNames.get();
        } catch (Throwable t) {
            return players;
        }

        if (names == null) names = new String[0];
        if (!Arrays.equals(names, lastPlayerNames)) {
            lastPlayerNames = names.clone();
            List<String> kept = Arrays.stream(names).filter(n -> n != null && n.length() >= MIN_NAME_LENGTH).toList();
            players = kept.isEmpty() ? null : wholeWords(kept);
        }
        return players;
    }

    /** Case-insensitive patterns of a path written with either separator. */
    private static List<Pattern> pathPatterns(Path path) {
        String s = path.toString();
        Set<String> forms = new LinkedHashSet<>(List.of(s, s.replace('\\', '/'), s.replace('/', '\\')));
        List<Pattern> out = new ArrayList<>();
        for (String f : forms) out.add(Pattern.compile(Pattern.quote(f), Pattern.CASE_INSENSITIVE));
        return out;
    }

    /** Case-insensitive pattern matching any of the words, not inside a longer word. */
    private static Pattern wholeWords(List<String> words) {
        StringJoiner alternatives = new StringJoiner("|");
        // Longest first, so that "Steve2" is matched before "Steve".
        words.stream().sorted(Comparator.comparingInt(String::length).reversed()).forEach(w -> alternatives.add(Pattern.quote(w)));
        return Pattern.compile("(?<![\\p{L}\\p{N}_])(?:" + alternatives + ")(?![\\p{L}\\p{N}_])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
