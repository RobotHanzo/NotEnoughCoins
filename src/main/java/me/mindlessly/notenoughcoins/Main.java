package me.mindlessly.notenoughcoins;

import me.mindlessly.notenoughcoins.commands.NECCommand;
import me.mindlessly.notenoughcoins.commands.subcommands.Help;
import me.mindlessly.notenoughcoins.commands.subcommands.Subcommand;
import me.mindlessly.notenoughcoins.commands.subcommands.Toggle;
import me.mindlessly.notenoughcoins.events.ChatReceivedEvent;
import me.mindlessly.notenoughcoins.events.OnTick;
import me.mindlessly.notenoughcoins.events.OnTooltip;
import me.mindlessly.notenoughcoins.events.OnWorldJoin;
import me.mindlessly.notenoughcoins.objects.AverageItem;
import me.mindlessly.notenoughcoins.utils.ApiHandler;
import me.mindlessly.notenoughcoins.utils.Utils;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION)
public class Main {
    public static Config config = new Config();
    public static Authenticator authenticator;
    public static boolean checkedForUpdate = false;
    public static NECCommand commandManager = new NECCommand(new Subcommand[]{
        new Toggle(),
        new Help()
    });
    public static Map<String, AverageItem> averageItemMap = new HashMap<>();
    public static Map<String, Date> processedItem = new HashMap<>(); // Date is the expiry time, indicates when the auction ends and should be purged to save memory for the long run
    public static Map<String, Integer> lbinItem = new HashMap<>();
    public static Map<String, Integer> bazaarItem = new HashMap<>(); // Long is the item's instant sell price
    public static Map<String, Integer> npcItem = new HashMap<>();
    public static double balance = 0;
    public static boolean justPlayedASound = false; // This is to prevent multiple flips coming in at once and dinging the heck out of the user

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ProgressManager.ProgressBar progressBar = ProgressManager.push("Not Enough Coins", 3);
        authenticator = new Authenticator(progressBar);
        try {
            authenticator.authenticate(true);
        } catch (Exception e) {
            while (progressBar.getStep() < (progressBar.getSteps() - 1))
                progressBar.step("loading-failed-" + progressBar.getStep());
            e.printStackTrace();
        }
        progressBar.step("Registering events, commands, hooks & tasks");
        config.preload();
        ClientCommandHandler.instance.registerCommand(commandManager);
        MinecraftForge.EVENT_BUS.register(new OnWorldJoin());
        MinecraftForge.EVENT_BUS.register(new OnTick());
        MinecraftForge.EVENT_BUS.register(new OnTooltip());
        MinecraftForge.EVENT_BUS.register(new ChatReceivedEvent());
        Tasks.updateAverageItem.start();
        Tasks.updateBalance.start();
        Tasks.updateBazaarItem.start();
        Tasks.updateLBINItem.start();
        Utils.runInAThread(ApiHandler::updateNPC);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Reference.logger.info("Logging out...");
            try {
                authenticator.logout();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        ProgressManager.pop(progressBar);
    }
}
