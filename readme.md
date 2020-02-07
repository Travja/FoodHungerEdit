# FoodHungerEdit

[Download Latest Build](https://github.com/Travja/FoodHungerEdit/raw/master/builds/FoodHungerEdit.jar)

## Tasks

- [ ] Fighting a user for more than 8 seconds should bring your hunger down by 2.
- [ ] Allow items to stack when picking up and moving around inventory.
- [x] I want a plugin that just adjusts how fast you loose hunger and the hunger gained by eating food/consuming non edible foods.
- [x] Food should "rot" after 3 minecraft days. It should turn into Rotten Flesh named the following
Name: &4Crow Food
Lore: &7[&cRotten&7]
- [x] If eaten the player should be given nausea for 10 seconds and remove 4 hunger bars
- [x] Food should change status once every minecraft day and should change regardless of where it has been placed. inventory/chest/shulker box/backpack/etc [If placed in an enderchest, if possible, deny rotting until it is removed again]
- [x] Fresh, stale, and musty food should not be allowed to stack with eachother, but food items that are fresh can stack with fresh and etc.
- [x] Added lore on all non rotten food items should show how old the food is.
Day one should be "&7[&bFresh&7]"
Day two should be "&7[&9Stale&7]"
Day three should be "&7[&1Musty&7]"
- [x] One hunger bar removed every 2 minutes.
- [x] Running/walking/etc should not bring down hunger.
- [x] Making your hunger bar full by eating should give you strength 1 for 30 seconds. but should give you slowness 1 for 10 seconds as a "you are full" effect.
- [x] Having 0 hunger and eating so your bar is full in under 30 seconds should give you nausea for 5 seconds. [Optional. its not needed but would be cool!]

### FOOD ITEMS

- [x] Glistering_Melon_Slice [Same effects as suspicious stew. Randomized effect given when eaten if possible] [Only gives 3 hunger bars]
- [x] [All Food Items should restore 5 Hunger][Poison effects from Spider Eye should be removed] [All other consumable food items not listed should give 1 hunger bar and slowness 1]
- [x] Mutton
- [x] Potato
- [x] Cooked_Mutton
- [x] Beetroot
- [x] Black_Dye [Give only half a hunger bar]
[You can just make the item disappear and give a hunger bar since dye isn't consumable]
- [x] Dried_Kelp
- [x] Spider_Eye
- [x] Cooked_Chicken
- [x] Cooked_Beef [Gives only 2 Hunger but gives strength 2 for 1 minute]
- [x] Raw_Porkchop [Gives 1 hunger bar and nausea for 5 seconds]
- [x] Raw_Rabbit
- [x] Raw_Chicken
- [x] Raw_Beef
- [x] Raw_Cod
- [x] Raw_Salmon
- [x] Prismarine_Shard [Gives 1 hunger bar and gives dolphins grace for 5 seconds]
- [x] Cooked_Porkchop
- [x] Melon_Slice [Gives only 2 Hunger but gives strength 2 for 1 minute]


~~Ideas for commands:~~
~~/FoodRot Pause~~
~~[Pauses all food from rotting in its current phase until resumed]~~

~~/FoodRot Resume~~
~~[Resumes food rotting]~~

~~Ideas for permissions: All players affected by this.~~
~~Food.PauseResume~~
~~[Allows user with permission to use the two commands. Users without the permission cannot run the commands]~~
