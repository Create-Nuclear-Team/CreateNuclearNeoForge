package net.nuclearteam.createnuclear.foundation.telemetry.privacy.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.nuclearteam.createnuclear.CreateNuclear;
import net.nuclearteam.createnuclear.foundation.telemetry.privacy.PrivacyFilter;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;


/**
 * GameTest coverage for {@link PrivacyFilter}, built with fixed directories, OS user and online
 * players. Mock player and entity come from the test level; they are never added to the world.
 */
@GameTestHolder(CreateNuclear.MOD_ID)
@PrefixGameTestTemplate(false)
public class PrivacyFilterGameTest {

    private static final String STRUCTURE = "empty_platform";
    private static final Path HOME = Path.of("telemetry-test-home", "alice").toAbsolutePath().normalize();
    private static final Path GAME = HOME.resolve("minecraft");

    private static PrivacyFilter filter(String... onlinePlayers) {
        return new PrivacyFilter(GAME, HOME.toString(), "alice", () -> onlinePlayers);
    }

    private static void checkEquals(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual))
            throw new GameTestAssertException(what + ": expected <" + expected + "> but got <" + actual + ">");
    }

    /** UUIDs and IPv4 are masked; versions and decimals are kept. */
    @GameTest(template = STRUCTURE)
    public static void masksUuidAndIpv4Only(GameTestHelper helper) {
        PrivacyFilter f = filter();
        checkEquals("id <uuid> from <ip> on 1.21.1 at 0.99",
                f.text("id 123e4567-e89b-12d3-a456-426614174000 from 192.168.1.20 on 1.21.1 at 0.99"), "text");
        helper.succeed();
    }

    /**
     * Versions with four numbers are kept (glued to '@', '-' or a letter, longer, or with a byte
     * over 255); an address followed by a port or ending a sentence is masked.
     */
    @GameTest(template = STRUCTURE)
    public static void keepsFourPartVersions(GameTestHelper helper) {
        PrivacyFilter f = filter();
        String versions = "jei@19.21.0.247 jei-19.43.0.390 v1.2.3.4 1.2.3.4.5 300.1.1.1";
        checkEquals(versions, f.text(versions), "versions");
        checkEquals("to <ip>:25565, /<ip> ip=<ip>, from <ip>.",
                f.text("to 10.0.0.1:25565, /192.168.0.1 ip=127.0.0.1, from 192.168.1.20."), "addresses");
        helper.succeed();
    }

    /** The game directory becomes '.', the home directory '~', whatever the separator. */
    @GameTest(template = STRUCTURE)
    public static void relativizesGameAndHomeDirectories(GameTestHelper helper) {
        PrivacyFilter f = filter();
        String game = GAME.toString();
        checkEquals("./logs", f.text(game.replace('\\', '/') + "/logs"), "game dir with /");
        checkEquals(".\\logs", f.text(game.replace('/', '\\') + "\\logs"), "game dir with \\");
        checkEquals("~/docs", f.text(HOME.toString().replace('\\', '/') + "/docs"), "home dir");
        helper.succeed();
    }

    /** Online players and the OS user are replaced as whole words of at least 3 characters. */
    @GameTest(template = STRUCTURE)
    public static void replacesNamesAsWholeWords(GameTestHelper helper) {
        PrivacyFilter f = filter("Steve", "Al");
        checkEquals("<player> joined, Al stayed", f.text("steve joined, Al stayed"), "players");
        checkEquals("<user> ran it, malice kept", f.text("alice ran it, malice kept"), "OS user");
        helper.succeed();
    }

    /** Objects are reduced before their text could leak anything. */
    @GameTest(template = STRUCTURE)
    public static void reducesObjects(GameTestHelper helper) {
        PrivacyFilter f = filter();
        checkEquals(PrivacyFilter.PLAYER, f.value(helper.makeMockPlayer(GameType.SURVIVAL)), "player");
        checkEquals("minecraft:pig", f.value(EntityType.PIG.create(helper.getLevel())), "entity");
        checkEquals(PrivacyFilter.UUID_MASK, f.value(UUID.randomUUID()), "uuid");
        checkEquals(PrivacyFilter.IP_MASK, f.value(InetAddress.getLoopbackAddress()), "address");
        checkEquals("config/x.toml", f.value(GAME.resolve("config").resolve("x.toml")), "path in game dir");
        checkEquals("~/notes.txt", f.value(HOME.resolve("notes.txt")), "path in home dir");
        checkEquals(PrivacyFilter.PATH_MASK, f.value(Path.of("elsewhere-xyz").toAbsolutePath().getRoot().resolve("tmp")), "other path");
        checkEquals(12, f.value(12), "number kept");
        checkEquals(GameType.SURVIVAL.name(), f.value(GameType.SURVIVAL), "enum name");
        helper.succeed();
    }
}
