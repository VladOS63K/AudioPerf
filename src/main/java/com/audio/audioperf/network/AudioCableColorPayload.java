package com.audio.audioperf.network;

import com.audio.audioperf.AudioPerf;
import com.audio.audioperf.tile.TileAudioCable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;

public record AudioCableColorPayload(BlockPos pos, int color) implements CustomPacketPayload {

    public static final Type<AudioCableColorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AudioPerf.MODID, "audio_cable_color"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, AudioCableColorPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeInt(p.color);
            },
            buf -> new AudioCableColorPayload(buf.readBlockPos(), buf.readInt())
    );

    private static Method setBlocksDirty;

    public static void handle(AudioCableColorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.getBlockEntity(payload.pos) instanceof TileAudioCable cable) {
                cable.setColor(payload.color);
                if (level.isClientSide) {
                    forceRerender(level, payload.pos);
                }
            }
        });
    }

    private static void forceRerender(Level level, BlockPos pos) {
        if (setBlocksDirty == null) {
            try {
                setBlocksDirty = Class.forName("net.minecraft.client.level.ClientLevel")
                        .getMethod("setBlocksDirty", int.class, int.class, int.class, int.class, int.class, int.class);
            } catch (Exception e) {
                return;
            }
        }
        try {
            setBlocksDirty.invoke(level,
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX(), pos.getY(), pos.getZ());
        } catch (Exception ignored) {
        }
    }
}
