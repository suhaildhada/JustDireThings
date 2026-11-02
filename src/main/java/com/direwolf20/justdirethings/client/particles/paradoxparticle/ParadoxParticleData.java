package com.direwolf20.justdirethings.client.particles.paradoxparticle;

import com.direwolf20.justdirethings.client.particles.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.UUID;

public record ParadoxParticleData(double targetX, double targetY, double targetZ, int ticksPerBlock, UUID paradox_uuid, ItemStack itemStack) implements ParticleOptions {
    public static final MapCodec<ParadoxParticleData>                             MAP_CODEC    = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ItemStack.CODEC.fieldOf("itemStack").forGetter(p -> p.itemStack),
                    Codec.DOUBLE.fieldOf("targetX").forGetter(p -> p.targetX),
                    Codec.DOUBLE.fieldOf("targetY").forGetter(p -> p.targetY),
                    Codec.DOUBLE.fieldOf("targetZ").forGetter(p -> p.targetZ),
                    Codec.INT.fieldOf("ticksPerBlock").forGetter(p -> p.ticksPerBlock),
                    UUIDUtil.CODEC.fieldOf("paradox_uuid").forGetter(p -> p.paradox_uuid)
            ).apply(instance, ParadoxParticleData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParadoxParticleData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ParadoxParticleData::itemStack,
            ByteBufCodecs.DOUBLE,
            ParadoxParticleData::targetX,
            ByteBufCodecs.DOUBLE,
            ParadoxParticleData::targetY,
            ByteBufCodecs.DOUBLE,
            ParadoxParticleData::targetZ,
            ByteBufCodecs.INT,
            ParadoxParticleData::ticksPerBlock,
            UUIDUtil.STREAM_CODEC,
            ParadoxParticleData::paradox_uuid,
            ParadoxParticleData::new
    );

    public ParadoxParticleData(ItemStack itemStack, double tx, double ty, double tz, int ticks, UUID paradox_uuid) {
        //Forge: Fix stack updating after the fact causing particle changes.
        this(tx, ty, tz, ticks, paradox_uuid, itemStack.copy());
    }

    @Nonnull
    @Override
    public ParticleType<ParadoxParticleData> getType() {
        return ModParticles.PARADOXPARTICLE.get();
    }
}

