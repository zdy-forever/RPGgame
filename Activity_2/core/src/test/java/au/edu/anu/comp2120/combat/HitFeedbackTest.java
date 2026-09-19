package au.edu.anu.comp2120.combat;

import au.edu.anu.comp2120.enemy.TrainingDummy;
import au.edu.anu.comp2120.world.CollisionBox;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Confirms disposable art feedback cannot change damage, duplicate reactions or retain old scopes.
 * @author Daoyan Zhu (u7782042)
 */
final class HitFeedbackTest {
    /** Independent shared damage pipeline. */
    private final CombatSystem combat = new CombatSystem();
    /** Stationary receiver reusing actual health/scoring implementation. */
    private final TrainingDummy target = new TrainingDummy(new CollisionBox(40,0,40,40));
    /** Distinct player-faction source, with no renderer or asset dependency. */
    private final Combatant source = new Combatant(new TrainingDummy(new CollisionBox(0,0,20,20)),
            Faction.PLAYER,new CollisionBox(0,0,20,20));

    /** Registers a new isolated combat scope per test. */
    HitFeedbackTest() { combat.register(source); combat.register(target.getCombatant()); }

    /** Accepted damage creates one immutable transferable observation, not a new attack. */
    @Test void feedbackIsOnceAndDoesNotRedeliverDamage() {
        hit(ElementType.FIRE);
        var feedback = combat.drainHitFeedback();
        assertEquals(1,feedback.size());
        assertEquals(ElementType.FIRE,feedback.get(0).element());
        assertEquals(target.getCombatant().getHurtbox(),feedback.get(0).bounds());
        assertFalse(feedback.get(0).blocked());
        assertTrue(combat.drainHitFeedback().isEmpty());
        assertEquals(10,target.getTotalDamage());
        combat.update(1);
        assertTrue(combat.drainHitFeedback().isEmpty());
        assertEquals(10,target.getTotalDamage());
    }

    /** The reported reaction is the pipeline's confirmed ordered result, not a second lookup. */
    @Test void feedbackObservesReactionAndDamage() {
        hit(ElementType.FIRE); combat.drainHitFeedback();
        hit(ElementType.WATER);
        assertEquals("vaporize",combat.drainHitFeedback().get(0).reaction());
        assertEquals(30,target.getTotalDamage());
    }

    /** A miss produces no hit art; an intercepted hit is explicitly distinguished from damage. */
    @Test void missesAndBlocksHaveDistinctFeedback() {
        combat.tryAttack(source,new AttackSpec("miss",10,0.1,0),new CollisionBox(200,0,10,10));
        combat.update(0.2);
        assertTrue(combat.drainHitFeedback().isEmpty());
        target.getCombatant().setHitInterceptor(attack -> true);
        hit(ElementType.FIRE);
        assertTrue(combat.drainHitFeedback().get(0).blocked());
        assertEquals(0,target.getTotalDamage());
    }

    /** An absent renderer cannot grow feedback indefinitely, and map clear discards it. */
    @Test void feedbackIsBoundedAndScopeLocal() {
        for(int i=0;i<200;i++) { hit(ElementType.NONE); }
        assertEquals(128,combat.drainHitFeedback().size());
        assertEquals(2000,target.getTotalDamage());
        hit(ElementType.NONE);
        combat.clear();
        assertTrue(combat.drainHitFeedback().isEmpty());
    }

    /**
     * Settles a fresh finite attack through the actual shared pipeline.
     * @param element ordinary direct attack tag
     */
    private void hit(ElementType element) {
        assertTrue(combat.tryAttack(source,new AttackSpec("feedback",10,0.1,0,element),
                target.getCombatant().getHurtbox()).isPresent());
        combat.update(0.2);
    }
}
