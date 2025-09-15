package igentuman.nc.block.entity.processor;

import igentuman.nc.content.processors.Processors;
import igentuman.nc.recipes.ingredient.FluidStackIngredient;
import igentuman.nc.recipes.ingredient.ItemStackIngredient;
import igentuman.nc.recipes.type.NcRecipe;
import igentuman.nc.util.capability.CustomEnergyStorage;
import igentuman.nc.util.annotation.NBTField;
import igentuman.nc.util.annotation.NothingNullByDefault;
import igentuman.nc.handler.sided.SidedContentHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import static igentuman.nc.handler.config.CommonConfig.ENERGY_GENERATION;

public class SteamTurbineBE extends NCProcessorBE {

    @NBTField
    public double efficiency = 1.0;
    public long newstorage = 0;
    public SteamTurbineBE(BlockPos pPos, BlockState pBlockState) {
        super(pPos, pBlockState, Processors.STEAM_TURBINE);
        particle1 = ParticleTypes.EFFECT;
    }

    @Override
    public void tickServer()
    {
        sendOutPower();
        efficiency = Math.max(0.0001, Math.min(10, efficiency));
        refresh();
        if(newstorage > 0) newstorage = sendOutHugePower(newstorage);
        if(energyStorage().getEnergyStored()>=energyStorage().getMaxEnergyStored()) {
            return;
        }
        super.tickServer();
    }

    @Override
    public void processRecipe() {
        if(!hasRecipe()) {
            updateRecipe();
        }
        if(!hasRecipe()) return;
        if(contentHandler().fluidHandler.getFluidInSlot(0).getAmount()> recipe.getInputFluids(0).get(0).getAmount()){
        FluidStack coolant = recipe.getOutputFluids().get(0);
        FluidStack steam = recipe.getInputFluids(0).get(0);
        FluidStack currentSteam = contentHandler().fluidHandler.getFluidInSlot(0);
        FluidStack currentOutput = contentHandler().fluidHandler.getFluidInSlot(1);
        double capacity = contentHandler().fluidHandler.tanks.get(1).getCapacity() - currentOutput.getAmount();
        int ops = Math.min((int) (capacity/coolant.getAmount()),(int)(currentSteam.getAmount()/steam.getAmount()));     
        contentHandler().fluidHandler.tanks.get(0).drain(ops*steam.getAmount(), IFluidHandler.FluidAction.EXECUTE);  
        FluidStack out = coolant.copy();
        out.setAmount(ops*coolant.getAmount());
        contentHandler().fluidHandler.tanks.get(1).fill(out, IFluidHandler.FluidAction.EXECUTE);
	long flux = (long)(getEnergyTransferPerTick() * 4 * recipe.getEnergy()*ENERGY_GENERATION.GENERATION_MULTIPLIER.get() * ops);
        if(flux < 2048000000L){
        energyStorage().addEnergy((int) (getEnergyTransferPerTick()*4*recipe.getEnergy()*ENERGY_GENERATION.GENERATION_MULTIPLIER.get()*ops));
        sendOutPower();
        } else{
        	newstorage += flux;
        }
        return;
}
        if (!recipeInfo().process(speedMultiplier()*efficiency)) {
            return;
        }
        energyStorage().addEnergy((int) (getEnergyTransferPerTick()*recipe.getEnergy()*ENERGY_GENERATION.GENERATION_MULTIPLIER.get()));
    }

    @Override
    public double speedMultiplier()
    {
        return super.speedMultiplier();
    }

    @Override
    protected CustomEnergyStorage createEnergy() {
        //todo read config
        return new CustomEnergyStorage(2048000000, 0, 2048000000) {
            @Override
            protected void onEnergyChanged() {
                setChanged();
            }
        };
    }
    protected void refresh(){
        if(contentHandler().fluidHandler.tanks.get(1).getCapacity() < 10000000){
	contentHandler().fluidHandler.tanks.get(0).setCapacity(10000000);
	contentHandler().fluidHandler.tanks.get(1).setCapacity(10000000);
	}
    }
    protected int getEnergyTransferPerTick() {
        return ENERGY_GENERATION.STEAM_TURBINE.get();
    }

    protected int getMaxEnergyStored() {
        return 2048000000;
    }
    @Override
    public int getEnergyCapacity()
    {
	return 2048000000;
    }

    @NothingNullByDefault
    public static class Recipe extends NcRecipe {
        public Recipe(ResourceLocation id,
                      ItemStackIngredient[] input, ItemStackIngredient[] output,
                      FluidStackIngredient[] inputFluids, FluidStackIngredient[] outputFluids,
                      double timeModifier, double powerModifier, double heatModifier, double rarity) {
            super(id, input, output, inputFluids, outputFluids, timeModifier, powerModifier, heatModifier, 1);
        }

        @Override
        public String getCodeId() {
            return Processors.STEAM_TURBINE;
        }
    }
}

