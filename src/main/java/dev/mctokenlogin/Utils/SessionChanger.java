package dev.mctokenlogin.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SessionChanger {
    private static final String SESSION_FIELD = "session";
    private static final String SESSION_FIELD_OBF = "field_71449_j";

    public static void setSession(Session session) {
        try {
            Field sessionField = ReflectionHelper.findField(Minecraft.class, SESSION_FIELD, SESSION_FIELD_OBF);
            ReflectionHelper.setPrivateValue(Field.class, sessionField, sessionField.getModifiers() & ~Modifier.FINAL, "modifiers");
            ReflectionHelper.setPrivateValue(Minecraft.class, Minecraft.getMinecraft(), session, SESSION_FIELD, SESSION_FIELD_OBF);
        } catch (Exception e) {
            System.err.println("Failed to set session: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unable to change session", e);
        }
    }
}
