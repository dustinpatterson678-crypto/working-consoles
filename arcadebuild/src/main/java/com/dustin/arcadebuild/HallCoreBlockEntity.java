package com.dustin.arcadebuild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers which storefront this hall belongs to, and sends players who walk out the doors back there. */
public class HallCoreBlockEntity extends BlockEntity {
   private String returnDim = "minecraft:overworld";
   private long returnPos = Long.MIN_VALUE;
   private int returnFacing = 2;
   public int version;

   public HallCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   /** The hall's floor-origin cell (lx 0, ly 0, lz 0). */
   public BlockPos origin() {
      return this.worldPosition.offset(GrandHall.CORE_LX, -GrandHall.CORE_LY, -GrandHall.CORE_LZ);
   }

   public void setReturn(ResourceKey<Level> dim, BlockPos facade, Direction facing) {
      this.returnDim = dim.identifier().toString();
      this.returnPos = facade.asLong();
      this.returnFacing = facing.get3DDataValue();
      this.setChanged();
   }

   public ServerLevel returnLevel() {
      if (!(this.level instanceof ServerLevel sl)) return null;
      Identifier id = Identifier.tryParse(this.returnDim);
      return id == null ? null : sl.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
   }

   public BlockPos returnPos() {
      return this.returnPos == Long.MIN_VALUE ? null : BlockPos.of(this.returnPos);
   }

   public Direction returnFacing() {
      Direction d = Direction.from3DDataValue(this.returnFacing);
      return d.getAxis().isHorizontal() ? d : Direction.NORTH;
   }

   void serverTick() {
      if (!(this.level instanceof ServerLevel sl) || sl.getGameTime() % 4 != 0) return;
      BlockPos o = this.origin();
      for (ServerPlayer p : new java.util.ArrayList<>(sl.players())) {      // copy: walking out changes the list
         double lx = (o.getX() + 0.5) - p.getX(), lz = p.getZ() - o.getZ(), ly = p.getY() - o.getY();
         if (Math.abs(lx) <= 2.6 && lz < 0.45 && lz > -0.5 && ly >= 0.5 && ly < 5) GrandHall.exit(p, this);
      }
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putString("rdim", this.returnDim);
      out.putLong("rpos", this.returnPos);
      out.putInt("rface", this.returnFacing);
      out.putInt("ver", this.version);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.returnDim = in.getStringOr("rdim", "minecraft:overworld");
      this.returnPos = in.getLongOr("rpos", Long.MIN_VALUE);
      this.returnFacing = in.getIntOr("rface", 2);
      this.version = in.getIntOr("ver", 1);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
