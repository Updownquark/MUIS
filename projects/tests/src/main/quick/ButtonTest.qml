<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <model name="model" builder="default-model">
        	<variable name="counter">0</variable>
        	<value name="counterMod">counter%3</value>
        	<action name="incCounter">this.counter++</action>
        	<value name="color">new java.awt.Color(this.counterMod==0 ? 255 : 0, this.counterMod==1 ? 255 : 0, this.counterMod==2 ? 255 : 0)</value>
        </model>
    </head>
    <body layout="base:simple">
    	<block layout="simple" width="100%" top="0" height="100%">
    		<label left="3" right="3xp" top="0" height="25%">This is a Quick Document</label>
			<button left="0" right="100%" top="25%" height="50%" action="model.incCounter">
				The first set of Content is short
				<label style="font.color=red;font.weight=bold;bg.color=purple">The second set of Content is a bit longer than the first set</label>
			</button>
			<block left="3" right="3xp" top="75%" height="25%" style="bg.color=model.color">This block should change color with button clicks</block>
    	</block>
    </body>
</quick>
