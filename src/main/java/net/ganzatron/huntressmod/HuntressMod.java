package net.ganzatron.huntressmod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod("huntressmod")
public class HuntressMod {
    // Global variables for easy configuration
    private static final int COOLDOWN_TICKS = 100;  // Cooldown on ability
    private static final double RANGE = 10.0;       // Range for detecting entities
    private static final int SPEED_DURATION = 100;  // Speed duration
    private static final int STRENGTH_DURATION = 100; // Strength duration

    private LivingEntity entityWithLowestHealth = null;
    private Wolf companionWolf = null;

    public HuntressMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRightClickWithEchoShard(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level world = player.getCommandSenderWorld();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.getItem() == Items.ECHO_SHARD) {
            if (!player.getCooldowns().isOnCooldown(Items.ECHO_SHARD)) {
                player.getCooldowns().addCooldown(Items.ECHO_SHARD, COOLDOWN_TICKS);

                LivingEntity lowestHealthEntity = findLowestHealthEntity(player, world);
                if (lowestHealthEntity != null) {
                    applyEffectsToPlayer(player);
                    highlightEntity(lowestHealthEntity);
                    spawnWolf(player, world);
                    entityWithLowestHealth = lowestHealthEntity;

                    player.sendSystemMessage(Component.literal("Huntress ability activated!"));
                }
            }
        }
    }

    private void spawnWolf(Player player, Level world) {
        Wolf wolf = EntityType.WOLF.create(world);

        Vec3 playerPos = player.position();
        wolf.setPos(playerPos.x + 1, playerPos.y, playerPos.z + 1);

        wolf.setOwnerUUID(player.getUUID());
        wolf.setTame(true);
        wolf.setCustomName(Component.literal("Huntress Companion"));
        wolf.setCustomNameVisible(true);
        wolf.setCollarColor(DyeColor.byId(2));
        wolf.setTarget(entityWithLowestHealth);

        world.addFreshEntity(wolf);

    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (entityWithLowestHealth != null && event.getEntity() == entityWithLowestHealth) {
            entityWithLowestHealth = null;
            Player player = (Player) event.getSource().getEntity();

            if (player != null) {
                Level world = player.getCommandSenderWorld(); // Fixed world access
                LivingEntity nextEntity = findLowestHealthEntity(player, world);
                if (nextEntity != null) {
                    applyEffectsToPlayer(player);
                    highlightEntity(nextEntity);
                    entityWithLowestHealth = nextEntity;

                    player.sendSystemMessage(Component.literal("New target highlighted!"));
                }
            }
        }
    }

    private LivingEntity findLowestHealthEntity(Player player, Level world) {
        Vec3 playerPos = player.position();
        AABB box = new AABB(playerPos.subtract(RANGE, RANGE, RANGE), playerPos.add(RANGE, RANGE, RANGE));
        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());

        return entities.stream().min((e1, e2) -> Float.compare(e1.getHealth(), e2.getHealth())).orElse(null);
    }

    private void applyEffectsToPlayer(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPEED_DURATION, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, STRENGTH_DURATION, 0));
    }

    private void highlightEntity(LivingEntity entity) {
        entity.setGlowingTag(true);
    }
}
