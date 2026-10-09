Village Recruits - configuration
================================

Six files, split by subject. Every option lives in exactly one of them, under the
[section] named below. If you cannot find a key, search all six for its name: the
section it is in is what decides the file, not what it sounds like it is about.

  economy.toml     [economy]  [materials] [equipment] [trade] [merchant]
                   [convoy]   [firearms]  [armory]
                   money, stockpiles, production, the village merchant,
                   auto-sell, convoys, troop equipment and the Armory

  military.toml    [attack]   [combat]    [squads]   [movement] [troopmove]
                   [elites]   [rewards]   [intel]    [fog]      [battles]
                   [tanks]
                   war, patrols, commanders, sieges, field battles, scouting,
                   fog of war and the tank factory

  politics.toml    [diplomacy] [rebellion] [claims]  [leader]
                   [expansion] [special]   [contracts]
                   alliances and wars, rebellions, village founding and
                   spacing, faction archetypes, the Noble's contract board

  building.toml    [roads] [repair] [demolition] [spawnprotect]
                   roads and bridges, the repairman, demolition when a claim
                   is deleted, grief and mob-spawn protection

  general.toml     [promotion] [announcements] [travel] [offline] [update]
                   promotions, chat announcements, the offline village model,
                   the update notice

  technology.toml  [technology]
                   the tech tree: rates, AI priorities, what is disabled

Other things in this folder
---------------------------

  .internal/   one empty file per one-shot config migration that has already been
               applied. Deleting one makes that migration RUN AGAIN and overwrite
               the setting it changed, so leave them alone unless that is what you
               want.

  backups/     copies made by /vrconfig reset, named <file>.toml.bak-<timestamp>.
               Nothing ever deletes these; remove them yourself when you no longer
               want them.

Useful to know
--------------

  * Changing a DEFAULT in a mod update does not change a file that already exists.
    Forge never rewrites a key your file already has. To take new defaults for a
    whole file:  /vrconfig reset <file> confirm   (it backs the old one up first).

  * /vrconfig reset all confirm   resets every file.

  * Most values apply live. A few are read once at world load.
