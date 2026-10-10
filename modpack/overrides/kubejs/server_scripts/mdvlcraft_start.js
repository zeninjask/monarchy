// MDVLCraft first-join kit and clean-up

PlayerEvents.loggedIn(event => {
  const player = event.player
  const data = player.persistentData

  // A Book and Quill for notes, once.
  if (!data.getBoolean('mdvlcraft_starting_book')) {
    data.putBoolean('mdvlcraft_starting_book', true)
    player.give('minecraft:writable_book')
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
