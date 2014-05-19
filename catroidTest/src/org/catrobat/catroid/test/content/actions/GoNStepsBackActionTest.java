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

import android.test.AndroidTestCase;

import com.badlogic.gdx.scenes.scene2d.Group;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExtendedActions;
import org.catrobat.catroid.content.actions.GoNStepsBackAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class GoNStepsBackActionTest extends AndroidTestCase {

	private static final int STEPS = 13;
	private static final String NOT_NUMERICAL_STRING = "NOT_NUMERICAL_STRING";
	private final Formula steps = new Formula(STEPS);

	public void testSteps() {
		Group parentGroup = new Group();
		for (int i = 0; i < 20; i++) {
			Sprite spriteBefore = new Sprite("before" + i);
			parentGroup.addActor(spriteBefore.look);
		}
		Sprite sprite = new Sprite("testSprite");
		parentGroup.addActor(sprite.look);
		assertEquals("Unexpected initial sprite Z position", 20, sprite.look.getZIndex());

		int oldPosition = sprite.look.getZIndex();

		ExtendedActions.goNStepsBack(sprite, steps).act(1.0f);
		assertEquals("Incorrect sprite Z position after GoNStepsBackBrick executed",
				(oldPosition - STEPS), sprite.look.getZIndex());

		oldPosition = sprite.look.getZIndex();

		ExtendedActions.goNStepsBack(sprite, new Formula(-STEPS)).act(1.0f);
		assertEquals("Incorrect sprite Z position after GoNStepsBackBrick executed",
				(oldPosition + STEPS), sprite.look.getZIndex());
	}

	public void testNullSprite() {
		GoNStepsBackAction action = ExtendedActions.goNStepsBack(null, steps);
		try {
			action.act(1.0f);
			fail("Execution of GoNStepsBackBrick with null Sprite did not cause a NullPointerException to be thrown");
		} catch (NullPointerException expected) {
			assertTrue("Exception thrown successful", true);
		}
	}

	public void testBoundarySteps() {
		Group parentGroup = new Group();

		Sprite background = new Sprite("background");
		parentGroup.addActor(background.look);
		assertEquals("Unexpected initial sprite Z position", 0, background.look.getZIndex());

		Sprite sprite = new Sprite("testSprite");
		parentGroup.addActor(sprite.look);
		assertEquals("Unexpected initial sprite Z position", 1, sprite.look.getZIndex());

		Sprite sprite2 = new Sprite("testSprite2");
		parentGroup.addActor(sprite2.look);
		assertEquals("Unexpected initial sprite Z position", 2, sprite2.look.getZIndex());

		GoNStepsBackAction action = ExtendedActions.goNStepsBack(sprite, new Formula(Integer.MAX_VALUE));
		sprite.look.addAction(action);
		action.act(1.0f);
		assertEquals("GoNStepsBackBrick execution failed. Z position should be zero.", 1, sprite.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 2, sprite2.look.getZIndex());

		action = ExtendedActions.goNStepsBack(sprite, new Formula(Integer.MIN_VALUE));
		sprite.look.addAction(action);
		action.act(1.0f);
		assertEquals("An unwanted Integer overflow occured during GoNStepsBackBrick execution.", 2,
				sprite.look.getZIndex());
	}

	public void testBrickWithStringFormula() {
		Group parentGroup = new Group();
		Sprite background = new Sprite("background");
		parentGroup.addActor(background.look);
		Sprite sprite = new Sprite("testSprite");
		parentGroup.addActor(sprite.look);
		Sprite sprite2 = new Sprite("testSprite2");
		parentGroup.addActor(sprite2.look);

		GoNStepsBackAction action = ExtendedActions.goNStepsBack(sprite2, new Formula(String.valueOf(STEPS)));
		action.act(1.0f);
		assertEquals("Unexpected initial sprite Z position", 0, background.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 1, sprite2.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 2, sprite.look.getZIndex());

		action = ExtendedActions.goNStepsBack(sprite, new Formula(String.valueOf(NOT_NUMERICAL_STRING)));
		action.act(1.0f);
		assertEquals("Unexpected initial sprite Z position", 0, background.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 1, sprite2.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 2, sprite.look.getZIndex());
	}

	public void testNullFormula() {
		Group parentGroup = new Group();
		Sprite background = new Sprite("background");
		parentGroup.addActor(background.look);
		Sprite sprite = new Sprite("testSprite");
		parentGroup.addActor(sprite.look);
		Sprite sprite2 = new Sprite("testSprite2");
		parentGroup.addActor(sprite2.look);

		GoNStepsBackAction action = ExtendedActions.goNStepsBack(sprite2, null);
		action.act(1.0f);
		assertEquals("Unexpected initial sprite Z position", 0, background.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 1, sprite.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 2, sprite2.look.getZIndex());
	}

	public void testNotANumberFormula() {
		Group parentGroup = new Group();
		Sprite background = new Sprite("background");
		parentGroup.addActor(background.look);
		Sprite sprite = new Sprite("testSprite");
		parentGroup.addActor(sprite.look);
		Sprite sprite2 = new Sprite("testSprite2");
		parentGroup.addActor(sprite2.look);

		GoNStepsBackAction action = ExtendedActions.goNStepsBack(sprite2, new Formula(Double.NaN));
		action.act(1.0f);
		assertEquals("Unexpected initial sprite Z position", 0, background.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 1, sprite.look.getZIndex());
		assertEquals("Unexpected sprite Z position", 2, sprite2.look.getZIndex());
	}
}
