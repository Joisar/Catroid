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
package org.catrobat.catroid;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.FileChecksumContainer;
import org.catrobat.catroid.common.MessageContainer;
import org.catrobat.catroid.common.ScreenModes;
import org.catrobat.catroid.common.StandardProjectHandler;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
<<<<<<< HEAD
=======
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick;
import org.catrobat.catroid.content.bricks.IfLogicElseBrick;
import org.catrobat.catroid.content.bricks.IfLogicEndBrick;
import org.catrobat.catroid.content.bricks.LoopBeginBrick;
import org.catrobat.catroid.content.bricks.LoopEndBrick;
import org.catrobat.catroid.io.LoadProjectTask;
>>>>>>> master
import org.catrobat.catroid.io.LoadProjectTask.OnLoadProjectCompleteListener;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.livewallpaper.R;
import org.catrobat.catroid.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ProjectManager implements OnLoadProjectCompleteListener {
	private static final ProjectManager INSTANCE = new ProjectManager();
	private static final String TAG = ProjectManager.class.getSimpleName();

	private Project project;
	private Script currentScript;
	private Sprite currentSprite;
	private boolean asynchronTask = true;

	private FileChecksumContainer fileChecksumContainer = new FileChecksumContainer();

	private ProjectManager() {
	}

	public static ProjectManager getInstance() {
		return INSTANCE;
	}

	public boolean loadProject(String projectName, Context context, boolean errorMessage) {
		fileChecksumContainer = new FileChecksumContainer();
		Project oldProject = project;
		MessageContainer.createBackup();
		project = StorageHandler.getInstance().loadProject(projectName);

		if (project == null) {
			if (oldProject != null) {
				project = oldProject;
				MessageContainer.restoreBackup();
			} else {
				project = Utils.findValidProject();
				if (project == null) {
					try {
						project = StandardProjectHandler.createAndSaveStandardProject(context);
						MessageContainer.clearBackup();
					} catch (IOException ioException) {
						if (errorMessage) {
							Utils.showErrorDialog(context, R.string.error_load_project);
						}
						Log.e(TAG, "Cannot load project.", ioException);
						return false;
					}
				}
			}
			if (errorMessage) {
				Utils.showErrorDialog(context, R.string.error_load_project);
			}
			return false;
		} else if (project.getCatrobatLanguageVersion() > Constants.CURRENT_CATROBAT_LANGUAGE_VERSION) {
			project = oldProject;
			if (errorMessage) {
				Utils.showErrorDialog(context, R.string.error_outdated_pocketcode_version);
				// TODO insert update link to Google Play
			}
			return false;
		} else {
			if (project.getCatrobatLanguageVersion() == 0.8f) {
				//TODO insert in every "When project starts" script list a "show" brick
				project.setCatrobatLanguageVersion(0.9f);
			}
			if (project.getCatrobatLanguageVersion() == 0.9f) {
				project.setCatrobatLanguageVersion(0.91f);
				//no convertion needed - only change to white background
			}
			if (project.getCatrobatLanguageVersion() == 0.91f) {
				project.setCatrobatLanguageVersion(0.92f);
				project.setScreenMode(ScreenModes.STRETCH);
                checkNestingBrickReferences();
			}
			//insert further convertions here

			if (project.getCatrobatLanguageVersion() == Constants.CURRENT_CATROBAT_LANGUAGE_VERSION) {
				//project seems to be converted now and can be loaded
				localizeBackgroundSprite(context);
				return true;
			}
			//project cannot be converted
			project = oldProject;
			if (errorMessage) {
				Utils.showErrorDialog(context, R.string.error_project_compatability);
			}
			return false;
		}
	}

	private void localizeBackgroundSprite(Context context) {
		// Set generic localized name on background sprite and move it to the back.
		if (project.getSpriteList().size() > 0) {
			project.getSpriteList().get(0).setName(context.getString(R.string.background));
			project.getSpriteList().get(0).look.setZIndex(0);
		}
		MessageContainer.clearBackup();
		currentSprite = null;
		currentScript = null;
		Utils.saveToPreferences(context, Constants.PREF_PROJECTNAME_KEY, project.getName());
	}

	public boolean cancelLoadProject() {
		return StorageHandler.getInstance().cancelLoadProject();
	}

	public boolean canLoadProject(String projectName) {
		return StorageHandler.getInstance().loadProject(projectName) != null;
	}

	public void saveProject() {
		if (project == null) {
			return;
		}

		if (asynchronTask) {
			SaveProjectAsynchronousTask saveTask = new SaveProjectAsynchronousTask();
			saveTask.execute();
		} else {
			StorageHandler.getInstance().saveProject(project);
		}
	}

	public boolean initializeDefaultProject(Context context) {
		try {
			fileChecksumContainer = new FileChecksumContainer();
			project = StandardProjectHandler.createAndSaveStandardProject(context);
			currentSprite = null;
			currentScript = null;
			return true;
		} catch (IOException ioException) {
			Log.e(TAG, "Cannot initialize default project.", ioException);
			Utils.showErrorDialog(context, R.string.error_load_project);
			return false;
		}
	}

	public void initializeNewProject(String projectName, Context context, boolean empty) throws IOException {
		fileChecksumContainer = new FileChecksumContainer();

		if (empty) {
			project = StandardProjectHandler.createAndSaveEmptyProject(projectName, context);
		} else {
			project = StandardProjectHandler.createAndSaveStandardProject(projectName, context);
		}

		currentSprite = null;
		currentScript = null;
	}

	public Project getCurrentProject() {
		return project;
	}

	public void setProject(Project project) {
		currentScript = null;
		currentSprite = null;

		this.project = project;
	}

	public void deleteCurrentProject() {
		StorageHandler.getInstance().deleteProject(project);
		project = null;
	}

	public boolean renameProject(String newProjectName, Context context) {
		if (StorageHandler.getInstance().projectExists(newProjectName)) {
			Utils.showErrorDialog(context, R.string.error_project_exists);
			return false;
		}

		String oldProjectPath = Utils.buildProjectPath(project.getName());
		File oldProjectDirectory = new File(oldProjectPath);

		String newProjectPath = Utils.buildProjectPath(newProjectName);
		File newProjectDirectory = new File(newProjectPath);

		boolean directoryRenamed = false;

		if (oldProjectPath.equalsIgnoreCase(newProjectPath)) {
			String tmpProjectPath = Utils.buildProjectPath(createTemporaryDirectoryName(newProjectName));
			File tmpProjectDirectory = new File(tmpProjectPath);
			directoryRenamed = oldProjectDirectory.renameTo(tmpProjectDirectory);
			if (directoryRenamed) {
				directoryRenamed = tmpProjectDirectory.renameTo(newProjectDirectory);
			}
		} else {
			directoryRenamed = oldProjectDirectory.renameTo(newProjectDirectory);
		}

		if (directoryRenamed) {
			project.setName(newProjectName);
			saveProject();
		}

		if (!directoryRenamed) {
			Utils.showErrorDialog(context, R.string.error_rename_project);
		}

		return directoryRenamed;
	}

	public Sprite getCurrentSprite() {
		return currentSprite;
	}

	public void setCurrentSprite(Sprite sprite) {
		currentSprite = sprite;
	}

	public Script getCurrentScript() {
		return currentScript;
	}

	public void setCurrentScript(Script script) {
		if (script == null) {
			currentScript = null;
		} else if (currentSprite.getScriptIndex(script) != -1) {
			currentScript = script;
		}
	}

	public void addSprite(Sprite sprite) {
		project.addSprite(sprite);
	}

	public void addScript(Script script) {
		currentSprite.addScript(script);
	}

	public boolean spriteExists(String spriteName) {
		for (Sprite sprite : project.getSpriteList()) {
			if (sprite.getName().equalsIgnoreCase(spriteName)) {
				return true;
			}
		}
		return false;
	}

	public int getCurrentSpritePosition() {
		return project.getSpriteList().indexOf(currentSprite);
	}

	public int getCurrentScriptPosition() {
		int currentSpritePosition = this.getCurrentSpritePosition();
		if (currentSpritePosition == -1) {
			return -1;
		}

		return project.getSpriteList().get(currentSpritePosition).getScriptIndex(currentScript);
	}

	private String createTemporaryDirectoryName(String projectDirectoryName) {
		String temporaryDirectorySuffix = "_tmp";
		String temporaryDirectoryName = projectDirectoryName + temporaryDirectorySuffix;
		int suffixCounter = 0;
		while (Utils.checkIfProjectExistsOrIsDownloadingIgnoreCase(temporaryDirectoryName)) {
			temporaryDirectoryName = projectDirectoryName + temporaryDirectorySuffix + suffixCounter;
			suffixCounter++;
		}
		return temporaryDirectoryName;
	}

	public FileChecksumContainer getFileChecksumContainer() {
		return this.fileChecksumContainer;
	}

	public void setFileChecksumContainer(FileChecksumContainer fileChecksumContainer) {
		this.fileChecksumContainer = fileChecksumContainer;
	}

	private class SaveProjectAsynchronousTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			StorageHandler.getInstance().saveProject(project);
			return null;
		}
	}

	@Override
	public void onLoadProjectSuccess(boolean startProjectActivity) {

	}

	@Override
	public void onLoadProjectFailure() {

	}

	public void checkNestingBrickReferences() {
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		if (currentProject != null) {
			for (Sprite currentSprite : currentProject.getSpriteList()) {
				int numberOfScripts = currentSprite.getNumberOfScripts();
				for (int pos = 0; pos < numberOfScripts; pos++) {
					Script script = currentSprite.getScript(pos);
					boolean scriptCorrect = true;
					for (Brick currentBrick : script.getBrickList()) {
						if (currentBrick instanceof IfLogicBeginBrick) {
							IfLogicElseBrick elseBrick = ((IfLogicBeginBrick) currentBrick).getIfElseBrick();
							IfLogicEndBrick endBrick = ((IfLogicBeginBrick) currentBrick).getIfEndBrick();
							if (elseBrick == null || endBrick == null || elseBrick.getIfBeginBrick() == null
									|| elseBrick.getIfEndBrick() == null || endBrick.getIfBeginBrick() == null
									|| endBrick.getIfElseBrick() == null
									|| !elseBrick.getIfBeginBrick().equals(currentBrick)
									|| !elseBrick.getIfEndBrick().equals(endBrick)
									|| !endBrick.getIfBeginBrick().equals(currentBrick)
									|| !endBrick.getIfElseBrick().equals(elseBrick)) {
								scriptCorrect = false;
								Log.d("REFERENCE ERROR!!", "Brick has wrong reference:" + currentSprite + " "
										+ currentBrick);
							}
						} else if (currentBrick instanceof LoopBeginBrick) {
							LoopEndBrick endBrick = ((LoopBeginBrick) currentBrick).getLoopEndBrick();
							if (endBrick == null || endBrick.getLoopBeginBrick() == null
									|| !endBrick.getLoopBeginBrick().equals(currentBrick)) {
								scriptCorrect = false;
								Log.d("REFERENCE ERROR!!", "Brick has wrong reference:" + currentSprite + " "
										+ currentBrick);
							}
						}
					}
					if (!scriptCorrect) {
						//correct references
						ArrayList<IfLogicBeginBrick> ifBeginList = new ArrayList<IfLogicBeginBrick>();
						ArrayList<LoopBeginBrick> loopBeginList = new ArrayList<LoopBeginBrick>();
						for (Brick currentBrick : script.getBrickList()) {
							if (currentBrick instanceof IfLogicBeginBrick) {
								ifBeginList.add((IfLogicBeginBrick) currentBrick);
							} else if (currentBrick instanceof LoopBeginBrick) {
								loopBeginList.add((LoopBeginBrick) currentBrick);
							} else if (currentBrick instanceof LoopEndBrick) {
								LoopBeginBrick loopBeginBrick = loopBeginList.get(loopBeginList.size() - 1);
								loopBeginBrick.setLoopEndBrick((LoopEndBrick) currentBrick);
								((LoopEndBrick) currentBrick).setLoopBeginBrick(loopBeginBrick);
								loopBeginList.remove(loopBeginBrick);
							} else if (currentBrick instanceof IfLogicElseBrick) {
								IfLogicBeginBrick ifBeginBrick = ifBeginList.get(ifBeginList.size() - 1);
								ifBeginBrick.setIfElseBrick((IfLogicElseBrick) currentBrick);
								((IfLogicElseBrick) currentBrick).setIfBeginBrick(ifBeginBrick);
							} else if (currentBrick instanceof IfLogicEndBrick) {
								IfLogicBeginBrick ifBeginBrick = ifBeginList.get(ifBeginList.size() - 1);
								IfLogicElseBrick elseBrick = ifBeginBrick.getIfElseBrick();
								ifBeginBrick.setIfEndBrick((IfLogicEndBrick) currentBrick);
								elseBrick.setIfEndBrick((IfLogicEndBrick) currentBrick);
								((IfLogicEndBrick) currentBrick).setIfBeginBrick(ifBeginBrick);
								((IfLogicEndBrick) currentBrick).setIfElseBrick(elseBrick);
								ifBeginList.remove(ifBeginBrick);
							}
						}
					}
				}
			}
		}
	}
}
