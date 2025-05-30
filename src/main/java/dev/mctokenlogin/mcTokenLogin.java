package dev.mctokenlogin;

import dev.mctokenlogin.GUI.ChangerGUI;
import dev.mctokenlogin.GUI.SessionGUI;
import dev.mctokenlogin.Utils.APIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.util.Session;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = mcTokenLogin.MODID, version = mcTokenLogin.VERSION)
public class mcTokenLogin {
    public static final String MODID = "mcTokenLogin";
    public static final String VERSION = "0.1";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final String STATUS_VALID = "§2✔ Valid";
    private static final String STATUS_INVALID = "§4╳ Invalid";
    private static final String STATUS_ONLINE = "§2✔ Online";
    private static final String STATUS_OFFLINE = "§4╳ Offline";

    private static final Minecraft minecraftClient = Minecraft.getMinecraft();
    public static final Session originalSession = minecraftClient.getSession();
    private static volatile String onlineStatus = STATUS_OFFLINE;
    private static volatile String sessionValidStatus = STATUS_VALID;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiMultiplayer)) return;
        event.buttonList.add(new GuiButton(2100, event.gui.width - 90, 5, 80, 20, "Login"));
        event.buttonList.add(new GuiButton(2200, event.gui.width - 180, 5, 80, 20, "Changer"));
        EXECUTOR.submit(() -> {
            try {
                synchronized (mcTokenLogin.class) {
                    sessionValidStatus = APIUtils.validateSession(minecraftClient.getSession().getToken()) ? STATUS_VALID : STATUS_INVALID;
                    onlineStatus = APIUtils.checkOnline(minecraftClient.getSession().getUsername()) ? STATUS_ONLINE : STATUS_OFFLINE;
                }
            } catch (Exception ex) {
                System.err.println("Failed to validate session or check online status: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiMultiplayer)) return;
        String displayText = String.format("§fUser: %s  §f|  %s  §f|  %s",
                minecraftClient.getSession().getUsername(), onlineStatus, sessionValidStatus);
        minecraftClient.fontRendererObj.drawString(displayText, 5, 10, Color.RED.getRGB());
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiMultiplayer)) return;
        if (event.button.id == 2100) {
            minecraftClient.displayGuiScreen(new SessionGUI(event.gui));
        } else if (event.button.id == 2200) {
            minecraftClient.displayGuiScreen(new ChangerGUI(event.gui));
        }
    }
}
