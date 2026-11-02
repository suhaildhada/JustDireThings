package com.direwolf20.justdirethings.common.blockentities.basebe;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public interface FluidMachineBE {
    ContainerData getFluidContainerData();

    default void setFluidStack(Fluid fluid, int amt) {
        getFluidTank().setFluid(new FluidStack(fluid, amt));
    }

    FluidTank getFluidTank();

    default int getAmountStored() {
        return getFluidTank().getFluidAmount();
    }

    default void setAmountStored(int value) {
        getFluidTank().getFluid().setAmount(value);
    }

    default boolean isFull() {
        return getFluidStack().getAmount() >= getMaxMB();
    }

    default FluidStack getFluidStack() {
        return getFluidTank().getFluid();
    }

    default int getMaxMB() {
        return 8000;
    }
}
