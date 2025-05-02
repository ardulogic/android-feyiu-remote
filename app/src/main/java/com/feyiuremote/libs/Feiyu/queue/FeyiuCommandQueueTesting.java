package com.feyiuremote.libs.Feiyu.queue;

import com.feyiuremote.libs.Feiyu.FeyiuState;

public class FeyiuCommandQueueTesting {

    public void doTests() {
        FeyiuState.getInstance().last_command = System.currentTimeMillis();

//        CommandManager.submit(new JoyCommandLooselyTimed(CommandManager.Axis.TILT, 100, 5000));
//        CommandManager.submit(new JoyCommandLooselyTimed(CommandManager.Axis.PAN, 20, 5000));


        // Above works as intended

        // Now this is will be the challenge.
        // First thing is that it needs to cancel all commands that does not have opposite axis values and are later than 1000ms from now
        // Secondly it needs to adjust the current timestamps, so that it can fit perfectly and execute the command exactly on time
        // we have a minimum execution time between commands of 40ms
        // gap between commands cant be larger that  150ms

//        CommandManager.submit(new StrictlyTimedJoyCommand(CommandManager.Axis.TILT, 0, 200, 400));
//        CommandManager.submit(new StrictlyTimedJoyCommand(CommandManager.Axis.PAN, 0, 300, 430));
//        CommandManager.debugScheduledCommands();


//        CommandManager.debugScheduledCommands();
//        CommandManager.submit(new LooselyTimedJoyCommand(CommandManager.Axis.PAN, 99, 500));

    }

}
