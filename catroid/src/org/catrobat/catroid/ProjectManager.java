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

public final class ProjectManager implements OnLoadProjectCompleteListener,
		OnCheckTokenCompleteListener {
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

	public void uploadProject(String projectName,
			FragmentActivity fragmentActivity) {
		if (getCurrentProject() == null
				|| !getCurrentProject().getName().equals(projectName)) {
			LoadProjectTask loadProjectTask = new LoadProjectTask(
					fragmentActivity, projectName, false, false);
			loadProjectTask.setOnLoadProjectCompleteListener(this);
			loadProjectTask.execute();
		}
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(fragmentActivity);
		String token = preferences.getString(Constants.TOKEN,
				Constants.NO_TOKEN);
		String username = preferences.getString(Constants.USERNAME,
				Constants.NO_USERNAME);

		if (token.equals(Constants.NO_TOKEN)
				|| token.length() != ServerCalls.TOKEN_LENGTH
				|| token.equals(ServerCalls.TOKEN_CODE_INVALID)) {
			showLoginRegisterDialog(fragmentActivity);
		} else {
			CheckTokenTask checkTokenTask = new CheckTokenTask(
					fragmentActivity, token, username);
			checkTokenTask.setOnCheckTokenCompleteListener(this);
			checkTokenTask.execute();
		}
	}

	public boolean loadProject(String projectName, Context context,
			boolean errorMessage) {
		fileChecksumContainer = new FileChecksumContainer();
		Project oldProject = project;
		MessageContainer.createBackup();
		project = StorageHandler.getInstance().loadProject(projectName);

		loadVirtualGamepadImages(projectName, context);

		if (project == null) {
			if (oldProject != null) {
				project = oldProject;
				MessageContainer.restoreBackup();
			} else {
				project = Utils.findValidProject();
				if (project == null) {
					try {
						project = StandardProjectHandler
								.createAndSaveStandardProject(context);
						MessageContainer.clearBackup();
					} catch (IOException ioException) {
						if (errorMessage) {
							Utils.showErrorDialog(context,
									R.string.error_load_project);
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
				Utils.showErrorDialog(context,
						R.string.error_outdated_pocketcode_version);
				// TODO insert update link to Google Play
			}
			return false;
		} else {
			if (project.getCatrobatLanguageVersion() == 0.8f) {
				// TODO insert in every "When project starts" script list a
				// "show" brick
				project.setCatrobatLanguageVersion(0.9f);
			}
			if (project.getCatrobatLanguageVersion() == 0.9f) {
				project.setCatrobatLanguageVersion(0.91f);
				// no convertion needed - only change to white background
			}
			if (project.getCatrobatLanguageVersion() == 0.91f) {
				project.setCatrobatLanguageVersion(0.92f);
				project.setScreenMode(ScreenModes.STRETCH);
				checkNestingBrickReferences();
			}
			// insert further convertions here

			if (project.getCatrobatLanguageVersion() == Constants.CURRENT_CATROBAT_LANGUAGE_VERSION) {
				// project seems to be converted now and can be loaded
				localizeBackgroundSprite(context);
				return true;
			}
			// project cannot be converted
			project = oldProject;
			if (errorMessage) {
				Utils.showErrorDialog(context,
						R.string.error_project_compatability);
			}
			return false;
		}
	}

	private void localizeBackgroundSprite(Context context) {
		// Set generic localized name on background sprite and move it to the
		// back.
		if (project.getSpriteList().size() > 0) {
			project.getSpriteList().get(0)
					.setName(context.getString(R.string.background));
			project.getSpriteList().get(0).look.setZIndex(0);
		}
		MessageContainer.clearBackup();
		currentSprite = null;
		currentScript = null;
		Utils.saveToPreferences(context, Constants.PREF_PROJECTNAME_KEY,
				project.getName());
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
			project = StandardProjectHandler
					.createAndSaveStandardProject(context);
			currentSprite = null;
			currentScript = null;
			return true;
		} catch (IOException ioException) {
			Log.e(TAG, "Cannot initialize default project.", ioException);
			Utils.showErrorDialog(context, R.string.error_load_project);
			return false;
		}
	}

	public void initializeNewProject(String projectName, Context context,
			boolean empty) throws IOException {
		fileChecksumContainer = new FileChecksumContainer();

		if (empty) {
			project = StandardProjectHandler.createAndSaveEmptyProject(
					projectName, context);
		} else {
			project = StandardProjectHandler.createAndSaveStandardProject(
					projectName, context);
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
			String tmpProjectPath = Utils
					.buildProjectPath(createTemporaryDirectoryName(newProjectName));
			File tmpProjectDirectory = new File(tmpProjectPath);
			directoryRenamed = oldProjectDirectory
					.renameTo(tmpProjectDirectory);
			if (directoryRenamed) {
				directoryRenamed = tmpProjectDirectory
						.renameTo(newProjectDirectory);
			}
		} else {
			directoryRenamed = oldProjectDirectory
					.renameTo(newProjectDirectory);
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

		return project.getSpriteList().get(currentSpritePosition)
				.getScriptIndex(currentScript);
	}

	private String createTemporaryDirectoryName(String projectDirectoryName) {
		String temporaryDirectorySuffix = "_tmp";
		String temporaryDirectoryName = projectDirectoryName
				+ temporaryDirectorySuffix;
		int suffixCounter = 0;
		while (Utils
				.checkIfProjectExistsOrIsDownloadingIgnoreCase(temporaryDirectoryName)) {
			temporaryDirectoryName = projectDirectoryName
					+ temporaryDirectorySuffix + suffixCounter;
			suffixCounter++;
		}
		return temporaryDirectoryName;
	}

	public FileChecksumContainer getFileChecksumContainer() {
		return this.fileChecksumContainer;
	}

	public void setFileChecksumContainer(
			FileChecksumContainer fileChecksumContainer) {
		this.fileChecksumContainer = fileChecksumContainer;
	}

	private class SaveProjectAsynchronousTask extends
			AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			StorageHandler.getInstance().saveProject(project);
			return null;
		}
	}

	public static void loadVirtualGamepadImages(String projectName,
			Context context) {

		String path = Utils.buildPath(Utils.buildProjectPath(projectName),
				Constants.IMAGE_DIRECTORY);
		String[] imagePath = new String[] {
				Utils.buildPath(path, Constants.VGP_IMAGE_PAD_CENTER),
				Utils.buildPath(path, Constants.VGP_IMAGE_PAD_STRAIGHT),
				Utils.buildPath(path, Constants.VGP_IMAGE_PAD_DIAGONAL),
				Utils.buildPath(path, Constants.VGP_IMAGE_BUTTON_TOUCH),
				Utils.buildPath(path, Constants.VGP_IMAGE_BUTTON_HOLD),
				Utils.buildPath(path, Constants.VGP_IMAGE_BUTTON_SWIPE) };
		int[] resList = new int[] {
				org.catrobat.catroid.R.drawable.vgp_dpad_center,
				org.catrobat.catroid.R.drawable.vgp_dpad_straight,
				org.catrobat.catroid.R.drawable.vgp_dpad_diagonal,
				org.catrobat.catroid.R.drawable.vgp_button_touch,
				org.catrobat.catroid.R.drawable.vgp_button_hold,
				org.catrobat.catroid.R.drawable.vgp_button_swipe };

		for (int i = 0; i < imagePath.length; i++) {
			File file = new File(imagePath[i]);
			try {
				if (!file.exists()) {
					file.createNewFile();
				}
				InputStream in = context.getResources().openRawResource(
						resList[i]);
				OutputStream out = new BufferedOutputStream(
						new FileOutputStream(file), Constants.BUFFER_8K);
				byte[] buffer = new byte[Constants.BUFFER_8K];
				int length = 0;
				while ((length = in.read(buffer)) > 0) {
					out.write(buffer, 0, length);
				}
				in.close();
				out.flush();
				out.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onTokenNotValid(FragmentActivity fragmentActivity) {
		showLoginRegisterDialog(fragmentActivity);
	}

	@Override
	public void onCheckTokenSuccess(FragmentActivity fragmentActivity) {
		UploadProjectDialog uploadProjectDialog = new UploadProjectDialog();
		uploadProjectDialog.show(fragmentActivity.getSupportFragmentManager(),
				UploadProjectDialog.DIALOG_FRAGMENT_TAG);
	}

	private void showLoginRegisterDialog(FragmentActivity fragmentActivity) {
		LoginRegisterDialog loginRegisterDialog = new LoginRegisterDialog();
		loginRegisterDialog.show(fragmentActivity.getSupportFragmentManager(),
				LoginRegisterDialog.DIALOG_FRAGMENT_TAG);
	}

	@Override
	public void onLoadProjectSuccess(boolean startProjectActivity) {

	}

	@Override
	public void onLoadProjectFailure() {

	}

	public void checkNestingBrickReferences() {
		Project currentProject = ProjectManager.getInstance()
				.getCurrentProject();
		if (currentProject != null) {
			for (Sprite currentSprite : currentProject.getSpriteList()) {
				int numberOfScripts = currentSprite.getNumberOfScripts();
				for (int pos = 0; pos < numberOfScripts; pos++) {
					Script script = currentSprite.getScript(pos);
					boolean scriptCorrect = true;
					for (Brick currentBrick : script.getBrickList()) {
						if (currentBrick instanceof IfLogicBeginBrick) {
							IfLogicElseBrick elseBrick = ((IfLogicBeginBrick) currentBrick)
									.getIfElseBrick();
							IfLogicEndBrick endBrick = ((IfLogicBeginBrick) currentBrick)
									.getIfEndBrick();
							if (elseBrick == null
									|| endBrick == null
									|| elseBrick.getIfBeginBrick() == null
									|| elseBrick.getIfEndBrick() == null
									|| endBrick.getIfBeginBrick() == null
									|| endBrick.getIfElseBrick() == null
									|| !elseBrick.getIfBeginBrick().equals(
											currentBrick)
									|| !elseBrick.getIfEndBrick().equals(
											endBrick)
									|| !endBrick.getIfBeginBrick().equals(
											currentBrick)
									|| !endBrick.getIfElseBrick().equals(
											elseBrick)) {
								scriptCorrect = false;
								Log.d("REFERENCE ERROR!!",
										"Brick has wrong reference:"
												+ currentSprite + " "
												+ currentBrick);
							}
						} else if (currentBrick instanceof LoopBeginBrick) {
							LoopEndBrick endBrick = ((LoopBeginBrick) currentBrick)
									.getLoopEndBrick();
							if (endBrick == null
									|| endBrick.getLoopBeginBrick() == null
									|| !endBrick.getLoopBeginBrick().equals(
											currentBrick)) {
								scriptCorrect = false;
								Log.d("REFERENCE ERROR!!",
										"Brick has wrong reference:"
												+ currentSprite + " "
												+ currentBrick);
							}
						}
					}
					if (!scriptCorrect) {
						// correct references
						ArrayList<IfLogicBeginBrick> ifBeginList = new ArrayList<IfLogicBeginBrick>();
						ArrayList<LoopBeginBrick> loopBeginList = new ArrayList<LoopBeginBrick>();
						for (Brick currentBrick : script.getBrickList()) {
							if (currentBrick instanceof IfLogicBeginBrick) {
								ifBeginList
										.add((IfLogicBeginBrick) currentBrick);
							} else if (currentBrick instanceof LoopBeginBrick) {
								loopBeginList
										.add((LoopBeginBrick) currentBrick);
							} else if (currentBrick instanceof LoopEndBrick) {
								LoopBeginBrick loopBeginBrick = loopBeginList
										.get(loopBeginList.size() - 1);
								loopBeginBrick
										.setLoopEndBrick((LoopEndBrick) currentBrick);
								((LoopEndBrick) currentBrick)
										.setLoopBeginBrick(loopBeginBrick);
								loopBeginList.remove(loopBeginBrick);
							} else if (currentBrick instanceof IfLogicElseBrick) {
								IfLogicBeginBrick ifBeginBrick = ifBeginList
										.get(ifBeginList.size() - 1);
								ifBeginBrick
										.setIfElseBrick((IfLogicElseBrick) currentBrick);
								((IfLogicElseBrick) currentBrick)
										.setIfBeginBrick(ifBeginBrick);
							} else if (currentBrick instanceof IfLogicEndBrick) {
								IfLogicBeginBrick ifBeginBrick = ifBeginList
										.get(ifBeginList.size() - 1);
								IfLogicElseBrick elseBrick = ifBeginBrick
										.getIfElseBrick();
								ifBeginBrick
										.setIfEndBrick((IfLogicEndBrick) currentBrick);
								elseBrick
										.setIfEndBrick((IfLogicEndBrick) currentBrick);
								((IfLogicEndBrick) currentBrick)
										.setIfBeginBrick(ifBeginBrick);
								((IfLogicEndBrick) currentBrick)
										.setIfElseBrick(elseBrick);
								ifBeginList.remove(ifBeginBrick);
							}
						}
					}
				}
			}
		}
	}
}
