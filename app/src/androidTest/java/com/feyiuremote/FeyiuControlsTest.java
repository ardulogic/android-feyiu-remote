package com.feyiuremote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.feyiuremote.libs.Feiyu.FeyiuControls;
import com.feyiuremote.libs.Feiyu.FeyiuState;
import com.feyiuremote.libs.Feiyu.controls.ConsolidatedJoystickState;
import com.feyiuremote.libs.Feiyu.controls.SensitivityState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class FeyiuControlsTest {


    private static final String TAG = "FeyiuControlsTest";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPanTiltImmediateValues() {
        FeyiuControls.setPanJoy(50, "Initial pan");
        FeyiuControls.setTiltJoy(75, "Initial tilt");

        ConsolidatedJoystickState consolidated = FeyiuControls.getConsolidatedJoystickState();

        assertEquals(50, consolidated.joyPan);
        assertEquals(75, consolidated.joyTilt);
    }

    @Test
    public void testSensitivityWhenItDoesntMatch() throws InterruptedException {
        FeyiuControls.cancelQueuedCommands();

        // Set initial sensitivity states for testing
        FeyiuState.joy_sens_pan = 10;
        FeyiuState.joy_sens_tilt = 15;

        // Update the pan and tilt sensitivities
        FeyiuControls.setPanSensitivity(25);
        FeyiuControls.setTiltSensitivity(20);

        // Check that the queuedSensitivityStates list has at least 2 elements
        assertEquals("Sensitivity queue should contain two items", 2, FeyiuControls.queuedSensitivityStates.size());

        // Retrieve the sensitivity states from the queue
        SensitivityState s1 = FeyiuControls.queuedSensitivityStates.get(0);
        SensitivityState s2 = FeyiuControls.queuedSensitivityStates.get(1);

        // Verify that both sensitivity states are not null
        assertNotNull("First sensitivity state should not be null", s1);
        assertNotNull("Second sensitivity state should not be null", s2);

        // Validate the types of the states
        assertEquals("First state type should be pan (0)", Integer.valueOf(0), s1.type);
        assertEquals("Second state type should be tilt (1)", Integer.valueOf(1), s2.type);

        assertEquals("First state type should be pan (0)", Integer.valueOf(25), s1.sens);
        assertEquals("Second state type should be tilt (1)", Integer.valueOf(20), s2.sens);
    }

    @Test
    public void testSensitivityWhenItMatches() throws InterruptedException {
        FeyiuControls.cancelQueuedCommands();

        // Set initial sensitivity states for testing
        FeyiuState.joy_sens_pan = 10;
        FeyiuState.joy_sens_tilt = 15;

        // Update the pan and tilt sensitivities
        FeyiuControls.setPanSensitivity(10);
        FeyiuControls.setTiltSensitivity(15);

        // Check that the queuedSensitivityStates list has at least 2 elements
        assertEquals("Sensitivity queue should contain zero", 0, FeyiuControls.queuedSensitivityStates.size());
    }


    @Test
    public void testPanTiltOverlap() throws InterruptedException {
        FeyiuControls.cancelQueuedCommands();

        FeyiuControls.setPanJoy(50, "Initial pan");
        FeyiuControls.setTiltJoy(75, "Initial tilt");

        FeyiuControls.setPanJoyAfter(60, 250, "Initial pan");
        FeyiuControls.setTiltJoyAfter(85, 250, "Initial tilt");

        FeyiuControls.setPanJoyAfter(10, 300, "Initial pan");
        FeyiuControls.setTiltJoyAfter(15, 300, "Initial tilt");

        ConsolidatedJoystickState states = FeyiuControls.getConsolidatedJoystickState();

        assertEquals(50, states.joyPan);
        assertEquals(75, states.joyTilt);

        Thread.sleep(250);

        states = FeyiuControls.getConsolidatedJoystickState();

        assertEquals(60, states.joyPan);
        assertEquals(85, states.joyTilt);

        Thread.sleep(50);

        states = FeyiuControls.getConsolidatedJoystickState();

        assertEquals(10, states.joyPan);
        assertEquals(15, states.joyTilt);


        Thread.sleep(500);

        states = FeyiuControls.getConsolidatedJoystickState();

        assertEquals(0, states.joyPan);
        assertEquals(0, states.joyTilt);
    }

    // Your test cases will go here
    @Test
    public void testPanTiltConsolidation() throws InterruptedException {

    }
}
