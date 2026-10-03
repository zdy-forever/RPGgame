## F1 Movement

### US-1.1 Eight-directional movement

- Story: As a player, I want my character to keep moving while I hold a movement key (like WASD) so that I can explore the map freely.
- Acceptance Criteria
   1. Given my character is standing in open space When I hold "up" Then it keeps moving up.
   2. Given my character is moving When I let go of all movement keys Then it stops straight away.
   3. Given my character is standing in open space When I hold "up" and "right" together Then it moves diagonally and is not faster than moving in one direction.
   4. Given my character is standing in open space When I hold "left" and "right" together Then it does not move.
- Estimated time: about 4 hours | Priority: Must

### US-1.2 Wall collision

- Story: As a player, I want my character to be blocked by walls and obstacles so that the map feels solid and the level layout actually matters.
- Acceptance Criteria
   1. Given there is a wall in front of my character When I keep walking into it Then my character stops at the edge of the wall and does not overlap it.
   2. Given my character is touching a vertical wall When I walk diagonally into the wall Then my character slides along the wall instead of getting stuck.
   3. Given my character is moving very fast (for example while dashing) When it runs into a thin wall Then it still gets blocked and does not go through the wall.
- Estimated time: about 6 hours | Priority: Must

### US-1.3 Map boundaries

- Story: As a player, I want my character to stay inside the map so that I don't end up lost in empty areas with nothing in them.
- Acceptance Criteria
   1. Given my character is at the left edge of the map When I keep pressing "left" Then it stays inside the map.
   2. Given my character is in a corner of the map When I move diagonally towards the outside Then it does not cross either edge.
- Estimated time: about 2 hours | Priority: Must

### US-1.4 Dash

- Story: As a player, I want to dash a short distance in the direction I am facing so that I can dodge the Boss's attacks.
- Acceptance Criteria
   1. Given my dash is not on cooldown When I press the dash key Then my character moves a short distance much faster than normal walking.
   2. Given I have just dashed (the cooldown is about 1 second) When I press the dash key again Then nothing happens.
   3. Given there is a wall in the way When I dash Then my character stops at the wall and does not go through it.
- Estimated time: about 6 hours | Priority: Should

### US-1.5 Camera follow

- Story: As a player, I want the camera to follow my character so that I can always see myself on maps that are bigger than the screen.
- Acceptance Criteria
   1. Given the map is bigger than the screen When my character walks right Then the view scrolls with it and my character stays on screen.
   2. Given my character reaches the edge of the map When it keeps walking towards the edge Then the camera stops scrolling and does not show anything outside the map.
   3. Given the map is not bigger than the screen (like the Memorial Hall) When my character moves Then the view does not move.
- Estimated time: about 4 hours | Priority: Must

---

## F2 Map / Level

### US-2.1 Safe spawn in the Hub

- Story: As a new player, I want to start the game in a safe spot in the Hub so that I can look around before deciding where to go.
- Acceptance Criteria
   1. Given I start the game When the game screen finishes loading Then my character appears at the Hub spawn point.
   2. Given my character has just spawned When I check where it is Then it is not stuck in any obstacle and can move right away.
   3. Given I have just entered the Hub When I look around Then there are no enemies nearby that can attack me.
- Estimated time: about 4 hours | Priority: Must

### US-2.2 Travel through portals

- Story: As a player, I want to be taken to another map when I walk into an open portal so that I can move between levels.
- Acceptance Criteria
   1. Given a portal is open When my character walks into it Then my character appears at the spawn point of the destination map.
   2. Given my character has just arrived through a portal When it is standing next to the return portal Then it is not immediately sent back.
   3. Given the map change has finished When I look at the screen Then there are no attacks or effects left over from the old map.
- Estimated time: about 6 hours | Priority: Must

### US-2.3 Closed portals

- Story: As a player, I want a portal to stay closed and look closed until I am allowed to use it so that I know I can't enter that level yet.
- Acceptance Criteria
   1. Given a portal is closed When my character walks into it Then nothing happens.
   2. Given a portal is closed When I look at it Then it looks different from an open portal.
- Estimated time: about 4 hours | Priority: Should

### US-2.4 Choosing a destination with the Portal Guide

- Story: As a player, I want to tell the Portal Guide whether the portal should lead to the Training Ground, the Battle Map or the Memorial Hall so that one portal can take me to different places.
- Acceptance Criteria
   1. Given my character is next to the Portal Guide When I press E Then I see three options: Training Ground, Battle Map and Memorial Hall.
   2. Given the options are showing When I pick one with Up / Down and press E Then the portal is set to that map but I am not teleported yet.
   3. Given I have not accepted a Boss quest When I pick "Battle Map" Then the option shows as locked and the portal does not open.
   4. Given the options are showing When I press Escape Then the menu closes and the portal stays the same.
- Estimated time: about 6 hours | Priority: Should

### US-2.5 Unlocking a battle by accepting a quest

- Story: As a player, I want a guardian's battle map to unlock only after I accept that guardian's quest in the Hub so that the game has a clear order and goal.
- Acceptance Criteria
   1. Given I am talking to the Quest Keeper When I choose a guardian and confirm Then that quest becomes my current quest.
   2. Given I have accepted a quest When I choose "Battle Map" at the Portal Guide and walk into the portal Then I arrive in that guardian's arena.
   3. Given I have beaten a guardian When I open the quest list again Then that guardian's quest is marked as completed..
- Estimated time: about 6 hours | Priority: Should

### US-2.6 Five different arenas

- Story: As a player, I want every guardian to have its own arena so that each fight looks fresh.
- Acceptance Criteria
   1. Given I have accepted guardian A's quest When I enter the battle map Then I see guardian A's arena background and name.
   2. Given I have accepted guardian B's quest When I enter the battle map Then the background and Boss name are different from A's.
- Estimated time: about 6 hours | Priority: Could

### US-2.7 Training Ground

- Story: As a player, I want to hit a training target that doesn't fight back and see my damage numbers so that I can practise my skills safely and compare my damage.
- Acceptance Criteria
   1. Given I enter the Training Ground When I look at the map Then there is only one training target and it never moves or attacks.
   2. Given I attack the training target When my attack hits Then the screen shows total damage, last damage and number of hits and the target never dies.
   3. Given some damage has been recorded When I press R Then the numbers reset and my health is refilled.
   4. Given I am in the Training Ground When I talk to the return NPC and confirm Then I go back to the Hub and get no Boss reward.
- Estimated time: about 6 hours | Priority: Could

### US-2.8 Minimap

- Story: As a player, I want a minimap that shows obstacles, portals, NPCs, me and the Boss so that I can find my target quickly and don't get lost.
- Acceptance Criteria
   1. Given the window is big enough When I am in the Hub or a battle map Then a minimap with my position shows up in a corner of the screen.
   2. Given the Boss is alive on a battle map When I look at the minimap Then the Boss shows as a red dot and the dot disappears once the Boss dies.
   3. Given the minimap is showing When I press M Then a bigger map opens and pressing M again closes it.
   4. Given the bigger map is open When I press movement keys Then my character doesn't move until I close the map.
- Estimated time: about 6 hours | Priority: Should

### US-2.9 Memorial Hall and ending

- Story: As a player, I want the guardian stones in the Memorial Hall to light up as I win and an ending to play once all five are lit so that I can see my progress and feel like I have finished the game.
- Acceptance Criteria
   1. Given I have beaten 2 different guardians When I enter the Memorial Hall Then exactly 2 stones are lit.
   2. Given I have beaten the same guardian several times When I enter the Memorial Hall Then the ending dialogue plays and I can then choose to view the credits..
   3. Given I have beaten all 5 guardians When I talk to the Old Sage in the Memorial Hall Then the ending dialogue plays once and then I can choose to see the credits.
- Estimated time: about 6 hours | Priority: Could

### US-2.10 Live coordinates

- Story: As a player, I want to see my character's coordinates on the HUD so that I can follow directions like "the merchant is at (650, 250)" and tell others exactly where I am.
- Acceptance Criteria
   1. Given the window is big enough When my character moves Then the coordinates on the HUD update straight away.
   2. Given my character stops When I look at the HUD Then the coordinates stay the same.
   3. Given the window is made very small When I look at the HUD Then the coordinates are hidden but my health is still shown.
- Estimated time: about 2 hours | Priority: Could

---

## F3 Enemies / Boss

### US-3.1 Seeing the Boss in the battle map

- Story: As a player, I want to see the Boss in the arena when I enter a battle map so that I know right away where my target is.
- Acceptance Criteria
   1. Given I have accepted a quest When I enter its battle map Then exactly one Boss appears in the arena.
   2. Given the Boss has appeared When I check where it is Then it is near the middle of the arena and not stuck in a wall.
   3. Given I enter the Hub or the Memorial Hall When I look around Then there is no Boss.
- Estimated time: about 4 hours | Priority: Must

### US-3.2 Boss health bar

- Story: As a player, I want to see the Boss's health bar so that I know how much health it has left and how close I am to winning.
- Acceptance Criteria
   1. Given the Boss has full health When I look at its health bar Then the bar is full.
   2. Given I hit the Boss When the damage is applied Then the bar goes down by the right amount.
   3. Given the Boss's health reaches 0 When I look at the screen Then the bar is empty and the Boss is gone.
- Estimated time: about 4 hours | Priority: Must

### US-3.3 Taking damage and seeing my health

- Story: As a player, I want to lose health when the Boss hits me and see my health on the HUD so that I can tell how much danger I am in.
- Acceptance Criteria
   1. Given I have full health When a Boss attack hits me Then my health on the HUD goes down by that amount of damage.
   2. Given my health is low When I look at the HUD Then the length of my health bar matches how much health I have left.
   3. Given my health is very low When I take more damage than I have left Then my health shows 0 and never goes negative.
   4. Given I have been hit When I look at my health bar Then the health I have left and the health I lost are in different colours so I can tell them apart easily.
- Estimated time: about 4 hours | Priority: Must

### US-3.4 Boss chases me

- Story: As a player, I want the Boss to come after me once it notices me so that the fight feels tense instead of just standing and trading hits.
- Acceptance Criteria
   1. Given there is nothing between the Boss and me When I move around the arena Then the Boss moves towards where I am.
   2. Given the Boss is chasing me When it reaches attack range Then it stops moving forward and prepares to attack.
   3. Given the Boss is moving When it runs into a wall Then it does not go through the wall.
- Estimated time: about 6 hours | Priority: Must

### US-3.5 Boss walks around obstacles

- Story: As a player, I want the Boss to find its way around obstacles when I hide behind them so that I can't win just by hiding behind a wall.
- Acceptance Criteria
   1. Given a wall is between the Boss and me and there is a way around it When the Boss chases me Then it walks around the wall to reach me.
   2. Given I am somewhere the Boss can't reach at all When the Boss chases me Then it does not get stuck in or pass through the wall.
- Estimated time: about 6 hours | Priority: Should

### US-3.6 Warning before Boss melee attacks

- Story: As a player, I want to see a warning area before the Boss's melee attack lands so that I have a chance to react and dodge.
- Acceptance Criteria
   1. Given I am in the Boss's melee range When the Boss starts a melee attack Then a warning area shows up about 0.8 seconds before the hit.
   2. Given the warning is showing When I get out of the area before the hit lands Then I take no damage.
   3.Given the warning is showing When I remain in the area until the attack lands Then I take damage.
- Estimated time: about 6 hours | Priority: Should

### US-3.7 Boss elemental projectiles

- Story: As a player, I want the Boss to shoot elemental projectiles at me from far away so that staying far away isn't always safe and the fight has more variety.
- Acceptance Criteria
   1. Given I am in the Boss's shooting range When its ranged attack is off cooldown Then the Boss shoots an elemental projectile at me.
   2. Given there is a wall in the projectile's way When the projectile hits the wall Then it disappears and can't hurt me through the wall.
   3. Given a projectile is travelling towards me When it hits my character Then I take damage.
- Estimated time: about 6 hours | Priority: Should

### US-3.8 Boss second phase

- Story: As a player, I want the Boss to enter a second phase and change its attacks when its health gets low enough so that the end of the fight is harder and more interesting.
- Acceptance Criteria
   1. Given the Boss's health is above the phase threshold When its health drops to the threshold or lower Then it enters the second phase after a short transition.
   2. Given the Boss is in the second phase When I watch its attacks Then they are different from the first phase.
   3. Given the Boss is already in the second phase When it gets healed or time passes Then it does not go back to the first phase.
- Estimated time: about 6 hours | Priority: Should

### US-3.9 Winning and returning to the Hub

- Story: As a player, I want to see a victory message and go back to the Hub automatically after beating the Boss so that I clearly know the fight is over and can move on.
- Acceptance Criteria
   1. Given the Boss has only a little health left When I land the last hit Then a victory message appears.
   2. Given the victory message is showing When a short moment has passed Then I go back to the Hub automatically with full health.
   3. Given I am back in the Hub When I open the quest list Then that guardian's quest is marked as completed..
- Estimated time: about 6 hours | Priority: Must

### US-3.10 Going back to the Hub after losing

- Story: As a player, I want to go back to the Hub with full health after the Boss beats me so that I can get ready and try again without restarting the whole game.
- Acceptance Criteria
   1. Given I am in a fight When my health reaches 0 Then a defeat message appears and then I go back to the Hub with full health.
   2. Given I have just lost and returned to the Hub When I check my quest Then the quest is still active and I can attempt it again.
   3. Given I have just lost and come back When I enter that battle map again Then the Boss starts again with full health.
   4. Given I have just lost and come back When I check my class and ultimate mode Then they have not changed.
- Estimated time: about 6 hours | Priority: Must

### US-3.11 Stronger Boss in rematches

- Story: As a player, I want a Boss I have already beaten to be stronger when I fight it again so that rematches are still a challenge.
- Acceptance Criteria
   1. Given I have never beaten a guardian When I enter its arena Then the Boss has its normal health.
   2. Given I have beaten that guardian once When I fight it again Then its max health is higher than normal.
   3. Given I have beaten that guardian many times When I fight it again Then its max health is never more than 4 times the normal amount.
   4.Given I lost the previous fight When I challenge the Boss again Then its maximum health does not increase because of that loss.
- Estimated time: about 4 hours | Priority: Could

### US-3.12 A unique mechanic for each guardian

- Story: As a player, I want each guardian to have its own special combat mechanic so that the five Boss fights feel different instead of being the same fight with a new look.
- Acceptance Criteria
   1. Given I fight all five guardians When I watch each fight Then every guardian has at least one special effect I can notice (for example putting a status effect on me).
   2. Given the Boss has put a status effect on me When I look at the screen Then I can see a text or icon for that effect.
   3. Given the status effect runs out When the fight keeps going Then I can control my character normally again.
- Estimated time: about 10 hours | Priority: Could

---

## F4 Inventory

### US-4.1 Opening and closing the bag

- Story: As a player, I want to press a key to open my bag and see what items I have and how many so that I always know what resources I have.
- Acceptance Criteria
   1. Given my bag is closed When I press B Then the bag opens and lists every item with its name and amount.
   2. Given my bag is open When I press B or Escape Then the bag closes.
   3. Given I have more than one page of items When I press Left / Right Then I go to the previous or next page.
   4. Given my bag is empty When I open it Then it shows an empty bag instead of an error.
- Estimated time: about 4 hours | Priority: Must

### US-4.2 Getting rewards from the Boss

- Story: As a player, I want the Boss's reward materials to go into my bag automatically after I win so that my fights actually give me something.
- Acceptance Criteria
   1. Given I beat a Boss When I open my bag back in the Hub Then the Boss's materials are in my bag.
   2. Given I already have the same material When I get it again Then the amount goes up instead of adding a new row.
   3. Given I lose a Boss fight When I open my bag Then I have not received any reward.
   4. Given I have already received the reward for a Boss victory When the reward is checked again Then no duplicate reward is added.
- Estimated time: about 6 hours | Priority: Must

### US-4.3 Using health potions

- Story: As a player, I want to drink health potions to get health back so that I can survive hard fights.
- Acceptance Criteria
   1. Given I am hurt and have a potion When I right-click the potion in my bag or press P while playing Then I recover up to 30 HP and one potion is consumed.
   2. Given I am only missing 10 HP When I use a potion Then my health goes back to full and never goes over the max.
   3. Given I have full health When I try to use a potion Then the potion is not used up.
   4. Given I have no potions When I press P Then nothing happens.
- Estimated time: about 4 hours | Priority: Must

### US-4.4 Equipping and unequipping gear

- Story: As a player, I want to put on and take off weapons and armour from my bag so that I can change my combat setup whenever I want.
- Acceptance Criteria
   1. Given I have gear in my bag that I am not wearing When I right-click it Then it shows `[Worn]` and the attack and defence numbers at the bottom update.
   2. Given I am wearing an item When I right-click it again Then I take it off, `[Worn]` goes away and the numbers go back.
   3. Given I am already wearing something in a slot When I put on another item for the same slot Then the old one is swapped out and only one item in that slot counts.
   4. Given I am only wearing armour When I look at the weapon slot Then it says `No weapon equipped`.
- Estimated time: about 6 hours | Priority: Should

### US-4.5 Gear changes combat damage

- Story: As a player, I want my gear to really boost my attack or defence so that getting and wearing gear is worth it.
- Acceptance Criteria
   1. Given I have no weapon equipped When I use a skill in the Training Ground Then I get a base damage value.
   2. Given I am wearing the staff that gives +20% attack When I use the same skill on the same target Then I deal about 1.2 times the base damage.
   3. Given I am wearing armour that boosts defence When the same Boss attack hits me Then I take less damage than without the armour.
- Estimated time: about 6 hours | Priority: Should

### US-4.6 Set bonuses

- Story: As a player, I want an extra bonus for wearing several guardian armour pieces together so that I have a reason to collect the whole set.
- Acceptance Criteria
   1. Given I am wearing 1 guardian armour piece When I check my stats Then there is no set bonus.
   2. Given I am wearing 2 pieces When I check my stats Then I see a +5% defence set bonus.
   3. Given I am wearing 4 pieces When I check my stats Then I also see a +10% attack set bonus.
   4. Given I have the 4-piece bonus When I take one piece off Then the attack bonus goes away straight away and only the 2-piece bonus is left.
- Estimated time: about 4 hours | Priority: Could

### US-4.7 Moving items with the storage chest

- Story: As a player, I want to move items between the storage chest in the Hub and my bag so that I can keep things I don't need right now somewhere safe.
- Acceptance Criteria
   1. Given my character is next to the storage chest When I press E Then the storage screen opens and shows what is inside.
   2. Given the storage screen is open When I right-click an item Then the whole stack moves to the other side and both amounts update.
   3. Given I am wearing my only copy of an item When I put it in the chest Then I lose that item's bonus.
   4. Given the storage screen is open When I press E or Escape Then the screen closes.
- Estimated time: about 6 hours | Priority: Should

### US-4.8 Getting a weapon from the merchant

- Story: As a new player, I want to get my first weapon by talking to the Weapon Merchant so that I have some basic gear before my first Boss fight.
- Acceptance Criteria
   1. Given I have never claimed a weapon When I talk to the Weapon Merchant and finish the dialogue Then a Lightning staff goes into my bag.
   2. Given I have already claimed the weapon When I talk to the merchant again Then I don't get a second one.
- Estimated time: about 4 hours | Priority: Should

---

## F5 Elemental Reaction Combat (Special — Daoyan Zhu)

### US-5.1 Basic attacks hit enemies

- Story: As a player, I want to press the attack key to attack in the direction I am facing and damage enemies so that I can bring the Boss's health down and win.
- Acceptance Criteria
   1. Given the Boss is in range and my character is facing it When I press the attack key (J) Then the Boss loses health.
   2. Given my attack is still on cooldown When I press the attack key again Then no new attack starts.
   3. Given there is no enemy where I am attacking When I attack Then no enemy loses health.
   4. Given one attack touches the same enemy several times When the attack is still active Then the enemy only takes damage once.
- Estimated time: about 6 hours | Priority: Must

### US-5.2 Choosing a class

- Story: As a player, I want to pick Archer, Mage or Katana from the Class Master in the Hub so that I can play in the style I like.
- Acceptance Criteria
   1. Given I am talking to the Class Master in the Hub When I pick "Mage" and confirm Then my skills change to Mage skills.
   2. Given I try each class When I press J / K / L Then the skills are different for each class.
   3. Given I am on a battle map or in the Training Ground When I try to change class Then I can't.
- Estimated time: about 6 hours | Priority: Should

### US-5.3 Mage switching elements

- Story: As a Mage player, I want to press a key to cycle through elements so that I can plan which reactions I want to set up.
- Acceptance Criteria
   1. Given my current element is Fire When I keep pressing K Then it changes in the order Water → Lightning → Ice → Wind → Fire.
   2. Given I have switched element When I look at the HUD Then it shows the element I picked.
   3. Given I have switched to Water When I press J to fire a magic bolt Then the bolt is a Water bolt.
   4. Given I am playing as a Mage When I press K to switch elements Then the element changes without triggering an attack.
- Estimated time: about 4 hours | Priority: Should

### US-5.4 Seeing the element on an enemy

- Story: As a player, I want to see which element is currently on an enemy so that I can work out which element to use next for a reaction.
- Acceptance Criteria
   1. Given the Boss has no element on it When a Fire attack hits it Then a "Fire" marker shows up near the Boss.
   2. Given my attack misses or is blocked by a wall When I look at the Boss Then no element is added to it.
   3. Given an elemental effect is active on the Boss When the effect expires Then the marker disappears.
- Estimated time: about 4 hours | Priority: Should

### US-5.5 Damage boost reactions (Vaporize / Melt)

- Story: As a player, I want certain element combos to deal extra damage so that I get rewarded for mixing elements well.
- Acceptance Criteria
   1. Given the Boss has Fire on it When a Water attack hits it Then "Vaporize" happens and that hit does 2 times the base damage.
   2. Given the Boss has Water on it When a Fire attack hits it Then "Vaporize" happens and that hit does 1.5 times the base damage.
   3. Given the Boss has Ice on it When a Fire attack hits it Then "Melt" happens and that hit does 2 times the base damage.
   4. Given a reaction has happened When I look at the Boss Then the old element is used up.
- Estimated time: about 6 hours | Priority: Should

### US-5.6 Freeze reaction

- Story: As a player, I want to freeze the Boss by combining Water and Ice so that I get a short safe window to deal damage.
- Acceptance Criteria
   1. Given the Boss has Water on it When an Ice attack hits it Then the Boss freezes and a frozen status is shown.
   2. Given the Boss is frozen When the freeze effect is active Then the Boss cannot move or start new attacks.
   3. Given the freeze runs out When the fight keeps going Then the Boss acts normally again.
- Estimated time: about 6 hours | Priority: Should

### US-5.7 Overload reaction (area damage and knockback)

- Story: As a player, I want Fire and Lightning together to cause an explosion that knocks enemies back so that I can make space and hit an area.
- Acceptance Criteria
   1. Given the Boss has Fire on it When a Lightning attack hits it Then "Overload" happens and the Boss takes extra damage and gets knocked back.
   2. Given there is a wall behind the Boss When Overload knocks it back Then it stops at the wall and is not pushed into it.
- Estimated time: about 6 hours | Priority: Should

### US-5.8 Swirl reaction (elemental ground area)

- Story: As a player, I want Wind to spread the element on an enemy into an area on the ground so that I can use that area to keep putting elements on enemies.
- Acceptance Criteria
   1. Given the Boss has Fire on it When a Wind attack hits it Then a Fire area appears on the ground where it was hit.
   2. Given there is an element area on the ground When an enemy stands in it Then that element is put on the enemy (a Water area also deals damage).
   3. Given a ground area has appeared When a set amount of time has passed Then the area disappears by itself.
   4. Given a ground area is active When its effects are applied Then it never sets off an endless chain of new reactions.
- Estimated time: about 6 hours | Priority: Should

### US-5.9 Walls block attacks

- Story: As a player, I want both my attacks and the Boss's attacks to be blocked by walls so that I can use walls as cover and the fight is fair.
- Acceptance Criteria
   1. Given there is a wall between the Boss and me When I use a melee or area attack Then the Boss takes no damage and gets no element.
   2. Given I am hiding behind a wall When the Boss attacks from the other side Then I take no damage.
   3. Given a projectile is flying at a wall When it hits the wall Then it disappears.
- Estimated time: about 6 hours | Priority: Should

### US-5.10 Ultimate modes A / B

- Story: As a player, I want to choose ultimate mode A or B for my class in the Hub so that the same class can be played in different ways.
- Acceptance Criteria
   1. Given I am talking to the Class Master in the Hub When I choose mode B for the Mage Then pressing O uses the mode B ultimate.
   2. Given I picked B for the Mage and A for the Archer When I switch between them Then each class remembers its own choice.
   3. Given I am on a battle map When I try to change my ultimate mode Then I can't.
- Estimated time: about 6 hours | Priority: Should

---

## F6 Save / Load (Special — Hao Liu)

### US-6.1 Saving manually

- Story: As a player, I want to press a key to save my game in a safe place so that I can stop playing anytime without losing progress.
- Acceptance Criteria
   1. Given I am in the Hub or the Memorial Hall and it is safe to save When I press F5 Then the game saves to the manual slot and a "saved" message appears.
   2. Given the save was successful When I look at the manual slot Then I can see basic save information such as the save timestamp.
   3. Given a manual save has been written When I check the automatic slot Then the automatic save has not changed.
- Estimated time: about 6 hours | Priority: Should

### US-6.2 Confirming before overwriting a save

- Story: As a player, I want the game to ask me first before it overwrites my manual save so that I don't wipe important progress by accident.
- Acceptance Criteria
   1. Given there is already a manual save When I press F5 Then the game asks me to confirm the overwrite.
   2. Given the game is asking me to confirm When I press Enter Then the old save is replaced with my current progress.
   3. Given the game is asking me to confirm When I press Escape Then nothing is saved and the old save stays the same.
- Estimated time: about 4 hours | Priority: Should

### US-6.3 No saving when it isn't safe

- Story: As a player, I want the game to stop me from saving and tell me why when I am in a fight or have a menu open so that I never end up with a save that can't be loaded properly.
- Acceptance Criteria
   1. Given I am on a battle map or in the Training Ground When I press F5 Then nothing is saved and a "can't save now" message appears.
   2. Given a dialogue, my bag or the storage screen is open When I press F5 Then nothing is saved and the game tells me to close it first.
   3. Given a "can't save" message is showing When the problem is gone or about 4 seconds have passed Then the message disappears by itself.
- Estimated time: about 4 hours | Priority: Should

### US-6.4 Autosave after beating a Boss

- Story: As a player, I want the game to save automatically every time I beat a Boss so that I don't lose my wins even if I forget to save.
- Acceptance Criteria
   1. Given I beat a Boss When I am back in the Hub and the rewards have been collected Then the automatic slot is saved with the new rewards and quest progress.
   2. Given I lose a fight When I go back to the Hub Then nothing is autosaved.
   3. Given there is already a manual save When an autosave happens Then the manual save does not change.
- Estimated time: about 6 hours | Priority: Should

### US-6.5 Loading puts me back where I was

- Story: As a returning player, I want loading a save to put me back on the same map, in the same spot and with the same health so that I can carry on from where I stopped.
- Acceptance Criteria
   1. Given there is a manual save When I press F9, choose 2 (manual) and press Enter Then my character is back on the saved map and spot with the saved health.
   2. Given the game is asking me to confirm loading When I press Escape Then nothing is loaded and my current game stays the same.
   3. Given loading has finished When I check the save files Then loading has not changed any of them.
- Estimated time: about 6 hours | Priority: Should

### US-6.6 Loading brings back my bag, gear and quests

- Story: As a returning player, I want loading to bring back my bag, gear, class and quest progress so that I keep everything I collected and finished.
- Acceptance Criteria
   1. Given I saved with 3 potions and the staff equipped When I load that save Then I have 3 potions and the staff still shows `[Worn]`.
   2. Given I saved after beating 2 guardians When I load that save Then the quest list shows 2 done and 2 stones are lit in the Memorial Hall.
   3. Given I was a Mage when I saved When I load that save Then I am still a Mage with the same ultimate mode.
- Estimated time: about 6 hours | Priority: Should

### US-6.7 Missing or broken saves

- Story: As a player, I want a clear message when a save doesn't exist or is broken so that the game doesn't crash and my current progress isn't ruined.
- Acceptance Criteria
   1. Given the automatic slot is empty When I press F9 and choose 1 (automatic) Then a "no save" message appears and my current game stays the same.
   2. Given a save file is broken (for example missing fields or bad values) When I try to load it Then an error message appears, my current game stays the same and nothing crashes.
   3. Given I start the game for the first time When I get into the game Then a new game starts and any existing save files are left alone.
- Estimated time: about 6 hours | Priority: Should

---

## F7 NPC Dialogue & Tutorial (Special — Jiaqi Chen)

### US-7.1 Prompt when near an NPC

- Story: As a player, I want to see an NPC's name and a "press E to talk" prompt when I walk up to it so that I know who I can talk to.
- Acceptance Criteria
   1. Given my character walks close to an NPC When I look at the screen Then the NPC's name and a prompt show up near it.
   2. Given my character walks away When I look at the screen Then the prompt goes away.
   3. Given two NPCs are within interaction range When I press E Then my character talks only to the closest NPC.
- Estimated time: about 4 hours | Priority: Should

### US-7.2 Dialogue with several pages

- Story: As a player, I want to read an NPC's dialogue one page at a time by pressing E so that I can read longer text at my own pace.
- Acceptance Criteria
   1. Given my character is next to an NPC When I press E Then a dialogue box opens on page 1.
   2. Given I am not on the last page When I press E Then the next page shows.
   3. Given I am on the last page When I press E Then the dialogue box closes.
   4. Given a dialogue box is open When I press Escape Then it closes straight away.
   5. Given a dialogue box is open When I press movement keys Then my character doesn't move until the dialogue closes.
- Estimated time: about 4 hours | Priority: Should

### US-7.3 Dialogue box fits any window size

- Story: As a player, I want the dialogue text to always fit in the box whatever size my window is so that I can read the whole conversation.
- Acceptance Criteria
   1. Given the dialogue is long When the dialogue box shows up Then the text wraps and all of it fits inside the box.
   2. Given I have made the window smaller When the dialogue box shows up Then the title, text and control hints don't overlap.
   3. Given my window is not 16:9 When the dialogue box shows up Then it isn't stretched or squashed.
- Estimated time: about 6 hours | Priority: Should

### US-7.4 Tutorial NPC in the Hub

- Story: As a new player, I want a guide NPC in the Hub that I can ask about the controls at any time so that I can learn how to play quickly.
- Acceptance Criteria
   1. Given I am in the Hub When I talk to the Old Sage Then it explains the basics of moving, fighting, the bag and portals.
   2. Given I have talked to the guide before When I talk to it again Then I can read the tutorial again.
   3. Given I talk to the guide When the dialogue ends Then I don't get any item and the ending is not triggered.
- Estimated time: about 4 hours | Priority: Should

### US-7.5 Picking options in dialogue

- Story: As a player, I want to pick dialogue options with Up / Down and confirm them so that I can make choices by talking (like choosing where to go or which class to play).
- Acceptance Criteria
   1. Given the dialogue shows several options When I press Up / Down Then the highlight moves and doesn't go past the first or last option.
   2. Given an option is highlighted When I press E Then that option is carried out.
   3. Given an option is locked When I try to confirm it Then the option is not executed and the game tells me why it is locked.
- Estimated time: about 4 hours | Priority: Should

### US-7.6 Notice board in the Hub

- Story: As a new player, I want to read a notice board in the Hub that lists the controls so that I can check the controls quickly without talking to anyone.
- Acceptance Criteria
   1. Given my character is near the notice board When I look at the screen Then the prompt says "[E] Read Notice Board" instead of "Talk to".
   2. Given my character is next to the notice board When I press E Then it shows the controls for moving, interacting, the bag, gear and portals.
   3. Given the notice board is open When I press E or Escape Then it closes and I don't get any item.
- Estimated time: about 4 hours | Priority: Should
