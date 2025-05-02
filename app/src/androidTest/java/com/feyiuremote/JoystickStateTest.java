package com.feyiuremote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import com.feyiuremote.libs.Feiyu.controls.JoystickState;

import org.junit.Test;

public class JoystickStateTest {


    private static final String TAG = "FeyiuStateTest";


    // Your test cases will go here
    @Test
    public void testJoystickState() {
        JoystickState jp = new JoystickState(150, 1000, JoystickState.AXIS_PAN, 500, "Test Pan");
        JoystickState jt = new JoystickState(150, 1000, JoystickState.AXIS_TILT, 500, "Test Pan");

        Log.d(TAG, jp.toString());
        Log.d(TAG, jt.toString());

        assertEquals("Pan", jp.axisToString());
        assertEquals("Tilt", jt.axisToString());

        assertTrue("Execution time does not match!" + jt.executesInMs(), jt.executesInMs() < 500 && jp.executesInMs() >= 498);
        assertTrue("Execution time does not match!" + jt.executesInMs(), jp.executesInMs() < 500 && jp.executesInMs() >= 498);

        assertTrue("Time difference is wrong!" + jt.executesInDiffMs(jp), jt.executesInDiffMs(jp) < 1);

        jp = new JoystickState(150, 1000, JoystickState.AXIS_PAN, 1000, "Test Pan");
        jt = new JoystickState(150, 1000, JoystickState.AXIS_TILT, 500, "Test Tilt");

        assertTrue("Time difference is wrong!" + jp.executesInDiffMs(jt), jp.executesInDiffMs(jt) < 500 && jp.executesInDiffMs(jt) > 498);

        jp = new JoystickState(150, 100, JoystickState.AXIS_PAN, 100, "Test Pan");
        jt = new JoystickState(150, 200, JoystickState.AXIS_TILT, 200, "Test Tilt");
        // JP ends 200ms sooner hence -200
        assertTrue("Time difference is wrong!" + jp.executionEndsDiffMs(jt), jp.executionEndsDiffMs(jt) == -200);
    }
}
