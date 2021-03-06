/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.content.actions;

import android.test.InstrumentationTestCase;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExtendedActions;
import org.catrobat.catroid.content.actions.SetGhostEffectAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetGhostEffectActionTest extends InstrumentationTestCase {

	private Formula effectValue = new Formula(50.5f);

	public void testGhostEffect() {
		Sprite sprite = new Sprite("testSprite");
		assertEquals("Unexpected initial sprite ghost effect value", 0f,
				sprite.look.getTransparencyInUserInterfaceDimensionUnit());
		SetGhostEffectAction action = ExtendedActions.setGhostEffect(sprite, effectValue);
		action.act(1.0f);
		assertEquals("Incorrect sprite scale value after SetGhostEffectBrick executed",
				effectValue.interpretFloat(sprite), sprite.look.getTransparencyInUserInterfaceDimensionUnit());

		action = ExtendedActions.setGhostEffect(sprite, new Formula(-50.0));
		action.act(1.0f);
		assertEquals("Incorrect sprite scale value after SetGhostEffectBrick executed", 0f,
				sprite.look.getTransparencyInUserInterfaceDimensionUnit());

		action = ExtendedActions.setGhostEffect(sprite, new Formula(150.0));
		action.act(1.0f);
		assertEquals("Incorrect sprite scale value after SetGhostEffectBrick executed", 100f,
				sprite.look.getTransparencyInUserInterfaceDimensionUnit());
	}

	public void testNullSprite() {
		SetGhostEffectAction action = ExtendedActions.setGhostEffect(null, effectValue);
		try {
			action.act(1.0f);
			fail("Execution of SetGhostEffectBrick with null Sprite did not cause a NullPointerException to be thrown");
		} catch (NullPointerException expected) {
			assertTrue("Exception thrown as expected", true);
		}
	}
}
