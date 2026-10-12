package fr.luuuccasss.schizocraft.mixin;

import fr.luuuccasss.schizocraft.Paranoid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements Paranoid {
    @Unique private float schizocraft$paranoia;

    @Override
    public float schizocraft$getParanoia() {
        return schizocraft$paranoia;
    }

    @Override
    public void schizocraft$setParanoia(float paranoia) {
        schizocraft$paranoia = MathHelper.clamp(paranoia, 0f, 100f);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void schizocraft$write(NbtCompound nbt, CallbackInfo ci) {
        nbt.putFloat("SchizocraftParanoia", schizocraft$paranoia);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void schizocraft$read(NbtCompound nbt, CallbackInfo ci) {
        schizocraft$setParanoia(nbt.getFloat("SchizocraftParanoia", 0f));
    }

    /** Mourir secoue : on revient à moitié moins paranoïaque, pas guéri. */
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void schizocraft$copyFrom(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        float before = ((Paranoid) old).schizocraft$getParanoia();
        schizocraft$setParanoia(alive ? before : before / 2);
    }
}
