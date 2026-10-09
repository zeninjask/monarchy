// MDVLCraft first-join kit and clean-up

PlayerEvents.loggedIn(event => {
  const player = event.player
  const data = player.persistentData

  // One Map Atlas per player, once. An atlas with no data gets Map Atlases' free empty maps when first carried.
  if (!data.getBoolean('mdvlcraft_starting_atlas')) {
    data.putBoolean('mdvlcraft_starting_atlas', true)
    player.give('map_atlases:atlas')
  }

  // Villager Recruits hands out its manual on first login; take it away once that has happened.
  event.server.scheduleInTicks(20, () => {
    const inventory = player.inventory
    for (let slot = 0; slot < inventory.getContainerSize(); slot++) {
      const stack = inventory.getItem(slot)
      if (stack.id == 'minecraft:written_book' && stack.nbt && stack.nbt.getBoolean('VRManual')) {
        inventory.setItem(slot, Item.empty)
      }
    }
  })
})
