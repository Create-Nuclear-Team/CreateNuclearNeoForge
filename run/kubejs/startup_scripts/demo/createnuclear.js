CreateNuclearEvents.fluidRodStats(event => {
    event.fuel('minecraft:blaze_rod')
        .baseRodHeat(8)
        .proximityRodHeat(0.5)
        .rodTimer(2400)

    event.cooler('minecraft:packed_ice')
        .baseRodHeat(-4)
        .proximityRodHeat(0.25)
        .rodTimer(1200)
        .ratio(2)

    event.fluid('minecraft:water')
        .maxHeat(500)
        .efficiency(50)
})
