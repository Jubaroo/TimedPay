package org.nyxcode.wurm.timedpay;

import com.wurmonline.server.Players;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.players.Player;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

public class TimedPay implements WurmServerMod, Initable, PreInitable, Configurable {
    long lastPayout;
    private static Logger logger = Logger.getLogger("org.gotti.wurmunlimited.mods.timedpaymod.TimedPayMod");
    private int amountCash;
    private int amountKarma;
    private int payoutInterval;
    private boolean playerMessage;
    private int messageRed;
    private int messageGreen;
    private int messageBlue;

    public TimedPay() {
    }

    public void configure(Properties properties) {
        this.amountCash = Integer.parseInt(properties.getProperty("amountCash"));
        this.amountKarma = Integer.parseInt(properties.getProperty("amountKarma"));
        this.payoutInterval = 8 * Integer.parseInt(properties.getProperty("payoutInterval"));
        this.playerMessage = Boolean.parseBoolean(properties.getProperty("playerMessage"));
        this.messageRed = Integer.parseInt(properties.getProperty("red"));
        this.messageGreen = Integer.parseInt(properties.getProperty("green"));
        this.messageBlue = Integer.parseInt(properties.getProperty("blue"));

    }

    public void init() {
        HookManager.getInstance().registerHook("com.wurmonline.server.Players", "pollPlayers", "()V", () -> {
            return (object, method, args) -> {
                Players players = (Players)object;
                this.addMoneyToLoggedPlayers(players);
                return method.invoke(object, args);
            };
        });
        this.lastPayout = WurmCalendar.currentTime;
    }

    public void preInit() {
    }

    public long addMoneyToLoggedPlayers(Players players) {
        Set<String> processedSteamIds = new HashSet();
        long currentTime = WurmCalendar.getCurrentTime();
        if (currentTime > this.lastPayout + (long)this.payoutInterval) {
            logger.log(Level.FINE, "executing payout");
            this.lastPayout = currentTime;
            List<Player> playerList = Arrays.asList(players.getPlayers());
            Iterator var6 = playerList.iterator();

            while(var6.hasNext()) {
                Player player = (Player)var6.next();
                String steamId = player.SteamId;
                if (!processedSteamIds.contains(steamId)) {
                    this.addMoneyAndKarma(player);
                    processedSteamIds.add(steamId);
                }
            }
        }

        return currentTime;
    }

    public void addMoneyAndKarma(Player player) {
        try {
            StringBuilder sb = new StringBuilder();
            if (this.amountCash > 0) {
                sb.append("Giving cash ");
                player.addMoney((long)this.amountCash);
                if (playerMessage) { player.getCommunicator().sendServerMessage("You have just received " + amountCash + " iron coins just for being on this server!",messageRed,messageGreen,messageBlue); }
            }

            if (this.amountKarma > 0) {
                if (sb.length() > 0) {
                    sb.append("and karma ");
                } else {
                    sb.append("Giving karma ");
                }

                player.setKarma(player.getKarma() + this.amountKarma);
                if (playerMessage) { player.getCommunicator().sendServerMessage("You have just received " + amountKarma + " karma points just for being on this server!",messageRed,messageGreen,messageBlue); }
            }

            sb.append("to player " + player.getName());
            logger.log(Level.INFO, sb.toString());
        } catch (IOException var3) {
            ;
        }

    }
}
