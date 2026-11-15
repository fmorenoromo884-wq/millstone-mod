package com.fer.millstone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Solo cliente: bucle de "minecart riding" (muy bajo y grave) mientras la rueda gira. */
public class MillstoneClientSounds {
    private static final float VOLUME = 0.12F;
    private static final float PITCH = 0.6F;

    public static void startLoop(MillstoneBlockEntity be) {
        if (be.loopSound instanceof LoopSound s && !s.isStopped()) return;
        LoopSound sound = new LoopSound(be.getBlockPos());
        be.loopSound = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static class LoopSound extends AbstractTickableSoundInstance {
        private final BlockPos pos;

        LoopSound(BlockPos pos) {
            super(SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.pos = pos;
            this.looping = true;
            this.delay = 0;
            this.volume = VOLUME;
            this.pitch = PITCH;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
        }

        @Override
        public void tick() {
            Level level = Minecraft.getInstance().level;
            if (level == null
                    || !(level.getBlockEntity(pos) instanceof MillstoneBlockEntity)
                    || !level.getBlockState(pos).getValue(MillstoneBlock.LIT)) {
                stop();
            }
        }
    }
}
