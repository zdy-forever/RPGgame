# Activity 1 — User Stories

## 0. Overall Assumptions

### 0.1 Selected Features

Our team originally had four members, but one of us is absent.
So only 7 features in this document (tutor has already permitted). 4 of them are required basic features
and others are special features. But the game contains more than 7 features.

| No. | Feature | Type | Owner | Other contributors | Main issues |
| --- | --- | --- | --- | --- | --- |
| F1 | Movement | Basic | Daoyan Zhu | — | #1, #4, #8 |
| F2 | Map / Level | Basic | Daoyan Zhu | Jiaqi Chen (minimap, coordinate HUD) | #2, #3, #18, #24, #25, #29, #34, #45 |
| F3 | Enemies / Boss | Basic | Daoyan Zhu | Jiaqi Chen (player health bar) | #5, #7, #13, #16, #27, #28, #46, #49 |
| F4 | Inventory | Basic | Hao Liu | Jiaqi Chen (inventory, weapon and storage chest prototypes); Daoyan Zhu (P potion hotkey) | #33, #35, #47, #50 |
| F5 | Elemental Reaction Combat | Special | Daoyan Zhu | — | #6, #8–#12, #14, #17, #19, #20 |
| F6 | Save / Load | Special | Hao Liu | — | #48 |
| F7 | NPC Dialogue & Tutorial | Special | Jiaqi Chen | Daoyan Zhu (responsive dialogue layout, dialogue option menu) | #22, #25, #30, #36 |


### 0.2 Level of Prioritisation

- **Must**: without it, the game cannot complete the basic loop of "move → enter a level → fight the Boss → receive a reward".
- **Should**: significantly improves playability, or is a core behaviour of a special feature.
- **Could**: nice to have; can be postponed if time runs short.

---

## F1 Movement

### US-1.1 Eight-directional movement

- Story: As a player, I want my character to keep moving while I hold a movement key (like WASD) so that I can explore the map freely.
#### Acceptance Criteria
1. Given if my character stands in a spacious place and presses the When button, Then the character will continue to move up.
2. Given When my character is moving and when he moves his hand away from all the moving keys, Then the character will stop immediately.
3. Given My character is standing in a spacious place. Holding down the When "up" and "right" keys will make the Then move obliquely, and it will not move faster than moving in one direction.
4. Given if my character stands in a spacious place and keeps pressing When "left" and "right", Then will not move.
#### Estimated time: about 4 hours | Priority: Must

### US-1.2 Wall collision

- Story: As a player, I want my character to be blocked by walls and obstacles so that the map feels solid and the level layout actually matters.
#### Acceptance Criteria
1. Given When there is a wall in front of my character, if I keep walking there like this, Then the character will stop at the edge of the wall and not overlap.
2. Given When my character touches a vertical wall, when if I walk obliquely towards the wall, Then the character can't move and will slide along the wall.
3. Given When my character moves fast (for example, when the dash), when it touches a thin wall, Then will still stop and not go through the wall.
#### Estimated time: about 6 hours | Priority: Must

### US-1.3 Map boundaries

- Story: As a player, I want my character to stay inside the map so that I don't end up lost in empty areas with nothing in them.
#### Acceptance Criteria
1. If you keep pressing "left" when your role is at the left end of the map, the role will remain in the map.
2. If your character moves obliquely outward when it is in the corner of the map, neither end of the character will cross it.
#### Estimated time: about 2 hours | Priority: Must

### US-1.4 Dash

- Story: As a player, I want to dash a short distance in the direction I am facing so that I can dodge the Boss's attacks.
#### Acceptance Criteria
1. Premise: Operation when your dash is not in cooldown: Press the dash key. Result: Your character moves much faster than normal walking at the same time.
2. Premise: When you just dash (cooldown is about 1 second) Operation: Press the dash key again. Result: Nothing will happen.
3. Premise: Operation when there is a wall in the way: Result after dash: Your character stops at the wall and doesn't go through the wall.
#### Estimated time: about 6 hours | Priority: Should

### US-1.5 Camera follow

- Story: As a player, I want the camera to follow my character so that I can always see myself on maps that are bigger than the screen.
#### Acceptance Criteria
1. If the premise map is greater than screen, if my character moves to the right, the result screen will move together, and the character will always be displayed on the screen.
2. On the premise that when my character reaches the edge of the map, if I continue to walk to the edge like this, the camera will stop and nothing can be seen on the outside of the map.
3. When the premise map is not larger than screen (like Memorial Hall), if my character moves, the result screen will not move.
#### Estimated time: about 4 hours | Priority: Must

---

## F2 Map / Level

### US-2.1 Safe spawn in the Hub

- Story: As a new player, I want to start the game in a safe spot in the Hub so that I can look around before deciding where to go.
#### Acceptance Criteria
1. The screen reading of Whengame from given game ends. Then my character appears on the spawn point of Hub.
2. Given, my character was not caught in the When obstacle where just spawn confirmed where it was, and soon moved.
3. Given has just entered the Hub now, and there is no enemy around When that can attack me.
#### estimated time:about4hours:Priority

### US-2.2 Travel through portals

- Story: As a player, I want to be taken to another map when I walk into an open portal so that I can move between levels.
#### Acceptance Criteria
1. When the given portal opens, where is my character? Then my character is on the spawn point of the destination map.
2. Given, my character has just arrived at When through the portal, and Then standing next to the portal can't go back for a while.
3. Look at the When screen at the end of the given map change, without the attack and effect of the map before Then.
#### Estimated time: about 6 hours | Priority: Must

### US-2.3 Closed portals

- Story: As a player, I want a portal to stay closed and look closed until I am allowed to use it so that I know I can't enter that level yet.
#### Acceptance Criteria
1. Premise: When the portal is closed, operation: Even if your role enters it, the result: nothing will happen.
2. Premise: When the portal is closed, the operation: View the result: It looks different from the open portal.
#### Estimated time: about 4 hours | Priority: Should

### US-2.4 Choosing a destination with the Portal Guide

- Story: As a player, I want to tell the Portal Guide whether the portal should lead to the Training Ground, the Battle Map or the Memorial Hall so that one portal can take me to different places.
#### Acceptance Criteria
1. Given When my character is next to the Portal Guide, I can see the three options of then "training ground", "battle map" and "memorial hall" by pressing the e key.
2. When the given option is displayed, I use the up and down keys to select one, and press the E key, then Then portal will be set as the map, but it has not been forwarded.
   When I choose "Battle Map" when I haven't received the Boss's request, Then the option seems to be locked, and the portal can't be opened.
4. When I press the Escape key when 4. Given option is displayed, the Then menu closes and the portal settings remain unchanged.
#### Estimated time: about 6 hours | Priority: Should

### US-2.5 Unlocking a battle by accepting a quest

- Story: As a player, I want a guardian's battle map to unlock only after I accept that guardian's quest in the Hub so that the game has a clear order and goal.
#### Acceptance Criteria
1. Given I'm talking to quest Keeper When I will choose the guardian to make sure that quest becomes the Quest I'm doing now.
2. Given I received quest when, and I selected "Battle Map" in the portal guide to enter the portal, Then went to its guardian arena.
3. Given I won the guardian When I opened the quest list Then the guardian was marked as "completed".
#### Estimated time: about 6 hours | Priority: Should

### US-2.6 Five different arenas

- Story: As a player, I want every guardian to have its own arena so that each fight looks fresh.
#### Acceptance Criteria
1. You can see the background and name of arena of Then guardian A by entering the When battle map that accepts the given guardian a quest.
2. if you enter the When battle map that accepts given guardian b quest, Then the background and Boss's name are different from those of a.
#### Estimated time: about 6 hours | Priority: Could

### US-2.7 Training Ground

- Story: As a player, I want to hit a training target that doesn't fight back and see my damage numbers so that I can practise my skills safely and compare my damage.
#### Acceptance Criteria
1. there is only one Then training target in the given training ground that can see the When map. It will never move or attack me.
2. Attack 2. The Then screen hit by the When attack of the given training target shows the total damage, the last damage and the number of attacks, and the target will never fall.
3. Press the When R button that recorded several given injuries, Then the number will be reset, and all your physical strength will be restored.
4. Go back to the Then Hub, which talks with and confirms the NPC used for the When return in 4. Given Training Ground, and can't get the reward from the Boss.
#### Estimated time: about 6 hours | Priority: Could

### US-2.8 Minimap

- Story: As a player, I want a minimap that shows obstacles, portals, NPCs, me and the Boss so that I can find my target quickly and don't get lost.
#### Acceptance Criteria
1. When the given window is large enough, when I am on the Hub or battle map, Then knows that the minimap of my position will be displayed in the corner of the screen.
2. When given Boss is alive, when I look at minimap, Then Boss will use a red dot, and when boss dies, that point will disappear.
3. When the given minimap is displayed, I press the M key to open the big map, and Then press the M key to close it again.
4. When the given map is open, even if I press the move button, the character will not move until the Then map is closed.
#### Estimated time: about 6 hours | Priority: Should

### US-2.9 Memorial Hall and ending

- Story: As a player, I want the guardian stones in the Memorial Hall to light up as I win and an ending to play once all five are lit so that I can see my progress and feel like I have finished the game.
#### Acceptance Criteria
1. Given when I won two different guardians, I entered the When Memorial Hall, and there were just two stones shining.
2. Given when I beat the same guardian many times, if I enter When Memorial Hall, only one stone of the guardian is shining.
3. Given when I win all five guardians, if I talk to Old Sage in the Venn model hall, I will shed a line of Then ending, and then I can choose to watch credits.
#### Estimated time: about 6 hours | Priority: Could

### US-2.10 Live coordinates

- Story: As a player, I want to see my character's coordinates on the HUD so that I can follow directions like "the merchant is at (650, 250)" and tell others exactly where I am.
#### Acceptance Criteria
1. When the screen is large enough, as long as your role moves, HUD's coordinates will be updated immediately.
2. When your role stops, if you see HUD, coordinates will not change.
3. When the screen is very small, if you see HUD, coordinates will disappear, but your health will be displayed.
#### Estimated time: about 2 hours | Priority: Could

---

## F3 Enemies / Boss

### US-3.1 Seeing the Boss in the battle map

- Story: As a player, I want to see the Boss in the arena when I enter a battle map so that I know right away where my target is.
#### Acceptance Criteria
1. In the Then arena that is receiving the When of 1. Given quest into its battery diagram, the Boss is just reflected as 1.
2. Make sure that the location of When Boss that given boss has appeared is near the middle of arena, and there is no wall.
3. there is no When boss around the given hub or Memorial Hall.
#### Estimated time: about 4 hours | Priority: Must

### US-3.2 Boss health bar

- Story: As a player, I want to see the Boss's health bar so that I know how much health it has left and how close I am to winning.
#### Acceptance Criteria

1. When given Boss is full of physical strength, when I look at the boss's health bar, then Then bar is full.
2. Given if I get hurt When I attack Boss, Then bar will reduce the correct amount.
3. When given Boss's physical strength is zero, when I look at the screen, Then bar is empty and the boss disappears.

#### Estimated time: about 4 hours | Priority: Must

### US-3.3 Taking damage and seeing my health

- Story: As a player, I want to lose health when the Boss hits me and see my health on the HUD so that I can tell how much danger I am in.
#### Acceptance Criteria
1. When given is full of physical strength, if When Boss's attack hits him, Then HUD's physical strength will reduce the amount of that damage.
2. when given doesn't have much physical strength, he sees When HUD, and the length of Then physical strength bar is equal to the amount of remaining physical strength.
3. When given has little physical strength, if the injury is greater than when's remaining physical strength, Then physical strength will become zero and will not become negative.
4. When given was beaten, when saw his strength stick, Then's remaining strength and reduced strength turned into different colors, and it was easy to tell.
#### Estimated time: about 4 hours | Priority: Must

### US-3.4 Boss chases me

- Story: As a player, I want the Boss to come after me once it notices me so that the fight feels tense instead of just standing and trading hits.
#### Acceptance Criteria
1. When there is nothing between given boss and me, when I walk around in arena, Then Boss will move towards where I am.
2. When the given boss is close to the level that can be attacked, if the When game is moving, Then Boss will stop moving and prepare to attack.
3. When the given boss runs into the when wall, it will not pass through the Then wall.
#### Estimated time: about 6 hours | Priority: Must

### US-3.5 Boss walks around obstacles

- Story: As a player, I want the Boss to find its way around obstacles when I hide behind them so that I can't win just by hiding behind a wall.
#### Acceptance Criteria
1. The premise assumes that there is a wall between the Boss and himself, and there is a road around the wall. If the Boss catches up with himself, he will reach himself around the wall he should do.
2. the premise assumes that you are in a place where you can never reach the Boss. If the Boss catches up with himself, he should not fall into the wall or go through it.
#### Estimated time: about 6 hours | Priority: Should

### US-3.6 Warning before Boss melee attacks

- Story: As a player, I want to see a warning area before the Boss's melee attack lands so that I have a chance to react and dodge.
#### Acceptance Criteria
1. Given warning area appeared about 0.8 seconds before Then Boss started to attack at close range.
2. Before the When attack hit by 2. Given warning, it will not be harmed by Then from this area.
3. When with Then warning will be damaged at the end if he stays in that area all the time.
#### Estimated time: about 6 hours | Priority: Should

### US-3.7 Boss elemental projectiles

- Story: As a player, I want the Boss to shoot elemental projectiles at me from far away so that staying far away isn't always safe and the fight has more variety.
#### Acceptance Criteria
1. Given if I'm in the shooting range of Boss, When the cooldown of long-range attack is over, Then Boss will shoot the elemental projectile at me.
2. If there is a wall on the road of the given project, when When projectile hits the wall, it will disappear, and it will never hurt me on the other side of the wall.
3. If 3. Given Project hits me and When hits are counted, Then I will be hurt.
#### estimated time:about 6 hours|priority:should

### US-3.8 Boss second phase

- Story: As a player, I want the Boss to enter a second phase and change its attacks when its health gets low enough so that the end of the fight is harder and more interesting.
#### Acceptance Criteria
1. When the HP of given boss is greater than the stage standard, when enters the second stage after a short switching time if its HP is equal to or less than the standard.
2. When 2. Given Boss is in the second stage, Then sees that its attack is different from that in the first stage.
3. When the given boss is already in the second stage, even if When is restored or after a period of time, it will not return to the first stage of when.
#### Estimated time: about 6 hours | Priority: Should

### US-3.9 Winning and returning to the Hub

- Story: As a player, I want to see a victory message and go back to the Hub automatically after beating the Boss so that I clearly know the fight is over and can move on.
#### Acceptance Criteria
1. Given Boss has only a little physical strength left, and When's last blow gave Then the message of victory.
2. Given victory message When after a period of time, Then physical strength automatically returns to the Hub in a state of recovery.
3. Open the When request list that returns to 3. Given Hub. Then the guardian item is marked as completion.
#### Estimated time: about 6 hours | Priority: Must

### US-3.10 Going back to the Hub after losing

- Story: As a player, I want to go back to the Hub with full health after the Boss beats me so that I can get ready and try again without restarting the whole game.
#### Acceptance Criteria
1. When one's own Health becomes 0 during the given war, Then will send out a failure message, and Health will return to the Hub in a full state.
2. When given lost and just came back, when confirmed his quest, Then the quest was left, and you can challenge it again.
3. Given lost the map of When fighting again when he just came back, Then Boss will start from the Health filled with oil again.
4. When given lost and just came back, when confirmed his class and ultimate mode, Then they didn't change.
#### Estimated time: about 6 hours | Priority: Must

### US-3.11 Stronger Boss in rematches

- Story: As a player, I want a Boss I have already beaten to be stronger when I fight it again so that rematches are still a challenge.
#### Acceptance Criteria
1. If I enter the arena without knocking down guardian once, the Boss will keep normal health.
2. if I beat that old shop and play again, Boss's maximum health will be more than usual.
3. If I beat the oligarch several times and fight again, the maximum health of Boss will not be more than four times than the normal amount.
4. if you challenge again in the state of losing the last battle, Boss will not become stronger at all.
#### Estimated time: about 4 hours | Priority: Could

### US-3.12 A unique mechanic for each guardian

- Story: As a player, I want each guardian to have its own special combat mechanic so that the five Boss fights feel different instead of being the same fight with a new look.
#### Acceptance Criteria
1. 5 When fighting with all oligarchs, each oligarch has at least one special effect that he has noticed (for example, adding status effect to himself, etc.).
2. When the boss installs the status effect for himself, if he sees the screen, he can see the text or icon of the effect.
3. When the status effect expires, if the battle continues, you can operate your role normally.
#### Estimated time: about 10 hours | Priority: Could

---

## F4 Inventory

### US-4.1 Opening and closing the bag

- Story: As a player, I want to press a key to open my bag and see what items I have and how many so that I always know what resources I have.
#### Acceptance Criteria
1. When the bag is closed, press the B key, and the bag will open and list the names and quantities of all item.
2. When the bag is open, press B or Escape, and the bag should be closed.
3. When 3. item can't hold one page, press the Left/Right key to go to the previous page or the next page.
4. When the bag is empty, if the bag is opened, the empty bag will be displayed instead of an error.
#### Estimated time: about 4 hours | Priority: Must

### US-4.2 Getting rewards from the Boss

- Story: As a player, I want the Boss's reward materials to go into my bag automatically after I win so that my fights actually give me something.
#### Acceptance Criteria
1. Go back to When Hub, which defeated given boss, and open the package. The materials of Then Boss are in the package.
2. When given already has the same material or Then who gets the same thing does not add new lines, and the number will increase.
3. Open the When package that lost the 3. Given Boss competition and didn't get any Then reward.
4. Continue the When game that has already received the reward of given, and can't receive the reward twice because of the same victory of Then.
#### Estimated time: about 6 hours | Priority: Must

### US-4.3 Using health potions

- Story: As a player, I want to drink health potions to get health back so that I can survive hard fights.
#### Acceptance Criteria
1. Given When I am injured and holding a pot, right-click the potion the when bag, or press the p button in the game, Then can recover up to 30 HP and reduce one pot.
2. When the given HP is only 10, Then HP will become full and will not exceed the maximum.
3. When 3. Given HP is full, when the pot is tried, the pot will not decrease.
4. Nothing will happen if you press the When P button without 4. Given potion.
#### Estimated time: about 4 hours | Priority: Must

### US-4.4 Equipping and unequipping gear

- Story: As a player, I want to put on and take off weapons and armour from my bag so that I can change my combat setup whenever I want.
#### Acceptance Criteria
1. When the given bag has a gear that has not been worn yet, when right-click it, it will display Then `[Worn] ]`, and the numbers of attack and defence below will change.
2. When you click on the given item again when you put it on, Then it will fall off, ` [Worn] ` will disappear, and the number will be restored.
3. When given has installed an item in a slot, if other items in the same slot are installed when, the items before Then will be exchanged, and only one item in the slot is valid.
4. When only 4. Given armour is opened, then `No weapon equipped` will be displayed when you see the when weapon slot.
#### Estimated time: about 6 hours | Priority: Should

### US-4.5 Gear changes combat damage

- Story: As a player, I want my gear to really boost my attack or defence so that getting and wearing gear is worth it.
#### Acceptance Criteria
1. When using skill on the Training Ground without weapons, the value of base damage will appear.
2. When installing 2. attack +20% crutches, if you use the same skill for the same target, you will be given about 1.2 times the base damage.
3. When you open armour, which promotes 3. defence, even if you encounter an attack from the same Boss, the interference is less than when you don't open armour.
#### Estimated time: about 6 hours | Priority: Should

### US-4.6 Set bonuses

- Story: As a player, I want an extra bonus for wearing several guardian armour pieces together so that I have a reason to collect the whole set.
#### Acceptance Criteria
1. When you look at your stats while wearing only one 1. Given Guardian Armour part, there will be no Then package bonus.
2. If you look at When's own stats while wearing two given parts, you can see Then +5% defence's positioning bonus.
3. If you look at When's own stats while wearing four given parts, you can also see Then plus +10% attack positioning bonus.
4. If one When part is removed with the bonus of 4. Given 4 parts, the bonus of Then attack will disappear immediately, leaving only the bonus of 2 parts.
#### Estimated time: about 4 hours | Priority: Could

### US-4.7 Moving items with the storage chest


- Story: As a player, I want to move items between the storage chest in the Hub and my bag so that I can keep things I don't need right now somewhere safe.
#### Acceptance Criteria
1. Given my character presses the When E key next to storage chest, and the picture of Then storage will open, displaying the content.
2. Right-click the When item that opens the 2. Given storage screen, and the Then stack is all moved to the other side, and both numbers are updated.
3. Given is equipped with the only item I have, When, and put it in the Then of chest. The bonus of that item has disappeared.
4. The given storage screen opens. Press the When E key or the Escape key to close the screen.
#### Estimated time: about 6 hours | Priority: Should

### US-4.8 Getting a weapon from the merchant

- Story: As a new player, I want to get my first weapon by talking to the Weapon Merchant so that I have some basic gear before my first Boss fight.
#### Acceptance Criteria
1. The conversation with When Wear Merchant, who has never obtained 1. Given weapon, ends. Then Lightning staff enters the bag.
2. Given has got the weapon. When and When talking to Merchant can't get the second one.
#### Estimated time: about 4 hours | Priority: Should

---

## F5 Elemental Reaction Combat (Special — Daoyan Zhu)

### US-5.1 Basic attacks hit enemies

- Story: As a player, I want to press the attack key to attack in the direction I am facing and damage enemies so that I can bring the Boss's health down and win.
#### Acceptance Criteria
1. (Condition) When the Boss is within the attack range and his role is oriented to the Boss (operation), if the attack key (J) is pressed (result), the HP of the Boss will be reduced.
2. (Conditional) Pressing (operating) the attack key (result) again during the attack cooling process will not start a new attack.
3. (Condition) When there is no enemy in the attack place (Operation) Attacking (Result) No enemy's HP will be reduced.
4. (Condition) Even if the same enemy is attacked (operated) for many times, the enemy will only be hurt once during the attack (result).
#### Estimated time: about 6 hours | Priority: Must

### US-5.2 Choosing a class

- Story: As a player, I want to pick Archer, Mage or Katana from the Class Master in the Hub so that I can play in the style I like.
#### Acceptance Criteria
1. Given chooses When「Mage "that I chat with Class Master on the Hub to make sure that Then my skills will become Mage's skills.
2. try given. all classes have different skills according to whenj/k/L then.
   3.given then can't change my When class in battle map or Training Ground.
#### Estimated time: about 6 hours | Priority: Should

### US-5.3 Mage switching elements

- Story: As a Mage player, I want to press a key to cycle through elements so that I can plan which reactions I want to set up.
#### Acceptance Criteria
1. When the current element is Fire, if the K key is pressed continuously, it will change in the order of Water → Lightning → Ice → Wind → Fire.
2. After switching the element, view the HUD and display the selected element.
3. Press the J key to shoot the magic bolt when switching to Water, which will become the Water bolt.
4. When you press "k" to switch the element, don't just switch and turn it into an attack.
#### Estimated time: about 4 hours | Priority: Should
5.
### US-5.4 Seeing the element on an enemy

- Story: As a player, I want to see which element is currently on an enemy so that I can work out which element to use next for a reaction.
#### Acceptance Criteria
1. If there is no element on the Boss, if you encounter a Fire attack, a "Fire" sign will appear near the Boss.
2. If my attack falls off or is blocked by the wall, even if I see the Boss, I won't add any element.
3. If the effect of the element expires, the tag will disappear when you see the Boss.
#### Estimated time: about 4 hours | Priority: Should

### US-5.5 Damage boost reactions (Vaporize / Melt)

- Story: As a player, I want certain element combos to deal extra damage so that I get rewarded for mixing elements well.
#### Acceptance Criteria
1. The When Water attack with Fire on the given boss hit the Then "Vaporize ",which is twice that of the base damage.
2. A When Fire attack with Water hit the then "vaporation" of 2. Given Boss, which was 1.5 times that of base damage.
3. After the When Fire attack with Ice is hit on 3. Given Boss, Then "Melt "will happen, which is twice as much as that of base damage.
4. When the given reaction happened, I think the elements before the Boss's Then disappeared.
#### Estimated time: about 6 hours | Priority: Should

### US-5.6 Freeze reaction

- Story: As a player, I want to freeze the Boss by combining Water and Ice so that I get a short safe window to deal damage.
#### Acceptance Criteria
1. In the case of Boss with Water, if Ice is attacked, Boss will freeze and display frozen status.
2. When the Boss is frozen, the boss can't move or make new attacks while the freezing time lasts.
3. After the freezing time, if the fighting continues, Boss will operate normally again.
#### Estimated time: about 6 hours | Priority: Should

### US-5.7 Overload reaction (area damage and knockback)

- Story: As a player, I want Fire and Lightning together to cause an explosion that knocks enemies back so that I can make space and hit an area.
#### Acceptance Criteria
1. When the Boss is equipped with Fire, if Lightning attacks, "Overload" will occur, and the boss will fly backwards with additional damage.
2. When there is a wall behind the Boss, if the boss flies backwards in the Overload, it will stop at the wall and cannot enter the wall.
#### Estimated time: about 6 hours | Priority: Should
3.
### US-5.8 Swirl reaction (elemental ground area)

- Story: As a player, I want Wind to spread the element on an enemy into an area on the ground so that I can use that area to keep putting elements on enemies.
#### Acceptance Criteria
1. When there is a Fire on the given boss, if there is a When Wind attack, a Fire area will appear on the ground where the Then hits.
2. When there is an element area on the given ground, Then if the enemy stands in it, the element will hit the enemy (the Water area will also cause damage).
3. When the given ground area comes out, when a fixed time passes, that area will naturally disappear.
4. When the given ground area is effective, even when its effect is started, there will never be an infinite and continuous chain of Then new reactions.
#### Estimated time: about 6 hours | Priority: Should

### US-5.9 Walls block attacks

- Story: As a player, I want both my attacks and the Boss's attacks to be blocked by walls so that I can use walls as cover and the fight is fair.
#### Acceptance Criteria
1. there is a wall when between given boss and me. if I attack melee or area, Then Boss won't be hurt and won't get element.
   If I hide behind the wall and attack from the opposite side, Then I won't be hurt.
3. given projection flies to the wall When if it touches the wall, Then will disappear.
#### Estimated time: about 6 hours | Priority: Should

### US-5.10 Ultimate modes A / B

- Story: As a player, I want to choose ultimate mode A or B for my class in the Hub so that the same class can be played in different ways.
#### Acceptance Criteria
1. chat with Class Master on 1. Given Hub. if When Mage selects mode b, press the Then O key to mute mode B ..
2. If you choose B as 2. Given Mage and A as Archer, Then even if you switch When classes, then each class will remember its chosen mode.
3. On the given battle map, even if you want to change When ultimate mode, you can't change Then.
#### Estimated time: about 6 hours | Priority: Should

---

## F6 Save / Load (Special — Hao Liu)

### US-6.1 Saving manually

- Story: As a player, I want to press a key to save my game in a safe place so that I can stop playing anytime without losing progress.
#### Acceptance Criteria
1. Given when I'm in the Hub or Memorial Hall and it's safe to save, press the When F5 key, then Then game will be saved to the manual slot, and the message "saved" will appear.
2. When given is saved successfully, you can check When manual slot is saved to see the basic details such as when Then is saved.
3. When writing 3. Given manual save, if When automatic slot is confirmed, Then automatic save has not changed.
#### Estimated time: about 6 hours | Priority: Should

### US-6.2 Confirming before overwriting a save

- Story: As a player, I want the game to ask me first before it overwrites my manual save so that I don't wipe important progress by accident.
#### Acceptance Criteria
1. When I press F5 when given has been saved manually, Then the game will say, "Can you overwrite it?" I'll make sure.
2. When the given game is confirmed, if when I press Enter, Then the old save will be replaced with the current progress.
   When the given game is confirmed, if I press Escape, Then nothing will be saved, and the old save will remain the same.
#### Estimated time: about 4 hours | Priority: Should

### US-6.3 No saving when it isn't safe

- Story: As a player, I want the game to stop me from saving and tell me why when I am in a fight or have a menu open so that I never end up with a save that can't be loaded properly.
#### Acceptance Criteria
1. When F5 key on given battle map or Training Ground has not been saved, and the message "Can't save now" appears.
2. The given session, package and When F5 key opened in the storage screen have not been saved, and the game will tell you to close it first.
3. The When problem of the given message "Cannot save" disappears, or Then message disappears by itself after about 4 seconds.
#### Estimated time: about 4 hours | Priority: Should

### US-6.4 Autosave after beating a Boss

- Story: As a player, I want the game to save automatically every time I beat a Boss so that I don't lose my wins even if I forget to save.
#### Acceptance Criteria
1. If the Boss is overthrown, if the rewards are all received after returning to the Hub, then the progress of new rewards and quest will be saved in the automatically saved slot.
2. If the battle fails, if you return to the Hub, then nothing will be saved automatically.
3. If it has been saved manually, if it is saved automatically, it will be saved manually.
#### Estimated time: about 6 hours | Priority: Should

### US-6.5 Loading puts me back where I was

- Story: As a returning player, I want loading a save to put me back on the same map, in the same spot and with the same health so that I can carry on from where I stopped.
#### Acceptance Criteria
1. Given save When manually, I press F9, 2 (manually) select enter then my role, and return the saved map and location through the saved health.
2. Ask me if the given game can be loaded. When I press escape then, nothing will be loaded, and now the game is like this.
3. When after the given loading, there is no change in the saved file during the Then loading when I confirmed the saved file.
#### Estimated time: about 6 hours | Priority: Should

### US-6.6 Loading brings back my bag, gear and quests

- Story: As a returning player, I want loading to bring back my bag, gear, class and quest progress so that I keep everything I collected and finished.
#### Acceptance Criteria
1. Having three given potion, When saved with staff has three Then potion, and staff also keeps ` [worn ]`.
2. When saved after knocking down two statues 2. Given guardian is displayed as two in the Then request list loaded with this save, and stone of Memorial Hall glows as two.
3. When given is saved, when it is loaded for Mage, the saved Then is still Mage, and the ultimate mode is the same.
#### Estimated time: about 6 hours | Priority: Should

### US-6.7 Missing or broken saves

- Story: As a player, I want a clear message when a save doesn't exist or is broken so that the game doesn't crash and my current progress isn't ruined.
#### Acceptance Criteria
1. The given automatic slot is empty. Press When F9 to pop up the message "No Save" for selecting No.1 (automatic) to keep the current game state.
2. The given saved file is damaged (for example, there is no item, and the value is very strange). When tries to load it, Then error message appears, and the current game state remains unchanged, so the game will not be strange.
3. Given started the game for the first time and entered the When game. Then the new game began, and the original saved file remained unchanged.
#### Estimated time: about 6 hours | Priority: Should

---

## F7 NPC Dialogue & Tutorial (Special — Jiaqi Chen)

### US-7.1 Prompt when near an NPC

- Story: As a player, I want to see an NPC's name and a "press E to talk" prompt when I walk up to it so that I know who I can talk to.
#### Acceptance Criteria
1. Given if my character sees the When screen when he is near the NPC, the name and prompt of the NPC will be displayed next to the Then.
2. Given When my character leaves, Then prompt will disappear when he sees the picture.
3. Given 2 When two NPCs are nearby, if my character is close to one side, pressing Then E can only talk to the nearest NPC.
#### Estimated time: about 4 hours | Priority: Should

### US-7.2 Dialogue with several pages

- Story: As a player, I want to read an NPC's dialogue one page at a time by pressing E so that I can read longer text at my own pace.
#### Acceptance Criteria
1. When the given character is next to NPC, pressing the When E button will open the dialogue box on the first page.
   If given is not the last page, pressing the When E button will display the next page.
3. Given pressing the When E button on the last page will close Then dialogue box.
4. when the given dialog box is open, press the When Escape button Then to close it immediately.
5. When the given dialogue box is open, even if the when move key is pressed, the role will not move until the Then dialogue ends.
#### Estimated time: about 4 hours | Priority: Should

### US-7.3 Dialogue box fits any window size

- Story: As a player, I want the dialogue text to always fit in the box whatever size my window is so that I can read the whole conversation.
#### Acceptance Criteria
1. When the given dialogue is long, when the dialog box comes out, Then the text will be folded back and all will enter the box.
2. Given When the dialog box comes out when I shrink the window, Then the title, text and operation tips will not overlap.
3. Given When my window is not 16:9, when the dialog box comes out, the Then box will not stretch or shrink.
#### Estimated time: about 6 hours | Priority: Should

### US-7.4 Tutorial NPC in the Hub

- Story: As a new player, I want a guide NPC in the Hub that I can ask about the controls at any time so that I can learn how to play quickly.
#### Acceptance Criteria
1. Given when I am in the state of Hub, if I talk to When Old Sage, it will explain to me the basic contents of Then moving, fighting, package and portal.
2. In the state that given had a conversation with guide before, you can read Then and tutorial by having a conversation with When again.
3. When talking to 3. Given guide, even when the conversation ends, you won't get any Then props, and there will be no ending.
#### Estimated time: about 4 hours | Priority: Should

### US-7.5 Picking options in dialogue

- Story: As a player, I want to pick dialogue options with Up / Down and confirm them so that I can make choices by talking (like choosing where to go or which class to play).
#### Acceptance Criteria
1. When there are some choices in the given dialogue, when I press Up/Down, then then highlight will move and will not pass the first or last choice.
2. When the given option is selected, if I press E, Then the content of this option will be executed.
3. When the given option is locked, when if I try to decide that, Then nothing happens, it will tell me why the game is locked.
#### Estimated time: about 4 hours | Priority: Should

### US-7.6 Notice board in the Hub

- Story: As a new player, I want to read a notice board in the Hub that lists the controls so that I can check the controls quickly without talking to anyone.
#### Acceptance Criteria
1. Given my character is near the notice board, looking at the When screen, instead of writing ""Talk to ""on the Then message, it says "[E] Read Notice Board" ".
2. Given my role is next to the notice board, press the When E key to display the operation methods of Then mobile, interactive, bag, gear and portal.
3. Given notice board opens, press When E or Escape button, Then the screen closes, and there is nothing in the item.
#### Estimated time: about 4 hours | Priority: Should
