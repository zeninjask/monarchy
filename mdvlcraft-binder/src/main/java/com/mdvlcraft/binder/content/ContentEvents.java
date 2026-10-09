package com.mdvlcraft.binder.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class ContentEvents {
    private ContentEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawn event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && DisabledContent.isDisabled(event.getEntity().getType())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onEntityInteract(EntityInteract event) {
        if (!event.getLevel().isClientSide() && event.getTarget() instanceof Merchant merchant) {
            merchant.getOffers()
                .removeIf(
                    offer -> DisabledContent.isForbidden(offer.getResult())
                        || DisabledContent.isForbidden(offer.getBaseCostA())
                        || DisabledContent.isForbidden(offer.getCostB())
                );
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        if (DisabledContent.isForbidden(event.getLevel().getBlockState(event.getPos()).getBlock().asItem())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> forbidden = new ArrayList<>();

        for (Entry<ItemStack, ?> entry : event.getEntries()) {
            if (DisabledContent.isForbidden(entry.getKey())) {
                forbidden.add(entry.getKey());
            }
        }

        forbidden.forEach(event.getEntries()::remove);
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> DisabledContent.isForbidden(drop.getItem()));
    }
}
