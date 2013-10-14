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

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.ChangeVariableBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType;
import org.catrobat.catroid.formulaeditor.Operators;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class ChangeVariableActionTest extends AndroidTestCase {

	private static final String TEST_USERVARIABLE = "testUservariable";
	private static final int CHANGE_VARIABLE_VALUE = 11;
	private Sprite testSprite;
	private StartScript testScript;
	private Project project;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testSprite = new Sprite("testSprite");
		project = new Project(null, "testProject");
		testScript = new StartScript(testSprite);
		ProjectManager.getInstance().setProject(project);
		ProjectManager.getInstance().setCurrentSprite(testSprite);
		ProjectManager.getInstance().setCurrentScript(testScript);
		ProjectManager.getInstance().getCurrentProject().getUserVariables().deleteUserVariableByName(TEST_USERVARIABLE);
		ProjectManager.getInstance().getCurrentProject().getUserVariables().addProjectUserVariable(TEST_USERVARIABLE);
	}

	public void testChangeUserVariable() throws InterruptedException {

		UserVariable userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);

		ChangeVariableBrick changeBrick = new ChangeVariableBrick(testSprite, new Formula(CHANGE_VARIABLE_VALUE),
				userVariable);
		testScript.addBrick(changeBrick);
		testSprite.addScript(testScript);
		project.addSprite(testSprite);
		testSprite.createStartScriptActionSequence();
		testSprite.look.act(1f);

		userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);
		assertEquals("Variable not changed", CHANGE_VARIABLE_VALUE, userVariable.getValue().intValue());
	}

	public void testInvalidUserVariable() throws InterruptedException {

		UserVariable userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);

		ChangeVariableBrick changeBrick = new ChangeVariableBrick(testSprite, new Formula(CHANGE_VARIABLE_VALUE));
		Formula validFormula = new Formula(0);
		validFormula.setRoot(new FormulaElement(ElementType.OPERATOR, Operators.SMALLER_THAN.name(), null,
				new FormulaElement(ElementType.NUMBER, "1", null), new FormulaElement(ElementType.NUMBER, "2", null)));
		testScript.addBrick(changeBrick);
		testSprite.addScript(testScript);
		project.addSprite(testSprite);
		testSprite.createStartScriptActionSequence();
		testSprite.look.act(1f);

		userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);
		assertEquals("Variable changed, but should not!", 0, userVariable.getValue().intValue());
	}

	public void testChangeUserVariableWithString() {

		UserVariable userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);

		String offset = "3";
		Formula changeFormula = new Formula(0);
		changeFormula.setRoot(new FormulaElement(ElementType.STRING, offset, null, null, null));
		ChangeVariableBrick changeVariableBrick = new ChangeVariableBrick(testSprite, changeFormula, userVariable);
		testScript.addBrick(changeVariableBrick);
		testSprite.addScript(testScript);
		project.addSprite(testSprite);
		testSprite.createStartScriptActionSequence();
		testSprite.look.act(1f);

		userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);
		assertEquals("String UserVariable not changed!", Integer.valueOf(offset).intValue(), userVariable.getValue()
				.intValue());
	}

	public void testChangeUserVariableWithInvalidString() {

		UserVariable userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);

		String myString = "myString";
		Formula validFormula = new Formula(0);
		validFormula.setRoot(new FormulaElement(ElementType.STRING, myString, null, null, null));
		ChangeVariableBrick changeVariableBrick = new ChangeVariableBrick(testSprite,
				new Formula(CHANGE_VARIABLE_VALUE), userVariable);
		testScript.addBrick(changeVariableBrick);
		testSprite.addScript(testScript);
		project.addSprite(testSprite);
		testSprite.createStartScriptActionSequence();
		testSprite.look.act(1f);

		userVariable = ProjectManager.getInstance().getCurrentProject().getUserVariables()
				.getUserVariable(TEST_USERVARIABLE, null);
		assertEquals("String UserVariable not changed!", CHANGE_VARIABLE_VALUE, userVariable.getValue().intValue());
	}

}
