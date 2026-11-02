package com.dustin.arcadebuild.hall;

import com.dustin.arcadebuild.ArcadeBuildMod;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class HallParts {
   private HallParts() {
   }

   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

   public static class Fx extends Block {
      public Fx(Properties p) {
         super(p);
         this.registerDefaultState(this.defaultBlockState().setValue(LIT, true));
      }

      @Override
      protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
         b.add(LIT);
      }

      @Override
      protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
         return ItemStack.EMPTY;
      }
   }

   public static class Fs extends Fx {
      public Fs(Properties p) {
         super(p);
         this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
      }

      @Override
      protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
         b.add(LIT, FACING);
      }
   }

   public static class Fd extends Block {
      public Fd(Properties p) {
         super(p);
         this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
      }

      @Override
      protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
         b.add(FACING, HALF);
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         if (!level.isClientSide() && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            HallWiring.a4(sl, state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos, sp);
         }
         return InteractionResult.SUCCESS;
      }

      @Override
      protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
         return ItemStack.EMPTY;
      }
   }

   public static class Fp extends Block {
      public Fp(Properties p) {
         super(p);
      }

      @Override
      protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean data) {
         return ItemStack.EMPTY;
      }
   }

   private static Properties props(SoundType sound) {
      return Properties.of().strength(-1.0F, 3600000.0F).noLootTable().sound(sound).isValidSpawn((s, l, p, e) -> false);
   }

   private static <B extends Block> B reg(String name, Function<Properties, B> f, Properties p) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ArcadeBuildMod.id(name));
      return Registry.register(BuiltInRegistries.BLOCK, key, f.apply(p.setId(key)));
   }

   public static final Block P_A = reg(K.d("Mhj024iEdFw1H83M"), Fp::new, props(SoundType.WOOL));
   public static final Block P_B = reg(K.d("Mhj024iEdFw1H83P"), Fp::new, props(SoundType.WOOL));
   public static final Block P_C = reg(K.d("Mhj024iEdFw1H83O"), Fp::new, props(SoundType.WOOL));
   public static final Block P_D = reg(K.d("Mhj024iEdFw1H83J"), Fp::new, props(SoundType.WOOL));
   public static final Block P_E = reg(K.d("Mhj024iEdFw1H83I"), Fp::new, props(SoundType.WOOL));
   public static final Block F_A = reg(K.d("Mhj024iSeV0/Ac3M"), Fp::new, props(SoundType.WOOL));
   public static final Block F_B = reg(K.d("Mhj024iSeV0/Ac3P"), Fp::new, props(SoundType.MUD));
   public static final Block C_A = reg(K.d("Mhj024iXcFs8GvzKko8="), Fp::new, props(SoundType.WOOL));
   public static final Block P_F = reg(K.d("Mhj024iEdFw1H83L"), Fp::new, props(SoundType.STONE));
   public static final Block F_C = reg(K.d("Mhj024iSeV0/Ac3O"), Fp::new, props(SoundType.STONE));
   public static final Block P_G = reg(K.d("Mhj024iEdFw1H83K"), Fp::new, props(SoundType.METAL));
   public static final Block F_D = reg(K.d("Mhj024iSeV0/Ac3J"), Fp::new, props(SoundType.METAL));
   public static final Block T_A = reg(K.d("Mhj024iAZ1s9LPM="), Fp::new, props(SoundType.METAL));
   public static final Block T_B = reg(K.d("Mhj024iAZ1s9LPA="), Fp::new, props(SoundType.STONE));
   public static final Fx L_A = reg(K.d("Mhj024iYdF8gLPM="), Fx::new, props(SoundType.GLASS).lightLevel(s -> s.getValue(LIT) ? 15 : 0));
   public static final Fx L_B = reg(K.d("Mhj024iYdF8gLPA="), Fx::new, props(SoundType.METAL).lightLevel(s -> s.getValue(LIT) ? 12 : 0));
   public static final Fs S_A = reg(K.d("Mhj024iHfFU+LPM="), Fs::new, props(SoundType.METAL).lightLevel(s -> s.getValue(LIT) ? 6 : 0));
   public static final Fd D_A = reg(K.d("Mhj024iQel0iLPM="), Fd::new, props(SoundType.METAL));

   public static final SoundEvent Q1 = sound(K.d("Mhj02/mVeFAPQg==")), Q2 = sound(K.d("Mhj02/mVeFAPQQ==")), Q3 = sound(K.d("Mhj02/mVeFAPQA==")), Q4 = sound(K.d("Mhj02/mVeFAPRw==")),
      Q5 = sound(K.d("Mhj02/mVeFAPRg==")), Q6 = sound(K.d("Mhj02/mVeFAPRQ==")), Q7 = sound(K.d("Mhj02/mVeFAPRA==")), Q8 = sound(K.d("Mhj02/mVeFAPSw=="));

   private static SoundEvent sound(String name) {
      Identifier id = ArcadeBuildMod.id(name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static boolean lamp(BlockState s) {
      return s.getBlock() instanceof Fx && !(s.getBlock() instanceof Fs);
   }

   public static void init() {
   }
}
