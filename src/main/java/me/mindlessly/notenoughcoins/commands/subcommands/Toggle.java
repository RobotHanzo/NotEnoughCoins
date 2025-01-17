package me.mindlessly.notenoughcoins.commands.subcommands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.essential.universal.USound;
import me.mindlessly.notenoughcoins.Authenticator;
import me.mindlessly.notenoughcoins.Config;
import me.mindlessly.notenoughcoins.Main;
import me.mindlessly.notenoughcoins.utils.Utils;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static me.mindlessly.notenoughcoins.utils.Utils.blacklistMessage;

public class Toggle implements Subcommand {
    public static boolean running = false;

    public Toggle() {
    }

    public static void updateConfig(boolean showMessage) {
        if (showMessage) {
            if (Config.enabled) {
                Utils.sendMessageWithPrefix("&aFlipper enabled.");
            } else {
                Utils.sendMessageWithPrefix("&cFlipper disabled.");
            }
        }
        if (Config.enabled && !running) { // Prevent duplicating the while loop
            flip();
        }
    }

    public static void flip() {
        if (running) return;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Utils.sendMessageWithPrefix("&eFlipper starting...");
                while (Config.enabled) {
                    Date start = new Date();
                    JsonElement json;
                    try {
                        running = true;
                        json = Objects.requireNonNull(Authenticator.getAuthenticatedJson("https://nec.robothanzo.dev/profit"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        blacklistMessage();
                        break;
                    }
                    for (JsonElement element : json.getAsJsonObject().getAsJsonArray("result")) {
                        JsonObject item = element.getAsJsonObject();
                        String itemID = item.get("auction_id").getAsString();
                        /* example:
                         * {"amount":1,"auction_id":"770d177104dd4c62b3f5610bcb0269e0","auctioneer":"5c003dfe48e741e497dcedbb2fe13475",
                         * "bin":true,"category":"accessories","dungeon_level":null,"enchantments":{},"end":1638823899955,"hpb_count":0,
                         * "id":"SHARP_SHARK_TOOTH_NECKLACE","item_name":"Sharp Shark Tooth Necklace",
                         * "pet_info":null,"price":135000, "profile_id":"d2bcb7b76cd14837b19c03ea258e51fd","profit":5000,"rarity":"EPIC",
                         * "recombobulated":false,"reforge":null,"starred":false,"start":1638802299955,"generated_at":1638802300983}
                         */
                        if (!Main.processedItem.containsKey(itemID)) { // havent been processed
                            Main.processedItem.put(itemID, new Date(item.get("end").getAsLong()));
                            if (!Config.categoryFilter.contains(item.get("category").getAsString().toUpperCase(Locale.ROOT)) && !Arrays.asList(Config.blacklistedIDs.split("\n")).contains(item.get("id").getAsString())) { // blacklist checks
                                int price = item.get("price").getAsInt();
                                int profit = Utils.getTaxedProfit(price, item.get("profit").getAsInt());
                                int demand;
                                try {
                                    demand = Main.averageItemMap.get(item.get("id").getAsString()).demand;
                                } catch (NullPointerException e) {
                                    Main.processedItem.remove(itemID);
                                    continue;
                                }
                                double profitPercentage = ((double) profit / (double) price);
                                if (price <= Main.balance && profit >= Config.minProfit && profitPercentage >= Config.minProfitPercentage && demand >= Config.minDemand) { // min profit etc checks
                                    if (!((price + item.get("profit").getAsInt()) * 0.6 > Main.averageItemMap.get(item.get("id").getAsString()).ahAvgPrice)) { // Manipulation checks
                                        if (!Authenticator.myUUID.toLowerCase(Locale.ROOT).replaceAll("-", "").equals(item.get("auctioneer").getAsString())) { //not self
                                            Utils.sendMessageWithPrefix("&e" + item.get("item_name").getAsString() + " " + // item name
                                                    Utils.getProfitText(profit) + " " + // profit
                                                    "&ePP: &a" + (int) Math.floor(profitPercentage * 100) + "% " + "&eSPD: &a" + demand + " " + // price percentage and demand
                                                    (Config.debug ? "&eL: &a" + (new Date().getTime() - start.getTime()) + "ms" : ""), // debug
                                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewauction " + itemID));
                                            if (Config.alertSounds && !Main.justPlayedASound) {
                                                Main.justPlayedASound = true;
                                                USound.INSTANCE.playPlingSound();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Main.justPlayedASound = false;
                }
                Utils.sendMessageWithPrefix("&eFlipper stopping...");
                running = false;
            }
        }, 0);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                while (Config.enabled) {
                    Main.processedItem.keySet().removeIf(itemID -> (Main.processedItem.get(itemID).getTime() - new Date().getTime()) <= 0);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0);
    }

    @Override
    public String getCommandName() {
        return "toggle";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "";
    }

    @Override
    public boolean processCommand(ICommandSender sender, String[] args) {
        Config.enabled = !Config.enabled;
        updateConfig(true);
        return true;
    }
}
