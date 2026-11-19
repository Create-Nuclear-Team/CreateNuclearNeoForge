ServerEvents.recipes(event => {
    // sortie, entrée
    event.recipes.createnuclear.enriched('minecraft:diamond', 'minecraft:coal_block')
    event.recipes.createnuclear.enriched(
        ['2x minecraft:glowstone_dust', { item: 'minecraft:gold_nugget', chance: 0.25 }],
        '#c:dusts/redstone'
    )

    // Seulement sans Create Dragons Plus (sinon : recettes freezing de Dragons Plus)
    if (Platform.isLoaded('create_dragons_plus') === false) {
        event.recipes.createnuclear.snow_powder('minecraft:packed_ice', 'minecraft:ice')
    }
})
