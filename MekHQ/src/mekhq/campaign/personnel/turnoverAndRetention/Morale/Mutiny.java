package mekhq.campaign.personnel.turnoverAndRetention.Morale;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.HitData;
import megamek.common.ToHitData;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.rating.FieldManualMercRevDragoonsRating;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.moraleDialogs.TransitMutinyOnsetDialog;
import mekhq.gui.dialog.moraleDialogs.TransitMutinyToe;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mekhq.campaign.personnel.turnoverAndRetention.Morale.Desertion.processDesertion;
import static mekhq.gui.dialog.moraleDialogs.TransitMutinyBattleConditionDialog.transitMutinyBattleConditionDialog;
import static mekhq.gui.dialog.moraleDialogs.TransitMutinyCampaignOverDialog.transitMutinyCampaignOverDialog;
import static mekhq.gui.dialog.moraleDialogs.TransitMutinyConclusionDialog.transitMutinyConclusionDialog;

public class Mutiny {
    static void processMutiny(Campaign campaign, List<Person> loyalists, HashMap<Person, Integer> mutineers, int targetNumber, ResourceBundle resources) {
        Random random = new Random();

        if (campaign.getCampaignOptions().getMutinyMethod().isCampaignOperations()) {
            processCamOpsMutiny(campaign, random, loyalists, mutineers, targetNumber, resources);
        } else {
            // TODO this is where Advanced Mutinies call goes
            return;
        }
    }

    static void processCamOpsMutiny(Campaign campaign, Random random, List<Person> loyalists, HashMap<Person, Integer> mutineers, int targetNumber, ResourceBundle resources) {
        // if the mutineers are out-numbered 4:1 (or worse), they're treated as if having deserted
        if ((loyalists.size() / 4) >= mutineers.keySet().size()) {
            if (mutineers.keySet().size() == 1) {
                campaign.addReport(resources.getString("mutinyThwartedSingular.text"));
            } else {
                campaign.addReport(String.format(resources.getString("mutinyThwartedPlural.text"), mutineers.keySet().size()));
            }

            processDesertion(campaign, mutineers, targetNumber, resources);

            return;
        }

        // next we pick the mutiny & loyalist leaders.
        // CamOps doesn't have any special handling for unit leader, so we don't either
        Person mutineerLeader = campaign.getHighestRankedPerson(new ArrayList<>(mutineers.keySet()), true);
        Person loyalistLeader = campaign.getHighestRankedPerson(loyalists, true);

        // this dialog alerts the player to the mutiny
        TransitMutinyOnsetDialog.transitMutinyOnsetDialog(resources);

        // this dialog tells the player about the array of forces and gives them the option to support the mutineers or loyalists

        if (campaign.getLocation().isOnPlanet()) {
            processAbstractBattle(campaign, random, resources, loyalistLeader, loyalists, mutineerLeader, mutineers);
        }


    }

    private static void processAbstractBattle(Campaign campaign, Random random, ResourceBundle resources,
                                              Person loyalistLeader, List<Person> loyalists,
                                              Person mutineerLeader, HashMap<Person, Integer> mutineers) {
        // Start by gathering the battle powers for each side
        HashMap<String, Integer> loyalistBattlePower = getAbstractBattlePower(loyalistLeader, loyalists);
        HashMap<String, Integer> mutinyBattlePower = getAbstractBattlePower(mutineerLeader, new ArrayList<>(mutineers.keySet()));

        // inform the player of the measure of power among each side
        int support = TransitMutinyToe.transitMutinyToeDialog(resources,
                loyalistLeader, loyalists.size(), loyalistBattlePower.get("attack"), loyalistBattlePower.get("defense"),
                mutineerLeader, mutineers.keySet().size(), mutinyBattlePower.get("attack"), mutinyBattlePower.get("defense"));

        // process the battle
        int victor = processAbstractBattleRound(campaign, random, resources, loyalists, loyalistBattlePower, new ArrayList<>(mutineers.keySet()), mutinyBattlePower);

        // broadcast the conclusion
        transitMutinyConclusionDialog(resources, victor);

        // if the player supported the wrong side (and didn't pick 'victor') end their campaign
        if ((victor != support) && (support != 2)) {
            transitMutinyCampaignOverDialog(campaign.getName(), resources);

            campaign.getWarehouse().getParts().clear();
            campaign.getPersonnel().clear();
            campaign.getUnits().clear();
            campaign.getFinances().getTransactions().clear();

            return;
        }

        if (victor == 0) {
            for (Person person : mutineers.keySet()) {
                person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER, true);
            }
        } else {
            for (Person person : loyalists) {
                person.setPrisonerStatus(campaign, PrisonerStatus.PRISONER, true);
            }
        }

        // damage any vessels the unit was traveling in
        if (!campaign.getLargeCraftAndWarShips().isEmpty()) {
            damageShips(campaign, resources);
        } else {
            // issue a fine based on damage issued to any hired transports
            // I did want this to check whether the company was needing additional transports,
            // but I couldn't get that to work
            abstractDamageShips(campaign, resources);
        }
    }

    private static void abstractDamageShips(Campaign campaign, ResourceBundle resources) {
        int clusters = Compute.d6(10);

        int damage = IntStream.range(0, clusters)
                .map(clusterCount -> Compute.d6(2))
                .sum();

        int fine = (damage / 80) * 100000;
        campaign.getFinances().debit(TransactionType.FINE, campaign.getLocalDate(), Money.of(fine), resources.getString("mutinyDropShipDamageFine.text"));
        campaign.addReport(String.format(resources.getString("mutinyDropShipDamage.text"), fine));
    }


    private static void damageShips(Campaign campaign, ResourceBundle resources) {
        // this reflects internal damage sustained during the mutiny
        for (Unit unit : campaign.getLargeCraftAndWarShips()) {
            Entity entity = unit.getEntity();
            boolean isDestroyed = false;

            int numClusters = Compute.d6(10);

            for (int clusterCount = 0; clusterCount < numClusters; clusterCount++) {
                int location = Compute.randomInt(4);

                HitData HitData = entity.rollHitLocation(ToHitData.HIT_NORMAL, location);
                int resultingArmor = Math.max(0, entity.getArmor(HitData) - Compute.d6(2));
                entity.setArmor(resultingArmor, location);

                if (entity.getArmor(location) == 0) {
                    isDestroyed = true;
                }
            }

            if (isDestroyed) {
                campaign.getHangar().removeUnit(unit.getId());
                campaign.addReport(String.format(resources.getString("mutinyUnitDestroyed.text"), unit.getName()));
            } else {
                unit.runDiagnostic(true);
            }
        }
    }

    private static HashMap<String, Integer> getAbstractBattlePower(Person loyalistLeader, List<Person> loyalists) {
        HashMap<String, Integer> battlePower = new HashMap<>();

        int attackPower = 0;
        int defensePower = 0;

        for (Person person : loyalists) {
            if (person.hasSkill(SkillType.S_SMALL_ARMS)) {
                attackPower += person.getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() / 2;
                defensePower += person.getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_ADMIN)) {
                defensePower += person.getSkill(SkillType.S_ADMIN).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_DOCTOR)) {
                defensePower += person.getSkill(SkillType.S_DOCTOR).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_TACTICS)) {
                attackPower += person.getSkill(SkillType.S_TACTICS).getExperienceLevel() / 2;
            }

            if (person.hasSkill(SkillType.S_STRATEGY)) {
                attackPower += person.getSkill(SkillType.S_STRATEGY).getExperienceLevel() / 2;
            }
        }

        if (loyalistLeader.hasSkill(SkillType.S_LEADER)) {
            attackPower += loyalistLeader.getSkill(SkillType.S_LEADER).getExperienceLevel() / 2;
            defensePower += loyalistLeader.getSkill(SkillType.S_LEADER).getExperienceLevel() / 2;
        }

        battlePower.put("attack", attackPower);
        battlePower.put("defense", defensePower);

        return battlePower;
    }

    public static int processAbstractBattleRound(Campaign campaign, Random random, ResourceBundle resources,
                                                 List<Person> loyalists, HashMap<String, Integer> loyalistBattlePower,
                                                 List<Person> mutineers, HashMap<String, Integer> mutinyBattlePower) {

        // the loyalist side of the battle (ten rounds of combat)
        int hits = 0;
        int combatRound = 0;

        while (combatRound < 10) {
            hits += combatRound(loyalistBattlePower.get("attack"), mutinyBattlePower.get("defense"));
            combatRound++;
        }

        distributeHits(campaign, mutineers, hits, random);

        // the mutineer side of the battle (ten rounds of combat)
        hits = 0;
        combatRound = 0;

        while (combatRound < 10) {
            hits += combatRound(mutinyBattlePower.get("attack"), loyalistBattlePower.get("defense"));
            combatRound++;
        }

        distributeHits(campaign, loyalists, hits, random);

        // post-battle clean up
        List<Person> mutineerGraveyard = fillGraveyard(campaign, mutineers);
        List<Person> loyalistsGraveyard = fillGraveyard(campaign, loyalists);

        for (Person person : mutineerGraveyard) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
        }

        for (Person person : loyalistsGraveyard) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
        }

        mutineers.removeAll(mutineerGraveyard);
        loyalists.removeAll(loyalistsGraveyard);

        // calculate results
        if ((mutineers.isEmpty()) && (loyalists.isEmpty())) {
            // they wiped each other out
            return -1;
        }

        // show some flavor text, to make the battle seem more dramatic
        transitMutinyBattleConditionDialog(resources, random, loyalistsGraveyard.size(), mutineerGraveyard.size());

        if (mutineerGraveyard.size() == loyalistsGraveyard.size()) {
            // if the battle is drawn, roll a d6
            int tieBreaker = (Compute.d6(1));

            switch (tieBreaker) {
                case 1:
                case 2:
                case 3:
                    // loyalist victory
                    return 0;
                case 4:
                case 5:
                case 6:
                    // mutineer victory
                    return 1;
                default:
                    throw new IllegalStateException("Unexpected value in processAbstractBattleRound: " + tieBreaker);
            }

        } else if (mutineerGraveyard.size() > loyalistsGraveyard.size()) {
            // loyalist victory
            return 0;
        } else {
            // mutineer victory
            return 1;
        }
    }

    private static List<Person> fillGraveyard(Campaign campaign, List<Person> personnel) {
        List<Person> graveyard;

        if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
            graveyard = personnel.stream()
                    .filter(person -> person.getInjuries().size() > 5)
                    .collect(Collectors.toList());
        } else {
            graveyard = personnel.stream()
                    .filter(person -> person.getHits() > 5)
                    .collect(Collectors.toList());
        }
        return graveyard;
    }

    private static void distributeHits(Campaign campaign, List<Person> potentialVictims, int hits, Random random) {
        for (int hit = 0; hit < hits; hit++) {
            boolean victimFound = false;
            int maxAttempts = 3;

            Person victim = null;

            while ((!victimFound) || (maxAttempts == 0)) {
                victim = potentialVictims.get(random.nextInt(potentialVictims.size()));

                if ((campaign.getCampaignOptions().isUseAdvancedMedical()) && (victim.getInjuries().size() < 6)) {
                    victimFound = true;
                } else if (victim.getHits() < 6) {
                    victimFound = true;
                }

                // this prevents an infinite loop from occurring when all combatants are dead
                maxAttempts--;
            }

            int injuryCount = MathUtility.clamp(Compute.randomInt(4) + Compute.randomInt(4) + 2, 2, 5);

            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                InjuryUtil.resolveCombatDamage(campaign, victim, injuryCount);
                victim.setHits(victim.getHits() + injuryCount);
            } else {
                victim.setHits(victim.getHits() + injuryCount);
            }
        }
    }

    private static int combatRound(int attackerAttackPower, int defenderDefensePower) {
        int attackerHits = 0;
        int attackerCriticalHits = 0;

        for (int attackRoll = 0; attackRoll < attackerAttackPower; attackRoll++) {
            int roll = Compute.d6(1);

            if (roll == 6) {
                attackerCriticalHits++;
            } else if (roll >= 4) {
                attackerHits++;
            }
        }

        int defenderDefense = (int) IntStream.range(0, defenderDefensePower)
                .filter(defenseRoll -> Compute.d6(1) == 6)
                .count();

        return Math.max(0, (attackerHits - defenderDefense)) + attackerCriticalHits;
    }
}
