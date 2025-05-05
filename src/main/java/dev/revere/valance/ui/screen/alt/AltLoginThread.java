package dev.revere.valance.ui.screen.alt;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import dev.revere.valance.ClientLoader;
import dev.revere.valance.service.IAltService;
import dev.revere.valance.util.LoggerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Session;

import java.net.Proxy;
import java.util.Optional;

/**
 * @author Remi
 * @project valance
 * @date 5/5/2025
 */
public final class AltLoginThread extends Thread {
    private final String password;
    private final String username;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final String LOG_PREFIX = "[" + ClientLoader.CLIENT_NAME + ":AltLoginThread]";
    private final IAltService altService;

    public AltLoginThread(String username, String password) {
        super("Alt Login Thread");
        this.username = username;
        this.password = password;

        Optional<IAltService> altOpt = ClientLoader.getService(IAltService.class);
        if (altOpt.isEmpty()) {
            LoggerUtil.error(LOG_PREFIX, "Failed to get AltService instance.");
            throw new IllegalStateException("AltService is not available.");
        }
        this.altService = altOpt.get();
    }

    private Session createSession(String username, String password) {
        YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(Proxy.NO_PROXY, "");
        YggdrasilUserAuthentication auth = (YggdrasilUserAuthentication) service.createUserAuthentication(Agent.MINECRAFT);

        auth.setUsername(username);
        auth.setPassword(password);

        try {
            auth.logIn();
            return new Session(auth.getSelectedProfile().getName(), auth.getSelectedProfile().getId().toString(), auth.getAuthenticatedToken(), "mojang");
        } catch (AuthenticationException exception) {
            LoggerUtil.error(LOG_PREFIX, "Failed to login: " + exception.getMessage());
            return null;
        }
    }

    @Override
    public void run() {
        if (password.isEmpty()) {
            if (altService.isValidCrackedAlt(username)) {
                mc.session = new Session(username, "", "", "mojang");
                altService.setStatus("");
            } else {
                altService.setStatus(EnumChatFormatting.RED + "Invalid Username!");
            }
            return;
        }

        Session auth = createSession(username, password);
        if (auth == null) {
            altService.setStatus(EnumChatFormatting.RED + "Failed to login.");
            return;
        }

        mc.session = auth;
        altService.setStatus("");
    }
}