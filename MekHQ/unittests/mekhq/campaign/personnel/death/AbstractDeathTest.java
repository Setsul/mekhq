/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.death;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Personnel Testing Tracker:
 * 1) Death:
 *  a) AbstractDeath
 * 2) Divorce:
 *  a) AbstractDivorce
 * 3) Marriage:
 *  a) AbstractMarriage
 * 4) Procreation:
 *  a) AbstractProcreation
 * 5) Enums:
 *  a) Profession
 * Unhandled:
 * 1) Generator: All
 * 2) Ranks: All
 * 3) General: All
 *
 * Other Testing Tracker:
 * 1) GUI Enums: Most are partially tested currently
 * 2) Universe Enums: Most are unimplemented currently
 */
@ExtendWith(value = MockitoExtension.class)
public class AbstractDeathTest {
// Saved for Future Test Usage
/*
    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }
 */

    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractDeath mockDeath;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    //region Constructors
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testConstructorInitializesCauses() {

    }
    //endregion Constructors

    @Test
    public void testCanDie() {
        doCallRealMethod().when(mockDeath).canDie(any(), any(), anyBoolean());

        final Map<AgeGroup, Boolean> enabledAgeGroups = new HashMap<>();
        enabledAgeGroups.put(AgeGroup.CHILD, false);
        enabledAgeGroups.put(AgeGroup.ADULT, true);
        when(mockDeath.getEnabledAgeGroups()).thenReturn(enabledAgeGroups);

        final Person mockPerson = mock(Person.class);

        // Can be retired
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.RETIRED);
        assertNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, false));

        // Can't be dead
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.KIA);
        assertNotNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, false));

        // Can't randomly die if immortal
        when(mockPerson.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(mockPerson.isImmortal()).thenReturn(true);
        assertNotNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));
        
        // Age Group must be enabled
        when(mockPerson.isImmortal()).thenReturn(false);
        assertNotNull(mockDeath.canDie(mockPerson, AgeGroup.CHILD, true));

        // Can't be Clan Personnel with Random Clan Death Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockDeath.isUseRandomClanPersonnelDeath()).thenReturn(false);
        when(mockDeath.isUseRandomPrisonerDeath()).thenReturn(true);
        assertNotNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));

        // Can be Non-Clan Personnel with Random Clan Death Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));

        // Can be a Non-Prisoner with Random Prisoner Death Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockDeath.isUseRandomPrisonerDeath()).thenReturn(false);
        assertNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));

        // Can't be a Prisoner with Random Prisoner Death Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));

        // Can be a Clan Prisoner with Random Clan and Random Prisoner Death Enabled
        lenient().when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockDeath.isUseRandomClanPersonnelDeath()).thenReturn(true);
        when(mockDeath.isUseRandomPrisonerDeath()).thenReturn(true);
        assertNull(mockDeath.canDie(mockPerson, AgeGroup.ADULT, true));
    }

    //region New Day
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessNewDay() {

    }
    //endregion New Day

    //region Cause
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetCause() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetDefaultCause() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDetermineIfInjuriesCausedTheDeath() {

    }
    //endregion Cause

    //region File I/O
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testInitializeCauses() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testInitializeCausesFromFile() {

    }
    //endregion File I/O
}
