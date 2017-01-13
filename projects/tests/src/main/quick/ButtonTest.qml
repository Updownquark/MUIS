<?xml version="1.0" encoding="UTF-8"?>

<quick xmlns:base="../../../../base/QuickRegistry.xml" xmlns:test="../../../QuickRegistry.xml">
    <head>
        <title>Testing Buttons</title>
        <model name="model" builder="default-model">
        	<variable name="counter" min="0" max="10">10</variable>
        	<switch name="counterColor" value="this.counter">
        		<case value="10">org.quick.core.style.Colors.blue</case>
        		<case value="9">org.quick.core.style.Colors.purple</case>
        		<case value="8">org.quick.core.style.Colors.red</case>
        		<case value="7">org.quick.core.style.Colors.orange</case>
        		<case value="6">org.quick.core.style.Colors.yellow</case>
        		<case value="5">org.quick.core.style.Colors.green</case>
        		<case value="4">org.quick.core.style.Colors.blue</case>
        		<case value="3">org.quick.core.style.Colors.cyan</case>
        		<case value="2">org.quick.core.style.Colors.magenta</case>
        		<case value="1">org.quick.core.style.Colors.white</case>
        		<case value="0">org.quick.core.style.Colors.saddleBrown</case>
        		<default>org.quick.core.style.Colors.orange</default>
        	</switch>
        </model>
    </head>
    <body layout="base:simple">
    	<block layout="simple" width="100%" top="0" height="100%">
			<button left="0" right="100%" top="25%" height="50%" cross-align="center" action="model.counter--">
				The first set of Content is short
				<label style="font.color=red;font.weight=bold;bg.color=purple">The second set of Content is a bit longer than the first set</label>
				<label style="font.weight=bold;font.size=20" format="${integer}" value="model.counter" />
			</button>
			<button left="45%" right="55%" top="75%" bottom="80%" action="model.counter+=10">Reset</button>
			<block left="3" right="3xp" top="80%" height="25%" style="bg.transparency=0;bg.color=${model.counterColor}">This block should change color with button clicks</block>
    	</block>
    </body>
</quick>
