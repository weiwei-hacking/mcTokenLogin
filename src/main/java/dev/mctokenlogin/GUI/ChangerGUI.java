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
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangerGUI extends GuiScreen {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final String STATUS_MAIN_ACCOUNT_PROTECTED = "§4Prevented name change on main account!";
    private static final String STATUS_SUCCESS = "§2Successfully changed %s!";
    private static final String STATUS_TOO_MANY_REQUESTS = "§4Error: Too many requests!";
    private static final String STATUS_INVALID_INPUT = "§4Error: Invalid %s!";
    private static final String STATUS_INVALID_TOKEN = "§4Error: Invalid token!";
    private static final String STATUS_UNAVAILABLE = "§4Error: %s unavailable or recently changed!";
    private static final String STATUS_UNKNOWN_ERROR = "§4An unknown error occurred!";

    private final GuiScreen previousScreen;
    private volatile String status = "";
    private GuiTextField nameField;
    private GuiTextField skinField;
    private ScaledResolution scaledResolution;
    private final ArrayList<GuiTextField> textFields = new ArrayList<>();

    public ChangerGUI(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        scaledResolution = new ScaledResolution(mc);
        nameField = new GuiTextField(1, mc.fontRendererObj, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2, 97, 20);
        nameField.setMaxStringLength(16);
        nameField.setFocused(true);
        skinField = new GuiTextField(2, mc.fontRendererObj, scaledResolution.getScaledWidth() / 2 + 3, scaledResolution.getScaledHeight() / 2, 97, 20);
        skinField.setMaxStringLength(32767);
        textFields.add(nameField);
        textFields.add(skinField);
        buttonList.add(new GuiButton(3100, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2 + 25, 97, 20, "Change Name"));
        buttonList.add(new GuiButton(3200, scaledResolution.getScaledWidth() / 2 + 3, scaledResolution.getScaledHeight() / 2 + 25, 97, 20, "Change Skin"));
        buttonList.add(new GuiButton(3300, scaledResolution.getScaledWidth() / 2 - 100, scaledResolution.getScaledHeight() / 2 + 50, 200, 20, "Back"));
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
                scaledResolution.getScaledHeight() / 2 - 40, Color.WHITE.getRGB());
        mc.fontRendererObj.drawString("Name:", scaledResolution.getScaledWidth() / 2 - 99, scaledResolution.getScaledHeight() / 2 - 15, Color.WHITE.getRGB());
        mc.fontRendererObj.drawString("Skin:", scaledResolution.getScaledWidth() / 2 + 4, scaledResolution.getScaledHeight() / 2 - 15, Color.WHITE.getRGB());
        nameField.drawTextBox();
        skinField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 3100) {
            String newName = nameField.getText().trim();
            if (!isValidMinecraftName(newName)) {
                updateStatus(String.format(STATUS_INVALID_INPUT, "name"));
                return;
            }
            if (Objects.equals(mcTokenLogin.originalSession.getToken(), mc.getSession().getToken())) {
                updateStatus(STATUS_MAIN_ACCOUNT_PROTECTED);
                return;
            }
            EXECUTOR.submit(() -> {
                try {
                    int statusCode = APIUtils.changeName(newName, mc.getSession().getToken());
                    updateStatus(handleApiResponse(statusCode, "name"));
                    if (statusCode == 200) {
                        SessionChanger.setSession(new Session(newName, mc.getSession().getPlayerID(), mc.getSession().getToken(), "mojang"));
                    }
                } catch (Exception e) {
                    updateStatus(STATUS_UNKNOWN_ERROR);
                    System.err.println("Failed to change name: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else if (button.id == 3200) {
            String newSkin = skinField.getText().trim();
            if (!isValidSkinUrl(newSkin)) {
                updateStatus(String.format(STATUS_INVALID_INPUT, "skin"));
                return;
            }
            EXECUTOR.submit(() -> {
                try {
                    int statusCode = APIUtils.changeSkin(newSkin, mc.getSession().getToken());
                    updateStatus(handleApiResponse(statusCode, "skin"));
                } catch (Exception e) {
                    updateStatus(STATUS_UNKNOWN_ERROR);
                    System.err.println("Failed to change skin: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else if (button.id == 3300) {
            mc.displayGuiScreen(previousScreen);
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        nameField.textboxKeyTyped(typedChar, keyCode);
        skinField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(previousScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField text : textFields) {
            text.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private void updateStatus(String newStatus) {
        synchronized (this) {
            status = newStatus;
        }
    }

    private String handleApiResponse(int statusCode, String operation) {
        switch (statusCode) {
            case 200: return String.format(STATUS_SUCCESS, operation);
            case 429: return STATUS_TOO_MANY_REQUESTS;
            case 400: return String.format(STATUS_INVALID_INPUT, operation);
            case 401: return STATUS_INVALID_TOKEN;
            case 403: return String.format(STATUS_UNAVAILABLE, operation);
            default: return STATUS_UNKNOWN_ERROR;
        }
    }

    private boolean isValidMinecraftName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    private boolean isValidSkinUrl(String url) {
        return url != null && url.matches("^https?://.*\\.png$");
    }
}
