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
package org.catrobat.catroid.stage;

import java.util.List;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenVirtualButtonScript;
import org.catrobat.catroid.content.WhenVirtualPadScript;
import org.catrobat.catroid.content.bricks.WhenVirtualPadBrick.Direction;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Stage;

public class VirtualGamepadStage extends Stage {

	private Sprite vgpPadSprite;

	private float dPadInitValue = -100000.0f;
	private float dPadStartX = -100000.0f;
	private float dPadStartY = -100000.0f;
	private double dPadMinMotion = 20.0;
	private double dPadMaxMotion = 90.0;
	private DPadThread dPadThread;

	private boolean dPadUp = false;
	private boolean dPadDown = false;
	private boolean dPadLeft = false;
	private boolean dPadRight = false;

	private float width;
	private float height;

	public VirtualGamepadStage(float width, float height, boolean keepAspectRatio) {
		super(width, height, keepAspectRatio);

		this.width = width;
		this.height = height;
	}

	private void setDPadDirection(boolean dPadUp, boolean dPadDown, boolean dPadLeft, boolean dPadRight) {
		this.dPadUp = dPadUp;
		this.dPadDown = dPadDown;
		this.dPadLeft = dPadLeft;
		this.dPadRight = dPadRight;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		try {
			//				Log.e("touchDown", "x=" + screenX);
			//				Log.e("touchDown", "y=" + screenY);
			//				Log.e("touchDown", "pointer=" + pointer);
			//				Log.e("touchDown", "button=" + button);

			//					if (pointer == 0) {
			dPadThread = new DPadThread();
			dPadThread.start();

			vgpPadSprite.look.setXInUserInterfaceDimensionUnit(screenX - width / 2.0f);
			vgpPadSprite.look.setYInUserInterfaceDimensionUnit(height / 2.0f - screenY);

			vgpPadSprite.look.setVisible(true);
			//					}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		try {
			//				Log.i("touchDragged", "x=" + screenX);
			//				Log.i("touchDragged", "y=" + screenY);
			//				Log.i("touchDragged", "pointer=" + pointer);

			//init start values
			if (dPadStartX == dPadInitValue || dPadStartY == dPadInitValue) {
				dPadStartX = screenX;
				dPadStartY = screenY;
				return false;
			}

			double distance = Math.sqrt(Math.pow(dPadStartX - screenX, 2) + Math.pow(dPadStartY - screenY, 2));
			if (distance <= dPadMinMotion || distance > dPadMaxMotion) {
				setDPadDirection(false, false, false, false);
			} else {
				boolean tmpUp = false;
				boolean tmpDown = false;
				boolean tmpLeft = false;
				boolean tmpRight = false;

				//direction
				if (screenX > dPadStartX && (screenX - dPadMinMotion) > dPadStartX) {
					tmpRight = true;
				} else if (screenX < dPadStartX && (screenX + dPadMinMotion) < dPadStartX) {
					tmpLeft = true;
				}

				if (screenY > dPadStartY && (screenY - dPadMinMotion) > dPadStartY) {
					tmpDown = true;
				} else if (screenY < dPadStartY && (screenY + dPadMinMotion) < dPadStartY) {
					tmpUp = true;
				}

				setDPadDirection(tmpUp, tmpDown, tmpLeft, tmpRight);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		try {
			//				Log.e("touchUp", "x=" + screenX);
			//				Log.e("touchUp", "y=" + screenY);
			//				Log.e("touchUp", "pointer=" + pointer);
			//				Log.e("touchUp", "button=" + button);

			//					if (pointer == 0) {
			dPadStartX = dPadInitValue;
			dPadStartY = dPadInitValue;

			dPadThread.stopThread();

			vgpPadSprite.look.setVisible(false);
			//					}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Sprite getVgpPadSprite() {
		return vgpPadSprite;
	}

	public void setVgpPadSprite(Sprite vgpPadSprite) {
		this.vgpPadSprite = vgpPadSprite;
	}

	class DPadThread extends Thread {

		private boolean running = false;

		@Override
		public void run() {
			try {
				Log.e("DPadThread<run>", "thread started");

				while (running) {

					Sprite sprite = ProjectManager.INSTANCE.getCurrentSprite();
					if (sprite == null) {
						List<Sprite> spriteList = ProjectManager.INSTANCE.getCurrentProject().getSpriteList();
						for (Sprite tmp : spriteList) {
							for (int script = 0; script < tmp.getNumberOfScripts(); script++) {
								if (tmp.getScript(script) instanceof WhenVirtualPadScript
										|| tmp.getScript(script) instanceof WhenVirtualButtonScript) {
									sprite = tmp;
									break;
								}
							}
							if (sprite != null) {
								break;
							}
						}
						if (sprite == null) {
							running = false;
							Log.e("DPadThread<run>", "sprite is null");
							return;
						}
					} else if (sprite.isPaused) {
						running = false;
						return;
					}

					boolean changeImage = true;
					if (dPadUp && dPadLeft) {
						for (LookData lookData : vgpPadSprite.getLookDataList()) {
							if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_UPLEFT)) {
								vgpPadSprite.look.setLookData(lookData);
								break;
							}
						}
						changeImage = false;
					} else if (dPadUp && dPadRight) {
						for (LookData lookData : vgpPadSprite.getLookDataList()) {
							if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_UPRIGHT)) {
								vgpPadSprite.look.setLookData(lookData);
								break;
							}
						}
						changeImage = false;
					} else if (dPadDown && dPadLeft) {
						for (LookData lookData : vgpPadSprite.getLookDataList()) {
							if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_DOWNLEFT)) {
								vgpPadSprite.look.setLookData(lookData);
								break;
							}
						}
						changeImage = false;
					} else if (dPadDown && dPadRight) {
						for (LookData lookData : vgpPadSprite.getLookDataList()) {
							if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_DOWNRIGHT)) {
								vgpPadSprite.look.setLookData(lookData);
								break;
							}
						}
						changeImage = false;
					}

					if (dPadUp) {
						if (changeImage) {
							for (LookData lookData : vgpPadSprite.getLookDataList()) {
								if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_UP)) {
									vgpPadSprite.look.setLookData(lookData);
									break;
								}
							}
						}
						sprite.createWhenVirtualPadScriptActionSequence(Direction.UP.getId());
					}
					if (dPadDown) {
						if (changeImage) {
							for (LookData lookData : vgpPadSprite.getLookDataList()) {
								if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_DOWN)) {
									vgpPadSprite.look.setLookData(lookData);
									break;
								}
							}
						}
						sprite.createWhenVirtualPadScriptActionSequence(Direction.DOWN.getId());
					}
					if (dPadLeft) {
						if (changeImage) {
							for (LookData lookData : vgpPadSprite.getLookDataList()) {
								if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_LEFT)) {
									vgpPadSprite.look.setLookData(lookData);
									break;
								}
							}
						}
						sprite.createWhenVirtualPadScriptActionSequence(Direction.LEFT.getId());
					}
					if (dPadRight) {
						if (changeImage) {
							for (LookData lookData : vgpPadSprite.getLookDataList()) {
								if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_RIGHT)) {
									vgpPadSprite.look.setLookData(lookData);
									break;
								}
							}
						}
						sprite.createWhenVirtualPadScriptActionSequence(Direction.RIGHT.getId());
					}
					if (!dPadUp && !dPadDown && !dPadLeft && !dPadRight) {
						//center
						for (LookData lookData : vgpPadSprite.getLookDataList()) {
							if (lookData.getLookName().equals(Constants.VGP_IMAGE_PAD_CENTER)) {
								vgpPadSprite.look.setLookData(lookData);
								break;
							}
						}
					}

					Thread.sleep(20);
				}

				Log.e("DPadThread<run>", "thread stopped");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void start() {
			try {
				running = true;
				Log.e("DPadThread<start>", "start thread");
				super.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void stopThread() {
			running = false;
		}

	}

}
