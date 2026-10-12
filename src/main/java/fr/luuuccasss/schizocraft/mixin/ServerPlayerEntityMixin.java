package fr.luuuccasss.schizocraft.mixin;

import fr.luuuccasss.schizocraft.Paranoid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin implements Paranoid {
    @Unique private float schizocraft$paranoia;

    @Override
    public float schizocraft$getParanoia() {
        return schizocraft$paranoia;
    }

    @Override
    public void schizocraft$setParanoia(float paranoia) {
        schizocraft$paranoia = Mth.clamp(paranoia, 0f, 100f);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void schizocraft$write(ValueOutput output, CallbackInfo ci) {
        output.putFloat("SchizocraftParanoia", schizocraft$paranoia);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void schizocraft$read(ValueInput input, CallbackInfo ci) {
        schizocraft$setParanoia(input.getFloatOr("SchizocraftParanoia", 0f));
    }

    /** Mourir secoue : on revient à moitié moins paranoïaque, pas guéri. */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void schizocraft$copyFrom(ServerPlayer old, boolean alive, CallbackInfo ci) {
        float before = ((Paranoid) old).schizocraft$getParanoia();
        schizocraft$setParanoia(alive ? before : before / 2);
    }
}
