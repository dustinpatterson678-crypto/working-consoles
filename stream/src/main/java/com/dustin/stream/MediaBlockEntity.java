package com.dustin.stream;

import com.dustin.wcore.block.ConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Smart TV / Cinema Projector. A console as far as cables, outlets and power go; its own picture is
 * the built-in browser, and HDMI 1-3 show consoles plugged in with an HDMI Cable.
 */
public class MediaBlockEntity extends ConsoleBlockEntity {
   public static final int PORTS = 3;
   private final long[] hdmi = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
   private int input;

   public MediaBlockEntity(BlockPos pos, BlockState state) {
      super(StreamMod.MEDIA_BE, pos, state);
   }

   @Override
   public String consoleName() {
      return this.getBlockState().getBlock() instanceof ProjectorBlock ? "Cinema Projector" : "Smart TV";
   }

   /** 0 = the TV's own apps (browser), 1..3 = HDMI port. */
   public int input() {
      return this.input;
   }

   public BlockPos hdmi(int port) {
      return port < 1 || port > PORTS || this.hdmi[port - 1] == Long.MIN_VALUE ? null : BlockPos.of(this.hdmi[port - 1]);
   }

   /** Plugs a console in (same port if it's already connected) and switches to it. Returns the port. */
   public int plugHdmi(BlockPos console) {
      int port = -1;
      for (int i = 0; i < PORTS && port < 0; i++) {
         if (this.hdmi[i] == console.asLong()) {
            port = i;
         }
      }
      for (int i = 0; i < PORTS && port < 0; i++) {
         if (this.hdmi[i] == Long.MIN_VALUE) {
            port = i;
         }
      }
      if (port < 0) {
         port = PORTS - 1;
      }
      this.hdmi[port] = console.asLong();
      this.input = port + 1;
      this.sync();
      return port + 1;
   }

   public int unplugAllHdmi() {
      int n = 0;
      for (int i = 0; i < PORTS; i++) {
         if (this.hdmi[i] != Long.MIN_VALUE) {
            this.hdmi[i] = Long.MIN_VALUE;
            n++;
         }
      }
      this.input = 0;
      this.sync();
      return n;
   }

   /** Smart TV -> HDMI 1 -> HDMI 2 -> HDMI 3, skipping empty ports. */
   public int nextInput() {
      int next = this.input;
      for (int step = 0; step < PORTS + 1; step++) {
         next = (next + 1) % (PORTS + 1);
         if (next == 0 || this.hdmi(next) != null) {
            break;
         }
      }
      this.input = next;
      this.sync();
      return next;
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      for (int i = 0; i < PORTS; i++) {
         out.putLong("hdmi" + (i + 1), this.hdmi[i]);
      }
      out.putInt("input", this.input);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      for (int i = 0; i < PORTS; i++) {
         this.hdmi[i] = in.getLongOr("hdmi" + (i + 1), Long.MIN_VALUE);
      }
      this.input = in.getIntOr("input", 0);
   }
}
