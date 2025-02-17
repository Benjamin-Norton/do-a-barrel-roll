package nl.enjarai.doabarrelroll;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.SmoothUtil;
import nl.enjarai.doabarrelroll.api.RollEntity;
import nl.enjarai.doabarrelroll.api.RollMouse;
import nl.enjarai.doabarrelroll.api.event.RollEvents;
import nl.enjarai.doabarrelroll.api.event.RollGroup;
import nl.enjarai.doabarrelroll.config.ActivationBehaviour;
import nl.enjarai.doabarrelroll.config.ModConfig;
import nl.enjarai.doabarrelroll.config.SyncedModConfig;
import nl.enjarai.doabarrelroll.flight.RotationModifiers;
import nl.enjarai.doabarrelroll.net.HandshakeClient;
import nl.enjarai.doabarrelroll.render.HorizonLineWidget;
import nl.enjarai.doabarrelroll.render.MomentumCrosshairWidget;
import nl.enjarai.doabarrelroll.util.MixinHooks;
import org.joml.Vector2d;

public class DoABarrelRollClient {
    public static final HandshakeClient<SyncedModConfig> HANDSHAKE_CLIENT = new HandshakeClient<>(
            SyncedModConfig.TRANSFER_CODEC,
            ModConfig.INSTANCE::notifyPlayerOfServerConfig
    );
    public static final SmoothUtil PITCH_SMOOTHER = new SmoothUtil();
    public static final SmoothUtil YAW_SMOOTHER = new SmoothUtil();
    public static final SmoothUtil ROLL_SMOOTHER = new SmoothUtil();
    public static final RollGroup FALL_FLYING_GROUP = RollGroup.of(DoABarrelRoll.id("fall_flying"));
    public static double throttle = 0;

    public static void init() {
        FALL_FLYING_GROUP.trueIf(DoABarrelRollClient::isFallFlying);

        // Keyboard modifiers
        RollEvents.EARLY_CAMERA_MODIFIERS.register(context -> context
                .useModifier(RotationModifiers::manageThrottle, ModConfig.INSTANCE::getEnableThrust)
                .useModifier(RotationModifiers.buttonControls(1800)),
                2000, FALL_FLYING_GROUP);

        // Mouse modifiers, including swapping axes
        RollEvents.EARLY_CAMERA_MODIFIERS.register(context -> context
                .useModifier(ModConfig.INSTANCE::configureRotation),
                1000, FALL_FLYING_GROUP);

        // Generic movement modifiers, banking and such
        RollEvents.LATE_CAMERA_MODIFIERS.register(context -> context
                .useModifier(RotationModifiers.smoothing(
                        PITCH_SMOOTHER, YAW_SMOOTHER, ROLL_SMOOTHER,
                        ModConfig.INSTANCE.getSmoothing()
                ), ModConfig.INSTANCE::getSmoothingEnabled)
                .useModifier(RotationModifiers::banking, ModConfig.INSTANCE::getEnableBanking),
                1000, FALL_FLYING_GROUP);

        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if (!isFallFlying()) {
                clearValues();
            }
        });
    }

    public static void onRenderCrosshair(DrawContext context, float tickDelta, int scaledWidth, int scaledHeight) {
        if (!isFallFlying()) return;

        var matrices = context.getMatrices();
        var entity = MinecraftClient.getInstance().getCameraEntity();
        var rollEntity = ((RollEntity) entity);
        if (entity != null) {
            if (ModConfig.INSTANCE.getShowHorizon()) {
                HorizonLineWidget.render(matrices, scaledWidth, scaledHeight,
                        rollEntity.doABarrelRoll$getRoll(tickDelta), entity.getPitch(tickDelta));
            }

            if (ModConfig.INSTANCE.getMomentumBasedMouse() && ModConfig.INSTANCE.getShowMomentumWidget()) {
                var rollMouse = (RollMouse) MinecraftClient.getInstance().mouse;

                MomentumCrosshairWidget.render(matrices, scaledWidth, scaledHeight, new Vector2d(rollMouse.doABarrelRoll$getMouseTurnVec()));
            }
        }
    }

    private static void clearValues() {
        PITCH_SMOOTHER.clear();
        YAW_SMOOTHER.clear();
        ROLL_SMOOTHER.clear();
        throttle = 0;
    }

    public static boolean isFallFlying() {
        if (!HANDSHAKE_CLIENT.getConfig().map(SyncedModConfig::forceEnabled).orElse(false)) {
            var hybrid = ModConfig.INSTANCE.getActivationBehaviour() == ActivationBehaviour.HYBRID ||
                    ModConfig.INSTANCE.getActivationBehaviour() == ActivationBehaviour.HYBRID_TOGGLE;
            if (hybrid && !MixinHooks.thirdJump) {
                return false;
            }

            if (!ModConfig.INSTANCE.getModEnabled()) {
                return false;
            }
        }

        var player = MinecraftClient.getInstance().player;
        return player != null && player.isFallFlying();
    }
}
