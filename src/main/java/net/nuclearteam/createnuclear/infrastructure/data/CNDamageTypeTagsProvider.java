package net.nuclearteam.createnuclear.infrastructure.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.nuclearteam.createnuclear.CNDamageTypes;
import net.nuclearteam.createnuclear.CreateNuclear;

import java.util.concurrent.CompletableFuture;

// DamageType is a datapack registry: our entries (CNDamageTypes) only exist in the enriched
// HolderLookup.Provider produced by GeneratedEntriesProvider, not in Registrate's own lookup
// provider. That's why this is a plain TagsProvider wired manually in CreateNuclearDatagen,
// instead of a Registrate dynamic tag generator (which would fail validation, missing references).
public class CNDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public CNDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, CreateNuclear.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.NO_KNOCKBACK)
                .add(CNDamageTypes.RADIATION)
                .add(CNDamageTypes.FAN_RADIATION)
        ;
    }

    @Override
    public String getName() {
        return "CreateNuclear Damage Type Tags";
    }
}
