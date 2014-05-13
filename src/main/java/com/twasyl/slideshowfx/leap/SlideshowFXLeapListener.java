package com.twasyl.slideshowfx.leap;

import com.leapmotion.leap.*;
import com.twasyl.slideshowfx.app.SlideshowFX;
import javafx.scene.input.KeyCode;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author Thierry Wasylczenko
 */
public class SlideshowFXLeapListener extends Listener {

    private static final Logger LOGGER = Logger.getLogger(SlideshowFXLeapListener.class.getName());

    private boolean tracking = false;
    private SwipeGesture lastSwipeGesture;

    public boolean isTracking() { return tracking; }
    public void setTracking(boolean tracking) { this.tracking = tracking; }

    @Override
    public void onInit(Controller controller) {
        super.onInit(controller);
        LOGGER.finest("SlideshowFXLeapListener - onInit");
    }

    @Override
    public void onExit(Controller controller) {
        super.onExit(controller);
        LOGGER.finest("SlideshowFXLeapListener - onExit");
    }

    @Override
    public void onConnect(Controller controller) {
        super.onConnect(controller);
        LOGGER.finest("LeapMotion controller connected");
        controller.enableGesture(Gesture.Type.TYPE_SWIPE);
        controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
    }

    @Override
    public void onDisconnect(Controller controller) {
        super.onDisconnect(controller);
        LOGGER.finest("LeapMotion controller disconnected");
    }

    @Override
    public void onFrame(final Controller controller) {
        super.onFrame(controller);

        final Frame frame = controller.frame();

        if(frame.isValid()) {
            if(frame.pointables().isEmpty()) {
                SlideshowFX.getSlideShowScene().hidePointer();
            }
        }

        if(isTracking()) {

            // Manage gestures
            if(!frame.gestures().isEmpty()) {
                Gesture gesture = null;
                Iterator<Gesture> gesturesIterator = frame.gestures().iterator();

                while(gesturesIterator.hasNext()) {
                    gesture = gesturesIterator.next();

                    if(gesture.isValid()) {
                        switch(gesture.type()) {
                            case TYPE_SWIPE:
                                manageSwipe(controller, gesture);
                                break;
                            case TYPE_KEY_TAP:
                                manageKeyTap(controller, gesture);
                                break;
                        }
                    }
                }
            } else {
                movePointer(controller);
            }
        }
    }

    private void manageSwipe(final Controller controller, final Gesture gesture) {
        final Frame frame = controller.frame();

        final SwipeGesture swipe = new SwipeGesture(gesture);

        // The gesture is finished
        if(swipe.state() == Gesture.State.STATE_STOP) {

            // Only compute the swipe if it executed more than 1 second after the previous one
            boolean computeGesture = this.lastSwipeGesture == null;
            if(!computeGesture) {
                // Timestamp are in microseconds
                long lastSwipeTimestamp = this.lastSwipeGesture.frame().timestamp();
                long currentTimestamp = swipe.frame().timestamp();

                computeGesture = (lastSwipeTimestamp + 1000000l) <= currentTimestamp;
            }


            if(computeGesture) {
                if(!frame.hands().isEmpty() && frame.hands().count() == 1) {
                    final Hand hand = frame.hands().get(0);

                    if(hand.isValid()) {
                        // Only allow index and major fingers
                        if(!hand.fingers().isEmpty() && hand.fingers().count() == 2) {
                            boolean swipeValid = true;

                            Iterator<Finger> fingerIterator = hand.fingers().iterator();

                            // Check that each finger is valid
                            while(fingerIterator.hasNext() && swipeValid) {
                                swipeValid = fingerIterator.next().isValid();
                            }

                            if(swipeValid) {

                                // Check the gesture is a swipe and determine direction
                                if(swipe.direction().getX() > 0) {
                                    SlideshowFX.getSlideShowScene().hidePointer();
                                    SlideshowFX.getSlideShowScene().sendKey(KeyCode.LEFT);
                                } else if(swipe.direction().getX() < 0) {
                                    SlideshowFX.getSlideShowScene().hidePointer();
                                    SlideshowFX.getSlideShowScene().sendKey(KeyCode.RIGHT);
                                }
                            }
                        }
                    }
                }
            }

            this.lastSwipeGesture = swipe;
        }
    }

    private void manageKeyTap(final Controller controller, final Gesture gesture) {
        final Frame frame = controller.frame();

        if(frame.isValid()) {

            if(!frame.hands().isEmpty() && frame.hands().count() == 1) {
                final Hand  hand = frame.hands().get(0);

                if(hand.isValid() && !hand.fingers().isEmpty()) {
                    // Enable/disable highlighting
                    if(hand.fingers().count() == 5) {
                        if(gesture.state().equals(Gesture.State.STATE_STOP)) {

                            boolean allFingersValid = true;
                            Iterator<Finger> fingerIterator = hand.fingers().iterator();

                            while(allFingersValid && fingerIterator.hasNext()) {
                                allFingersValid = fingerIterator.next().isValid();
                            }

                            if(allFingersValid) SlideshowFX.getSlideShowScene().hidePointer();
                        }
                    }
                }
            }
        }

    }

    private void movePointer(final Controller controller) {
            final Frame frame = controller.frame();

            if(!frame.hands().isEmpty() && frame.hands().count() == 1) {
                final Hand hand = frame.hands().get(0);

                if(hand.isValid()) {

                    // Only movePointer if one finger
                    if(!hand.fingers().isEmpty() && hand.fingers().count() == 1) {
                        final Finger finger = hand.fingers().get(0);

                        if(finger.isValid()) {
                            final InteractionBox box = frame.interactionBox();
                            final Vector normalizedPosition = box.normalizePoint(finger.tipPosition());

                            double screenWidth = SlideshowFX.getSlideShowScene().getWidth();
                            double screenHeight = SlideshowFX.getSlideShowScene().getHeight();

                            double computedX = normalizedPosition.getX() * screenWidth;
                            double computedY = screenHeight - (normalizedPosition.getY() * screenHeight);

                            SlideshowFX.getSlideShowScene().showPointer(computedX, computedY);
                        }
                    }
                }
            }
    }
}
