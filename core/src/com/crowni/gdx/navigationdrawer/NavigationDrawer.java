/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.crowni.gdx.navigationdrawer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;

/**
 * @author Crowni
 */
public class NavigationDrawer extends Table {

    // only visual window and using scissor to avoid GPU to draw out of left-edge screen.
    private float areaWidth;
    private float areaHeight;
    private final Rectangle areaBounds = new Rectangle();
    private final Rectangle scissorBounds = new Rectangle();

    // it's revealed with (widthStart = 60F;) when the user swipes a finger from the left edge of the screen with start touch.
    private float widthStart = 60f;
    // when the user swipes a finger from the right edge of the screen, it goes into off-screen after (widthBack = 20F;).
    private float widthBack = 20f;
    // speed of dragging
    private float speed = 15f;

    // some attributes to make real dragging
    private Vector2 clamp = new Vector2();
    private Vector2 posTap = new Vector2();
    private Vector2 end = new Vector2();
    private Vector2 first = new Vector2();
    private Vector2 last = new Vector2();

    private boolean show = false;
    private boolean isTouched = false;
    private boolean isStart = false;
    private boolean isBack = false;
    private boolean auto = false;
    private boolean enableDrag = true;

    public void setAreaWidth(float areaWidth) {
        this.areaWidth = areaWidth;
    }

    public void setAreaHeight(float areaHeight) {
        this.areaHeight = areaHeight;
    }

    public NavigationDrawer(float width, float height) {
        this.areaWidth = width;
        this.areaHeight = height;

        this.setSize(width, height);
    }

    private NavigationDrawerListener listener;

    public interface NavigationDrawerListener {
        void moving(Vector2 clamp);
    }

    public void setNavigationDrawerListener(NavigationDrawerListener listener) {
        this.listener = listener;

    }

    public void setWidthStartDrag(float widthStartDrag) {
        this.widthStart = widthStartDrag;
    }

    public void setWidthBackDrag(float widthBackDrag) {
        this.widthBack = widthBackDrag;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public void showManually(boolean show, float speed) {
        this.auto = true;
        this.show = show;
        this.speed = speed;
    }

    public void showManually(boolean show) {
        this.showManually(show, speed);
    }

    @Override
    public void draw(Batch batch, float alpha) {
        getStage().calculateScissors(areaBounds.set(0, 0, areaWidth, areaHeight), scissorBounds);
        batch.flush();
        if (ScissorStack.pushScissors(scissorBounds)) {
            super.draw(batch, alpha);
            batch.flush();
            ScissorStack.popScissors();
        }

        if (isTouched() && inputX() < stgToScrX(this.getWidth(), 0).x) {
            auto = false;
            if (!isTouched) {
                isTouched = true;
                first.set(scrToStgX(inputX(), 0));
            }
            last.set(scrToStgX(inputX(), 0)).sub(first);

            if (isCompletelyClosed()) // open = false, close = true;
                startDrag();

            if ((isStart || isBack) && enableDrag) // open = false, close =
                // false;
                if (inputX() > stgToScrX(widthStart, 0).x)
                    dragging();

            if (isCompletelyOpened()) // open = true, close = false;
                backDrag();

        } else
            noDrag();

        updatePosition();

        moving();

        rotateMenuButton();

        fadeBackground();

    }

    private boolean isMax = false;
    private boolean isMin = false;

    private void moving() {
        if (listener == null)
            return;
        if (!isCompletelyClosed() && !isCompletelyOpened()) {
            listener.moving(clamp);
        } else {
            if (!isMax && isCompletelyOpened()) {
                isMax = true;
                isMin = false;
                listener.moving(clamp);
            }
            if (!isMin && isCompletelyClosed()) {
                isMin = true;
                isMax = false;
                listener.moving(clamp);
            }
        }

    }

    private void updatePosition() {
        clamp.set(MathUtils.clamp(end.x, 0, this.getWidth()), 0);
        this.setPosition(clamp.x, 0, Align.bottomRight);
    }

    private void dragging() {
        if (isStart)
            end.set(scrToStgX(inputX(), 0));

        if (isBack && last.x < -widthBack)
            end.set(last.add(this.getWidth() + widthBack, 0));

    }

    private void backDrag() {
        isStart = false;
        isBack = true;
        show = false;
    }

    private void startDrag() {
        // check if the player touch on the drawer to OPEN it.
        if (inputX() < stgToScrX(widthStart, 0).x) {
            isStart = true;
            isBack = false;

            hintToOpen(); // hint to player if he want to open the drawer
        }
    }

    private void noDrag() {
        isStart = false;
        isBack = false;
        isTouched = false;

        // set end of X to updated X from clamp
        end.set(clamp);

        if (auto) {
            if (show)
                end.add(speed, 0); // player want to OPEN drawer
            else
                end.sub(speed, 0); // player want to CLOSE drawer
        } else {
            if (toOpen())
                end.add(speed, 0); // player want to OPEN drawer
            else if (toClose())
                end.sub(speed, 0); // player want to CLOSE drawer
        }

    }

    private void hintToOpen() {
        end.set(stgToScrX(widthStart, 0));
    }

    public boolean isCompletelyClosed() {
        return clamp.x == 0;
    }

    public boolean isCompletelyOpened() {
        return clamp.x == this.getWidth();
    }

    private boolean toOpen() {
        return clamp.x > this.getWidth() / 2;
    }

    private boolean toClose() {
        return clamp.x < this.getWidth() / 2;
    }

    private Vector2 stgToScrX(float x, float y) {
        return getStage().stageToScreenCoordinates(posTap.set(x, y));
    }

    private Vector2 scrToStgX(float x, float y) {
        return getStage().screenToStageCoordinates(posTap.set(x, y));
    }

    private float inputX() {
        return Gdx.input.getX();
    }

    private boolean isTouched() {
        return Gdx.input.isTouched();
    }

    /**
     * Optional
     **/
    private Actor menuButton = new Actor();
    private boolean isRotateMenuButton = false;
    private float menuButtonRotation = 0f;

    private void rotateMenuButton() {
        if (isRotateMenuButton)
            menuButton.setRotation(clamp.x / this.getWidth() * menuButtonRotation);
    }

    public void setRotateMenuButton(Actor actor, float rotation) {
        this.menuButton = actor;
        this.isRotateMenuButton = true;
        this.menuButtonRotation = rotation;
    }

    public void setEnableDrag(boolean enableDrag) {
        this.enableDrag = enableDrag;
    }

    /**
     * Optional
     **/
    private Actor background = new Actor();
    private boolean isFadeBackground = false;
    private float maxFade = 1f;

    private void fadeBackground() {
        if (isFadeBackground)
            background.setColor(background.getColor().r, background.getColor().g, background.getColor().b,
                    MathUtils.clamp(clamp.x / this.getWidth() / 2, 0, maxFade));
    }

    public void setFadeBackground(Actor background, float maxFade) {
        this.background = background;
        this.isFadeBackground = true;
        this.maxFade = maxFade;
    }

}