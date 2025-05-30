package dev.mctokenlogin.GUI;

import dev.mctokenlogin.Utils.APIUtils;
import dev.mctokenlogin.Utils.SessionChanger;
import dev.mctokenlogin.mcTokenLogin;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionGUI extends GuiScreen {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final String STATUS_LOGGED_IN = "§2Logged in as %s";
    private static final String STATUS_INVALID_TOKEN = "§4Invalid token";
    private static final String STATUS_RESTORED = "§2Restored session";

    private final GuiScreen previousScreen;
    private volatile String status = "Session";
    private GuiTextField sessionField;
    private ScaledResolution scaledResolution;

    public SessionGUI(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        scaledResolution = new ScaledResolution(mc);
        sessionField = new GuiTextField(1, mc.fontRendererObj, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2, 200, 20);
        sessionField.setMaxStringLength(32767);
        sessionField.setFocused(true);
        buttonList.add(new GuiButton(1400, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2 + 25, 97, 20, "Login"));
        buttonList.add(new GuiButton(1500, scaledResolution.getScaledWidth() / 2 + 3, scaledResolution.getScaledHeight() / 2 + 25, 97, 20, "Restore"));
        buttonList.add(new GuiButton(1600, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2 + 50, 200, 20, "Back"));
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        mc.fontRendererObj.drawString(status, scaledResolution.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(status) / 2,
                scaledResolution.getScaledHeight() / 2 - 30, Color.WHITE.getRGB());
        sessionField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1400) {
            String token = sessionField.getText().trim();
            if (!isValidToken(token)) {
                updateStatus(STATUS_INVALID_TOKEN);
                return;
            }
            EXECUTOR.submit(() -> {
                try {
                    String[] playerInfo = APIUtils.getProfileInfo(token);
                    SessionChanger.setSession(new Session(playerInfo[0], playerInfo[1], token, "mojang"));
                    updateStatus(String.format(STATUS_LOGGED_IN, playerInfo[0]));
                } catch (Exception e) {
                    updateStatus(STATUS_INVALID_TOKEN);
                    System.err.println("Failed to log in with token: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else if (button.id == 1500) {
            SessionChanger.setSession(mcTokenLogin.originalSession);
            updateStatus(STATUS_RESTORED);
        } else if (button.id == 1600) {
            mc.displayGuiScreen(previousScreen);
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        sessionField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(previousScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private void updateStatus(String newStatus) {
        synchronized (this) {
            status = newStatus;
        }
    }

    private boolean isValidToken(String token) {
        return token != null && !token.isEmpty() && token.length() > 10;
    }
}
