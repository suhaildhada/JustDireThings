package com.direwolf20.justdirethings.common.blockentities;

import com.direwolf20.justdirethings.common.blockentities.basebe.BaseMachineBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.FluidContainerData;
import com.direwolf20.justdirethings.common.blockentities.basebe.FluidMachineBE;
import com.direwolf20.justdirethings.common.blockentities.basebe.RedstoneControlledBE;
import com.direwolf20.justdirethings.common.capabilities.JustDireFluidTank;
import com.direwolf20.justdirethings.setup.Registration;
import com.direwolf20.justdirethings.util.MiscHelpers;
import com.direwolf20.justdirethings.util.interfacehelpers.RedstoneControlData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.LiquidBlock.LEVEL;

public class FluidCollectorT1BE extends BaseMachineBE implements RedstoneControlledBE, FluidMachineBE {
    public final FluidContainerData  fluidContainerData;
    public       RedstoneControlData redstoneControlData = new RedstoneControlData();
    List<BlockPos> positionsToPlace = new ArrayList<>();

    public FluidCollectorT1BE(BlockPos pPos, BlockState pBlockState) {
        this(Registration.FluidCollectorT1BE.get(), pPos, pBlockState);
    }

    public FluidCollectorT1BE(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        MACHINE_SLOTS      = 1; //Slot for a bucket
        fluidContainerData = new FluidContainerData(this);
    }

    @Override
    public ContainerData getFluidContainerData() {
        return fluidContainerData;
    }

    public JustDireFluidTank getFluidTank() {
        return getData(Registration.MACHINE_FLUID_HANDLER);
    }

    public boolean isFull() {
        return getFluidStack().getAmount() >= getMaxMB();
    }

    public int getMaxMB() {
        return 8000;
    }

    @Override
    public RedstoneControlData getRedstoneControlData() {
        return redstoneControlData;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void tickClient() {
    }

    @Override
    public void tickServer() {
        super.tickServer();
        handleItemStack();
        doFluidCollect();
    }

    public void handleItemStack() {
        FluidStack fluidStack = getFluidStack();
        if (fluidStack.isEmpty()) {
            return;
        }
        ItemStack itemStack = getItemStack();
        if (!isStackValid(itemStack, fluidStack)) {
            return;
        }
        IFluidHandlerItem fluidHandlerItem = itemStack.getCapability(Capabilities.FluidHandler.ITEM);
        int insertAmt = fluidHandlerItem.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
        if (insertAmt > 0) {
            FluidStack extractedStack = getFluidTank().drain(Math.min(insertAmt, 1000), IFluidHandler.FluidAction.EXECUTE);
            fluidHandlerItem.fill(extractedStack, IFluidHandler.FluidAction.EXECUTE);
            if (itemStack.getItem() instanceof BucketItem) {
                getMachineHandler().setStackInSlot(0, fluidHandlerItem.getContainer());
            }

        }
    }

    public void doFluidCollect() {
        if (clearTrackerIfNeeded()) {
            positionsToPlace.clear();
            return;
        }
        if (!canCollect()) {
            return;
        }
        FakePlayer fakePlayer = getFakePlayer((ServerLevel) level);
        if (isActiveRedstone() && canRun() && positionsToPlace.isEmpty()) {
            positionsToPlace = findSpotsToCollect(fakePlayer);
        }
        if (positionsToPlace.isEmpty()) {
            return;
        }
        if (canRun()) {
            BlockPos blockPos = positionsToPlace.removeFirst();
            collectFluid(blockPos);
        }
    }

    public ItemStack getItemStack() {
        return getMachineHandler().getStackInSlot(0);
    }

    public boolean isStackValid(ItemStack itemStack, FluidStack fluidStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        if (fluidStack.isEmpty()) {
            return false;
        }
        IFluidHandlerItem fluidHandlerItem = itemStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandlerItem == null) {
            return false;
        }
        int amtFilled = fluidHandlerItem.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
        return amtFilled != 0;
    }

    public boolean clearTrackerIfNeeded() {
        if (positionsToPlace.isEmpty()) {
            return false;
        }
        if (!canCollect()) {
            return true;
        }
        return !isActiveRedstone() && !redstoneControlData.redstoneMode.equals(MiscHelpers.RedstoneMode.PULSE);
    }

    public boolean canCollect() {
        return true;
    }

    public List<BlockPos> findSpotsToCollect(FakePlayer fakePlayer) {
        List<BlockPos> returnList = new ArrayList<>();
        BlockPos blockPos = getBlockPos().relative(getBlockState().getValue(BlockStateProperties.FACING));
        if (isBlockPosValid(blockPos, fakePlayer)) {
            returnList.add(blockPos);
        }
        return returnList;
    }

    public boolean collectFluid(BlockPos blockPos) {
        LiquidBlock liquidBlock = getLiquidBlockAt(blockPos);
        if (liquidBlock == null) {
            return false;
        }
        if (!isBlockValidForTank(liquidBlock)) {
            return false; //Check again here, in case we picked up water, and now we are operating on lava, before clearing the area list
        }
        FluidStack fluidStack = new FluidStack(liquidBlock.fluid, 1000);
        if (getFluidTank().fill(fluidStack, IFluidHandler.FluidAction.SIMULATE) < 1000) {
            return false;
        }
        if (level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3)) {
            getFluidTank().fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
            level.playSound(null, blockPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1F, 1.0F);
            return true;
        }
        return false;
    }

    public boolean isBlockPosValid(BlockPos blockPos, FakePlayer fakePlayer) {
        BlockState blockState = level.getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof LiquidBlock liquidBlock)) {
            return false;
        }
        if (blockState.getValue(LEVEL) != 0) {
            return false;
        }
        if (!isBlockValidForTank(liquidBlock)) {
            return false;
        }
        return canPlaceAt(level, blockPos, fakePlayer);
    }

    public LiquidBlock getLiquidBlockAt(BlockPos blockPos) {
        if (level.getBlockState(blockPos).getBlock() instanceof LiquidBlock liquidBlock) {
            return liquidBlock;
        }
        return null;
    }

    public boolean isBlockValidForTank(LiquidBlock liquidBlock) {
        return getFluidStack().isEmpty() || getFluidStack().is(liquidBlock.fluid);
    }

    public FluidStack getTankStack() {
        return getFluidTank().getFluid();
    }

    public boolean isStackValid(FluidStack fluidStack) {
        if (fluidStack.isEmpty()) {
            return false;
        }
        return fluidStack.getAmount() >= 1000;
    }
}
