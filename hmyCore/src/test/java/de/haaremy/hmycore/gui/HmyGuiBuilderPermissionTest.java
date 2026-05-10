package de.haaremy.hmycore.gui;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testet die Permission-Filter-Logik des HmyGuiBuilder ohne Bukkit-Server.
 * Player wird per Java-Proxy gemockt: nur hasPermission(String) ist unterstuetzt.
 */
class HmyGuiBuilderPermissionTest {

    private static Player fakePlayer(Set<String> grantedPermissions) {
        return (Player) Proxy.newProxyInstance(
            HmyGuiBuilderPermissionTest.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if ("hasPermission".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof String s) {
                    return grantedPermissions.contains(s);
                }
                if ("toString".equals(method.getName())) return "FakePlayer";
                if ("equals".equals(method.getName())) return proxy == args[0];
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                Class<?> ret = method.getReturnType();
                if (ret == boolean.class) return false;
                if (ret == int.class) return 0;
                if (ret == long.class) return 0L;
                if (ret == double.class) return 0.0;
                if (ret == float.class) return 0.0f;
                if (ret == short.class) return (short) 0;
                if (ret == byte.class) return (byte) 0;
                if (ret == char.class) return (char) 0;
                return null;
            }
        );
    }

    @Test
    void slotWithoutPermissionIsAlwaysVisible() {
        HmyGuiBuilder builder = new HmyGuiBuilder();
        builder.setItem(0, null);
        assertTrue(builder.isSlotVisible(0, null));
        assertTrue(builder.isSlotVisible(0, fakePlayer(Set.of())));
    }

    @Test
    void slotWithPermissionHiddenWhenPlayerLacksPermission() {
        HmyGuiBuilder builder = new HmyGuiBuilder();
        builder.setItemIfPermitted(3, "hmy.admin", null);
        assertFalse(builder.isSlotVisible(3, null), "ohne Player keine Permission");
        assertFalse(builder.isSlotVisible(3, fakePlayer(Set.of("hmy.other"))));
    }

    @Test
    void slotWithPermissionVisibleWhenPlayerHasPermission() {
        HmyGuiBuilder builder = new HmyGuiBuilder();
        builder.setItemIfPermitted(7, "hmy.vip.gui", null);
        assertTrue(builder.isSlotVisible(7, fakePlayer(Set.of("hmy.vip.gui"))));
    }

    @Test
    void emptyPermissionTreatedAsNoBinding() {
        HmyGuiBuilder builder = new HmyGuiBuilder();
        builder.setItemIfPermitted(2, "", null);
        assertTrue(builder.isSlotVisible(2, null));
    }

    @Test
    void nullPermissionTreatedAsNoBinding() {
        HmyGuiBuilder builder = new HmyGuiBuilder();
        builder.setItemIfPermitted(4, null, null);
        assertTrue(builder.isSlotVisible(4, null));
    }
}
