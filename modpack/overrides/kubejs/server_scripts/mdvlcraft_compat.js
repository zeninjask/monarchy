// MDVLCraft mod compatibility

ServerEvents.recipes(event => {
  // Naturalist's cooked egg and Farmer's Delight's fried egg both cook a plain egg; keep Farmer's Delight's
  event.remove({ id: 'naturalist:cooked_egg' })
  event.remove({ id: 'naturalist:cooked_egg_from_smoking' })
  event.remove({ id: 'naturalist:cooked_egg_from_campfire_cooking' })
})

// Serene Seasons growing seasons for Farmer's Delight crops
const FD_SEASONS = {
  spring: { blocks: ['farmersdelight:cabbages', 'farmersdelight:onions'], items: ['farmersdelight:cabbage_seeds', 'farmersdelight:onion'] },
  summer: { blocks: ['farmersdelight:budding_tomatoes', 'farmersdelight:tomatoes', 'farmersdelight:rice', 'farmersdelight:rice_panicles'], items: ['farmersdelight:tomato_seeds', 'farmersdelight:rice'] },
  autumn: { blocks: ['farmersdelight:cabbages', 'farmersdelight:onions'], items: ['farmersdelight:cabbage_seeds', 'farmersdelight:onion'] }
}

ServerEvents.tags('block', event => {
  for (const season in FD_SEASONS) event.add(`sereneseasons:${season}_crops`, FD_SEASONS[season].blocks)
})

ServerEvents.tags('item', event => {
  for (const season in FD_SEASONS) event.add(`sereneseasons:${season}_crops`, FD_SEASONS[season].items)
})
